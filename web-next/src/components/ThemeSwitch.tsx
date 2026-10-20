"use client";

import { useDarkMode, setDarkMode } from "@/lib/theme";
import { Icon } from "./Icon";
import styles from "./ThemeSwitch.module.css";

/** A sliding switch: the thumb carries a sun in light mode and a moon in dark mode. */
export function ThemeSwitch() {
  const dark = useDarkMode();
  return (
    <button
      type="button"
      role="switch"
      aria-checked={dark}
      aria-label="Modo oscuro"
      title={dark ? "Modo oscuro activado" : "Modo oscuro desactivado"}
      className={`${styles.switch} ${dark ? styles.on : ""}`}
      onClick={() => setDarkMode(!dark)}
    >
      <span className={styles.thumb}>
        <Icon name={dark ? "moon" : "sun"} size={14} />
      </span>
    </button>
  );
}
