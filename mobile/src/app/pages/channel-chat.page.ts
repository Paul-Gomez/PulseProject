import { DatePipe } from '@angular/common';
import { Component, inject, input, OnDestroy, signal, viewChild } from '@angular/core';
import {
  IonBackButton, IonButton, IonButtons, IonContent, IonFooter, IonHeader, IonIcon, IonItem, IonLabel, IonList, IonTextarea,
  IonTitle, IonToolbar, ViewWillEnter,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import { send } from 'ionicons/icons';
import { forkJoin, Subscription } from 'rxjs';
import { ChannelApi } from '../core/api/channel.api';
import { WorkspaceApi } from '../core/api/workspace.api';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../core/errors';
import { Channel, Message, TypingPayload } from '../core/models';
import { RealtimeService } from '../core/realtime/realtime.service';

@Component({
  selector: 'app-channel-chat',
  imports: [DatePipe, IonHeader, IonToolbar, IonButtons, IonBackButton, IonTitle, IonContent, IonList, IonItem, IonLabel,
    IonFooter, IonTextarea, IonButton, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-buttons slot="start"><ion-back-button [defaultHref]="'/tabs/workspaces/' + workspaceId()" /></ion-buttons>
        <ion-title># {{ channel()?.name }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content #content>
      <ion-list lines="none">
        @for (message of messages(); track message.id) {
          <ion-item [class.mine]="message.authorId === myId()">
            <ion-label class="ion-text-wrap">
              <h3>{{ names().get(message.authorId) ?? 'Usuario' }} <small>{{ message.createdAt | date: 'shortTime' }}</small></h3>
              <p class="text">{{ message.content }}</p>
              @if (message.reactions.length) {
                <p>
                  @for (reaction of message.reactions; track reaction.emoji) {
                    <span class="reaction">{{ reaction.emoji }} {{ reaction.count }}</span>
                  }
                </p>
              }
            </ion-label>
          </ion-item>
        }
      </ion-list>
    </ion-content>

    <ion-footer>
      @if (typing()) {
        <div class="typing">{{ typing() }} está escribiendo...</div>
      }
      @if (error()) {
        <div class="typing error">{{ error() }}</div>
      }
      <ion-toolbar>
        <ion-textarea [value]="text()" (ionInput)="onInput($event)" placeholder="Escribe un mensaje" autoGrow rows="1" maxlength="4000" />
        <ion-buttons slot="end">
          <ion-button (click)="sendMessage()" [disabled]="!text().trim()"><ion-icon slot="icon-only" name="send" /></ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-footer>
  `,
  styles: `
    .text { white-space: pre-wrap; color: var(--ion-text-color); }
    small { color: var(--ion-color-medium); font-weight: normal; }
    .mine { --background: rgba(var(--ion-color-primary-rgb), 0.08); }
    .reaction { border: 1px solid var(--ion-color-light-shade); border-radius: 12px; padding: 0 6px; margin-right: 4px; }
    .typing { font-size: 0.8rem; color: var(--ion-color-medium); padding: 2px 16px; font-style: italic; }
    .error { color: var(--ion-color-danger); }
    ion-textarea { padding-left: 12px; }
  `,
})
export class ChannelChatPage implements ViewWillEnter, OnDestroy {
  private readonly channelApi = inject(ChannelApi);
  private readonly workspaceApi = inject(WorkspaceApi);
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthService);
  private readonly content = viewChild(IonContent);
  private subscription: Subscription | null = null;
  private typingTimer: ReturnType<typeof setTimeout> | undefined;
  private lastTypingSent = 0;

  readonly workspaceId = input.required<string>();
  readonly channelId = input.required<string>();

  protected readonly channel = signal<Channel | null>(null);
  protected readonly messages = signal<Message[]>([]);
  protected readonly names = signal<Map<string, string>>(new Map());
  protected readonly text = signal('');
  protected readonly typing = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    addIcons({ send });
  }

  protected myId = () => this.auth.currentUser()?.id;

  ionViewWillEnter(): void {
    forkJoin({
      channel: this.channelApi.get(this.channelId()),
      messages: this.channelApi.messages(this.channelId(), 0, 40),
      members: this.workspaceApi.members(this.workspaceId()),
    }).subscribe(({ channel, messages, members }) => {
      this.channel.set(channel);
      this.messages.set([...messages.content].reverse());
      this.names.set(new Map(members.content.map((m) => [m.userId, m.user.displayName])));
      this.scrollToBottom();
    });

    this.subscription?.unsubscribe();
    this.subscription = this.realtime.watch<unknown>(`/topic/channel.${this.channelId()}`).subscribe((event) => {
      if (event.type === 'MESSAGE_CREATED' || event.type === 'MESSAGE_UPDATED') {
        this.upsert(event.payload as Message);
        this.typing.set(null);
      } else if (event.type === 'MESSAGE_DELETED') {
        const { messageId } = event.payload as { messageId: string };
        this.messages.update((list) => list.filter((m) => m.id !== messageId));
      } else if (event.type === 'TYPING') {
        const payload = event.payload as TypingPayload;
        if (payload.userId !== this.myId()) {
          this.typing.set(payload.displayName);
          clearTimeout(this.typingTimer);
          this.typingTimer = setTimeout(() => this.typing.set(null), 4000);
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    clearTimeout(this.typingTimer);
  }

  onInput(event: CustomEvent): void {
    this.text.set((event.detail.value as string) ?? '');
    if (Date.now() - this.lastTypingSent > 2000) {
      this.lastTypingSent = Date.now();
      this.realtime.sendTyping(this.channelId());
    }
  }

  sendMessage(): void {
    const content = this.text().trim();
    if (!content) return;
    this.text.set('');
    this.channelApi.send(this.channelId(), content).subscribe({
      next: (message) => this.upsert(message),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  /** Same message can arrive from the REST response and from the WebSocket; keep only one. */
  private upsert(message: Message): void {
    this.messages.update((list) => {
      const index = list.findIndex((m) => m.id === message.id);
      if (index === -1) return [...list, message];
      const copy = [...list];
      copy[index] = message;
      return copy;
    });
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    setTimeout(() => this.content()?.scrollToBottom(200));
  }
}
