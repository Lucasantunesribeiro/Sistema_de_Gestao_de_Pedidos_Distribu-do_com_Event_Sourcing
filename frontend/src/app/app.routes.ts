import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-list/order-list.component').then(
        (m) => m.OrderListComponent
      ),
  },
  {
    path: 'orders/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-create/order-create.component').then(
        (m) => m.OrderCreateComponent
      ),
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-detail/order-detail.component').then(
        (m) => m.OrderDetailComponent
      ),
  },
  {
    path: 'inventory',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/inventory/inventory.component').then(
        (m) => m.InventoryComponent
      ),
  },
  {
    path: '**',
    pathMatch: 'full',
    redirectTo: 'login',
  },
];
