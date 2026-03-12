import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { OrderService } from '../../core/services/order.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { OrderStatistics, WebSocketOrderEvent } from '../../core/models/order.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

type PageState = 'loading' | 'error' | 'success';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LoadingSpinnerComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly orderService = inject(OrderService);
  private readonly wsService = inject(WebSocketService);
  private wsSub!: Subscription;

  state: PageState = 'loading';
  stats: OrderStatistics | null = null;
  errorMessage = '';
  recentEvents: WebSocketOrderEvent[] = [];
  lastUpdated: Date | null = null;

  ngOnInit(): void {
    this.loadStats();
    this.wsSub = this.wsService.orderEvents$.subscribe((event) => {
      this.recentEvents = [event, ...this.recentEvents].slice(0, 5);
      this.loadStats(); // Refresh stats on new event
    });
  }

  loadStats(): void {
    this.state = 'loading';
    this.errorMessage = '';
    this.orderService.getStatistics().subscribe({
      next: (data) => {
        this.stats = data;
        this.state = 'success';
        this.lastUpdated = new Date();
      },
      error: (err: Error) => {
        this.errorMessage = err.message;
        this.state = 'error';
      },
    });
  }

  get confirmationRate(): number {
    if (!this.stats || this.stats.totalOrders === 0) return 0;
    return Math.round((this.stats.confirmedOrders / this.stats.totalOrders) * 100);
  }

  get cancellationRate(): number {
    if (!this.stats || this.stats.totalOrders === 0) return 0;
    return Math.round((this.stats.cancelledOrders / this.stats.totalOrders) * 100);
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }
}
