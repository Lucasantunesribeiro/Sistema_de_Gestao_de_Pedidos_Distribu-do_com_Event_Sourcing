import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthUser, LoginRequest, LoginResponse } from '../models/auth.model';

interface StoredSession {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: AuthUser;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storageKey = 'orderflow.auth.session';
  private readonly sessionSubject = new BehaviorSubject<StoredSession | null>(this.readSession());

  readonly session$ = this.sessionSubject.asObservable();
  readonly user$ = this.session$.pipe(map((session) => session?.user ?? null));

  login(payload: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/api/auth/login`, payload)
      .pipe(
        tap((response) => this.persistSession(response)),
        map((response) => response.user)
      );
  }

  logout(redirectTo = '/login'): void {
    localStorage.removeItem(this.storageKey);
    this.sessionSubject.next(null);
    void this.router.navigate([redirectTo]);
  }

  isAuthenticated(): boolean {
    const session = this.sessionSubject.value;
    if (!session) {
      return false;
    }

    const expiresAt = Date.parse(session.expiresAt);
    if (Number.isNaN(expiresAt) || expiresAt <= Date.now()) {
      this.clearExpiredSession();
      return false;
    }
    return true;
  }

  getAccessToken(): string | null {
    return this.isAuthenticated() ? this.sessionSubject.value?.accessToken ?? null : null;
  }

  getCurrentUser(): AuthUser | null {
    return this.sessionSubject.value?.user ?? null;
  }

  handleUnauthorized(): void {
    if (this.sessionSubject.value) {
      this.logout('/login');
    }
  }

  private persistSession(response: LoginResponse): void {
    const session: StoredSession = {
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      expiresAt: response.expiresAt,
      user: response.user,
    };
    localStorage.setItem(this.storageKey, JSON.stringify(session));
    this.sessionSubject.next(session);
  }

  private readSession(): StoredSession | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as StoredSession;
      if (!parsed.accessToken || !parsed.expiresAt || !parsed.user) {
        localStorage.removeItem(this.storageKey);
        return null;
      }
      return parsed;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private clearExpiredSession(): void {
    localStorage.removeItem(this.storageKey);
    this.sessionSubject.next(null);
  }
}
