"use client";

import { useParams } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api } from "./api";
import { useAuth } from "./auth";
import { useTopic } from "./realtime";
import type { Channel, Member, Page, Workspace } from "./types";

interface WorkspaceContextValue {
  workspaces: Workspace[];
  current: Workspace | null;
  channels: Channel[];
  members: Map<string, Member>;
  onlineUserIds: Set<string>;
  unreadNotifications: number;
  loading: boolean;
  reloadWorkspaces(): Promise<void>;
  reloadChannels(): Promise<void>;
  reloadMembers(): Promise<void>;
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null);

/**
 * State shared by the sidebar and the pages: the open workspace (from the URL), its channels,
 * its members and who is online, plus the unread notification counter. Presence and notifications
 * stay up to date through their WebSocket topics.
 */
export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const { workspaceId } = useParams<{ workspaceId?: string }>();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loaded, setLoaded] = useState<Workspace | null>(null);
  const [channels, setChannels] = useState<Channel[]>([]);
  const [members, setMembers] = useState<Map<string, Member>>(new Map());
  const [onlineUserIds, setOnlineUserIds] = useState<Set<string>>(new Set());
  const [unreadNotifications, setUnreadNotifications] = useState(0);

  // Derived instead of stored: the open workspace is whatever the URL says, once its data has arrived.
  const current = workspaceId && loaded?.id === workspaceId ? loaded : null;
  const loading = Boolean(workspaceId) && !current;

  const reloadWorkspaces = useCallback(async () => {
    setWorkspaces(await api<Workspace[]>("/workspaces"));
  }, []);

  const reloadChannels = useCallback(async () => {
    if (workspaceId) setChannels(await api<Channel[]>(`/workspaces/${workspaceId}/channels`));
  }, [workspaceId]);

  const reloadMembers = useCallback(async () => {
    if (!workspaceId) return;
    const page = await api<Page<Member>>(`/workspaces/${workspaceId}/members?size=100`);
    setMembers(new Map(page.content.map((m) => [m.userId, m])));
  }, [workspaceId]);

  useEffect(() => {
    if (!user) return;
    api<Workspace[]>("/workspaces").then(setWorkspaces);
    api<{ count: number }>("/notifications/unread-count").then(({ count }) => setUnreadNotifications(count));
  }, [user]);

  useEffect(() => {
    if (!user || !workspaceId) return;
    let cancelled = false;
    Promise.all([
      api<Workspace>(`/workspaces/${workspaceId}`),
      api<Channel[]>(`/workspaces/${workspaceId}/channels`),
      api<Page<Member>>(`/workspaces/${workspaceId}/members?size=100`),
      api<{ onlineUserIds: string[] }>(`/workspaces/${workspaceId}/presence`),
    ])
      .then(([workspace, channelList, memberPage, presence]) => {
        if (cancelled) return;
        setChannels(channelList);
        setMembers(new Map(memberPage.content.map((m) => [m.userId, m])));
        setOnlineUserIds(new Set(presence.onlineUserIds));
        setLoaded(workspace);
      });
    return () => {
      cancelled = true;
    };
  }, [user, workspaceId]);

  useTopic(workspaceId ? `/topic/workspace.${workspaceId}.presence` : null, ({ payload }) => {
    const { userId, status } = payload as { userId: string; status: string };
    setOnlineUserIds((previous) => {
      const next = new Set(previous);
      if (status === "OFFLINE") next.delete(userId);
      else next.add(userId);
      return next;
    });
  });

  useTopic(user ? `/topic/user.${user.id}` : null, (event) => {
    if (event.type === "NOTIFICATION_CREATED") setUnreadNotifications((count) => count + 1);
  });

  const value = useMemo(
    () => ({
      workspaces, current, channels, members, onlineUserIds, unreadNotifications, loading,
      reloadWorkspaces, reloadChannels, reloadMembers,
    }),
    [workspaces, current, channels, members, onlineUserIds, unreadNotifications, loading,
      reloadWorkspaces, reloadChannels, reloadMembers],
  );

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace(): WorkspaceContextValue {
  const context = useContext(WorkspaceContext);
  if (!context) throw new Error("useWorkspace must be used inside <WorkspaceProvider>");
  return context;
}
