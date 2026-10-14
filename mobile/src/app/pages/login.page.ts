import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { IonButton, IonContent, IonHeader, IonInput, IonItem, IonList, IonText, IonTitle, IonToolbar } from '@ionic/angular';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../core/errors';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonList, IonItem, IonInput, IonButton, IonText],
  template: `
    <ion-header>
      <ion-toolbar color="primary"><ion-title>Pulse</ion-title></ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <h2>Inicia sesión</h2>
      @if (error()) {
        <ion-text color="danger"><p>{{ error() }}</p></ion-text>
      }
      <form [formGroup]="form" (ngSubmit)="submit()">
        <ion-list>
          <ion-item>
            <ion-input label="Email" labelPlacement="stacked" type="email" formControlName="email" autocomplete="email" />
          </ion-item>
          <ion-item>
            <ion-input label="Contraseña" labelPlacement="stacked" type="password" formControlName="password" />
          </ion-item>
        </ion-list>
        <ion-button expand="block" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Entrando...' : 'Entrar' }}
        </ion-button>
      </form>
      <p class="ion-text-center"><a routerLink="/register">Crear cuenta</a></p>
    </ion-content>
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
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/tabs/workspaces', { replaceUrl: true });
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
