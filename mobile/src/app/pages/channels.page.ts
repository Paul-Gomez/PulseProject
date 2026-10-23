import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  IonBackButton, IonButtons, IonContent, IonHeader, IonIcon, IonItem, IonLabel, IonList, IonNote, IonTitle, IonToolbar, ViewWillEnter,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import { lockClosedOutline } from 'ionicons/icons';
import { forkJoin } from 'rxjs';
import { ChannelApi } from '../core/api/channel.api';
import { WorkspaceApi } from '../core/api/workspace.api';
import { Channel, Workspace } from '../core/models';

@Component({
  selector: 'app-channels',
  imports: [RouterLink, IonHeader, IonToolbar, IonButtons, IonBackButton, IonTitle, IonContent, IonList, IonItem, IonLabel, IonNote, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-buttons slot="start"><ion-back-button defaultHref="/tabs/workspaces" /></ion-buttons>
        <ion-title>{{ workspace()?.name ?? 'Canales' }}</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content>
      <ion-list>
        @for (channel of channels(); track channel.id) {
          <ion-item [routerLink]="['/tabs/workspaces', workspaceId(), 'c', channel.id]" detail>
            @if (channel.isPrivate) {
              <ion-icon slot="start" name="lock-closed-outline" aria-label="Canal privado" />
            }
            <ion-label>{{ channel.isPrivate ? '' : '# ' }}{{ channel.name }}</ion-label>
            @if (channel.type === 'ANNOUNCEMENT') {
              <ion-note slot="end">anuncios</ion-note>
            }
          </ion-item>
        } @empty {
          <ion-item><ion-note>Este workspace no tiene canales todavía.</ion-note></ion-item>
        }
      </ion-list>
    </ion-content>
  `,
})
export class ChannelsPage implements ViewWillEnter {
  private readonly workspaceApi = inject(WorkspaceApi);
  private readonly channelApi = inject(ChannelApi);
  readonly workspaceId = input.required<string>();
  protected readonly workspace = signal<Workspace | null>(null);
  protected readonly channels = signal<Channel[]>([]);

  constructor() {
    addIcons({ lockClosedOutline });
  }

  ionViewWillEnter(): void {
    forkJoin({
      workspace: this.workspaceApi.get(this.workspaceId()),
      channels: this.channelApi.list(this.workspaceId()),
    }).subscribe(({ workspace, channels }) => {
      this.workspace.set(workspace);
      this.channels.set(channels);
    });
  }
}
