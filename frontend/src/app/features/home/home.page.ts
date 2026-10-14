import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserApi } from '../../core/api/misc.api';
import { WorkspaceApi } from '../../core/api/workspace.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { WorkspaceState } from '../../core/state/workspace-state.service';

@Component({
  selector: 'app-home',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="page">
      @if (auth.currentUser(); as user) {
        <h1>Hola, {{ user.displayName }} 👋</h1>
        @if (!user.emailVerified) {
          <div class="alert alert-error row">
            <span>Tu email aún no está verificado.</span>
            <span class="spacer"></span>
            @if (verificationSent()) {
              <span>Enlace enviado.</span>
            } @else {
              <button class="btn btn-secondary" (click)="resendVerification()">Reenviar enlace</button>
            }
          </div>
        }
      }

      <h2>Tus workspaces</h2>
      <div class="grid">
        @for (workspace of state.workspaces(); track workspace.id) {
          <a class="card workspace" [routerLink]="['/w', workspace.id]">
            <div class="ws-icon">{{ workspace.name.charAt(0).toUpperCase() }}</div>
            <div>
              <strong>{{ workspace.name }}</strong>
              <p class="muted">{{ workspace.description || 'Sin descripción' }}</p>
            </div>
          </a>
        } @empty {
          <div class="empty-state">
            <div class="icon">🏠</div>
            <p>Todavía no perteneces a ningún workspace. Crea el primero.</p>
          </div>
        }
      </div>

      <h2>Crear workspace</h2>
      <form class="card" [formGroup]="form" (ngSubmit)="create()">
        @if (error()) {
          <div class="alert alert-error">{{ error() }}</div>
        }
        <div class="field">
          <label for="name">Nombre</label>
          <input id="name" class="input" formControlName="name" placeholder="Gaming Club" maxlength="100" />
        </div>
        <div class="field">
          <label for="description">Descripción (opcional)</label>
          <input id="description" class="input" formControlName="description" maxlength="500" />
        </div>
        <button class="btn" type="submit" [disabled]="form.invalid || creating()">Crear</button>
      </form>
    </div>
  `,
  styles: `
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
    .workspace { display: flex; gap: 0.9rem; align-items: center; color: inherit; padding: 1rem; }
    .workspace:hover { border-color: var(--primary); text-decoration: none; }
    .workspace p { margin: 0.2rem 0 0; font-size: 0.85rem; }
    .ws-icon { width: 44px; height: 44px; border-radius: 12px; background: var(--primary); color: #fff;
      display: flex; align-items: center; justify-content: center; font-weight: 800; flex-shrink: 0; }
    .empty-state { grid-column: 1 / -1; }
  `,
})
export class HomePage implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly state = inject(WorkspaceState);
  private readonly workspaceApi = inject(WorkspaceApi);
  private readonly userApi = inject(UserApi);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: [''],
  });
  protected readonly creating = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verificationSent = signal(false);

  ngOnInit(): void {
    this.state.close();
    this.state.loadWorkspaces();
  }

  create(): void {
    this.creating.set(true);
    const { name, description } = this.form.getRawValue();
    this.workspaceApi.create(name, description || null).subscribe({
      next: (workspace) => {
        this.state.loadWorkspaces();
        this.router.navigate(['/w', workspace.id]);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.creating.set(false);
      },
    });
  }

  resendVerification(): void {
    this.userApi.resendVerification().subscribe(() => this.verificationSent.set(true));
  }
}
