import type { Message } from "@/lib/types";

/** Adds or replaces a message by id: the author gets it from the REST response and the WebSocket. */
export function upsertMessage(list: Message[], message: Message): Message[] {
  const index = list.findIndex((m) => m.id === message.id);
  if (index === -1) return [...list, message];
  const copy = [...list];
  copy[index] = { ...message, reactions: message.reactions ?? list[index].reactions };
  return copy;
}

/**
 * Puts an older page (as the API returns it: newest first) in front of the current list.
 * New messages arriving in real time shift the pages, so the older page can repeat
 * messages we already show: those are skipped.
 */
export function prependOlder(list: Message[], olderPageNewestFirst: Message[]): Message[] {
  const known = new Set(list.map((m) => m.id));
  const older = [...olderPageNewestFirst].reverse().filter((m) => !known.has(m.id));
  return [...older, ...list];
}

/** Applies a REACTION_ADDED / REACTION_REMOVED event without mutating the previous state. */
export function applyReaction(
  list: Message[],
  payload: { messageId: string; userId: string; emoji: string },
  added: boolean,
): Message[] {
  return list.map((message) => {
    if (message.id !== payload.messageId) return message;
    const reactions = message.reactions.map((r) => ({ ...r, userIds: [...r.userIds] }));
    let reaction = reactions.find((r) => r.emoji === payload.emoji);
    if (added) {
      if (!reaction) {
        reaction = { emoji: payload.emoji, count: 0, userIds: [] };
        reactions.push(reaction);
      }
      if (!reaction.userIds.includes(payload.userId)) reaction.userIds.push(payload.userId);
    } else if (reaction) {
      reaction.userIds = reaction.userIds.filter((id) => id !== payload.userId);
    }
    return {
      ...message,
      reactions: reactions.map((r) => ({ ...r, count: r.userIds.length })).filter((r) => r.count > 0),
    };
  });
}
