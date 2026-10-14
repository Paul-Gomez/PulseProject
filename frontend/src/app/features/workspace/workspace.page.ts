import { Component, effect, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { WorkspaceState } from '../../core/state/workspace-state.service';

/** Entering a workspace: jump to its first channel, or explain how to create one. */
@Component({
  selector: 'app-workspace',
  imports: [RouterLink],
  template: `
    <div class="page">
      @if (state.loading()) {
        <div class="skeleton" style="height: 28px; width: 40%"></div>
      } @else if (state.current(); as workspace) {
        <div class="empty-state">
          <div class="icon">#</div>
          <h2>{{ workspace.name }}</h2>
          <p>Este workspace todavía no tiene canales. Crea uno con el botón + de la barra lateral.</p>
          <a class="btn btn-secondary" [routerLink]="['/w', workspace.id, 'settings']">Invitar a gente</a>
        </div>
      }
    </div>
  `,
})
export class WorkspacePage {
  protected readonly state = inject(WorkspaceState);
  private readonly router = inject(Router);
  readonly workspaceId = input.required<string>();

  constructor() {
    effect(() => this.state.open(this.workspaceId()));
    effect(() => {
      const first = this.state.channels()[0];
      if (!this.state.loading() && this.state.current()?.id === this.workspaceId() && first) {
        this.router.navigate(['/w', this.workspaceId(), 'c', first.id], { replaceUrl: true });
      }
    });
  }
}
