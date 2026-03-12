import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

type PageState = 'loading' | 'error' | 'empty' | 'success';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, LoadingSpinnerComponent, StatusBadgeComponent],
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.scss'],
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  state: PageState = 'loading';
  orders: Order[] = [];
  errorMessage = '';

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  // Filter
  selectedStatus: OrderStatus | '' = '';

  readonly statusOptions: Array<{ value: OrderStatus | ''; label: string }> = [
    { value: '', label: 'All Statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'INVENTORY_RESERVED', label: 'Inventory Reserved' },
    { value: 'PAYMENT_PROCESSING', label: 'Payment Processing' },
    { value: 'CONFIRMED', label: 'Confirmed' },
    { value: 'CANCELLED', label: 'Cancelled' },
    { value: 'FAILED', label: 'Failed' },
  ];

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.state = 'loading';
    this.errorMessage = '';
    const status = this.selectedStatus || undefined;
    this.orderService.getOrders(this.currentPage, this.pageSize, status).subscribe({
      next: (page) => {
        this.orders = page.content;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.state = this.orders.length === 0 ? 'empty' : 'success';
      },
      error: (err: Error) => {
        this.errorMessage = err.message;
        this.state = 'error';
      },
    });
  }

  onStatusChange(): void {
    this.currentPage = 0;
    this.loadOrders();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadOrders();
  }

  get pageNumbers(): number[] {
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + 5);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
