import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationApi } from '../../core/api/misc.api';
import { Notification } from '../../core/models';
import { NotificationState } from '../../core/state/notification-state.service';

const ICONS: Record<string, string> = {
  MENTION: '@',
  DIRECT_MESSAGE: '💬',
  INVITATION: '✉',
  MODERATION: '⚠',
  SYSTEM: 'ℹ',
};

@Component({
  selector: 'app-notifications',
  imports: [DatePipe],
  template: `
    <div class="page">
      <div class="row">
        <h1>Notificaciones</h1>
        <span class="spacer"></span>
        <label class="muted"><input type="checkbox" [checked]="unreadOnly()" (change)="toggleUnread()" /> Solo no leídas</label>
        <button class="btn btn-secondary" (click)="markAll()" [disabled]="state.unreadCount() === 0">Marcar todas como leídas</button>
      </div>

      @if (loading()) {
        @for (i of [1, 2, 3]; track i) {
          <div class="skeleton" style="height: 52px; margin-bottom: 8px"></div>
        }
      } @else {
        @for (notification of notifications(); track notification.id) {
          <button class="card item" [class.unread]="!notification.readAt" (click)="open(notification)">
            <span class="icon">{{ icon(notification) }}</span>
            <span class="text">{{ notification.text }}</span>
            <time class="muted">{{ notification.createdAt | date: 'short' }}</time>
          </button>
        } @empty {
          <div class="empty-state"><div class="icon">🔔</div><p>No tienes notificaciones.</p></div>
        }
        @if (page() < totalPages() - 1) {
          <button class="btn btn-secondary" (click)="loadMore()">Ver más</button>
        }
      }
    </div>
  `,
  styles: `
    .item { width: 100%; display: flex; align-items: center; gap: 0.75rem; padding: 0.8rem 1rem; margin-bottom: 0.5rem;
      text-align: left; font: inherit; color: var(--text); cursor: pointer; }
    .item.unread { border-left: 4px solid var(--primary); font-weight: 600; }
    .icon { width: 28px; text-align: center; font-size: 1.1rem; }
    .text { flex: 1; }
    time { font-size: 0.8rem; font-weight: normal; }
  `,
})
export class NotificationsPage implements OnInit {
  private readonly api = inject(NotificationApi);
  private readonly router = inject(Router);
  protected readonly state = inject(NotificationState);

  protected readonly notifications = signal<Notification[]>([]);
  protected readonly loading = signal(true);
  protected readonly unreadOnly = signal(false);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);

  ngOnInit(): void {
    this.load(0);
  }

  icon(notification: Notification): string {
    return ICONS[notification.type] ?? '•';
  }

  toggleUnread(): void {
    this.unreadOnly.update((value) => !value);
    this.load(0);
  }

  loadMore(): void {
    this.load(this.page() + 1);
  }

  markAll(): void {
    this.api.markAllAsRead().subscribe(() => {
      this.state.allRead();
      this.notifications.update((list) => list.map((n) => ({ ...n, readAt: n.readAt ?? new Date().toISOString() })));
    });
  }

  /** Marks it as read and goes to where it happened (channel or conversation). */
  open(notification: Notification): void {
    if (!notification.readAt) {
      this.api.markAsRead(notification.id).subscribe((updated) => {
        this.state.markedAsRead();
        this.notifications.update((list) => list.map((n) => (n.id === updated.id ? updated : n)));
      });
    }
    if (notification.conversationId) {
      this.router.navigate(['/dm', notification.conversationId]);
    } else if (notification.workspaceId && notification.channelId) {
      this.router.navigate(['/w', notification.workspaceId, 'c', notification.channelId]);
    } else if (notification.workspaceId && notification.type === 'INVITATION') {
      this.router.navigate(['/w', notification.workspaceId]);
    }
  }

  private load(page: number): void {
    this.loading.set(page === 0);
    this.api.list(page, this.unreadOnly()).subscribe((result) => {
      this.notifications.update((list) => (page === 0 ? result.content : [...list, ...result.content]));
      this.page.set(result.page);
      this.totalPages.set(result.totalPages);
      this.loading.set(false);
    });
  }
}
