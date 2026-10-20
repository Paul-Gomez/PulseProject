"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState, type KeyboardEvent } from "react";
import { useWorkspace } from "@/lib/workspace-context";
import { Icon, type IconName } from "./Icon";
import styles from "./CommandPalette.module.css";

interface Item {
  id: string;
  label: string;
  hint: string;
  icon: IconName;
  href: string;
}

/**
 * Ctrl+K quick switcher. It only lists what the user can already see (channels and workspaces),
 * so it needs no extra API calls. Mounted on open, so its state starts fresh every time.
 */
export function CommandPalette({ onClose }: { onClose(): void }) {
  const { current, channels, workspaces } = useWorkspace();
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);

  const items = useMemo<Item[]>(() => {
    const q = query.trim().toLowerCase();
    const channelItems: Item[] = current
      ? channels.map((c) => ({
          id: `c-${c.id}`,
          label: c.name,
          hint: current.name,
          icon: c.isPrivate ? "lock" : "hash",
          href: `/w/${current.id}/c/${c.id}`,
        }))
      : [];
    const workspaceItems: Item[] = workspaces.map((w) => ({
      id: `w-${w.id}`,
      label: w.name,
      hint: "Workspace",
      icon: "grid",
      href: `/w/${w.id}`,
    }));
    return [...channelItems, ...workspaceItems].filter((item) => item.label.toLowerCase().includes(q)).slice(0, 8);
  }, [query, current, channels, workspaces]);

  function go(item: Item) {
    router.push(item.href);
    onClose();
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Escape") onClose();
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActive((i) => (items.length ? (i + 1) % items.length : 0));
    }
    if (event.key === "ArrowUp") {
      event.preventDefault();
      setActive((i) => (items.length ? (i - 1 + items.length) % items.length : 0));
    }
    if (event.key === "Enter" && items[active]) go(items[active]);
  }

  return (
    <div className={styles.overlay} onMouseDown={onClose}>
      <div className={styles.panel} role="dialog" aria-modal="true" aria-label="Buscar" onMouseDown={(e) => e.stopPropagation()}>
        <div className={styles.inputRow}>
          <Icon name="search" size={18} />
          <input
            autoFocus
            className={styles.input}
            placeholder="Saltar a un canal o workspace…"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setActive(0);
            }}
            onKeyDown={onKeyDown}
          />
        </div>

        <ul className={styles.list}>
          {items.map((item, i) => (
            <li key={item.id}>
              <button
                type="button"
                className={`${styles.item} ${i === active ? styles.itemActive : ""}`}
                onMouseEnter={() => setActive(i)}
                onClick={() => go(item)}
              >
                <Icon name={item.icon} size={16} />
                <span className={styles.label}>{item.label}</span>
                <span className={styles.hint}>{item.hint}</span>
              </button>
            </li>
          ))}
          {items.length === 0 && <li className={styles.empty}>Sin resultados</li>}
        </ul>

        <footer className={styles.footer}>
          <kbd>↑</kbd><kbd>↓</kbd> moverse <kbd>Enter</kbd> abrir <kbd>Esc</kbd> cerrar
        </footer>
      </div>
    </div>
  );
}
