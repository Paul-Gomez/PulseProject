import { Component, effect, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { ChannelApi } from '../../core/api/channel.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { Notification } from '../../core/models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { NotificationState } from '../../core/state/notification-state.service';
import { WorkspaceState } from '../../core/state/workspace-state.service';
import { ThemeService } from '../../core/theme.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule, AvatarComponent, IconComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit, OnDestroy {
  protected readonly auth = inject(AuthService);
  protected readonly state = inject(WorkspaceState);
  protected readonly notifications = inject(NotificationState);
  protected readonly theme = inject(ThemeService);
  protected readonly realtime = inject(RealtimeService);
  private readonly channelApi = inject(ChannelApi);
  private readonly router = inject(Router);

  protected readonly sidebarOpen = signal(false);
  protected readonly creatingChannel = signal(false);
  protected readonly toast = signal<Notification | null>(null);
  protected readonly newChannelName = signal('');
  protected readonly newChannelPrivate = signal(false);
  protected readonly channelError = signal<string | null>(null);

  constructor() {
    // Small toast for notifications that arrive while the app is open.
    effect(() => {
      const latest = this.notifications.latest();
      if (latest) {
        this.toast.set(latest);
        setTimeout(() => this.toast.set(null), 5000);
      }
    });
  }

  ngOnInit(): void {
    this.auth.loadCurrentUser().subscribe((user) => {
      this.realtime.connect();
      this.notifications.start(user.id);
    });
    this.state.loadWorkspaces();
    // On mobile, close the sidebar after choosing where to go.
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => this.sidebarOpen.set(false));
  }

  ngOnDestroy(): void {
    this.notifications.stop();
    this.realtime.disconnect();
  }

  createChannel(): void {
    const workspace = this.state.current();
    const name = this.newChannelName().trim().toLowerCase().replace(/\s+/g, '-');
    if (!workspace || !name) {
      return;
    }
    this.channelApi.create(workspace.id, name, 'TEXT', this.newChannelPrivate()).subscribe({
      next: (channel) => {
        this.creatingChannel.set(false);
        this.newChannelName.set('');
        this.newChannelPrivate.set(false);
        this.channelError.set(null);
        this.state.refreshChannels();
        this.router.navigate(['/w', workspace.id, 'c', channel.id]);
      },
      error: (err) => this.channelError.set(errorMessage(err)),
    });
  }

  logout(): void {
    this.state.close();
    this.auth.logout();
  }
}
