import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  Order,
  CreateOrderRequest,
  OrderStatistics,
  OrdersPage,
  OrderStatus,
} from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/orders`;

  getOrders(page = 0, size = 10, status?: OrderStatus): Observable<OrdersPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) {
      params = params.set('status', status);
    }
    // Backend returns a plain Order[] array; wrap it into an OrdersPage for the component.
    return this.http
      .get<Order[]>(this.baseUrl, { params })
      .pipe(
        map((orders) => ({
          content: orders,
          totalElements: orders.length,
          totalPages: orders.length > 0 ? 1 : 0,
          size: orders.length,
          number: page,
        })),
        catchError(this.handleError)
      );
  }

  getOrderById(orderId: string): Observable<Order> {
    return this.http
      .get<Order>(`${this.baseUrl}/${orderId}`)
      .pipe(catchError(this.handleError));
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http
      .post<Order>(this.baseUrl, request)
      .pipe(catchError(this.handleError));
  }

  cancelOrder(orderId: string): Observable<Order> {
    return this.http
      .put<Order>(`${this.baseUrl}/${orderId}/cancel`, {})
      .pipe(catchError(this.handleError));
  }

  getStatistics(): Observable<OrderStatistics> {
    return this.http
      .get<OrderStatistics>(`${this.baseUrl}/statistics`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    let message = 'An unexpected error occurred. Please try again.';
    if (error && typeof error === 'object' && 'status' in error) {
      const httpError = error as { status: number; error?: { message?: string } };
      if (httpError.status === 404) {
        message = 'Order not found.';
      } else if (httpError.status === 400) {
        message = httpError.error?.message ?? 'Invalid request data.';
      } else if (httpError.status === 0) {
        message = 'Cannot connect to the server. Please check your connection.';
      }
    }
    console.error('OrderService error:', error);
    return throwError(() => new Error(message));
  }
}
