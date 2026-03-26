import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// sockjs-client references the Node.js `global` variable. Angular's esbuild
// application builder (unlike the legacy webpack builder) does not polyfill it,
// so we shim it before any module that touches SockJS is evaluated.
(window as any)['global'] = window;

bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error('Application bootstrap failed:', err)
);
