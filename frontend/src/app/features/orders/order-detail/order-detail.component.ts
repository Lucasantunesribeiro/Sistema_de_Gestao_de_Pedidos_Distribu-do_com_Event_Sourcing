import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Order } from '../../../core/models/order.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

type PageState = 'loading' | 'error' | 'success';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, LoadingSpinnerComponent, StatusBadgeComponent],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly notificationService = inject(NotificationService);

  state: PageState = 'loading';
  order: Order | null = null;
  errorMessage = '';
  cancelLoading = false;
  showCancelConfirm = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Invalid order ID.';
      this.state = 'error';
      return;
    }
    this.loadOrder(id);
  }

  loadOrder(id: string): void {
    this.state = 'loading';
    this.orderService.getOrderById(id).subscribe({
      next: (order) => {
        this.order = order;
        this.state = 'success';
      },
      error: (err: Error) => {
        this.errorMessage = err.message;
        this.state = 'error';
      },
    });
  }

  requestCancel(): void {
    this.showCancelConfirm = true;
  }

  confirmCancel(): void {
    if (!this.order) return;
    this.cancelLoading = true;
    this.showCancelConfirm = false;
    this.orderService.cancelOrder(this.order.orderId).subscribe({
      next: (updated) => {
        this.order = updated;
        this.cancelLoading = false;
        this.notificationService.success('Order cancelled successfully.');
      },
      error: (err: Error) => {
        this.cancelLoading = false;
        this.notificationService.error(`Failed to cancel order: ${err.message}`);
      },
    });
  }

  dismissCancel(): void {
    this.showCancelConfirm = false;
  }

  get canCancel(): boolean {
    return (
      !!this.order &&
      !['CANCELLED', 'FAILED', 'CONFIRMED'].includes(this.order.status)
    );
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('pt-BR');
  }
}
