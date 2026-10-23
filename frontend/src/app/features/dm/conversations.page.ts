import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ConversationApi } from '../../core/api/conversation.api';
import { UserApi } from '../../core/api/misc.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { Conversation, UserSummary } from '../../core/models';
import { WorkspaceState } from '../../core/state/workspace-state.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-conversations',
  imports: [RouterLink, DatePipe, AvatarComponent, IconComponent],
  template: `
    <div class="page">
      <h1>Mensajes privados</h1>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }

      @if (state.current(); as workspace) {
        <div class="card start">
          <h3>Nueva conversación</h3>
          <p class="muted">Personas de {{ workspace.name }}:</p>
          <div class="people">
            @for (member of otherMembers(); track member.id) {
              <button class="person" (click)="start(member.userId)">
                <app-avatar [name]="member.user.displayName" [avatarUrl]="member.user.avatarUrl" [size]="28" />
                {{ member.user.displayName }}
              </button>
            } @empty {
              <span class="muted">No hay más miembros en este workspace.</span>
            }
          </div>
        </div>
      } @else {
        <p class="muted">Abre un workspace para empezar una conversación con alguien de allí.</p>
      }

      @if (loading()) {
        <div class="skeleton" style="height: 56px; margin-bottom: 8px"></div>
        <div class="skeleton" style="height: 56px"></div>
      } @else {
        @for (conversation of conversations(); track conversation.id) {
          <a class="card conversation" [routerLink]="['/dm', conversation.id]">
            <app-avatar [name]="title(conversation)" [size]="36" />
            <div class="info">
              <strong>{{ title(conversation) }}</strong>
              <span class="muted small">{{ conversation.lastActivityAt | date: 'short' }}</span>
            </div>
            @if (conversation.unreadCount > 0) {
              <span class="badge">{{ conversation.unreadCount }}</span>
            }
          </a>
        } @empty {
          <div class="empty-state">
            <div class="icon"><app-icon name="message" [size]="40" /></div>
            <p>Aún no tienes conversaciones.</p>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .start { margin-bottom: 1.5rem; }
    .people { display: flex; flex-wrap: wrap; gap: 0.5rem; }
    .person { display: flex; align-items: center; gap: 0.4rem; padding: 0.35rem 0.7rem; border: 1px solid var(--border);
      border-radius: 999px; background: var(--surface); color: var(--text); cursor: pointer; font: inherit; }
    .person:hover { border-color: var(--primary); }
    .conversation { display: flex; align-items: center; gap: 0.75rem; padding: 0.8rem 1rem; margin-bottom: 0.5rem; color: inherit; }
    .conversation:hover { border-color: var(--primary); text-decoration: none; }
    .info { flex: 1; display: flex; flex-direction: column; }
    .small { font-size: 0.8rem; }
  `,
})
export class ConversationsPage implements OnInit {
  private readonly api = inject(ConversationApi);
  private readonly userApi = inject(UserApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly state = inject(WorkspaceState);

  protected readonly conversations = signal<Conversation[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  private readonly names = signal<Map<string, string>>(new Map());

  protected readonly otherMembers = computed(() =>
    this.state.members().filter((m) => m.userId !== this.auth.currentUser()?.id),
  );

  ngOnInit(): void {
    this.api.list().subscribe({
      next: (page) => {
        this.conversations.set(page.content);
        this.loading.set(false);
        this.loadNames(page.content);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  title(conversation: Conversation): string {
    if (conversation.name) {
      return conversation.name;
    }
    const myId = this.auth.currentUser()?.id;
    const others = conversation.memberIds.filter((id) => id !== myId);
    return others.map((id) => this.names().get(id) ?? '...').join(', ') || 'Conversación';
  }

  start(userId: string): void {
    this.api.start([userId]).subscribe({
      next: (conversation) => this.router.navigate(['/dm', conversation.id]),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  /** Conversations only carry user ids, so the names are fetched once per person. */
  private loadNames(conversations: Conversation[]): void {
    const ids = new Set(conversations.flatMap((c) => c.memberIds));
    ids.forEach((id) =>
      this.userApi.get(id).subscribe((user: UserSummary) =>
        this.names.update((names) => new Map(names).set(user.id, user.displayName)),
      ),
    );
  }
}
