import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  IonButton, IonContent, IonHeader, IonInput, IonItem, IonLabel, IonList, IonListHeader, IonNote, IonRefresher,
  IonRefresherContent, IonTitle, IonToolbar, ViewWillEnter,
} from '@ionic/angular';
import { WorkspaceApi } from '../core/api/workspace.api';
import { errorMessage } from '../core/errors';
import { Workspace } from '../core/models';

@Component({
  selector: 'app-workspaces',
  imports: [FormsModule, RouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonList, IonListHeader, IonItem, IonLabel, IonNote,
    IonInput, IonButton, IonRefresher, IonRefresherContent],
  template: `
    <ion-header>
      <ion-toolbar color="primary"><ion-title>Workspaces</ion-title></ion-toolbar>
    </ion-header>
    <ion-content>
      <ion-refresher slot="fixed" (ionRefresh)="load($event)"><ion-refresher-content /></ion-refresher>
      <ion-list>
        @for (workspace of workspaces(); track workspace.id) {
          <ion-item [routerLink]="['/tabs/workspaces', workspace.id]" detail>
            <ion-label>
              <h2>{{ workspace.name }}</h2>
              <p>{{ workspace.description || 'Sin descripción' }}</p>
            </ion-label>
          </ion-item>
        } @empty {
          <ion-item><ion-note>Todavía no perteneces a ningún workspace.</ion-note></ion-item>
        }
      </ion-list>

      <ion-list>
        <ion-list-header>Crear workspace</ion-list-header>
        <ion-item>
          <ion-input placeholder="Nombre" [ngModel]="newName()" (ngModelChange)="newName.set($event)" maxlength="100" />
        </ion-item>
        @if (error()) {
          <ion-item><ion-note color="danger">{{ error() }}</ion-note></ion-item>
        }
      </ion-list>
      <div class="ion-padding">
        <ion-button expand="block" (click)="create()" [disabled]="!newName().trim()">Crear</ion-button>
      </div>
    </ion-content>
  `,
})
export class WorkspacesPage implements ViewWillEnter {
  private readonly api = inject(WorkspaceApi);
  protected readonly workspaces = signal<Workspace[]>([]);
  protected readonly newName = signal('');
  protected readonly error = signal<string | null>(null);

  ionViewWillEnter(): void {
    this.load();
  }

  load(event?: CustomEvent): void {
    this.api.list().subscribe({
      next: (list) => {
        this.workspaces.set(list);
        (event?.target as HTMLIonRefresherElement | undefined)?.complete();
      },
      error: () => (event?.target as HTMLIonRefresherElement | undefined)?.complete(),
    });
  }

  create(): void {
    this.api.create(this.newName().trim(), null).subscribe({
      next: () => {
        this.newName.set('');
        this.error.set(null);
        this.load();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
