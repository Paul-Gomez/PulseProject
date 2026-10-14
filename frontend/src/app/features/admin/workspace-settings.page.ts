import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { WorkspaceApi } from '../../core/api/workspace.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { AuditLog, Ban, Member, RoleName } from '../../core/models';
import { WorkspaceState } from '../../core/state/workspace-state.service';
import { AvatarComponent } from '../../shared/avatar.component';

const RANK: Record<RoleName, number> = { OWNER: 4, ADMIN: 3, MODERATOR: 2, MEMBER: 1, GUEST: 0 };
const ROLES: RoleName[] = ['ADMIN', 'MODERATOR', 'MEMBER', 'GUEST'];

@Component({
  selector: 'app-workspace-settings',
  imports: [FormsModule, DatePipe, AvatarComponent],
  templateUrl: './workspace-settings.page.html',
  styles: `
    .section { margin-bottom: 1.25rem; }
    .inline { display: flex; gap: 0.5rem; }
    .who { display: flex; align-items: center; gap: 0.5rem; }
    select.input { width: auto; padding: 0.3rem 0.5rem; }
    .danger { border-color: var(--danger); }
    .small { font-size: 0.8rem; }
  `,
})
export class WorkspaceSettingsPage {
  private readonly api = inject(WorkspaceApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly state = inject(WorkspaceState);

  readonly workspaceId = input.required<string>();

  protected readonly roles = ROLES;
  protected readonly message = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly bans = signal<Ban[]>([]);
  protected readonly auditLogs = signal<AuditLog[]>([]);
  protected readonly name = signal('');
  protected readonly description = signal('');
  protected readonly inviteEmail = signal('');

  /** These only decide what to show; the backend checks every action again. */
  protected readonly myRole = computed<RoleName>(
    () => this.state.membersById().get(this.auth.currentUser()?.id ?? '')?.roleName ?? 'GUEST',
  );
  protected readonly canAdmin = computed(() => RANK[this.myRole()] >= RANK.ADMIN);
  protected readonly canModerate = computed(() => RANK[this.myRole()] >= RANK.MODERATOR);
  protected readonly isOwner = computed(() => this.myRole() === 'OWNER');

  constructor() {
    effect(() => this.state.open(this.workspaceId()));
    effect(() => {
      const workspace = this.state.current();
      if (workspace) {
        this.name.set(workspace.name);
        this.description.set(workspace.description ?? '');
      }
    });
    effect(() => {
      if (this.state.current()?.id === this.workspaceId() && this.canModerate()) {
        this.loadBans();
      }
      if (this.state.current()?.id === this.workspaceId() && this.canAdmin()) {
        this.api.auditLogs(this.workspaceId()).subscribe((page) => this.auditLogs.set(page.content));
      }
    });
  }

  canManage(member: Member): boolean {
    return member.userId !== this.auth.currentUser()?.id && RANK[this.myRole()] > RANK[member.roleName];
  }

  assignableRoles(): RoleName[] {
    return ROLES.filter((role) => RANK[role] < RANK[this.myRole()]);
  }

  save(): void {
    this.run(this.api.update(this.workspaceId(), { name: this.name(), description: this.description() }), 'Workspace actualizado.', () =>
      this.state.loadWorkspaces(),
    );
  }

  invite(): void {
    this.run(this.api.invite(this.workspaceId(), this.inviteEmail().trim()), 'Persona añadida.', () => {
      this.inviteEmail.set('');
      this.state.refreshMembers();
    });
  }

  changeRole(member: Member, role: string): void {
    this.run(this.api.changeRole(this.workspaceId(), member.userId, role as RoleName), 'Rol cambiado.', () =>
      this.state.refreshMembers(),
    );
  }

  removeMember(member: Member): void {
    if (confirm(`¿Expulsar a ${member.user.displayName}?`)) {
      this.run(this.api.removeMember(this.workspaceId(), member.userId), 'Miembro expulsado.', () =>
        this.state.refreshMembers(),
      );
    }
  }

  ban(member: Member): void {
    const reason = prompt(`¿Por qué baneas a ${member.user.displayName}? (opcional)`);
    if (reason !== null) {
      this.run(this.api.ban(this.workspaceId(), member.userId, reason || null), 'Usuario baneado.', () => {
        this.state.refreshMembers();
        this.loadBans();
      });
    }
  }

  unban(ban: Ban): void {
    this.run(this.api.unban(this.workspaceId(), ban.userId), 'Ban retirado.', () => this.loadBans());
  }

  deleteWorkspace(): void {
    const workspace = this.state.current();
    if (workspace && confirm(`Se borrará "${workspace.name}" con todos sus canales y mensajes. ¿Seguro?`)) {
      this.api.delete(workspace.id).subscribe({
        next: () => {
          this.state.close();
          this.state.loadWorkspaces();
          this.router.navigate(['/']);
        },
        error: (err) => this.error.set(errorMessage(err)),
      });
    }
  }

  private loadBans(): void {
    this.api.bans(this.workspaceId()).subscribe((page) => this.bans.set(page.content));
  }

  private run(request: import('rxjs').Observable<unknown>, success: string, after: () => void): void {
    this.error.set(null);
    this.message.set(null);
    request.subscribe({
      next: () => {
        this.message.set(success);
        after();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
