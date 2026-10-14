import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth.scss',
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="submit()">
      <div class="brand">Pulse</div>
      <p class="subtitle">Elige una contraseña nueva</p>

      @if (done()) {
        <div class="alert alert-success">Contraseña cambiada. Ya puedes iniciar sesión.</div>
      } @else if (!token()) {
        <div class="alert alert-error">El enlace no es válido.</div>
      } @else {
        @if (error()) {
          <div class="alert alert-error">{{ error() }}</div>
        }
        <div class="field">
          <label for="password">Contraseña nueva</label>
          <input id="password" class="input" type="password" formControlName="password" autocomplete="new-password" />
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <span class="field-error">Mínimo 8 caracteres.</span>
          }
        </div>
        <button class="btn btn-block" type="submit" [disabled]="loading()">Guardar contraseña</button>
      }

      <div class="links">
        <a routerLink="/login">Ir al inicio de sesión</a>
      </div>
    </form>
  `,
})
export class ResetPasswordPage {
  private readonly auth = inject(AuthService);

  /** ?token=... from the email link (bound by withComponentInputBinding). */
  readonly token = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
  });
  protected readonly loading = signal(false);
  protected readonly done = signal(false);
  protected readonly error = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.auth.resetPassword(this.token()!, this.form.getRawValue().password).subscribe({
      next: () => this.done.set(true),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
