import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth.scss',
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="submit()">
      <div class="brand">Pulse</div>
      <p class="subtitle">Crea tu cuenta</p>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }

      <div class="field">
        <label for="displayName">Nombre</label>
        <input id="displayName" class="input" formControlName="displayName" autocomplete="name" />
        @if (form.controls.displayName.touched && form.controls.displayName.invalid) {
          <span class="field-error">El nombre es obligatorio.</span>
        }
      </div>

      <div class="field">
        <label for="username">Usuario</label>
        <input id="username" class="input" formControlName="username" autocomplete="username" />
        @if (form.controls.username.touched && form.controls.username.invalid) {
          <span class="field-error">Entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo.</span>
        }
      </div>

      <div class="field">
        <label for="email">Email</label>
        <input id="email" class="input" type="email" formControlName="email" autocomplete="email" />
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <span class="field-error">Introduce un email válido.</span>
        }
      </div>

      <div class="field">
        <label for="password">Contraseña</label>
        <input id="password" class="input" type="password" formControlName="password" autocomplete="new-password" />
        @if (form.controls.password.touched && form.controls.password.invalid) {
          <span class="field-error">Mínimo 8 caracteres.</span>
        }
      </div>

      <button class="btn btn-block" type="submit" [disabled]="loading()">
        {{ loading() ? 'Creando cuenta...' : 'Crear cuenta' }}
      </button>

      <div class="links">
        <span class="muted">¿Ya tienes cuenta?</span>
        <a routerLink="/login">Inicia sesión</a>
      </div>
    </form>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // Same rules as the backend's RegisterRequest. The frontend checks them for a nicer UX;
  // the backend checks them again because the frontend is not a security layer.
  protected readonly form = inject(FormBuilder).nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    username: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9_.-]{3,50}$/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
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
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
