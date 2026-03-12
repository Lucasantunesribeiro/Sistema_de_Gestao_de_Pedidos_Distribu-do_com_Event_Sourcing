import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="spinner-wrapper" [class.overlay]="overlay">
      <div class="spinner" [style.width.px]="size" [style.height.px]="size"></div>
      <p class="spinner-text" *ngIf="message">{{ message }}</p>
    </div>
  `,
  styles: [`
    .spinner-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
    }
    .spinner-wrapper.overlay {
      position: fixed;
      inset: 0;
      background: rgba(255, 255, 255, 0.8);
      z-index: 9999;
    }
    .spinner {
      border: 3px solid #e2e8f0;
      border-top-color: #1e3a5f;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    .spinner-text {
      margin-top: 1rem;
      color: #64748b;
      font-size: 0.875rem;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() size = 40;
  @Input() message = '';
  @Input() overlay = false;
}
