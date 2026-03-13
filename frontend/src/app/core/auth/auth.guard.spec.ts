import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('auth guards', () => {
  let router: Router;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('allows authenticated users to access protected routes', () => {
    authService.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/orders' } as never)
    );

    expect(result).toBeTrue();
  });

  it('redirects guests to login with the requested returnUrl', () => {
    authService.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/orders' } as never)
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/login?returnUrl=%2Forders');
  });

  it('redirects authenticated guests away from the login screen', () => {
    authService.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      guestGuard({} as never, { url: '/login' } as never)
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/dashboard');
  });
});
