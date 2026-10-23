import { Component, input } from '@angular/core';

export type IconName =
  | 'hash' | 'lock' | 'x' | 'message' | 'arrow-down' | 'arrow-left' | 'paperclip' | 'reply' | 'edit' | 'trash'
  | 'home' | 'bell' | 'search' | 'mail' | 'alert' | 'info' | 'at' | 'settings' | 'menu' | 'sun' | 'moon' | 'logout';

/**
 * Stroke icons on a 24px grid, drawn with currentColor so they follow the theme.
 * Same drawings as the Next.js client, so both webs look alike. Decorative by default:
 * the button or link that contains the icon carries the accessible name.
 */
@Component({
  selector: 'app-icon',
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      @switch (name()) {
        @case ('hash') { <path d="M4 9h16M4 15h16M10 3L8 21M16 3l-2 18" /> }
        @case ('lock') { <rect x="5" y="11" width="14" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" /> }
        @case ('x') { <path d="M18 6L6 18M6 6l12 12" /> }
        @case ('message') { <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" /> }
        @case ('arrow-down') { <path d="M12 5v14M6 13l6 6 6-6" /> }
        @case ('arrow-left') { <path d="M19 12H5M11 6l-6 6 6 6" /> }
        @case ('paperclip') { <path d="M21 11.5l-8.6 8.6a5 5 0 0 1-7.1-7.1l8.6-8.6a3.3 3.3 0 0 1 4.7 4.7l-8.6 8.6a1.7 1.7 0 0 1-2.4-2.4l7.9-7.9" /> }
        @case ('reply') { <path d="M9 14L4 9l5-5" /><path d="M4 9h11a5 5 0 0 1 5 5v6" /> }
        @case ('edit') { <path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" /> }
        @case ('trash') { <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" /> }
        @case ('home') { <path d="M3 10.5L12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z" /> }
        @case ('bell') { <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" /> }
        @case ('search') { <circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /> }
        @case ('mail') { <rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 7l9 6 9-6" /> }
        @case ('alert') { <path d="M12 3l10 18H2z" /><path d="M12 10v4M12 17.5v.5" /> }
        @case ('info') { <circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 7.5v.5" /> }
        @case ('at') { <circle cx="12" cy="12" r="4" /><path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-4 8" /> }
        @case ('settings') {
          <circle cx="12" cy="12" r="3" />
          <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z" />
        }
        @case ('menu') { <path d="M4 6h16M4 12h16M4 18h16" /> }
        @case ('sun') { <circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /> }
        @case ('moon') { <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" /> }
        @case ('logout') { <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="M16 17l5-5-5-5M21 12H9" /> }
      }
    </svg>
  `,
  styles: `:host { display: inline-flex; vertical-align: middle; flex-shrink: 0; }`,
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input(18);
}
