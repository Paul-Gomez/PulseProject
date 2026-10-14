import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth.scss',
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="submit()">
      <div class="brand">Pulse</div>
      <p class="subtitle">Recuperar contraseña</p>

      @if (sent()) {
        <div class="alert alert-success">
          Si hay una cuenta con ese email, te hemos enviado un enlace para elegir una contraseña nueva.
        </div>
      } @else {
        @if (error()) {
          <div class="alert alert-error">{{ error() }}</div>
        }
        <div class="field">
          <label for="email">Email</label>
          <input id="email" class="input" type="email" formControlName="email" />
        </div>
        <button class="btn btn-block" type="submit" [disabled]="loading() || form.invalid">Enviar enlace</button>
      }

      <div class="links">
        <a routerLink="/login">Volver al inicio de sesión</a>
      </div>
    </form>
  `,
})
export class ForgotPasswordPage {
  private readonly auth = inject(AuthService);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });
  protected readonly loading = signal(false);
  protected readonly sent = signal(false);
  protected readonly error = signal<string | null>(null);

  submit(): void {
    this.loading.set(true);
    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: () => this.sent.set(true),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
