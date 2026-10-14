import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserApi } from '../../core/api/misc.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { ThemeService } from '../../core/theme.service';
import { AvatarComponent } from '../../shared/avatar.component';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, AvatarComponent],
  template: `
    <div class="page">
      <h1>Perfil y ajustes</h1>

      @if (auth.currentUser(); as user) {
        <div class="card section row">
          <app-avatar [name]="user.displayName" [avatarUrl]="user.avatarUrl" [size]="64" />
          <div>
            <strong>{{ user.displayName }}</strong>
            <div class="muted">&#64;{{ user.username }} · {{ user.email }}</div>
            <div class="muted small">{{ user.emailVerified ? 'Email verificado' : 'Email sin verificar' }}</div>
          </div>
        </div>
      }

      <form class="card section" [formGroup]="profileForm" (ngSubmit)="saveProfile()">
        <h3>Datos del perfil</h3>
        @if (profileMessage()) {
          <div class="alert" [class.alert-success]="profileOk()" [class.alert-error]="!profileOk()">{{ profileMessage() }}</div>
        }
        <div class="field">
          <label for="displayName">Nombre visible</label>
          <input id="displayName" class="input" formControlName="displayName" maxlength="100" />
        </div>
        <div class="field">
          <label for="avatarUrl">URL del avatar (opcional)</label>
          <input id="avatarUrl" class="input" formControlName="avatarUrl" placeholder="https://..." maxlength="500" />
        </div>
        <button class="btn" type="submit" [disabled]="profileForm.invalid">Guardar</button>
      </form>

      <form class="card section" [formGroup]="passwordForm" (ngSubmit)="changePassword()">
        <h3>Cambiar contraseña</h3>
        <p class="muted small">Al cambiarla se cerrará la sesión en todos tus dispositivos.</p>
        @if (passwordMessage()) {
          <div class="alert" [class.alert-success]="passwordOk()" [class.alert-error]="!passwordOk()">{{ passwordMessage() }}</div>
        }
        <div class="field">
          <label for="current">Contraseña actual</label>
          <input id="current" class="input" type="password" formControlName="currentPassword" autocomplete="current-password" />
        </div>
        <div class="field">
          <label for="new">Contraseña nueva (mínimo 8 caracteres)</label>
          <input id="new" class="input" type="password" formControlName="newPassword" autocomplete="new-password" />
        </div>
        <button class="btn" type="submit" [disabled]="passwordForm.invalid">Cambiar contraseña</button>
      </form>

      <div class="card section row">
        <span>Tema oscuro</span>
        <span class="spacer"></span>
        <button class="btn btn-secondary" (click)="theme.toggle()">{{ theme.dark() ? 'Desactivar' : 'Activar' }}</button>
      </div>
    </div>
  `,
  styles: `
    .section { margin-bottom: 1rem; }
    .small { font-size: 0.85rem; }
  `,
})
export class ProfilePage {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  private readonly userApi = inject(UserApi);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly profileForm = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    avatarUrl: ['', Validators.pattern(/^(https?:\/\/.*)?$/)],
  });
  protected readonly passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });
  protected readonly profileMessage = signal<string | null>(null);
  protected readonly profileOk = signal(false);
  protected readonly passwordMessage = signal<string | null>(null);
  protected readonly passwordOk = signal(false);

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (user) {
        this.profileForm.patchValue({ displayName: user.displayName, avatarUrl: user.avatarUrl ?? '' });
      }
    });
  }

  saveProfile(): void {
    const { displayName, avatarUrl } = this.profileForm.getRawValue();
    this.userApi.updateProfile({ displayName, avatarUrl }).subscribe({
      next: (user) => {
        this.auth.setCurrentUser(user);
        this.profileOk.set(true);
        this.profileMessage.set('Perfil guardado.');
      },
      error: (err) => {
        this.profileOk.set(false);
        this.profileMessage.set(errorMessage(err));
      },
    });
  }

  changePassword(): void {
    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.userApi.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.passwordOk.set(true);
        this.passwordMessage.set('Contraseña cambiada. Vuelve a iniciar sesión.');
        setTimeout(() => this.auth.logout(), 1500);
      },
      error: (err) => {
        this.passwordOk.set(false);
        this.passwordMessage.set(errorMessage(err));
      },
    });
  }
}
