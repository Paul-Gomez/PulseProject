import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  styleUrl: './auth.scss',
  template: `
    <div class="card">
      <div class="brand">Pulse</div>
      <p class="subtitle">Verificación de email</p>
      @switch (state()) {
        @case ('loading') { <p class="muted">Verificando...</p> }
        @case ('ok') { <div class="alert alert-success">¡Email verificado! Gracias.</div> }
        @case ('error') { <div class="alert alert-error">{{ error() }}</div> }
      }
      <div class="links"><a routerLink="/">Ir a Pulse</a></div>
    </div>
  `,
})
export class VerifyEmailPage implements OnInit {
  private readonly auth = inject(AuthService);
  readonly token = input<string>();
  protected readonly state = signal<'loading' | 'ok' | 'error'>('loading');
  protected readonly error = signal('');

  ngOnInit(): void {
    const token = this.token();
    if (!token) {
      this.error.set('El enlace no es válido.');
      this.state.set('error');
      return;
    }
    this.auth.verifyEmail(token).subscribe({
      next: () => this.state.set('ok'),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.state.set('error');
      },
    });
  }
}
