import { memo } from "react";
import type { Member, Message } from "@/lib/types";
import { Avatar } from "../Avatar";
import styles from "./Chat.module.css";

const QUICK_REACTIONS = ["👍", "❤️", "😂", "😮", "😢"];
const MENTION = /(@[a-zA-Z0-9_.-]{3,50})/g;

interface Props {
  message: Message;
  author: Member | undefined;
  currentUserId: string;
  currentUsername: string;
  onReact(message: Message, emoji: string): void;
  onDelete(message: Message): void;
}

/** One chat message. React escapes the text, so a message can never inject HTML. */
export const MessageItem = memo(function MessageItem({ message, author, currentUserId, currentUsername, onReact, onDelete }: Props) {
  const name = author?.user.displayName ?? "Usuario";
  const mentionsMe = message.content.includes(`@${currentUsername}`);
  const time = new Date(message.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

  return (
    <article className={`${styles.message} ${mentionsMe ? styles.mentionsMe : ""}`}>
      <Avatar name={name} size={36} />
      <div className={styles.body}>
        <header>
          <strong>{name}</strong> <time className="muted">{time}</time>
          {message.editedAt && <span className="muted"> (editado)</span>}
        </header>
        <p className={styles.content}>
          {message.content.split(MENTION).map((part, i) =>
            part.startsWith("@") ? <span key={i} className={styles.mention}>{part}</span> : part,
          )}
        </p>
        {message.reactions.length > 0 && (
          <div className={styles.reactions}>
            {message.reactions.map((r) => (
              <button key={r.emoji} type="button" onClick={() => onReact(message, r.emoji)}
                      className={`${styles.reaction} ${r.userIds.includes(currentUserId) ? styles.mine : ""}`}>
                {r.emoji} {r.count}
              </button>
            ))}
          </div>
        )}
      </div>
      <div className={styles.actions}>
        {QUICK_REACTIONS.map((emoji) => (
          <button key={emoji} type="button" className="btn btn-ghost" title={`Reaccionar ${emoji}`}
                  onClick={() => onReact(message, emoji)}>{emoji}</button>
        ))}
        {message.authorId === currentUserId && (
          <button type="button" className="btn btn-ghost" title="Borrar" onClick={() => onDelete(message)}>🗑</button>
        )}
      </div>
    </article>
  );
});
