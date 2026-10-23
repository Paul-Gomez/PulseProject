"use client";

import { useCallback, useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { api, json } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import { realtime, useTopic } from "@/lib/realtime";
import type { Message, Page, RealtimeEvent } from "@/lib/types";
import { useWorkspace } from "@/lib/workspace-context";
import { ConfirmDialog } from "../ConfirmDialog";
import { Icon } from "../Icon";
import { applyReaction, prependOlder, upsertMessage } from "./chat-state";
import { MessageItem } from "./MessageItem";
import styles from "./Chat.module.css";

const TYPING_TIMEOUT_MS = 4000;
const TYPING_THROTTLE_MS = 2000;
const PAGE_SIZE = 50;

export function ChannelChat({ channelId }: { channelId: string }) {
  const { user } = useAuth();
  const { channels, members, reloadMembers } = useWorkspace();
  const channel = channels.find((c) => c.id === channelId);

  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [text, setText] = useState("");
  const [typing, setTyping] = useState<Record<string, string>>({});
  const [olderPage, setOlderPage] = useState<number | null>(null); // next page to load, null = no more
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<Message | null>(null);
  const scroller = useRef<HTMLDivElement>(null);
  const typingTimers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  const lastTypingSent = useRef(0);

  const scrollToBottom = () => setTimeout(() => scroller.current?.scrollTo({ top: scroller.current.scrollHeight }));

  useEffect(() => {
    let cancelled = false;
    api<Page<Message>>(`/channels/${channelId}/messages?size=${PAGE_SIZE}`)
      .then((page) => {
        if (cancelled) return;
        setMessages([...page.content].reverse());
        setOlderPage(page.page + 1 < page.totalPages ? page.page + 1 : null);
        scrollToBottom();
      })
      .catch((err) => !cancelled && setError(errorMessage(err)))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [channelId]);

  // Keeps the message the user was reading in place: the older page grows the list upwards.
  async function loadOlder() {
    if (olderPage === null || loadingOlder) return;
    setLoadingOlder(true);
    const box = scroller.current;
    const heightBefore = box?.scrollHeight ?? 0;
    try {
      const page = await api<Page<Message>>(`/channels/${channelId}/messages?size=${PAGE_SIZE}&page=${olderPage}`);
      setMessages((list) => prependOlder(list, page.content));
      setOlderPage(page.page + 1 < page.totalPages ? page.page + 1 : null);
      requestAnimationFrame(() => {
        if (box) box.scrollTop += box.scrollHeight - heightBefore;
      });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingOlder(false);
    }
  }

  const stopTyping = useCallback((userId: string) => {
    clearTimeout(typingTimers.current[userId]);
    setTyping((current) => {
      const next = { ...current };
      delete next[userId];
      return next;
    });
  }, []);

  useTopic(`/topic/channel.${channelId}`, (event: RealtimeEvent) => {
    switch (event.type) {
      case "MESSAGE_CREATED":
      case "MESSAGE_UPDATED": {
        const message = event.payload as Message;
        if (!members.has(message.authorId)) reloadMembers();
        setMessages((list) => upsertMessage(list, message));
        stopTyping(message.authorId);
        scrollToBottom();
        break;
      }
      case "MESSAGE_DELETED": {
        const { messageId } = event.payload as { messageId: string };
        setMessages((list) => list.filter((m) => m.id !== messageId));
        break;
      }
      case "REACTION_ADDED":
      case "REACTION_REMOVED":
        setMessages((list) => applyReaction(list, event.payload as never, event.type === "REACTION_ADDED"));
        break;
      case "TYPING": {
        const { userId, displayName } = event.payload as { userId: string; displayName: string };
        if (userId === user?.id) break;
        setTyping((current) => ({ ...current, [userId]: displayName }));
        clearTimeout(typingTimers.current[userId]);
        typingTimers.current[userId] = setTimeout(() => stopTyping(userId), TYPING_TIMEOUT_MS);
        break;
      }
    }
  });

  async function send(event?: FormEvent) {
    event?.preventDefault();
    const content = text.trim();
    if (!content) return;
    setText("");
    try {
      const message = await api<Message>(`/channels/${channelId}/messages`, { method: "POST", body: json({ content }) });
      setMessages((list) => upsertMessage(list, message));
      scrollToBottom();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      send();
    }
  }

  function onChange(value: string) {
    setText(value);
    if (Date.now() - lastTypingSent.current > TYPING_THROTTLE_MS) {
      lastTypingSent.current = Date.now();
      realtime.sendTyping(channelId);
    }
  }

  const react = useCallback(
    (message: Message, emoji: string) => {
      const mine = message.reactions.some((r) => r.emoji === emoji && r.userIds.includes(user?.id ?? ""));
      const request = mine
        ? api(`/messages/${message.id}/reactions/${encodeURIComponent(emoji)}`, { method: "DELETE" })
        : api(`/messages/${message.id}/reactions`, { method: "POST", body: json({ emoji }) });
      request.catch((err) => setError(errorMessage(err)));
    },
    [user?.id],
  );

  const askDelete = useCallback((message: Message) => setPendingDelete(message), []);

  function confirmDelete() {
    const message = pendingDelete;
    setPendingDelete(null);
    if (!message) return;
    api(`/messages/${message.id}`, { method: "DELETE" })
      .then(() => setMessages((list) => list.filter((m) => m.id !== message.id)))
      .catch((err) => setError(errorMessage(err)));
  }

  const typingNames = Object.values(typing);

  return (
    <div className={styles.chat}>
      <header className={styles.header}>
        <h2 className={styles.title}>
          {channel && <Icon name={channel.isPrivate ? "lock" : "hash"} size={18} />}
          <span>{channel?.name ?? "…"}</span>
        </h2>
      </header>

      {error && (
        <div className={`alert ${styles.alert}`} role="alert">
          <span>{error}</span>
          <button type="button" className="icon-btn" onClick={() => setError(null)} aria-label="Cerrar aviso">
            <Icon name="x" size={16} />
          </button>
        </div>
      )}

      <div className={styles.scroller} ref={scroller}>
        {!loading && olderPage !== null && (
          <button type="button" className={`btn btn-ghost ${styles.loadOlder}`} onClick={loadOlder} disabled={loadingOlder}>
            {loadingOlder ? "Cargando..." : "Ver mensajes anteriores"}
          </button>
        )}
        {loading
          ? [1, 2, 3, 4].map((i) => <div key={i} className={`skeleton ${styles.skeletonRow}`} />)
          : messages.map((m) => (
              <MessageItem key={m.id} message={m} author={members.get(m.authorId)} currentUserId={user?.id ?? ""}
                           currentUsername={user?.username ?? ""} onReact={react} onDelete={askDelete} />
            ))}
        {!loading && messages.length === 0 && <p className="empty">Todavía no hay mensajes. ¡Escribe el primero!</p>}
      </div>

      <div className={styles.typing}>
        {typingNames.length === 1 && `${typingNames[0]} está escribiendo...`}
        {typingNames.length > 1 && `${typingNames.slice(0, 2).join(" y ")} están escribiendo...`}
      </div>

      <form className={styles.composer} onSubmit={send}>
        <textarea className="input" rows={1} maxLength={4000} value={text} placeholder={`Mensaje en #${channel?.name ?? ""}`}
                  onChange={(e) => onChange(e.target.value)} onKeyDown={onKeyDown} />
        <button className="btn" type="submit" disabled={!text.trim()}>Enviar</button>
      </form>

      {pendingDelete && (
        <ConfirmDialog title="Borrar mensaje" message="El mensaje desaparecerá para todos los miembros del canal."
                       confirmLabel="Borrar" onConfirm={confirmDelete} onCancel={() => setPendingDelete(null)} />
      )}
    </div>
  );
}
