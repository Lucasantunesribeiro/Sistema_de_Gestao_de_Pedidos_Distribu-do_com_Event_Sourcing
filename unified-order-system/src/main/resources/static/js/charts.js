// Interactive Charts and Data Visualizations
class ChartManager {
  constructor() {
    this.charts = new Map();
    this.chartConfigs = new Map();
    this.realTimeEnabled = true;
    this.updateInterval = null;
    this.init();
  }

  init() {
    this.loadChartLibrary().then(() => {
      this.setupDefaultConfigs();
      this.createDashboardCharts();
      this.bindEvents();
    });
  }

  async loadChartLibrary() {
    return new Promise((resolve) => {
      if (typeof Chart !== 'undefined') {
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js';
      script.onload = () => {
        // Register Chart.js components
        Chart.register(...Chart.registerables);
        resolve();
      };
      document.head.appendChild(script);
    });
  }

  setupDefaultConfigs() {
    // Default chart configuration
    Chart.defaults.font.family = 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    Chart.defaults.color = '#6b7280';
    Chart.defaults.borderColor = '#e5e7eb';
    Chart.defaults.backgroundColor = 'rgba(59, 130, 246, 0.1)';

    // Responsive defaults
    Chart.defaults.responsive = true;
    Chart.defaults.maintainAspectRatio = false;
    Chart.defaults.interaction.intersect = false;
    Chart.defaults.interaction.mode = 'index';

    // Animation defaults
    Chart.defaults.animation.duration = 750;
    Chart.defaults.animation.easing = 'easeOutQuart';
  }

  createDashboardCharts() {
    // Orders trend chart
    this.createOrdersTrendChart();

    // Revenue chart
    this.createRevenueChart();

    // Payment methods distribution
    this.createPaymentMethodsChart();

    // Service performance chart
    this.createServicePerformanceChart();

    // Order status distribution
    this.createOrderStatusChart();
  }

  createOrdersTrendChart() {
    const canvas = document.getElementById('orders-trend-chart');
    if (!canvas) return;

    const config = {
      type: 'line',
      data: {
        labels: this.generateTimeLabels(30), // Last 30 days
        datasets: [{
          label: 'Orders',
          data: this.generateOrdersData(30),
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4,
          pointBackgroundColor: '#3b82f6',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6
        }, {
          label: 'Revenue',
          data: this.generateRevenueData(30),
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4,
          pointBackgroundColor: '#10b981',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
          yAxisID: 'y1'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Orders & Revenue Trend (Last 30 Days)',
            font: { size: 16, weight: 'bold' },
            color: '#1f2937'
          },
          legend: {
            position: 'top',
            labels: {
              usePointStyle: true,
              padding: 20
            }
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#3b82f6',
            borderWidth: 1,
            cornerRadius: 8,
            displayColors: true,
            callbacks: {
              label: (context) => {
                const label = context.dataset.label;
                const value = context.parsed.y;
                if (label === 'Revenue') {
                  return `${label}: $${value.toLocaleString()}`;
                }
                return `${label}: ${value}`;
              }
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: false
            },
            ticks: {
              maxTicksLimit: 7
            }
          },
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            title: {
              display: true,
              text: 'Orders'
            },
            grid: {
              color: 'rgba(0, 0, 0, 0.05)'
            }
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            title: {
              display: true,
              text: 'Revenue ($)'
            },
            grid: {
              drawOnChartArea: false
            },
            ticks: {
              callback: (value) => '$' + value.toLocaleString()
            }
          }
        },
        interaction: {
          intersect: false,
          mode: 'index'
        },
        animation: {
          onComplete: () => {
            canvas.parentElement.classList.remove('chart-updating');
          }
        }
      }
    };

    const chart = new Chart(canvas, config);
    this.charts.set('orders-trend', chart);
    this.chartConfigs.set('orders-trend', config);
  }

  createRevenueChart() {
    const canvas = document.getElementById('revenue-chart');
    if (!canvas) return;

    const config = {
      type: 'bar',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [{
          label: 'Monthly Revenue',
          data: [65000, 59000, 80000, 81000, 56000, 75000],
          backgroundColor: [
            'rgba(59, 130, 246, 0.8)',
            'rgba(16, 185, 129, 0.8)',
            'rgba(245, 158, 11, 0.8)',
            'rgba(239, 68, 68, 0.8)',
            'rgba(139, 92, 246, 0.8)',
            'rgba(236, 72, 153, 0.8)'
          ],
          borderColor: [
            '#3b82f6',
            '#10b981',
            '#f59e0b',
            '#ef4444',
            '#8b5cf6',
            '#ec4899'
          ],
          borderWidth: 2,
          borderRadius: 8,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Monthly Revenue',
            font: { size: 16, weight: 'bold' },
            color: '#1f2937'
          },
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#3b82f6',
            borderWidth: 1,
            cornerRadius: 8,
            callbacks: {
              label: (context) => {
                return `Revenue: $${context.parsed.y.toLocaleString()}`;
              }
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: false
            }
          },
          y: {
            beginAtZero: true,
            grid: {
              color: 'rgba(0, 0, 0, 0.05)'
            },
            ticks: {
              callback: (value) => '$' + (value / 1000) + 'K'
            }
          }
        }
      }
    };

    const chart = new Chart(canvas, config);
    this.charts.set('revenue', chart);
    this.chartConfigs.set('revenue', config);
  }

  createPaymentMethodsChart() {
    const canvas = document.getElementById('payment-methods-chart');
    if (!canvas) return;

    const config = {
      type: 'doughnut',
      data: {
        labels: ['Credit Card', 'PIX', 'Bank Transfer', 'Debit Card', 'Other'],
        datasets: [{
          data: [45, 25, 15, 10, 5],
          backgroundColor: [
            '#3b82f6',
            '#10b981',
            '#f59e0b',
            '#ef4444',
            '#8b5cf6'
          ],
          borderColor: '#ffffff',
          borderWidth: 3,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Payment Methods Distribution',
            font: { size: 16, weight: 'bold' },
            color: '#1f2937'
          },
          legend: {
            position: 'bottom',
            labels: {
              padding: 20,
              usePointStyle: true
            }
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#3b82f6',
            borderWidth: 1,
            cornerRadius: 8,
            callbacks: {
              label: (context) => {
                const label = context.label;
                const value = context.parsed;
                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                const percentage = ((value / total) * 100).toFixed(1);
                return `${label}: ${percentage}%`;
              }
            }
          }
        },
        cutout: '60%',
        animation: {
          animateRotate: true,
          animateScale: true
        }
      }
    };

    const chart = new Chart(canvas, config);
    this.charts.set('payment-methods', chart);
    this.chartConfigs.set('payment-methods', config);
  }

  createServicePerformanceChart() {
    const canvas = document.getElementById('service-performance-chart');
    if (!canvas) return;

    const config = {
      type: 'radar',
      data: {
        labels: ['Response Time', 'Availability', 'Throughput', 'Error Rate', 'CPU Usage', 'Memory Usage'],
        datasets: [{
          label: 'Current Performance',
          data: [85, 98, 92, 95, 78, 82],
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.2)',
          borderWidth: 2,
          pointBackgroundColor: '#3b82f6',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4
        }, {
          label: 'Target Performance',
          data: [90, 99, 95, 98, 85, 85],
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          borderWidth: 2,
          borderDash: [5, 5],
          pointBackgroundColor: '#10b981',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Service Performance Metrics',
            font: { size: 16, weight: 'bold' },
            color: '#1f2937'
          },
          legend: {
            position: 'top',
            labels: {
              usePointStyle: true,
              padding: 20
            }
          }
        },
        scales: {
          r: {
            beginAtZero: true,
            max: 100,
            grid: {
              color: 'rgba(0, 0, 0, 0.1)'
            },
            angleLines: {
              color: 'rgba(0, 0, 0, 0.1)'
            },
            pointLabels: {
              font: {
                size: 12
              }
            },
            ticks: {
              stepSize: 20,
              callback: (value) => value + '%'
            }
          }
        }
      }
    };

    const chart = new Chart(canvas, config);
    this.charts.set('service-performance', chart);
    this.chartConfigs.set('service-performance', config);
  }

  createOrderStatusChart() {
    const canvas = document.getElementById('order-status-chart');
    if (!canvas) return;

    const config = {
      type: 'polarArea',
      data: {
        labels: ['Pending', 'Confirmed', 'Processing', 'Shipped', 'Delivered', 'Cancelled'],
        datasets: [{
          data: [23, 156, 45, 78, 234, 12],
          backgroundColor: [
            'rgba(245, 158, 11, 0.8)',
            'rgba(59, 130, 246, 0.8)',
            'rgba(139, 92, 246, 0.8)',
            'rgba(16, 185, 129, 0.8)',
            'rgba(34, 197, 94, 0.8)',
            'rgba(239, 68, 68, 0.8)'
          ],
          borderColor: [
            '#f59e0b',
            '#3b82f6',
            '#8b5cf6',
            '#10b981',
            '#22c55e',
            '#ef4444'
          ],
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Order Status Distribution',
            font: { size: 16, weight: 'bold' },
            color: '#1f2937'
          },
          legend: {
            position: 'bottom',
            labels: {
              padding: 15,
              usePointStyle: true
            }
          }
        },
        scales: {
          r: {
            beginAtZero: true,
            grid: {
              color: 'rgba(0, 0, 0, 0.1)'
            },
            angleLines: {
              color: 'rgba(0, 0, 0, 0.1)'
            }
          }
        }
      }
    };

    const chart = new Chart(canvas, config);
    this.charts.set('order-status', chart);
    this.chartConfigs.set('order-status', config);
  }

  generateTimeLabels(days) {
    const labels = [];
    const now = new Date();

    for (let i = days - 1; i >= 0; i--) {
      const date = new Date(now);
      date.setDate(date.getDate() - i);
      labels.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    }

    return labels;
  }

  generateOrdersData(days) {
    const data = [];
    for (let i = 0; i < days; i++) {
      // Generate realistic order data with some randomness
      const baseValue = 20 + Math.sin(i * 0.2) * 10;
      const randomVariation = (Math.random() - 0.5) * 10;
      data.push(Math.max(0, Math.round(baseValue + randomVariation)));
    }
    return data;
  }

  generateRevenueData(days) {
    const data = [];
    for (let i = 0; i < days; i++) {
      // Generate realistic revenue data correlated with orders
      const baseValue = 1500 + Math.sin(i * 0.2) * 500;
      const randomVariation = (Math.random() - 0.5) * 300;
      data.push(Math.max(0, Math.round(baseValue + randomVariation)));
    }
    return data;
  }

  updateChart(chartId, newData) {
    const chart = this.charts.get(chartId);
    if (!chart) return;

    // Add updating animation
    chart.canvas.parentElement.classList.add('chart-updating');

    // Update data
    if (newData.labels) {
      chart.data.labels = newData.labels;
    }

    if (newData.datasets) {
      newData.datasets.forEach((dataset, index) => {
        if (chart.data.datasets[index]) {
          Object.assign(chart.data.datasets[index], dataset);
        }
      });
    }

    // Animate update
    chart.update('active');
  }

  updateOrdersTrend(ordersData, revenueData) {
    this.updateChart('orders-trend', {
      datasets: [
        { data: ordersData },
        { data: revenueData }
      ]
    });
  }

  updatePaymentMethods(paymentData) {
    this.updateChart('payment-methods', {
      datasets: [{ data: paymentData }]
    });
  }

  updateServicePerformance(performanceData) {
    this.updateChart('service-performance', {
      datasets: [{ data: performanceData }]
    });
  }

  updateOrderStatus(statusData) {
    this.updateChart('order-status', {
      datasets: [{ data: statusData }]
    });
  }

  bindEvents() {
    // Listen for real-time updates
    if (window.webSocketManager) {
      window.webSocketManager.on('metricsUpdate', (data) => {
        this.handleRealtimeUpdate(data.data);
      });
    }

    // Handle window resize
    window.addEventListener('resize', () => {
      this.charts.forEach(chart => {
        chart.resize();
      });
    });

    // Handle theme changes
    document.addEventListener('themeChanged', (e) => {
      this.updateChartsTheme(e.detail.theme);
    });
  }

  handleRealtimeUpdate(metricsData) {
    if (!this.realTimeEnabled) return;

    // Update charts with new data
    if (metricsData.ordersToday && metricsData.revenueToday) {
      // Update trend chart with latest data point
      const ordersChart = this.charts.get('orders-trend');
      if (ordersChart) {
        const ordersDataset = ordersChart.data.datasets[0];
        const revenueDataset = ordersChart.data.datasets[1];

        // Shift data and add new point
        ordersDataset.data.shift();
        ordersDataset.data.push(metricsData.ordersToday);

        revenueDataset.data.shift();
        revenueDataset.data.push(metricsData.revenueToday);

        // Update labels
        ordersChart.data.labels.shift();
        ordersChart.data.labels.push(new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));

        ordersChart.update('none'); // No animation for real-time updates
      }
    }
  }

  updateChartsTheme(theme) {
    const textColor = theme === 'dark' ? '#f3f4f6' : '#1f2937';
    const gridColor = theme === 'dark' ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)';

    this.charts.forEach(chart => {
      // Update text colors
      if (chart.options.plugins.title) {
        chart.options.plugins.title.color = textColor;
      }

      // Update grid colors
      if (chart.options.scales) {
        Object.values(chart.options.scales).forEach(scale => {
          if (scale.grid) {
            scale.grid.color = gridColor;
          }
        });
      }

      chart.update('none');
    });
  }

  enableRealTime() {
    this.realTimeEnabled = true;
  }

  disableRealTime() {
    this.realTimeEnabled = false;
  }

  exportChart(chartId, format = 'png') {
    const chart = this.charts.get(chartId);
    if (!chart) return null;

    return chart.toBase64Image(format, 1.0);
  }

  destroyChart(chartId) {
    const chart = this.charts.get(chartId);
    if (chart) {
      chart.destroy();
      this.charts.delete(chartId);
      this.chartConfigs.delete(chartId);
    }
  }

  destroyAllCharts() {
    this.charts.forEach(chart => chart.destroy());
    this.charts.clear();
    this.chartConfigs.clear();
  }
}

// Initialize chart manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  window.chartManager = new ChartManager();
});

// Export for use in other modules
window.ChartManager = ChartManager;