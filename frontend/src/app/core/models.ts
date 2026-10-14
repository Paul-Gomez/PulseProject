// Mirrors the backend DTOs (com.pulse.*.dto). Keep in sync when the API changes.

export type UserStatus = 'ONLINE' | 'AWAY' | 'DO_NOT_DISTURB' | 'OFFLINE';
export type RoleName = 'OWNER' | 'ADMIN' | 'MODERATOR' | 'MEMBER' | 'GUEST';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  details: string[];
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface User {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  status: UserStatus;
  emailVerified: boolean;
  lastSeenAt: string | null;
  createdAt: string;
}

export interface UserSummary {
  id: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  status: UserStatus;
  lastSeenAt: string | null;
}

export interface Workspace {
  id: string;
  name: string;
  description: string | null;
  iconUrl: string | null;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface Member {
  id: string;
  userId: string;
  roleName: RoleName;
  joinedAt: string;
  user: UserSummary;
}

export interface Ban {
  userId: string;
  bannedBy: string;
  reason: string | null;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  workspaceId: string;
  actorId: string | null;
  actorUsername: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  details: string | null;
  createdAt: string;
}

export type ChannelType = 'TEXT' | 'ANNOUNCEMENT';

export interface Channel {
  id: string;
  workspaceId: string;
  name: string;
  type: ChannelType;
  isPrivate: boolean;
  createdBy: string;
  position: number;
  archivedAt: string | null;
  createdAt: string;
}

export interface Attachment {
  id: string;
  channelId: string;
  messageId: string | null;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  createdAt: string;
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
  deletedAt: string | null;
  createdAt: string;
  attachments: Attachment[];
  reactions: ReactionSummary[];
}

export interface Conversation {
  id: string;
  isGroup: boolean;
  name: string | null;
  memberIds: string[];
  unreadCount: number;
  createdAt: string;
  lastActivityAt: string;
}

export interface DirectMessage {
  id: string;
  conversationId: string;
  authorId: string;
  content: string;
  parentMessageId: string | null;
  createdAt: string;
}

export type NotificationType = 'MENTION' | 'DIRECT_MESSAGE' | 'INVITATION' | 'SYSTEM' | 'MODERATION';

export interface Notification {
  id: string;
  type: NotificationType;
  actorId: string | null;
  workspaceId: string | null;
  channelId: string | null;
  messageId: string | null;
  conversationId: string | null;
  text: string;
  readAt: string | null;
  createdAt: string;
}

export interface SearchResult {
  messageId: string;
  channelId: string;
  channelName: string;
  workspaceId: string;
  authorId: string;
  authorDisplayName: string;
  content: string;
  createdAt: string;
}

// Events pushed over STOMP: { type, payload }
export interface RealtimeEvent<T = unknown> {
  type: string;
  payload: T;
}

export interface TypingPayload {
  channelId: string;
  userId: string;
  displayName: string;
}

export interface PresencePayload {
  userId: string;
  status: UserStatus;
}

export interface ReactionPayload {
  messageId: string;
  userId: string;
  emoji: string;
}
