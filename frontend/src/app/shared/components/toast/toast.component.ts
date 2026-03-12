import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationService, Notification } from '../../../core/services/notification.service';
import { trigger, style, animate, transition } from '@angular/animations';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ transform: 'translateX(100%)', opacity: 0 }),
        animate('200ms ease-out', style({ transform: 'translateX(0)', opacity: 1 })),
      ]),
      transition(':leave', [
        animate('150ms ease-in', style({ transform: 'translateX(100%)', opacity: 0 })),
      ]),
    ]),
  ],
  template: `
    <div class="toast-container" aria-live="polite" aria-label="Notifications">
      <div
        *ngFor="let n of notifications; trackBy: trackById"
        class="toast"
        [ngClass]="'toast-' + n.type"
        [@slideIn]
        role="alert"
      >
        <span class="toast-icon">{{ iconFor(n.type) }}</span>
        <span class="toast-message">{{ n.message }}</span>
        <button class="toast-close" (click)="dismiss(n.id)" aria-label="Close notification">
          &times;
        </button>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 1.25rem;
      right: 1.25rem;
      z-index: 10000;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 22rem;
      width: 100%;
    }
    .toast {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      padding: 0.875rem 1rem;
      border-radius: 0.5rem;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      font-size: 0.875rem;
      line-height: 1.4;
    }
    .toast-icon { font-size: 1rem; flex-shrink: 0; }
    .toast-message { flex: 1; }
    .toast-close {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 1.25rem;
      line-height: 1;
      opacity: 0.6;
      padding: 0;
      flex-shrink: 0;
    }
    .toast-close:hover { opacity: 1; }
    .toast-success { background: #d1fae5; color: #065f46; }
    .toast-error   { background: #fee2e2; color: #991b1b; }
    .toast-info    { background: #dbeafe; color: #1e40af; }
    .toast-warning { background: #fef3c7; color: #92400e; }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  private sub!: Subscription;
  private readonly notificationService = inject(NotificationService);
  private readonly DURATION_MS = 5000;

  ngOnInit(): void {
    this.sub = this.notificationService.notifications$.subscribe((n) => {
      this.notifications.push(n);
      setTimeout(() => this.dismiss(n.id), this.DURATION_MS);
    });
  }

  dismiss(id: string): void {
    this.notifications = this.notifications.filter((n) => n.id !== id);
  }

  trackById(_: number, n: Notification): string {
    return n.id;
  }

  iconFor(type: string): string {
    const icons: Record<string, string> = {
      success: '✓',
      error: '✕',
      info: 'i',
      warning: '!',
    };
    return icons[type] ?? 'i';
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
