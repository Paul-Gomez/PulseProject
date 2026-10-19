import { describe, expect, it } from "vitest";
import type { Message } from "@/lib/types";
import { applyReaction, upsertMessage } from "./chat-state";

const message = (id: string, content = "hola"): Message => ({
  id, channelId: "c1", authorId: "u1", content, parentMessageId: null, editedAt: null,
  createdAt: "2026-01-01T00:00:00Z", reactions: [],
});

describe("upsertMessage", () => {
  it("does not duplicate a message that arrives twice (REST + WebSocket)", () => {
    const list = upsertMessage(upsertMessage([], message("m1")), message("m1"));
    expect(list).toHaveLength(1);
  });

  it("replaces an edited message in place", () => {
    const list = upsertMessage([message("m1"), message("m2")], message("m1", "editado"));
    expect(list.map((m) => m.content)).toEqual(["editado", "hola"]);
  });
});

describe("applyReaction", () => {
  it("adds, counts and removes reactions without mutating the previous state", () => {
    const initial = [message("m1")];

    const afterTwo = applyReaction(
      applyReaction(initial, { messageId: "m1", userId: "a", emoji: "👍" }, true),
      { messageId: "m1", userId: "b", emoji: "👍" },
      true,
    );
    expect(afterTwo[0].reactions).toEqual([{ emoji: "👍", count: 2, userIds: ["a", "b"] }]);
    expect(initial[0].reactions).toEqual([]);

    const afterRemoval = applyReaction(afterTwo, { messageId: "m1", userId: "a", emoji: "👍" }, false);
    expect(afterRemoval[0].reactions[0].count).toBe(1);

    const empty = applyReaction(afterRemoval, { messageId: "m1", userId: "b", emoji: "👍" }, false);
    expect(empty[0].reactions).toEqual([]);
  });

  it("ignores a repeated event from the same user", () => {
    const once = applyReaction([message("m1")], { messageId: "m1", userId: "a", emoji: "🎉" }, true);
    const twice = applyReaction(once, { messageId: "m1", userId: "a", emoji: "🎉" }, true);
    expect(twice[0].reactions[0].count).toBe(1);
  });
});
