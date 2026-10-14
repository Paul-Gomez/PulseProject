import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  IonBadge, IonContent, IonHeader, IonItem, IonLabel, IonList, IonListHeader, IonNote, IonSelect, IonSelectOption, IonTitle,
  IonToolbar, ViewWillEnter,
} from '@ionic/angular';
import { forkJoin, map, switchMap } from 'rxjs';
import { ConversationApi } from '../core/api/conversation.api';
import { WorkspaceApi } from '../core/api/workspace.api';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../core/errors';
import { Conversation, Member } from '../core/models';

@Component({
  selector: 'app-conversations',
  imports: [DatePipe, FormsModule, RouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonList, IonListHeader, IonItem,
    IonLabel, IonNote, IonBadge, IonSelect, IonSelectOption],
  template: `
    <ion-header>
      <ion-toolbar color="primary"><ion-title>Mensajes privados</ion-title></ion-toolbar>
    </ion-header>
    <ion-content>
      <ion-list>
        <ion-item>
          <ion-select label="Nueva conversación con" placeholder="Elige a alguien" interface="action-sheet"
                      (ionChange)="start($event.detail.value)">
            @for (person of people(); track person.userId) {
              <ion-select-option [value]="person.userId">{{ person.user.displayName }}</ion-select-option>
            }
          </ion-select>
        </ion-item>
        @if (error()) {
          <ion-item><ion-note color="danger">{{ error() }}</ion-note></ion-item>
        }
      </ion-list>

      <ion-list>
        <ion-list-header>Conversaciones</ion-list-header>
        @for (conversation of conversations(); track conversation.id) {
          <ion-item [routerLink]="['/tabs/dm', conversation.id]" detail>
            <ion-label>
              <h2>{{ title(conversation) }}</h2>
              <p>{{ conversation.lastActivityAt | date: 'short' }}</p>
            </ion-label>
            @if (conversation.unreadCount > 0) {
              <ion-badge slot="end" color="danger">{{ conversation.unreadCount }}</ion-badge>
            }
          </ion-item>
        } @empty {
          <ion-item><ion-note>Aún no tienes conversaciones.</ion-note></ion-item>
        }
      </ion-list>
    </ion-content>
  `,
})
export class ConversationsPage implements ViewWillEnter {
  private readonly api = inject(ConversationApi);
  private readonly workspaceApi = inject(WorkspaceApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly conversations = signal<Conversation[]>([]);
  /** Everyone you share a workspace with (the backend only allows messaging them). */
  protected readonly people = signal<Member[]>([]);
  protected readonly error = signal<string | null>(null);

  ionViewWillEnter(): void {
    this.api.list().subscribe((page) => this.conversations.set(page.content));
    this.workspaceApi
      .list()
      .pipe(
        switchMap((workspaces) => forkJoin(workspaces.map((w) => this.workspaceApi.members(w.id)))),
        map((pages) => pages.flatMap((page) => page.content)),
      )
      .subscribe((members) => {
        const unique = new Map(members.filter((m) => m.userId !== this.auth.currentUser()?.id).map((m) => [m.userId, m]));
        this.people.set([...unique.values()]);
      });
  }

  title(conversation: Conversation): string {
    if (conversation.name) return conversation.name;
    const names = conversation.memberIds
      .filter((id) => id !== this.auth.currentUser()?.id)
      .map((id) => this.people().find((p) => p.userId === id)?.user.displayName ?? 'Usuario');
    return names.join(', ') || 'Conversación';
  }

  start(userId: string): void {
    if (!userId) return;
    this.api.start([userId]).subscribe({
      next: (conversation) => this.router.navigate(['/tabs/dm', conversation.id]),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
