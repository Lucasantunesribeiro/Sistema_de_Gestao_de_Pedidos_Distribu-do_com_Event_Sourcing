import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const storageKey = 'orderflow.auth.session';
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    localStorage.clear();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('persists the authenticated session after login', () => {
    const service = TestBed.inject(AuthService);
    let authenticatedUser: string | undefined;

    service.login({ username: 'admin', password: 'change-this-admin-password' }).subscribe((user) => {
      authenticatedUser = user.username;
    });

    const request = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(request.request.method).toBe('POST');
    request.flush({
      accessToken: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      user: {
        userId: 'user-1',
        username: 'admin',
        email: 'admin@example.com',
        roles: ['ROLE_ADMIN'],
      },
    });

    expect(authenticatedUser).toBe('admin');
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.getAccessToken()).toBe('jwt-token');
    expect(JSON.parse(localStorage.getItem(storageKey) ?? '{}').accessToken).toBe('jwt-token');
  });

  it('clears expired persisted sessions on first access', () => {
    localStorage.setItem(storageKey, JSON.stringify({
      accessToken: 'expired-token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() - 60_000).toISOString(),
      user: {
        userId: 'user-2',
        username: 'expired',
        email: 'expired@example.com',
        roles: ['ROLE_ADMIN'],
      },
    }));

    const service = TestBed.inject(AuthService);

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.getAccessToken()).toBeNull();
    expect(localStorage.getItem(storageKey)).toBeNull();
  });
});
