// Dashboard JavaScript with Real-time Updates
class Dashboard {
  constructor() {
    this.refreshInterval = null;
    this.realTimeEnabled = true;
    this.lastUpdateTime = null;
    this.animationQueue = [];
    this.isAnimating = false;
    this.metricsHistory = new Map();
    this.init();
  }

  init() {
    this.loadSystemHealth();
    this.loadQuickStats();
    this.loadRecentOrders();
    this.setupRealTimeUpdates();
    this.startAutoRefresh();
    this.bindEvents();
  }

  setupRealTimeUpdates() {
    // Wait for WebSocket manager to be available
    const checkWebSocket = () => {
      if (window.webSocketManager) {
        this.bindWebSocketEvents();
      } else {
        setTimeout(checkWebSocket, 100);
      }
    };
    checkWebSocket();
  }

  bindWebSocketEvents() {
    const wsManager = window.webSocketManager;

    // Listen for health updates
    wsManager.on('healthUpdate', (data) => {
      this.updateHealthStatus(data.data);
    });

    // Listen for metrics updates
    wsManager.on('metricsUpdate', (data) => {
      this.updateMetrics(data.data);
    });

    // Listen for order updates
    wsManager.on('orderUpdate', (data) => {
      this.handleOrderUpdate(data);
    });

    // Listen for system updates
    wsManager.on('systemUpdate', (data) => {
      this.handleSystemUpdate(data);
    });

    // Listen for connection status changes
    wsManager.on('connectionStatusChanged', (data) => {
      this.updateConnectionStatus(data.status);
    });
  }

  bindEvents() {
    // Refresh button
    const refreshBtn = document.querySelector('[onclick="refreshDashboard()"]');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', (e) => {
        e.preventDefault();
        this.refresh();
      });
    }

    // Real-time toggle
    const realtimeToggle = document.getElementById('realtime-toggle');
    if (realtimeToggle) {
      realtimeToggle.addEventListener('change', (e) => {
        this.toggleRealTime(e.target.checked);
      });
    }

    // Auto-refresh toggle
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.stopAutoRefresh();
      } else {
        this.startAutoRefresh();
      }
    });
  }

  async loadSystemHealth() {
    try {
      const response = await fetch('/health');
      const health = await response.json();
      this.updateHealthStatus(health);
    } catch (error) {
      console.error('Failed to load system health:', error);
      this.showError('Failed to load system health');
    }
  }

  updateHealthStatus(health) {
    // Update overall system status banner
    this.updateSystemStatusBanner(health);

    // Update individual service cards
    this.updateServiceCards(health);

    // Store last update time
    this.lastUpdateTime = new Date();
  }

  updateSystemStatusBanner(health) {
    const statusDot = document.getElementById('overall-status-dot');
    const statusText = document.getElementById('system-status-text');
    const statusDetails = document.getElementById('system-status-details');

    if (statusDot && statusText && statusDetails) {
      const isHealthy = health.status === 'UP';

      // Update status dot with animation
      statusDot.className = 'status-dot';
      statusDot.classList.add(isHealthy ? 'status-up' : 'status-down');

      // Update text with smooth transition
      this.animateTextChange(statusText, isHealthy ? 'All Systems Operational' : 'System Issues Detected');

      // Update details
      const healthyServices = Object.values(health.services || {}).filter(s => s.status === 'UP').length;
      const totalServices = Object.keys(health.services || {}).length;

      this.animateTextChange(statusDetails,
        `${healthyServices}/${totalServices} services healthy • Last updated: ${new Date().toLocaleTimeString()}`
      );
    }
  }

  updateServiceCards(health) {
    const services = health.services || {};

    Object.entries(services).forEach(([serviceName, serviceHealth]) => {
      const card = document.querySelector(`[data-service="${serviceName}"]`);
      if (card) {
        this.updateServiceCard(card, serviceHealth, serviceName);
      }
    });
  }

  updateServiceCard(card, serviceHealth, serviceName) {
    const statusDot = card.querySelector('.status-dot');
    const statusText = card.querySelector('.status-text');
    const metricValue = card.querySelector('.metric-value');

    if (statusDot) {
      statusDot.className = 'status-dot';
      statusDot.classList.add(serviceHealth.status === 'UP' ? 'status-up' : 'status-down');
    }

    if (statusText) {
      this.animateTextChange(statusText, serviceHealth.status === 'UP' ? 'Healthy' : 'Unhealthy');
    }

    if (metricValue && serviceHealth.responseTime) {
      this.animateTextChange(metricValue, serviceHealth.responseTime);
    }

    // Add pulse animation for status changes
    card.classList.add('updated');
    setTimeout(() => card.classList.remove('updated'), 1000);
  }

  async loadQuickStats() {
    try {
      const response = await fetch('/api/orders/statistics');
      const stats = await response.json();
      this.updateMetrics(stats);
    } catch (error) {
      console.error('Failed to load statistics:', error);
      this.showError('Failed to load statistics');
    }
  }

  updateMetrics(stats) {
    // Update total orders with animation
    const totalOrdersEl = document.getElementById('total-orders');
    if (totalOrdersEl) {
      this.animateNumber(totalOrdersEl, stats.totalOrders || 0);
    }

    // Update total revenue with animation
    const totalRevenueEl = document.getElementById('total-revenue');
    if (totalRevenueEl) {
      this.animateNumber(totalRevenueEl, stats.totalRevenue || 0, {
        prefix: '$',
        decimals: 2
      });
    }

    // Update pending orders with animation
    const pendingOrdersEl = document.getElementById('pending-orders');
    if (pendingOrdersEl) {
      this.animateNumber(pendingOrdersEl, stats.pendingOrders || 0);
    }

    // Update success rate with animation
    const successRateEl = document.getElementById('success-rate');
    if (successRateEl) {
      this.animateNumber(successRateEl, stats.confirmationRate || 0, {
        suffix: '%',
        decimals: 1
      });
    }

    // Update change indicators with trend analysis
    this.updateChangeIndicators(stats);

    // Store metrics history for trend analysis
    this.storeMetricsHistory(stats);
  }

  storeMetricsHistory(stats) {
    const timestamp = Date.now();
    const key = Math.floor(timestamp / (5 * 60 * 1000)) * (5 * 60 * 1000); // 5-minute intervals

    this.metricsHistory.set(key, {
      ...stats,
      timestamp
    });

    // Keep only last 24 hours of data
    const cutoff = timestamp - (24 * 60 * 60 * 1000);
    for (const [key, value] of this.metricsHistory.entries()) {
      if (value.timestamp < cutoff) {
        this.metricsHistory.delete(key);
      }
    }
  }

  updateChangeIndicators(stats) {
    // Calculate trends from history
    const trends = this.calculateTrends(stats);

    this.updateChangeIndicator('orders-change', trends.orders);
    this.updateChangeIndicator('revenue-change', trends.revenue);
    this.updateChangeIndicator('pending-change', trends.pending);
    this.updateChangeIndicator('success-change', trends.success);
  }

  calculateTrends(currentStats) {
    const historyArray = Array.from(this.metricsHistory.values()).sort((a, b) => a.timestamp - b.timestamp);

    if (historyArray.length < 2) {
      return {
        orders: { value: 0, trend: 'neutral' },
        revenue: { value: 0, trend: 'neutral' },
        pending: { value: 0, trend: 'neutral' },
        success: { value: 0, trend: 'neutral' }
      };
    }

    const previous = historyArray[historyArray.length - 2];
    const current = currentStats;

    return {
      orders: this.calculateTrend(previous.totalOrders, current.totalOrders),
      revenue: this.calculateTrend(previous.totalRevenue, current.totalRevenue),
      pending: this.calculateTrend(previous.pendingOrders, current.pendingOrders, true), // Inverted for pending
      success: this.calculateTrend(previous.confirmationRate, current.confirmationRate)
    };
  }

  calculateTrend(oldValue, newValue, inverted = false) {
    if (!oldValue || !newValue) return { value: 0, trend: 'neutral' };

    const change = ((newValue - oldValue) / oldValue) * 100;
    const absChange = Math.abs(change);

    let trend = 'neutral';
    if (absChange > 0.1) { // Minimum 0.1% change to show trend
      if (inverted) {
        trend = change > 0 ? 'negative' : 'positive';
      } else {
        trend = change > 0 ? 'positive' : 'negative';
      }
    }

    return {
      value: change,
      trend: trend
    };
  }

  updateChangeIndicator(elementId, trendData) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const { value, trend } = trendData;
    const sign = value >= 0 ? '+' : '';
    const displayValue = `${sign}${value.toFixed(1)}%`;

    element.className = `stat-change ${trend}`;
    this.animateTextChange(element, displayValue);
  }

  async loadRecentOrders() {
    try {
      const response = await fetch('/api/orders?limit=5&sort=createdAt,desc');
      const orders = await response.json();
      this.updateRecentOrders(orders);
    } catch (error) {
      console.error('Failed to load recent orders:', error);
      this.showError('Failed to load recent orders');
    }
  }

  updateRecentOrders(orders) {
    const container = document.getElementById('recent-orders-list');
    if (!container) return;

    if (!orders || orders.length === 0) {
      container.innerHTML = `
                <div class="loading-state">
                    <div class="text-center py-8 text-secondary-500">
                        <div class="text-4xl mb-2">📦</div>
                        <div class="text-sm">No recent orders</div>
                    </div>
                </div>
            `;
      return;
    }

    const ordersHtml = orders.map(order => `
            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors order-item" data-order-id="${order.orderId}">
                <div class="flex items-center gap-3">
                    <div class="status-dot ${this.getStatusClass(order.status)}"></div>
                    <div>
                        <div class="font-medium text-sm">#${order.orderId}</div>
                        <div class="text-xs text-secondary">${order.customerName}</div>
                    </div>
                </div>
                <div class="text-right">
                    <div class="font-semibold text-sm">$${order.totalAmount.toFixed(2)}</div>
                    <div class="text-xs text-secondary">${this.formatTime(order.createdAt)}</div>
                </div>
            </div>
        `).join('');

    // Animate the update
    container.style.opacity = '0.7';
    setTimeout(() => {
      container.innerHTML = ordersHtml;
      container.style.opacity = '1';
    }, 150);
  }

  handleOrderUpdate(orderData) {
    // Update recent orders if this order is in the list
    const orderElement = document.querySelector(`[data-order-id="${orderData.orderId}"]`);
    if (orderElement) {
      const statusDot = orderElement.querySelector('.status-dot');
      if (statusDot) {
        statusDot.className = `status-dot ${this.getStatusClass(orderData.status)}`;
      }

      // Add update animation
      orderElement.classList.add('updated');
      setTimeout(() => orderElement.classList.remove('updated'), 1000);
    }

    // Refresh metrics to reflect the change
    this.loadQuickStats();
  }

  handleSystemUpdate(updateData) {
    switch (updateData.type) {
      case 'order_created':
        this.showRealtimeNotification('New order created', 'success');
        this.loadRecentOrders();
        break;

      case 'payment_processed':
        this.showRealtimeNotification('Payment processed', 'success');
        break;

      case 'inventory_updated':
        this.showRealtimeNotification('Inventory updated', 'info');
        break;

      case 'system_alert':
        this.handleSystemAlert(updateData.data);
        break;
    }
  }

  handleSystemAlert(alertData) {
    const severity = alertData.severity.toLowerCase();
    let type = 'info';

    if (severity === 'warning') type = 'warning';
    else if (severity === 'high' || severity === 'critical') type = 'error';

    this.showRealtimeNotification(alertData.message, type, {
      persistent: alertData.requiresAction
    });
  }

  showRealtimeNotification(message, type = 'info', options = {}) {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `realtime-notification ${type}`;
    notification.innerHTML = `
            <div class="notification-content">
                <i class="fas fa-${this.getNotificationIcon(type)}"></i>
                <span>${message}</span>
                <small>${new Date().toLocaleTimeString()}</small>
            </div>
        `;

    // Add to notification area
    const container = this.getNotificationContainer();
    container.appendChild(notification);

    // Auto-remove unless persistent
    if (!options.persistent) {
      setTimeout(() => {
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
      }, 3000);
    }
  }

  getNotificationContainer() {
    let container = document.getElementById('realtime-notifications');
    if (!container) {
      container = document.createElement('div');
      container.id = 'realtime-notifications';
      container.className = 'realtime-notifications';
      document.body.appendChild(container);
    }
    return container;
  }

  getNotificationIcon(type) {
    const icons = {
      success: 'check-circle',
      warning: 'exclamation-triangle',
      error: 'times-circle',
      info: 'info-circle'
    };
    return icons[type] || 'info-circle';
  }

  updateConnectionStatus(status) {
    const indicator = document.getElementById('connection-status');
    if (indicator) {
      indicator.className = `connection-status ${status}`;

      // Show notification for connection changes
      if (status === 'connected') {
        this.showRealtimeNotification('Real-time updates connected', 'success');
      } else if (status === 'disconnected') {
        this.showRealtimeNotification('Real-time updates disconnected', 'warning');
      }
    }
  }

  getStatusClass(status) {
    const statusMap = {
      'PENDING': 'status-warning',
      'CONFIRMED': 'status-up',
      'PROCESSING': 'status-loading',
      'SHIPPED': 'status-up',
      'DELIVERED': 'status-up',
      'CANCELLED': 'status-down'
    };
    return statusMap[status] || 'status-loading';
  }

  animateNumber(element, targetValue, options = {}) {
    const startValue = parseFloat(element.textContent.replace(/[^0-9.-]/g, '')) || 0;
    const duration = 1000;
    const startTime = performance.now();

    const animate = (currentTime) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Easing function for smooth animation
      const easeOutQuart = 1 - Math.pow(1 - progress, 4);

      const currentValue = startValue + (targetValue - startValue) * easeOutQuart;

      let displayValue = currentValue;
      if (options.decimals !== undefined) {
        displayValue = currentValue.toFixed(options.decimals);
      } else {
        displayValue = Math.floor(currentValue);
      }

      const prefix = options.prefix || '';
      const suffix = options.suffix || '';

      element.textContent = `${prefix}${displayValue.toLocaleString()}${suffix}`;

      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };

    requestAnimationFrame(animate);
  }

  animateTextChange(element, newText) {
    if (element.textContent === newText) return;

    element.style.opacity = '0.7';
    setTimeout(() => {
      element.textContent = newText;
      element.style.opacity = '1';
    }, 150);
  }

  formatTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  }

  toggleRealTime(enabled) {
    this.realTimeEnabled = enabled;

    if (enabled) {
      this.showRealtimeNotification('Real-time updates enabled', 'success');
    } else {
      this.showRealtimeNotification('Real-time updates disabled', 'info');
    }
  }

  refresh() {
    this.showLoading();
    Promise.all([
      this.loadSystemHealth(),
      this.loadQuickStats(),
      this.loadRecentOrders()
    ]).finally(() => {
      this.hideLoading();
    });
  }

  startAutoRefresh() {
    this.stopAutoRefresh();
    // Reduced frequency since we have real-time updates
    this.refreshInterval = setInterval(() => {
      if (!this.realTimeEnabled) {
        this.refresh();
      }
    }, 60000); // Refresh every minute as fallback
  }

  stopAutoRefresh() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  showLoading() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
      loadingOverlay.classList.remove('hidden');
    }
  }

  hideLoading() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
      loadingOverlay.classList.add('hidden');
    }
  }

  showError(message) {
    this.showRealtimeNotification(message, 'error');
  }
}

// Global functions for backward compatibility
function refreshDashboard() {
  if (window.dashboard) {
    window.dashboard.refresh();
  }
}

function toggleRealTime(enabled) {
  if (window.dashboard) {
    window.dashboard.toggleRealTime(enabled);
  }
}

function initializeDashboard() {
  window.dashboard = new Dashboard();
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', initializeDashboard);