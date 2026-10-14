import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  IonButton, IonButtons, IonContent, IonHeader, IonItem, IonLabel, IonList, IonNote, IonTitle, IonToolbar, ViewWillEnter,
} from '@ionic/angular';
import { NotificationApi } from '../core/api/misc.api';
import { Notification } from '../core/models';
import { TabsPage } from './tabs.page';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton, IonContent, IonList, IonItem, IonLabel, IonNote],
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-title>Notificaciones</ion-title>
        <ion-buttons slot="end"><ion-button (click)="markAll()">Leer todas</ion-button></ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content>
      <ion-list>
        @for (notification of notifications(); track notification.id) {
          <ion-item button (click)="open(notification)" [class.unread]="!notification.readAt">
            <ion-label class="ion-text-wrap">
              <p class="text">{{ notification.text }}</p>
              <p>{{ notification.createdAt | date: 'short' }}</p>
            </ion-label>
          </ion-item>
        } @empty {
          <ion-item><ion-note>No tienes notificaciones.</ion-note></ion-item>
        }
      </ion-list>
    </ion-content>
  `,
  styles: `
    .text { color: var(--ion-text-color); }
    .unread { --border-color: var(--ion-color-primary); font-weight: 600; border-left: 4px solid var(--ion-color-primary); }
  `,
})
export class NotificationsPage implements ViewWillEnter {
  private readonly api = inject(NotificationApi);
  private readonly router = inject(Router);
  private readonly tabs = inject(TabsPage);
  protected readonly notifications = signal<Notification[]>([]);

  ionViewWillEnter(): void {
    this.api.list().subscribe((page) => this.notifications.set(page.content));
  }

  markAll(): void {
    this.api.markAllAsRead().subscribe(() => {
      this.tabs.refreshUnread();
      this.ionViewWillEnter();
    });
  }

  open(notification: Notification): void {
    if (!notification.readAt) {
      this.api.markAsRead(notification.id).subscribe(() => this.tabs.refreshUnread());
    }
    if (notification.conversationId) {
      this.router.navigate(['/tabs/dm', notification.conversationId]);
    } else if (notification.workspaceId && notification.channelId) {
      this.router.navigate(['/tabs/workspaces', notification.workspaceId, 'c', notification.channelId]);
    }
  }
}
