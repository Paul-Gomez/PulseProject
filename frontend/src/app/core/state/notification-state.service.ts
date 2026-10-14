import { inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationApi } from '../api/misc.api';
import { Notification } from '../models';
import { RealtimeService } from '../realtime/realtime.service';

/** Unread counter for the bell, kept up to date with the user's private realtime topic. */
@Injectable({ providedIn: 'root' })
export class NotificationState {
  private readonly api = inject(NotificationApi);
  private readonly realtime = inject(RealtimeService);
  private subscription: Subscription | null = null;

  readonly unreadCount = signal(0);
  readonly latest = signal<Notification | null>(null);

  start(userId: string): void {
    this.api.unreadCount().subscribe(({ count }) => this.unreadCount.set(count));
    this.subscription?.unsubscribe();
    this.subscription = this.realtime.watch<Notification>(`/topic/user.${userId}`).subscribe((event) => {
      if (event.type === 'NOTIFICATION_CREATED') {
        this.unreadCount.update((count) => count + 1);
        this.latest.set(event.payload);
      }
    });
  }

  stop(): void {
    this.subscription?.unsubscribe();
    this.unreadCount.set(0);
  }

  markedAsRead(amount = 1): void {
    this.unreadCount.update((count) => Math.max(0, count - amount));
  }

  allRead(): void {
    this.unreadCount.set(0);
  }
}
