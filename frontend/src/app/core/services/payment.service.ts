import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Payment } from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/payments`;

  getPaymentByOrderId(orderId: string): Observable<Payment> {
    return this.http
      .get<Payment>(`${this.baseUrl}/order/${orderId}`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    let message = 'Failed to load payment data.';
    if (error && typeof error === 'object' && 'status' in error) {
      const httpError = error as { status: number };
      if (httpError.status === 404) {
        message = 'Payment record not found.';
      } else if (httpError.status === 0) {
        message = 'Cannot connect to the server.';
      }
    }
    console.error('PaymentService error:', error);
    return throwError(() => new Error(message));
  }
}
