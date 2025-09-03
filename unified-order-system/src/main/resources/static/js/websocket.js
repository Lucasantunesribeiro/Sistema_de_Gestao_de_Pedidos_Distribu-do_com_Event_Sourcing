// WebSocket Real-time Connection Manager
class WebSocketManager {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000;
    this.subscriptions = new Map();
    this.eventHandlers = new Map();
    this.connectionStatus = 'disconnected';
    this.init();
  }

  init() {
    this.setupConnectionStatusIndicator();
    this.connect();
    this.bindEvents();
  }

  connect() {
    try {
      // Use SockJS for better compatibility
      const socket = new SockJS('/ws');
      this.stompClient = Stomp.over(socket);

      // Disable debug logging in production
      this.stompClient.debug = (str) => {
        if (window.location.hostname === 'localhost') {
          console.log('STOMP: ' + str);
        }
      };

      const connectHeaders = {};

      this.stompClient.connect(connectHeaders,
        (frame) => this.onConnected(frame),
        (error) => this.onError(error)
      );

      this.updateConnectionStatus('connecting');

    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.scheduleReconnect();
    }
  }

  onConnected(frame) {
    console.log('WebSocket connected:', frame);
    this.connected = true;
    this.reconnectAttempts = 0;
    this.updateConnectionStatus('connected');

    // Subscribe to default topics
    this.subscribeToDefaultTopics();

    // Send subscription confirmation
    this.send('/app/subscribe', { clientId: this.generateClientId() });

    // Emit connected event
    this.emit('connected', { frame });
  }

  onError(error) {
    console.error('WebSocket error:', error);
    this.connected = false;
    this.updateConnectionStatus('error');

    // Emit error event
    this.emit('error', { error });

    // Schedule reconnection
    this.scheduleReconnect();
  }

  subscribeToDefaultTopics() {
    // Subscribe to system updates
    this.subscribe('/topic/system', (message) => {
      this.handleSystemUpdate(JSON.parse(message.body));
    });

    // Subscribe to order updates
    this.subscribe('/topic/orders', (message) => {
      this.handleOrderUpdate(JSON.parse(message.body));
    });

    // Subscribe to health updates
    this.subscribe('/topic/health', (message) => {
      this.handleHealthUpdate(JSON.parse(message.body));
    });

    // Subscribe to metrics updates
    this.subscribe('/topic/metrics', (message) => {
      this.handleMetricsUpdate(JSON.parse(message.body));
    });

    // Subscribe to user notifications (if user is logged in)
    const userId = this.getCurrentUserId();
    if (userId) {
      this.subscribe(`/user/${userId}/queue/notifications`, (message) => {
        this.handleUserNotification(JSON.parse(message.body));
      });
    }
  }

  subscribe(destination, callback) {
    if (!this.connected || !this.stompClient) {
      console.warn('Cannot subscribe: WebSocket not connected');
      return null;
    }

    try {
      const subscription = this.stompClient.subscribe(destination, callback);
      this.subscriptions.set(destination, subscription);
      console.log('Subscribed to:', destination);
      return subscription;
    } catch (error) {
      console.error('Subscription error:', error);
      return null;
    }
  }

  unsubscribe(destination) {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log('Unsubscribed from:', destination);
    }
  }

  send(destination, message) {
    if (!this.connected || !this.stompClient) {
      console.warn('Cannot send message: WebSocket not connected');
      return false;
    }

    try {
      this.stompClient.send(destination, {}, JSON.stringify(message));
      return true;
    } catch (error) {
      console.error('Send message error:', error);
      return false;
    }
  }

  handleSystemUpdate(update) {
    console.log('System update received:', update);

    switch (update.type) {
      case 'order_created':
        this.showNotification('New order created: #' + update.data.orderId, 'success');
        this.emit('orderCreated', update.data);
        break;

      case 'order_updated':
        this.showNotification(`Order #${update.data.orderId} ${update.data.status}`, 'info');
        this.emit('orderUpdated', update.data);
        break;

      case 'payment_processed':
        this.showNotification(update.data.message, 'success');
        this.emit('paymentProcessed', update.data);
        break;

      case 'inventory_updated':
        this.emit('inventoryUpdated', update.data);
        break;

      case 'system_alert':
        this.handleSystemAlert(update.data);
        break;

      default:
        this.emit('systemUpdate', update);
    }
  }

  handleOrderUpdate(update) {
    console.log('Order update received:', update);
    this.emit('orderUpdate', update);

    // Update order in UI if order management page is open
    if (window.ordersManager) {
      window.ordersManager.handleRealtimeUpdate(update);
    }
  }

  handleHealthUpdate(update) {
    console.log('Health update received:', update);
    this.emit('healthUpdate', update);

    // Update dashboard health indicators
    if (window.dashboard) {
      window.dashboard.updateHealthStatus(update.data);
    }
  }

  handleMetricsUpdate(update) {
    console.log('Metrics update received:', update);
    this.emit('metricsUpdate', update);

    // Update dashboard metrics
    if (window.dashboard) {
      window.dashboard.updateMetrics(update.data);
    }
  }

  handleUserNotification(notification) {
    console.log('User notification received:', notification);
    this.showNotification(notification.message, notification.type);
    this.emit('userNotification', notification);
  }

  handleSystemAlert(alert) {
    const severity = alert.severity.toLowerCase();
    let type = 'info';

    if (severity === 'warning') type = 'warning';
    else if (severity === 'high' || severity === 'critical') type = 'error';

    this.showNotification(alert.message, type, {
      duration: alert.requiresAction ? 0 : 5000, // Persistent for critical alerts
      title: `System Alert (${alert.severity})`
    });

    this.emit('systemAlert', alert);
  }

  showNotification(message, type = 'info', options = {}) {
    // Use Toast component if available
    if (window.Toast) {
      window.Toast.show(message, type, options);
    } else {
      // Fallback to browser notification
      console.log(`[${type.toUpperCase()}] ${message}`);
    }
  }

  scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      this.updateConnectionStatus('failed');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1); // Exponential backoff

    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
    this.updateConnectionStatus('reconnecting');

    setTimeout(() => {
      if (!this.connected) {
        this.connect();
      }
    }, delay);
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      // Unsubscribe from all topics
      this.subscriptions.forEach((subscription, destination) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      // Disconnect
      this.stompClient.disconnect(() => {
        console.log('WebSocket disconnected');
      });

      this.connected = false;
      this.updateConnectionStatus('disconnected');
    }
  }

  updateConnectionStatus(status) {
    this.connectionStatus = status;

    // Update connection indicator in UI
    const indicator = document.getElementById('connection-status');
    if (indicator) {
      indicator.className = `connection-status ${status}`;
      indicator.title = `Connection: ${status}`;
    }

    // Update connection status in header
    const statusText = document.getElementById('connection-status-text');
    if (statusText) {
      const statusMessages = {
        'connected': 'Connected',
        'connecting': 'Connecting...',
        'reconnecting': 'Reconnecting...',
        'disconnected': 'Disconnected',
        'error': 'Connection Error',
        'failed': 'Connection Failed'
      };
      statusText.textContent = statusMessages[status] || status;
    }

    this.emit('connectionStatusChanged', { status });
  }

  setupConnectionStatusIndicator() {
    // Add connection status indicator to header if it doesn't exist
    const header = document.querySelector('.header-content, .app-header');
    if (header && !document.getElementById('connection-status')) {
      const indicator = document.createElement('div');
      indicator.id = 'connection-status';
      indicator.className = 'connection-status disconnected';
      indicator.innerHTML = `
                <div class="status-dot"></div>
                <span id="connection-status-text">Disconnected</span>
            `;
      header.appendChild(indicator);
    }
  }

  getCurrentUserId() {
    // Get current user ID from session, localStorage, or other source
    return localStorage.getItem('userId') || 'anonymous';
  }

  generateClientId() {
    return 'client_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  // Event system
  on(eventName, callback) {
    if (!this.eventHandlers.has(eventName)) {
      this.eventHandlers.set(eventName, []);
    }
    this.eventHandlers.get(eventName).push(callback);
    return this;
  }

  off(eventName, callback) {
    const handlers = this.eventHandlers.get(eventName);
    if (handlers) {
      const index = handlers.indexOf(callback);
      if (index > -1) {
        handlers.splice(index, 1);
      }
    }
    return this;
  }

  emit(eventName, data) {
    const handlers = this.eventHandlers.get(eventName);
    if (handlers) {
      handlers.forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error(`Error in event handler for ${eventName}:`, error);
        }
      });
    }
  }

  bindEvents() {
    // Handle page visibility changes
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        // Page is hidden, reduce update frequency
        this.emit('pageHidden');
      } else {
        // Page is visible, resume normal updates
        this.emit('pageVisible');

        // Reconnect if disconnected while hidden
        if (!this.connected) {
          this.connect();
        }
      }
    });

    // Handle page unload
    window.addEventListener('beforeunload', () => {
      this.disconnect();
    });
  }

  // Public API methods
  isConnected() {
    return this.connected;
  }

  getConnectionStatus() {
    return this.connectionStatus;
  }

  forceReconnect() {
    this.disconnect();
    setTimeout(() => this.connect(), 1000);
  }
}

// Initialize WebSocket manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  // Load SockJS and STOMP libraries if not already loaded
  if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
    console.log('Loading WebSocket libraries...');

    const sockjsScript = document.createElement('script');
    sockjsScript.src = 'https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js';

    const stompScript = document.createElement('script');
    stompScript.src = 'https://cdn.jsdelivr.net/npm/@stomp/stompjs@6.1.2/bundles/stomp.umd.min.js';

    sockjsScript.onload = () => {
      stompScript.onload = () => {
        window.webSocketManager = new WebSocketManager();
      };
      document.head.appendChild(stompScript);
    };

    document.head.appendChild(sockjsScript);
  } else {
    window.webSocketManager = new WebSocketManager();
  }
});

// Export for use in other modules
window.WebSocketManager = WebSocketManager;