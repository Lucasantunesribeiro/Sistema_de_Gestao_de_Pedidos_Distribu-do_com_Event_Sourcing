import { Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { WebSocketService } from './core/services/websocket.service';
import { AuthService } from './core/services/auth.service';
import { ToastComponent } from './shared/components/toast/toast.component';

const MOBILE_QUERY = '(max-width: 768px)';

interface NavItem {
  path: string;
  label: string;
  /** SVG path (24x24, stroke) — consistent rendering instead of emoji glyphs. */
  icon: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ToastComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  private readonly wsService = inject(WebSocketService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly authService = inject(AuthService);
  readonly wsConnected$ = this.wsService.connected$;

  readonly navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: 'M4 4h7v7H4zM13 4h7v4h-7zM13 10h7v10h-7zM4 13h7v7H4z' },
    { path: '/orders', label: 'Orders', icon: 'M8 4h8M9 2h6v4H9zM6 4H5v18h14V4h-1M8 11h8M8 15h8M8 19h5' },
    { path: '/inventory', label: 'Inventory', icon: 'M3 7l9-4 9 4-9 4zM3 7v10l9 4 9-4V7M12 11v10' },
  ];

  isMobile = this.matchesMobile();
  sidebarOpen = !this.isMobile;

  ngOnInit(): void {
    this.authService.session$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((session) => {
      if (session?.accessToken) {
        this.wsService.connect(session.accessToken);
      } else {
        this.wsService.disconnect();
      }
    });

    // On small screens the sidebar is an overlay drawer: close it after navigating.
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.isMobile) {
          this.sidebarOpen = false;
        }
      });

    if (typeof window !== 'undefined' && window.matchMedia) {
      const query = window.matchMedia(MOBILE_QUERY);
      const onChange = (event: MediaQueryListEvent) => {
        this.isMobile = event.matches;
        this.sidebarOpen = !event.matches;
      };
      query.addEventListener('change', onChange);
      this.destroyRef.onDestroy(() => query.removeEventListener('change', onChange));
    }
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isMobile && this.sidebarOpen) {
      this.closeSidebar();
    }
  }

  logout(): void {
    this.wsService.disconnect();
    this.authService.logout();
  }

  private matchesMobile(): boolean {
    return typeof window !== 'undefined' && !!window.matchMedia && window.matchMedia(MOBILE_QUERY).matches;
  }
}
