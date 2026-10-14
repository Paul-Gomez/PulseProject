import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Attachment, Member, Message } from '../../core/models';
import { AvatarComponent } from '../../shared/avatar.component';

export const QUICK_REACTIONS = ['👍', '❤️', '😂', '😮', '😢'];

interface TextPart {
  text: string;
  mention: boolean;
}

@Component({
  selector: 'app-message-item',
  imports: [AvatarComponent, DatePipe, FormsModule],
  template: `
    <article class="message" [class.mentions-me]="mentionsMe()">
      <app-avatar [name]="authorName()" [avatarUrl]="author()?.user?.avatarUrl ?? null" [size]="36" />
      <div class="body">
        <header>
          <strong>{{ authorName() }}</strong>
          <time class="muted" [title]="message().createdAt | date: 'medium'">{{ message().createdAt | date: 'short' }}</time>
          @if (message().editedAt) {
            <span class="muted small">(editado)</span>
          }
        </header>

        @if (parent(); as parentMessage) {
          <div class="reply-ref">↪ {{ parentMessage.content }}</div>
        }

        @if (editing()) {
          <form class="edit" (ngSubmit)="saveEdit()">
            <input class="input" name="content" [(ngModel)]="draft" maxlength="4000" />
            <button class="btn" type="submit">Guardar</button>
            <button class="btn btn-secondary" type="button" (click)="editing.set(false)">Cancelar</button>
          </form>
        } @else {
          <!-- Rendered as text parts, never as HTML, so a message can't inject markup (XSS). -->
          <p class="content">@for (part of parts(); track $index) {<span [class.mention]="part.mention">{{ part.text }}</span>}</p>
        }

        @for (attachment of message().attachments; track attachment.id) {
          <button class="attachment" type="button" (click)="download.emit(attachment)">
            📎 {{ attachment.fileName }} <span class="muted">({{ sizeLabel(attachment) }})</span>
          </button>
        }

        @if (message().reactions.length) {
          <div class="reactions">
            @for (reaction of message().reactions; track reaction.emoji) {
              <button class="reaction" type="button" [class.mine]="reaction.userIds.includes(currentUserId())"
                      (click)="react.emit(reaction.emoji)">
                {{ reaction.emoji }} {{ reaction.count }}
              </button>
            }
          </div>
        }
      </div>

      <div class="actions">
        @for (emoji of quickReactions; track emoji) {
          <button class="btn btn-ghost" type="button" (click)="react.emit(emoji)" [title]="'Reaccionar ' + emoji">{{ emoji }}</button>
        }
        <button class="btn btn-ghost" type="button" (click)="reply.emit(message())" title="Responder">↩</button>
        @if (isMine()) {
          <button class="btn btn-ghost" type="button" (click)="startEdit()" title="Editar">✎</button>
        }
        @if (isMine() || canModerate()) {
          <button class="btn btn-ghost" type="button" (click)="remove.emit(message())" title="Borrar">🗑</button>
        }
      </div>
    </article>
  `,
  styles: `
    .message { position: relative; display: flex; gap: 0.75rem; padding: 0.5rem 1rem; }
    .message:hover { background: var(--surface-alt); }
    .message:hover .actions { display: flex; }
    .mentions-me { background: var(--mention); border-left: 3px solid #e0a800; }
    .body { min-width: 0; flex: 1; }
    header { display: flex; align-items: baseline; gap: 0.5rem; }
    time, .small { font-size: 0.75rem; }
    .content { margin: 0.15rem 0 0; white-space: pre-wrap; word-break: break-word; }
    .mention { background: var(--primary-soft); color: var(--primary); border-radius: 4px; padding: 0 2px; font-weight: 600; }
    .reply-ref { font-size: 0.8rem; color: var(--text-muted); border-left: 3px solid var(--border); padding-left: 0.5rem;
      margin: 0.2rem 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .attachment { display: inline-block; margin-top: 0.35rem; padding: 0.4rem 0.6rem; border: 1px solid var(--border);
      border-radius: var(--radius); background: var(--surface); color: var(--text); cursor: pointer; font: inherit; }
    .reactions { display: flex; flex-wrap: wrap; gap: 0.3rem; margin-top: 0.35rem; }
    .reaction { border: 1px solid var(--border); background: var(--surface); border-radius: 999px; padding: 0.1rem 0.5rem;
      cursor: pointer; font: inherit; font-size: 0.85rem; color: var(--text); }
    .reaction.mine { border-color: var(--primary); background: var(--primary-soft); }
    .actions { display: none; position: absolute; top: -12px; right: 1rem; background: var(--surface);
      border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); }
    .edit { display: flex; gap: 0.4rem; margin-top: 0.3rem; }
  `,
})
export class MessageItemComponent {
  readonly message = input.required<Message>();
  readonly author = input<Member | undefined>();
  readonly parent = input<Message | undefined>();
  readonly currentUserId = input.required<string>();
  readonly currentUsername = input<string>('');
  readonly canModerate = input(false);

  readonly react = output<string>();
  readonly reply = output<Message>();
  readonly edit = output<{ message: Message; content: string }>();
  readonly remove = output<Message>();
  readonly download = output<Attachment>();

  protected readonly quickReactions = QUICK_REACTIONS;
  protected readonly editing = signal(false);
  protected draft = '';

  protected readonly authorName = computed(() => this.author()?.user.displayName ?? 'Usuario');
  protected readonly isMine = computed(() => this.message().authorId === this.currentUserId());
  protected readonly mentionsMe = computed(
    () => !!this.currentUsername() && this.message().content.includes('@' + this.currentUsername()),
  );

  /** Splits the text so @mentions can be highlighted without using innerHTML. */
  protected readonly parts = computed<TextPart[]>(() =>
    this.message()
      .content.split(/(@[a-zA-Z0-9_.-]{3,50})/g)
      .filter((text) => text.length > 0)
      .map((text) => ({ text, mention: text.startsWith('@') })),
  );

  startEdit(): void {
    this.draft = this.message().content;
    this.editing.set(true);
  }

  saveEdit(): void {
    const content = this.draft.trim();
    if (content && content !== this.message().content) {
      this.edit.emit({ message: this.message(), content });
    }
    this.editing.set(false);
  }

  sizeLabel(attachment: Attachment): string {
    const kb = attachment.sizeBytes / 1024;
    return kb < 1024 ? `${Math.ceil(kb)} KB` : `${(kb / 1024).toFixed(1)} MB`;
  }
}
