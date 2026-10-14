import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription, forkJoin } from 'rxjs';
import { ChannelApi } from '../api/channel.api';
import { WorkspaceApi } from '../api/workspace.api';
import { Channel, Member, PresencePayload, Workspace } from '../models';
import { RealtimeService } from '../realtime/realtime.service';

/**
 * State of the workspace that is open right now: its channels, members and who is online.
 * The sidebar, the chat and the member list all read from here instead of asking the API separately.
 */
@Injectable({ providedIn: 'root' })
export class WorkspaceState {
  private readonly workspaceApi = inject(WorkspaceApi);
  private readonly channelApi = inject(ChannelApi);
  private readonly realtime = inject(RealtimeService);
  private presenceSubscription: Subscription | null = null;

  readonly workspaces = signal<Workspace[]>([]);
  readonly current = signal<Workspace | null>(null);
  readonly channels = signal<Channel[]>([]);
  readonly members = signal<Member[]>([]);
  readonly onlineUserIds = signal<Set<string>>(new Set());
  readonly loading = signal(false);

  readonly membersById = computed(() => new Map(this.members().map((m) => [m.userId, m])));

  loadWorkspaces(): void {
    this.workspaceApi.list().subscribe((list) => this.workspaces.set(list));
  }

  open(workspaceId: string): void {
    if (this.current()?.id === workspaceId) {
      return;
    }
    this.loading.set(true);
    forkJoin({
      workspace: this.workspaceApi.get(workspaceId),
      channels: this.channelApi.list(workspaceId),
      members: this.workspaceApi.members(workspaceId),
      online: this.workspaceApi.onlineUsers(workspaceId),
    }).subscribe({
      next: ({ workspace, channels, members, online }) => {
        this.current.set(workspace);
        this.channels.set(channels);
        this.members.set(members.content);
        this.onlineUserIds.set(new Set(online.onlineUserIds));
        this.watchPresence(workspaceId);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  close(): void {
    this.presenceSubscription?.unsubscribe();
    this.current.set(null);
    this.channels.set([]);
    this.members.set([]);
  }

  refreshChannels(): void {
    const workspace = this.current();
    if (workspace) {
      this.channelApi.list(workspace.id).subscribe((channels) => this.channels.set(channels));
    }
  }

  refreshMembers(): void {
    const workspace = this.current();
    if (workspace) {
      this.workspaceApi.members(workspace.id).subscribe((page) => this.members.set(page.content));
    }
  }

  displayName(userId: string): string {
    return this.membersById().get(userId)?.user.displayName ?? 'Usuario';
  }

  private watchPresence(workspaceId: string): void {
    this.presenceSubscription?.unsubscribe();
    this.presenceSubscription = this.realtime
      .watch<PresencePayload>(`/topic/workspace.${workspaceId}.presence`)
      .subscribe(({ payload }) => {
        const online = new Set(this.onlineUserIds());
        if (payload.status === 'OFFLINE') {
          online.delete(payload.userId);
        } else {
          online.add(payload.userId);
        }
        this.onlineUserIds.set(online);
      });
  }
}
