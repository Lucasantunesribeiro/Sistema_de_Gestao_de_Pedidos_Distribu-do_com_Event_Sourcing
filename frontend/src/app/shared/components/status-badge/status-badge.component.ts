import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [ngClass]="badgeClass">
      {{ label }}
    </span>
  `,
  styles: [`
    .badge {
      display: inline-flex;
      align-items: center;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      letter-spacing: 0.025em;
      text-transform: uppercase;
      white-space: nowrap;
    }
    .badge-pending      { background: #fef3c7; color: #92400e; }
    .badge-reserved     { background: #dbeafe; color: #1e40af; }
    .badge-processing   { background: #ede9fe; color: #5b21b6; }
    .badge-confirmed    { background: #d1fae5; color: #065f46; }
    .badge-cancelled    { background: #fee2e2; color: #991b1b; }
    .badge-failed       { background: #fce7f3; color: #9d174d; }
    .badge-default      { background: #f1f5f9; color: #475569; }
  `]
})
export class StatusBadgeComponent {
  @Input() status!: OrderStatus | string;

  get badgeClass(): string {
    const map: Record<string, string> = {
      PENDING: 'badge-pending',
      INVENTORY_RESERVED: 'badge-reserved',
      PAYMENT_PROCESSING: 'badge-processing',
      CONFIRMED: 'badge-confirmed',
      CANCELLED: 'badge-cancelled',
      FAILED: 'badge-failed',
    };
    return map[this.status] ?? 'badge-default';
  }

  get label(): string {
    const map: Record<string, string> = {
      PENDING: 'Pending',
      INVENTORY_RESERVED: 'Reserved',
      PAYMENT_PROCESSING: 'Processing',
      CONFIRMED: 'Confirmed',
      CANCELLED: 'Cancelled',
      FAILED: 'Failed',
    };
    return map[this.status] ?? this.status;
  }
}
