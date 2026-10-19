// Same shapes as the backend DTOs (com.pulse.*.dto).

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  details?: string[];
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export type UserStatus = "ONLINE" | "AWAY" | "DO_NOT_DISTURB" | "OFFLINE";

export interface User {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  emailVerified: boolean;
}

export interface Workspace {
  id: string;
  name: string;
  description: string | null;
}

export interface Member {
  id: string;
  userId: string;
  roleName: "OWNER" | "ADMIN" | "MODERATOR" | "MEMBER" | "GUEST";
  user: { id: string; username: string; displayName: string; avatarUrl: string | null; status: UserStatus };
}

export interface Channel {
  id: string;
  workspaceId: string;
  name: string;
  type: "TEXT" | "ANNOUNCEMENT";
  isPrivate: boolean;
}

export interface ReactionSummary {
  emoji: string;
  count: number;
  userIds: string[];
}

export interface Message {
  id: string;
  channelId: string;
  authorId: string;
  content: string;
  parentMessageId: string | null;
  editedAt: string | null;
  createdAt: string;
  reactions: ReactionSummary[];
}

export interface RealtimeEvent<T = unknown> {
  type: string;
  payload: T;
}
