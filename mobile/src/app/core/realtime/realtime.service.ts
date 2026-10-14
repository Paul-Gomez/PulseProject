import { inject, Injectable, OnDestroy, signal } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, filter, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { RealtimeEvent } from '../models';

/**
 * One STOMP connection for the whole app. Components ask for a topic with watch() and get an Observable;
 * if the connection drops, the client reconnects by itself and every active topic is subscribed again.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly connected$ = new BehaviorSubject<boolean>(false);
  private client: Client | null = null;

  readonly connected = signal(false);

  connect(): void {
    if (this.client?.active) {
      return;
    }
    this.client = new Client({
      brokerURL: environment.wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // Read the token on every (re)connection, so a refreshed token is used after a reconnect.
      beforeConnect: async () => {
        this.client!.connectHeaders = { Authorization: `Bearer ${this.auth.accessToken ?? ''}` };
      },
      onConnect: () => this.setConnected(true),
      onWebSocketClose: () => this.setConnected(false),
      onStompError: () => this.setConnected(false),
    });
    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.setConnected(false);
  }

  watch<T>(destination: string): Observable<RealtimeEvent<T>> {
    return new Observable<RealtimeEvent<T>>((subscriber) => {
      let stompSubscription: StompSubscription | null = null;
      const connection = this.connected$.pipe(filter(Boolean)).subscribe(() => {
        stompSubscription = this.client!.subscribe(destination, (frame) => {
          subscriber.next(JSON.parse(frame.body) as RealtimeEvent<T>);
        });
      });
      return () => {
        connection.unsubscribe();
        if (this.client?.connected) {
          stompSubscription?.unsubscribe();
        }
      };
    });
  }

  sendTyping(channelId: string): void {
    if (this.client?.connected) {
      this.client.publish({ destination: `/app/channel.${channelId}/typing`, body: '{}' });
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  private setConnected(value: boolean): void {
    this.connected.set(value);
    this.connected$.next(value);
  }
}
