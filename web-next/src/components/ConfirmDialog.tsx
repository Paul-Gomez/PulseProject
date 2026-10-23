"use client";

import { useEffect, useRef } from "react";
import styles from "./ConfirmDialog.module.css";

interface Props {
  title: string;
  message: string;
  confirmLabel: string;
  onConfirm(): void;
  onCancel(): void;
}

/**
 * Confirmation for destructive actions. Built on the native <dialog>: the browser handles
 * the backdrop, keeps the keyboard focus inside and closes it with Escape.
 */
export function ConfirmDialog({ title, message, confirmLabel, onConfirm, onCancel }: Props) {
  const dialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    dialog.current?.showModal();
  }, []);

  return (
    <dialog ref={dialog} className={styles.dialog} aria-labelledby="confirm-title" onCancel={onCancel}
            onClick={(e) => e.target === dialog.current && onCancel()}>
      <h2 id="confirm-title" className={styles.title}>{title}</h2>
      <p className={styles.message}>{message}</p>
      <div className={styles.actions}>
        <button type="button" className="btn btn-ghost" onClick={onCancel} autoFocus>Cancelar</button>
        <button type="button" className={`btn ${styles.danger}`} onClick={onConfirm}>{confirmLabel}</button>
      </div>
    </dialog>
  );
}
