import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { WebSocketService } from './core/services/websocket.service';
import { AuthService } from './core/services/auth.service';
import { ToastComponent } from './shared/components/toast/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ToastComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  private readonly wsService = inject(WebSocketService);
  readonly authService = inject(AuthService);
  sidebarOpen = true;

  ngOnInit(): void {
    this.authService.session$.subscribe((session) => {
      if (session?.accessToken) {
        this.wsService.connect(session.accessToken);
      } else {
        this.wsService.disconnect();
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  logout(): void {
    this.wsService.disconnect();
    this.authService.logout();
  }
}
