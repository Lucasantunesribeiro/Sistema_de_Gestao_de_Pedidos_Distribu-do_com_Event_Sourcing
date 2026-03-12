import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { InventoryStatus } from '../models/inventory.model';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/inventory`;

  getInventoryStatus(): Observable<InventoryStatus> {
    return this.http
      .get<InventoryStatus>(`${this.baseUrl}/status`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    let message = 'Failed to load inventory data.';
    if (error && typeof error === 'object' && 'status' in error) {
      const httpError = error as { status: number };
      if (httpError.status === 0) {
        message = 'Cannot connect to the server.';
      }
    }
    console.error('InventoryService error:', error);
    return throwError(() => new Error(message));
  }
}
