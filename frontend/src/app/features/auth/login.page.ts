import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth.scss',
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="submit()">
      <div class="brand">Pulse</div>
      <p class="subtitle">Inicia sesión para continuar</p>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }

      <div class="field">
        <label for="email">Email</label>
        <input id="email" class="input" type="email" formControlName="email" autocomplete="email" />
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <span class="field-error">Introduce un email válido.</span>
        }
      </div>

      <div class="field">
        <label for="password">Contraseña</label>
        <input id="password" class="input" type="password" formControlName="password" autocomplete="current-password" />
        @if (form.controls.password.touched && form.controls.password.invalid) {
          <span class="field-error">La contraseña es obligatoria.</span>
        }
      </div>

      <button class="btn btn-block" type="submit" [disabled]="loading()">
        {{ loading() ? 'Entrando...' : 'Entrar' }}
      </button>

      <div class="links">
        <a routerLink="/register">Crear cuenta</a>
        <a routerLink="/forgot-password">¿Olvidaste la contraseña?</a>
      </div>
    </form>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
