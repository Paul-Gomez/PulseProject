"use client";

import { Client, type StompSubscription } from "@stomp/stompjs";
import { useEffect, useRef } from "react";
import { tokens } from "./api";
import type { RealtimeEvent } from "./types";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws";

type Listener = (event: RealtimeEvent) => void;

/**
 * A single STOMP connection shared by the whole app. Topics are reference-counted:
 * the first component that needs a topic subscribes, the last one to leave unsubscribes,
 * and after a reconnection every active topic is subscribed again.
 */
class RealtimeClient {
  private client: Client | null = null;
  private topics = new Map<string, { listeners: Set<Listener>; subscription: StompSubscription | null }>();

  connect() {
    if (this.client) return;
    this.client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
      beforeConnect: async () => {
        // Read the token on every (re)connection in case it was refreshed meanwhile.
        this.client!.connectHeaders = { Authorization: `Bearer ${tokens.access ?? ""}` };
      },
      onConnect: () => this.topics.forEach((topic, destination) => (topic.subscription = this.subscribe(destination))),
    });
    this.client.activate();
  }

  disconnect() {
    this.client?.deactivate();
    this.client = null;
  }

  listen(destination: string, listener: Listener): () => void {
    let topic = this.topics.get(destination);
    if (!topic) {
      topic = { listeners: new Set(), subscription: this.client?.connected ? this.subscribe(destination) : null };
      this.topics.set(destination, topic);
    }
    topic.listeners.add(listener);
    return () => {
      topic!.listeners.delete(listener);
      if (topic!.listeners.size === 0) {
        if (this.client?.connected) topic!.subscription?.unsubscribe();
        this.topics.delete(destination);
      }
    };
  }

  sendTyping(channelId: string) {
    if (this.client?.connected) {
      this.client.publish({ destination: `/app/channel.${channelId}/typing`, body: "{}" });
    }
  }

  private subscribe(destination: string): StompSubscription {
    return this.client!.subscribe(destination, (frame) => {
      const event = JSON.parse(frame.body) as RealtimeEvent;
      this.topics.get(destination)?.listeners.forEach((listener) => listener(event));
    });
  }
}

export const realtime = new RealtimeClient();

/** Subscribes to a STOMP topic while the component is mounted. */
export function useTopic(destination: string | null, onEvent: Listener) {
  // Keep the latest callback without resubscribing on every render.
  const handler = useRef(onEvent);
  useEffect(() => {
    handler.current = onEvent;
  });

  useEffect(() => {
    if (!destination) return;
    return realtime.listen(destination, (event) => handler.current(event));
  }, [destination]);
}
