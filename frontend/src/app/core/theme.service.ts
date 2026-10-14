import { Injectable, signal } from '@angular/core';

const THEME_KEY = 'pulse.theme';

/** Light/dark mode: starts with the system preference and remembers the user's choice. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly dark = signal(false);

  init(): void {
    const saved = localStorage.getItem(THEME_KEY);
    const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    this.apply(saved ? saved === 'dark' : prefersDark);
  }

  toggle(): void {
    this.apply(!this.dark());
    localStorage.setItem(THEME_KEY, this.dark() ? 'dark' : 'light');
  }

  private apply(dark: boolean): void {
    this.dark.set(dark);
    document.documentElement.classList.toggle('dark', dark);
  }
}
