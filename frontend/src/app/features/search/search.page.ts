import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SearchApi, SearchFilters } from '../../core/api/misc.api';
import { errorMessage } from '../../core/errors';
import { Page, SearchResult } from '../../core/models';
import { WorkspaceState } from '../../core/state/workspace-state.service';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-search',
  imports: [FormsModule, RouterLink, DatePipe, IconComponent],
  template: `
    <div class="page">
      <h1>Buscar mensajes</h1>
      <form class="card filters" (ngSubmit)="search(0)">
        <input class="input" name="q" [(ngModel)]="text" placeholder="Ej: docker" maxlength="100" />
        <select class="input" name="workspace" [(ngModel)]="workspaceId">
          <option value="">Todos mis workspaces</option>
          @for (workspace of state.workspaces(); track workspace.id) {
            <option [value]="workspace.id">{{ workspace.name }}</option>
          }
        </select>
        <input class="input" type="date" name="from" [(ngModel)]="from" title="Desde" />
        <input class="input" type="date" name="to" [(ngModel)]="to" title="Hasta" />
        <button class="btn" type="submit">Buscar</button>
      </form>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }

      @if (results(); as page) {
        <p class="muted">{{ page.totalElements }} resultados</p>
        @for (result of page.content; track result.messageId) {
          <a class="card result" [routerLink]="['/w', result.workspaceId, 'c', result.channelId]">
            <div class="meta">
              <strong>{{ result.authorDisplayName }}</strong>
              <span class="muted">en #{{ result.channelName }} · {{ result.createdAt | date: 'short' }}</span>
            </div>
            <p>{{ result.content }}</p>
          </a>
        } @empty {
          <div class="empty-state"><div class="icon"><app-icon name="search" [size]="40" /></div><p>No hay mensajes que coincidan.</p></div>
        }
        @if (page.totalPages > 1) {
          <div class="row pager">
            <button class="btn btn-secondary" [disabled]="page.page === 0" (click)="search(page.page - 1)">Anterior</button>
            <span class="muted">Página {{ page.page + 1 }} de {{ page.totalPages }}</span>
            <button class="btn btn-secondary" [disabled]="page.page >= page.totalPages - 1" (click)="search(page.page + 1)">Siguiente</button>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .filters { display: grid; grid-template-columns: 2fr 1.5fr 1fr 1fr auto; gap: 0.5rem; margin-bottom: 1rem; }
    @media (max-width: 700px) { .filters { grid-template-columns: 1fr; } }
    .result { display: block; color: inherit; margin-bottom: 0.5rem; padding: 0.8rem 1rem; }
    .result:hover { border-color: var(--primary); text-decoration: none; }
    .result p { margin: 0.3rem 0 0; white-space: pre-wrap; }
    .meta { display: flex; gap: 0.5rem; flex-wrap: wrap; font-size: 0.9rem; }
    .pager { justify-content: center; margin-top: 1rem; }
  `,
})
export class SearchPage {
  private readonly api = inject(SearchApi);
  protected readonly state = inject(WorkspaceState);

  protected text = '';
  protected workspaceId = '';
  protected from = '';
  protected to = '';
  protected readonly results = signal<Page<SearchResult> | null>(null);
  protected readonly error = signal<string | null>(null);

  search(page: number): void {
    const filters: SearchFilters = {
      q: this.text.trim(),
      workspaceId: this.workspaceId,
      // Dates from the picker are local days; the API expects ISO-8601 instants.
      from: this.from ? new Date(this.from + 'T00:00:00').toISOString() : undefined,
      to: this.to ? new Date(this.to + 'T23:59:59').toISOString() : undefined,
      page,
    };
    this.api.messages(filters).subscribe({
      next: (result) => {
        this.results.set(result);
        this.error.set(null);
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
