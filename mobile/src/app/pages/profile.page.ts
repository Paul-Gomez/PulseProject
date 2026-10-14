import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  IonButton, IonContent, IonHeader, IonInput, IonItem, IonLabel, IonList, IonListHeader, IonNote, IonTitle, IonToolbar,
  ViewWillEnter,
} from '@ionic/angular';
import { UserApi } from '../core/api/misc.api';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../core/errors';

@Component({
  selector: 'app-profile',
  imports: [FormsModule, IonHeader, IonToolbar, IonTitle, IonContent, IonList, IonListHeader, IonItem, IonLabel, IonInput,
    IonButton, IonNote],
  template: `
    <ion-header>
      <ion-toolbar color="primary"><ion-title>Perfil</ion-title></ion-toolbar>
    </ion-header>
    <ion-content>
      @if (auth.currentUser(); as user) {
        <ion-list>
          <ion-item>
            <ion-label>
              <h2>{{ user.displayName }}</h2>
              <p>&#64;{{ user.username }} · {{ user.email }}</p>
              <p>{{ user.emailVerified ? 'Email verificado' : 'Email sin verificar' }}</p>
            </ion-label>
          </ion-item>
        </ion-list>
      }

      <ion-list>
        <ion-list-header>Nombre visible</ion-list-header>
        <ion-item>
          <ion-input [ngModel]="displayName()" (ngModelChange)="displayName.set($event)" maxlength="100" />
        </ion-item>
        @if (message()) {
          <ion-item><ion-note>{{ message() }}</ion-note></ion-item>
        }
      </ion-list>
      <div class="ion-padding">
        <ion-button expand="block" (click)="save()" [disabled]="!displayName().trim()">Guardar</ion-button>
        <ion-button expand="block" color="medium" fill="outline" (click)="auth.logout()">Cerrar sesión</ion-button>
      </div>
    </ion-content>
  `,
})
export class ProfilePage implements ViewWillEnter {
  protected readonly auth = inject(AuthService);
  private readonly userApi = inject(UserApi);
  protected readonly displayName = signal('');
  protected readonly message = signal<string | null>(null);

  ionViewWillEnter(): void {
    this.displayName.set(this.auth.currentUser()?.displayName ?? '');
  }

  save(): void {
    this.userApi.updateProfile({ displayName: this.displayName().trim() }).subscribe({
      next: (user) => {
        this.auth.setCurrentUser(user);
        this.message.set('Perfil guardado.');
      },
      error: (err) => this.message.set(errorMessage(err)),
    });
  }
}
