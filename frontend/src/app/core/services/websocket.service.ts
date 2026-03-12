import { Injectable, OnDestroy, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { WebSocketOrderEvent } from '../models/order.model';
import { WebSocketInventoryEvent } from '../models/inventory.model';
import { WebSocketPaymentEvent } from '../models/payment.model';
import { NotificationService } from './notification.service';

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private client!: Client;
  private connected = false;
  private readonly notificationService = inject(NotificationService);

  readonly orderEvents$ = new Subject<WebSocketOrderEvent>();
  readonly inventoryEvents$ = new Subject<WebSocketInventoryEvent>();
  readonly paymentEvents$ = new Subject<WebSocketPaymentEvent>();

  connect(): void {
    if (this.connected) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl) as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected = true;
        console.log('WebSocket connected');
        this.subscribeToTopics();
      },
      onDisconnect: () => {
        this.connected = false;
        console.log('WebSocket disconnected');
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      },
    });

    this.client.activate();
  }

  private subscribeToTopics(): void {
    this.client.subscribe('/topic/orders', (message: IMessage) => {
      try {
        const event: WebSocketOrderEvent = JSON.parse(message.body);
        this.orderEvents$.next(event);
        this.notificationService.show(
          `Order ${event.orderId.slice(0, 8)}... — Status: ${event.status}`,
          'info'
        );
      } catch (err) {
        console.error('Failed to parse order event:', err);
      }
    });

    this.client.subscribe('/topic/inventory', (message: IMessage) => {
      try {
        const event: WebSocketInventoryEvent = JSON.parse(message.body);
        this.inventoryEvents$.next(event);
      } catch (err) {
        console.error('Failed to parse inventory event:', err);
      }
    });

    this.client.subscribe('/topic/payments', (message: IMessage) => {
      try {
        const event: WebSocketPaymentEvent = JSON.parse(message.body);
        this.paymentEvents$.next(event);
      } catch (err) {
        console.error('Failed to parse payment event:', err);
      }
    });
  }

  disconnect(): void {
    if (this.client?.active) {
      this.client.deactivate();
      this.connected = false;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.orderEvents$.complete();
    this.inventoryEvents$.complete();
    this.paymentEvents$.complete();
  }
}
