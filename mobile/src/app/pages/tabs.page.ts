import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { IonBadge, IonIcon, IonLabel, IonTabBar, IonTabButton, IonTabs } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { chatbubbles, grid, notifications, person } from 'ionicons/icons';
import { Subscription } from 'rxjs';
import { NotificationApi } from '../core/api/misc.api';
import { AuthService } from '../core/auth/auth.service';
import { RealtimeService } from '../core/realtime/realtime.service';

@Component({
  selector: 'app-tabs',
  imports: [IonTabs, IonTabBar, IonTabButton, IonIcon, IonLabel, IonBadge],
  template: `
    <ion-tabs>
      <ion-tab-bar slot="bottom">
        <ion-tab-button tab="workspaces" href="/tabs/workspaces">
          <ion-icon name="grid" />
          <ion-label>Workspaces</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="dm" href="/tabs/dm">
          <ion-icon name="chatbubbles" />
          <ion-label>Mensajes</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="notifications" href="/tabs/notifications">
          <ion-icon name="notifications" />
          <ion-label>Avisos</ion-label>
          @if (unread() > 0) {
            <ion-badge color="danger">{{ unread() }}</ion-badge>
          }
        </ion-tab-button>
        <ion-tab-button tab="profile" href="/tabs/profile">
          <ion-icon name="person" />
          <ion-label>Perfil</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  `,
})
export class TabsPage implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly realtime = inject(RealtimeService);
  private readonly notificationApi = inject(NotificationApi);
  private subscription: Subscription | null = null;

  protected readonly unread = signal(0);

  constructor() {
    addIcons({ grid, chatbubbles, notifications, person });
  }

  ngOnInit(): void {
    this.auth.loadCurrentUser().subscribe((user) => {
      this.realtime.connect();
      this.refreshUnread();
      this.subscription = this.realtime.watch(`/topic/user.${user.id}`).subscribe((event) => {
        if (event.type === 'NOTIFICATION_CREATED') {
          this.unread.update((count) => count + 1);
        }
      });
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.realtime.disconnect();
  }

  refreshUnread(): void {
    this.notificationApi.unreadCount().subscribe(({ count }) => this.unread.set(count));
  }
}
