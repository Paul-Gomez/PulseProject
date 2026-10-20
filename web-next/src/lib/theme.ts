"use client";

import { useSyncExternalStore } from "react";

// The "dark" class lives on <html> (the inline script in layout.tsx sets it before paint).
// This store lets any component read it and react when the user flips the switch.
const EVENT = "pulse-theme-change";

function subscribe(onChange: () => void) {
  window.addEventListener(EVENT, onChange);
  return () => window.removeEventListener(EVENT, onChange);
}

export function useDarkMode(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => document.documentElement.classList.contains("dark"),
    () => false,
  );
}

export function setDarkMode(dark: boolean) {
  document.documentElement.classList.toggle("dark", dark);
  try {
    localStorage.setItem("pulse.theme", dark ? "dark" : "light");
  } catch {
    // Private mode or blocked storage: the theme still changes for this page view.
  }
  window.dispatchEvent(new Event(EVENT));
}
