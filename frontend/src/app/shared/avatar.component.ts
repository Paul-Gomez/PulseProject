import { Component, computed, input } from '@angular/core';

// Every colour keeps at least 4.5:1 with the white initials.
const COLORS = ['#5b5bd6', '#cd4257', '#27854c', '#a16823', '#2a77be', '#9b4fd6', '#23827b'];

/** Round avatar: the user's picture if they have one, otherwise their initials. Optional online dot. */
@Component({
  selector: 'app-avatar',
  template: `
    <span class="avatar" [style.width.px]="size()" [style.height.px]="size()" [style.font-size.px]="fontSize()"
          [style.background]="avatarUrl() ? 'transparent' : color()">
      @if (avatarUrl()) {
        <img [src]="avatarUrl()" [alt]="name()" />
      } @else {
        {{ initials() }}
      }
      @if (online() !== null) {
        <span class="dot" [class.on]="online()"></span>
      }
    </span>
  `,
  styles: `
    .avatar { position: relative; display: inline-flex; align-items: center; justify-content: center;
      border-radius: 50%; color: #fff; font-weight: 700; flex-shrink: 0; user-select: none; }
    img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
    .dot { position: absolute; right: -1px; bottom: -1px; width: 30%; height: 30%; min-width: 8px; min-height: 8px;
      border-radius: 50%; background: #9aa1ad; border: 2px solid var(--surface); }
    .dot.on { background: var(--success); }
  `,
})
export class AvatarComponent {
  readonly name = input.required<string>();
  readonly avatarUrl = input<string | null>(null);
  readonly size = input(32);
  readonly online = input<boolean | null>(null);

  /** 40% of the avatar, but never under 11px so the initials stay legible on small avatars. */
  protected readonly fontSize = computed(() => Math.max(11, this.size() * 0.4));

  protected readonly initials = computed(() =>
    this.name()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]!.toUpperCase())
      .join(''),
  );

  protected readonly color = computed(() => {
    let hash = 0;
    for (const char of this.name()) {
      hash = (hash * 31 + char.charCodeAt(0)) | 0;
    }
    return COLORS[Math.abs(hash) % COLORS.length];
  });
}
