import { Component, computed, effect, ElementRef, inject, input, OnDestroy, signal, viewChild } from '@angular/core';
import { Observable, Subscription, of, switchMap } from 'rxjs';
import { ChannelApi } from '../../core/api/channel.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { Attachment, Channel, Message, ReactionPayload, RealtimeEvent, TypingPayload } from '../../core/models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { WorkspaceState } from '../../core/state/workspace-state.service';
import { ComposerComponent, ComposerSubmit } from './composer.component';
import { MessageItemComponent } from './message-item.component';

const PAGE_SIZE = 30;
const TYPING_TIMEOUT_MS = 4000;
const TYPING_THROTTLE_MS = 2000;

@Component({
  selector: 'app-channel-chat',
  imports: [MessageItemComponent, ComposerComponent],
  templateUrl: './channel-chat.page.html',
  styleUrl: './channel-chat.page.scss',
})
export class ChannelChatPage implements OnDestroy {
  private readonly api = inject(ChannelApi);
  private readonly realtime = inject(RealtimeService);
  protected readonly auth = inject(AuthService);
  protected readonly state = inject(WorkspaceState);

  readonly workspaceId = input.required<string>();
  readonly channelId = input.required<string>();

  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');
  private channelSubscription: Subscription | null = null;
  private typingTimers = new Map<string, ReturnType<typeof setTimeout>>();
  private lastTypingSent = 0;
  private page = 0;

  protected readonly channel = signal<Channel | null>(null);
  protected readonly messages = signal<Message[]>([]);
  protected readonly loading = signal(true);
  protected readonly hasMore = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly unseenCount = signal(0);
  protected readonly typingNames = signal<Map<string, string>>(new Map());
  protected readonly replyTo = signal<Message | null>(null);

  protected readonly messagesById = computed(() => new Map(this.messages().map((m) => [m.id, m])));
  protected readonly typingText = computed(() => {
    const names = [...this.typingNames().values()];
    if (names.length === 0) return '';
    if (names.length === 1) return `${names[0]} está escribiendo...`;
    return `${names.slice(0, 2).join(' y ')} están escribiendo...`;
  });
  protected readonly canModerate = computed(() => {
    const me = this.state.membersById().get(this.auth.currentUser()?.id ?? '');
    return !!me && ['OWNER', 'ADMIN', 'MODERATOR'].includes(me.roleName);
  });
  protected readonly isAnnouncementLocked = computed(
    () => this.channel()?.type === 'ANNOUNCEMENT' && !this.canModerate(),
  );

  constructor() {
    effect(() => this.state.open(this.workspaceId()));
    effect(() => this.loadChannel(this.channelId()));
  }

  ngOnDestroy(): void {
    this.channelSubscription?.unsubscribe();
    this.typingTimers.forEach((timer) => clearTimeout(timer));
  }

  private loadChannel(channelId: string): void {
    this.channelSubscription?.unsubscribe();
    this.messages.set([]);
    this.typingNames.set(new Map());
    this.unseenCount.set(0);
    this.replyTo.set(null);
    this.loading.set(true);
    this.error.set(null);
    this.page = 0;

    this.api
      .get(channelId)
      .pipe(switchMap((channel) => (this.channel.set(channel), this.api.messages(channelId, 0, PAGE_SIZE))))
      .subscribe({
        next: (page) => {
          this.messages.set([...page.content].reverse());
          this.hasMore.set(page.totalPages > 1);
          this.loading.set(false);
          this.scrollToBottom();
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });

    this.channelSubscription = this.realtime
      .watch<unknown>(`/topic/channel.${channelId}`)
      .subscribe((event) => this.onRealtimeEvent(event));
  }

  loadOlder(): void {
    const element = this.scroller()?.nativeElement;
    const previousHeight = element?.scrollHeight ?? 0;
    this.page++;
    this.api.messages(this.channelId(), this.page, PAGE_SIZE).subscribe((page) => {
      this.messages.update((current) => [...[...page.content].reverse(), ...current]);
      this.hasMore.set(this.page < page.totalPages - 1);
      // Keep the view where it was instead of jumping when older messages are added on top.
      setTimeout(() => element && (element.scrollTop = element.scrollHeight - previousHeight));
    });
  }

  send({ content, file }: ComposerSubmit): void {
    const channelId = this.channelId();
    const parentId = this.replyTo()?.id ?? null;
    this.replyTo.set(null);
    const upload$: Observable<Attachment | null> = file ? this.api.upload(channelId, file) : of(null);
    upload$
      .pipe(switchMap((attachment) => this.api.send(channelId, content, parentId, attachment ? [attachment.id] : [])))
      .subscribe({
        next: (message) => {
          this.upsert(message);
          this.scrollToBottom();
        },
        error: (err) => this.error.set(errorMessage(err)),
      });
  }

  onTyping(): void {
    const now = Date.now();
    if (now - this.lastTypingSent > TYPING_THROTTLE_MS) {
      this.lastTypingSent = now;
      this.realtime.sendTyping(this.channelId());
    }
  }

  toggleReaction(message: Message, emoji: string): void {
    const myId = this.auth.currentUser()?.id ?? '';
    const alreadyReacted = message.reactions.some((r) => r.emoji === emoji && r.userIds.includes(myId));
    const request$ = alreadyReacted ? this.api.unreact(message.id, emoji) : this.api.react(message.id, emoji);
    request$.subscribe({ error: (err) => this.error.set(errorMessage(err)) });
  }

  edit(event: { message: Message; content: string }): void {
    this.api.edit(event.message.id, event.content).subscribe({
      next: (updated) => this.upsert(updated),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  remove(message: Message): void {
    if (!confirm('¿Seguro que quieres borrar este mensaje?')) {
      return;
    }
    this.api.delete(message.id).subscribe({
      next: () => this.removeLocally(message.id),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  download(attachment: Attachment): void {
    this.api.download(attachment.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = attachment.fileName;
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  onScroll(): void {
    if (this.isNearBottom()) {
      this.unseenCount.set(0);
    }
  }

  jumpToNewest(): void {
    this.unseenCount.set(0);
    this.scrollToBottom();
  }

  private onRealtimeEvent(event: RealtimeEvent<unknown>): void {
    switch (event.type) {
      case 'MESSAGE_CREATED': {
        const message = event.payload as Message;
        if (!this.state.membersById().has(message.authorId)) {
          // Someone who joined after we opened the workspace: reload members to show their name.
          this.state.refreshMembers();
        }
        const isNew = !this.messagesById().has(message.id);
        const wasAtBottom = this.isNearBottom();
        this.upsert(message);
        this.stopTyping(message.authorId);
        if (isNew && message.authorId !== this.auth.currentUser()?.id) {
          // Smart scroll: follow the conversation only if the user was already at the bottom.
          wasAtBottom ? this.scrollToBottom() : this.unseenCount.update((n) => n + 1);
        }
        break;
      }
      case 'MESSAGE_UPDATED':
        this.upsert(event.payload as Message);
        break;
      case 'MESSAGE_DELETED':
        this.removeLocally((event.payload as { messageId: string }).messageId);
        break;
      case 'REACTION_ADDED':
      case 'REACTION_REMOVED':
        this.applyReaction(event.payload as ReactionPayload, event.type === 'REACTION_ADDED');
        break;
      case 'TYPING': {
        const typing = event.payload as TypingPayload;
        if (typing.userId !== this.auth.currentUser()?.id) {
          this.showTyping(typing);
        }
        break;
      }
    }
  }

  /** Adds or replaces a message by id, so the same message arriving twice (REST + WebSocket) is shown once. */
  private upsert(message: Message): void {
    this.messages.update((list) => {
      const index = list.findIndex((m) => m.id === message.id);
      if (index === -1) {
        return [...list, message];
      }
      const copy = [...list];
      copy[index] = { ...message, reactions: message.reactions ?? list[index].reactions };
      return copy;
    });
  }

  private removeLocally(messageId: string): void {
    this.messages.update((list) => list.filter((m) => m.id !== messageId));
  }

  private applyReaction(payload: ReactionPayload, added: boolean): void {
    this.messages.update((list) =>
      list.map((message) => {
        if (message.id !== payload.messageId) return message;
        const reactions = message.reactions.map((r) => ({ ...r, userIds: [...r.userIds] }));
        let reaction = reactions.find((r) => r.emoji === payload.emoji);
        if (added) {
          if (!reaction) {
            reaction = { emoji: payload.emoji, count: 0, userIds: [] };
            reactions.push(reaction);
          }
          if (!reaction.userIds.includes(payload.userId)) {
            reaction.userIds.push(payload.userId);
          }
        } else if (reaction) {
          reaction.userIds = reaction.userIds.filter((id) => id !== payload.userId);
        }
        reactions.forEach((r) => (r.count = r.userIds.length));
        return { ...message, reactions: reactions.filter((r) => r.count > 0) };
      }),
    );
  }

  private showTyping(typing: TypingPayload): void {
    this.typingNames.update((names) => new Map(names).set(typing.userId, typing.displayName));
    clearTimeout(this.typingTimers.get(typing.userId));
    // The indicator disappears on its own if no new typing signal arrives.
    this.typingTimers.set(typing.userId, setTimeout(() => this.stopTyping(typing.userId), TYPING_TIMEOUT_MS));
  }

  private stopTyping(userId: string): void {
    clearTimeout(this.typingTimers.get(userId));
    this.typingTimers.delete(userId);
    this.typingNames.update((names) => {
      const copy = new Map(names);
      copy.delete(userId);
      return copy;
    });
  }

  private isNearBottom(): boolean {
    const element = this.scroller()?.nativeElement;
    return !element || element.scrollHeight - element.scrollTop - element.clientHeight < 120;
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const element = this.scroller()?.nativeElement;
      if (element) {
        element.scrollTop = element.scrollHeight;
      }
    });
  }
}
