"use client";

import { useState } from "react";
import styles from "./StatusButton.module.css";

const KEY = "pulse.status";
const STATUSES = [
  { label: "Disponible", color: "#2f9e5b" },
  { label: "Ausente", color: "#d68a2f" },
  { label: "No molestar", color: "#d64545" },
];

function readStatus(): number {
  try {
    const value = Number(localStorage.getItem(KEY));
    return Number.isInteger(value) && value >= 0 && value < STATUSES.length ? value : 0;
  } catch {
    return 0;
  }
}

/** Cycles through presence states. It is shown only in the client-rendered shell, so reading storage here is safe. */
export function StatusButton() {
  const [index, setIndex] = useState(readStatus);
  const status = STATUSES[index];

  function next() {
    const following = (index + 1) % STATUSES.length;
    setIndex(following);
    try {
      localStorage.setItem(KEY, String(following));
    } catch {
      // Storage blocked: the change still applies until the page reloads.
    }
  }

  return (
    <button type="button" className={styles.status} onClick={next} title="Cambiar estado">
      <span className={styles.dot} style={{ background: status.color }} />
      {status.label}
    </button>
  );
}
