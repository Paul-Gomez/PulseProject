import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  IonBackButton, IonButton, IonButtons, IonContent, IonHeader, IonInput, IonItem, IonList, IonNote, IonText, IonTitle, IonToolbar,
} from '@ionic/angular';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../core/errors';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, IonHeader, IonToolbar, IonTitle, IonButtons, IonBackButton, IonContent, IonList, IonItem,
    IonInput, IonButton, IonText, IonNote],
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-buttons slot="start"><ion-back-button defaultHref="/login" /></ion-buttons>
        <ion-title>Crear cuenta</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      @if (error()) {
        <ion-text color="danger"><p>{{ error() }}</p></ion-text>
      }
      <form [formGroup]="form" (ngSubmit)="submit()">
        <ion-list>
          <ion-item><ion-input label="Nombre" labelPlacement="stacked" formControlName="displayName" /></ion-item>
          <ion-item><ion-input label="Usuario" labelPlacement="stacked" formControlName="username" /></ion-item>
          <ion-item><ion-input label="Email" labelPlacement="stacked" type="email" formControlName="email" /></ion-item>
          <ion-item><ion-input label="Contraseña" labelPlacement="stacked" type="password" formControlName="password" /></ion-item>
        </ion-list>
        <ion-note>Usuario: 3-50 letras, números, punto o guion. Contraseña: mínimo 8 caracteres.</ion-note>
        <ion-button expand="block" type="submit" [disabled]="form.invalid || loading()">Crear cuenta</ion-button>
      </form>
    </ion-content>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    displayName: ['', Validators.required],
    username: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9_.-]{3,50}$/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  submit(): void {
    this.loading.set(true);
    this.auth.register(this.form.getRawValue()).subscribe({
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
