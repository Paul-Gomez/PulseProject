import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../shared/icon.component';

export interface ComposerSubmit {
  content: string;
  file: File | null;
}

/** Text box at the bottom of a chat. Enter sends, Shift+Enter adds a new line. */
@Component({
  selector: 'app-composer',
  imports: [FormsModule, IconComponent],
  template: `
    @if (replyingTo()) {
      <div class="reply-bar">
        <span>Respondiendo a: <em>{{ replyingTo() }}</em></span>
        <button class="btn btn-ghost" type="button" (click)="cancelReply.emit()" aria-label="Cancelar respuesta"><app-icon name="x" [size]="16" /></button>
      </div>
    }
    @if (file()) {
      <div class="reply-bar">
        <span><app-icon name="paperclip" [size]="16" /> {{ file()!.name }}</span>
        <button class="btn btn-ghost" type="button" (click)="file.set(null)" aria-label="Quitar archivo"><app-icon name="x" [size]="16" /></button>
      </div>
    }
    <form class="composer" (ngSubmit)="submit()">
      @if (allowFiles()) {
        <label class="btn btn-ghost attach" title="Adjuntar archivo" aria-label="Adjuntar archivo">
          <app-icon name="paperclip" [size]="20" />
          <input type="file" hidden accept=".png,.jpg,.jpeg,.gif,.webp,.pdf,.txt,.docx,.xlsx" (change)="pickFile($event)" />
        </label>
      }
      <textarea class="input" rows="1" [value]="text()" [placeholder]="placeholder()" [disabled]="disabled()"
                maxlength="4000" (keydown)="onKeydown($event)" (input)="onInput($event)"></textarea>
      <button class="btn" type="submit" [disabled]="disabled() || (!text().trim() && !file())">Enviar</button>
    </form>
  `,
  styles: `
    :host { display: block; padding: 0.75rem 1rem; border-top: 1px solid var(--border); background: var(--surface); }
    .composer { display: flex; gap: 0.5rem; align-items: flex-end; }
    textarea { resize: none; max-height: 140px; }
    .attach { cursor: pointer; font-size: 1.1rem; }
    .reply-bar { display: flex; align-items: center; justify-content: space-between; font-size: 0.85rem;
      color: var(--text-muted); margin-bottom: 0.4rem; }
  `,
})
export class ComposerComponent {
  readonly placeholder = input('Escribe un mensaje...');
  readonly disabled = input(false);
  readonly allowFiles = input(true);
  readonly replyingTo = input<string | null>(null);

  readonly send = output<ComposerSubmit>();
  readonly typing = output<void>();
  readonly cancelReply = output<void>();

  // A signal (not a plain field) so clearing it after sending always updates the textarea.
  protected readonly text = signal('');
  protected readonly file = signal<File | null>(null);

  onInput(event: Event): void {
    this.text.set((event.target as HTMLTextAreaElement).value);
    this.typing.emit();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.submit();
    }
  }

  pickFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file.set(input.files?.[0] ?? null);
    input.value = '';
  }

  submit(): void {
    const content = this.text().trim();
    if (!content && !this.file()) {
      return;
    }
    // The API requires some text, so a file sent alone gets its name as the message.
    this.send.emit({ content: content || this.file()!.name, file: this.file() });
    this.text.set('');
    this.file.set(null);
  }
}
