// Orders Management JavaScript
class OrdersManager {
  constructor() {
    this.currentPage = 1;
    this.pageSize = 10;
    this.totalOrders = 0;
    this.selectedOrders = new Set();
    this.filters = {};
    this.init();
  }

  init() {
    this.bindEvents();
    this.loadOrders();
  }

  bindEvents() {
    // Search form
    const searchForm = document.getElementById('order-filters');
    if (searchForm) {
      searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.applyFilters();
      });
    }

    // Real-time search
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
      let searchTimeout;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
          this.filters.search = e.target.value;
          this.loadOrders();
        }, 500);
      });
    }

    // Status filter
    const statusFilter = document.getElementById('status-filter');
    if (statusFilter) {
      statusFilter.addEventListener('change', (e) => {
        this.filters.status = e.target.value;
        this.loadOrders();
      });
    }

    // Date range filter
    const dateRange = document.getElementById('date-range');
    if (dateRange) {
      dateRange.addEventListener('change', (e) => {
        this.filters.dateRange = e.target.value;
        this.loadOrders();
      });
    }
  }

  async loadOrders() {
    try {
      this.showLoading();

      const params = new URLSearchParams({
        page: this.currentPage - 1,
        size: this.pageSize,
        ...this.filters
      });

      const response = await fetch(`/api/orders?${params}`);
      if (!response.ok) {
        throw new Error('Failed to load orders');
      }

      const data = await response.json();
      this.displayOrders(data.content || data);
      this.updatePagination(data);
      this.updateOrdersCount(data.totalElements || data.length);

    } catch (error) {
      console.error('Error loading orders:', error);
      this.showError('Failed to load orders');
      this.showEmptyState();
    } finally {
      this.hideLoading();
    }
  }

  displayOrders(orders) {
    const tbody = document.getElementById('orders-tbody');
    const ordersTable = document.getElementById('orders-table');
    const emptyState = document.getElementById('empty-state');

    if (!orders || orders.length === 0) {
      this.showEmptyState();
      return;
    }

    // Hide empty state and show table
    emptyState.classList.add('hidden');
    ordersTable.classList.remove('hidden');

    tbody.innerHTML = orders.map(order => this.createOrderRow(order)).join('');
  }

  createOrderRow(order) {
    const statusClass = this.getStatusClass(order.status);
    const statusIcon = this.getStatusIcon(order.status);
    const amount = typeof order.totalAmount === 'number' ? order.totalAmount.toFixed(2) : order.totalAmount;
    const shortId = order.orderId.length > 8 ? order.orderId.substring(0, 8) + '...' : order.orderId;

    return `
            <tr>
                <td>
                    <button class="link" onclick="ordersManager.viewOrder('${order.orderId}')">
                        #${shortId}
                    </button>
                </td>
                <td>${order.customerName || 'N/A'}</td>
                <td>
                    <span class="badge badge-${statusClass}">
                        <i class="${statusIcon}"></i>
                        ${order.status}
                    </span>
                </td>
                <td class="font-semibold">$${amount}</td>
                <td>${this.formatDate(order.createdAt)}</td>
                <td>
                    <button class="btn btn-ghost btn-sm"
                            onclick="ordersManager.viewOrder('${order.orderId}')"
                            title="View Details">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            </tr>
        `;
  }

  getStatusClass(status) {
    const statusMap = {
      'PENDING': 'warning',
      'CONFIRMED': 'info',
      'PROCESSING': 'info',
      'SHIPPED': 'success',
      'DELIVERED': 'success',
      'CANCELLED': 'danger'
    };
    return statusMap[status] || 'secondary';
  }

  getStatusIcon(status) {
    const iconMap = {
      'PENDING': 'fas fa-clock',
      'CONFIRMED': 'fas fa-check',
      'PROCESSING': 'fas fa-cog fa-spin',
      'SHIPPED': 'fas fa-truck',
      'DELIVERED': 'fas fa-check-circle',
      'CANCELLED': 'fas fa-times'
    };
    return iconMap[status] || 'fas fa-question';
  }

  async viewOrder(orderId) {
    try {
      const response = await fetch(`/api/orders/${orderId}`);
      if (!response.ok) {
        throw new Error('Failed to load order details');
      }

      const order = await response.json();
      this.showOrderModal(order);
    } catch (error) {
      console.error('Error loading order:', error);
      this.showError('Failed to load order details');
    }
  }

  showOrderModal(order) {
    const modal = document.getElementById('order-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalBody = document.getElementById('modal-body');

    modalTitle.textContent = `Order #${order.orderId}`;
    modalBody.innerHTML = this.createOrderDetailsHTML(order);
    modal.classList.remove('hidden');
  }

  createOrderDetailsHTML(order) {
    return `
            <div class="space-y-6">
                <!-- Order Info -->
                <div>
                    <h4 class="font-semibold mb-3">Order Information</h4>
                    <div class="grid grid-cols-2 gap-4 text-sm">
                        <div>
                            <span class="text-secondary">Status:</span>
                            <span class="badge badge-${this.getStatusClass(order.status)} ml-2">
                                ${order.status}
                            </span>
                        </div>
                        <div>
                            <span class="text-secondary">Total:</span>
                            <span class="font-semibold ml-2">$${order.totalAmount.toFixed(2)}</span>
                        </div>
                        <div>
                            <span class="text-secondary">Created:</span>
                            <span class="ml-2">${this.formatDateTime(order.createdAt)}</span>
                        </div>
                        <div>
                            <span class="text-secondary">Payment:</span>
                            <span class="ml-2">${order.paymentMethod || 'N/A'}</span>
                        </div>
                    </div>
                </div>

                <!-- Customer Info -->
                <div>
                    <h4 class="font-semibold mb-3">Customer Information</h4>
                    <div class="text-sm space-y-1">
                        <div><strong>Name:</strong> ${order.customerName}</div>
                        <div><strong>Email:</strong> ${order.customerEmail || 'N/A'}</div>
                        <div><strong>ID:</strong> ${order.customerId}</div>
                    </div>
                </div>

                <!-- Order Items -->
                <div>
                    <h4 class="font-semibold mb-3">Order Items</h4>
                    <div class="space-y-2">
                        ${order.items ? order.items.map(item => `
                            <div class="flex justify-between items-center p-3 bg-gray-50 rounded">
                                <div>
                                    <div class="font-medium">${item.productName}</div>
                                    <div class="text-xs text-secondary">Qty: ${item.quantity} × $${item.unitPrice.toFixed(2)}</div>
                                </div>
                                <div class="font-semibold">$${item.totalPrice.toFixed(2)}</div>
                            </div>
                        `).join('') : '<p class="text-secondary">No items found</p>'}
                    </div>
                </div>
            </div>
        `;
  }

  closeOrderModal() {
    const modal = document.getElementById('order-modal');
    modal.classList.add('hidden');
  }

  toggleOrderSelection(orderId) {
    if (this.selectedOrders.has(orderId)) {
      this.selectedOrders.delete(orderId);
    } else {
      this.selectedOrders.add(orderId);
    }
    this.updateBulkActionsVisibility();
  }

  toggleSelectAll() {
    const selectAllCheckbox = document.getElementById('select-all');
    const orderCheckboxes = document.querySelectorAll('.order-checkbox');

    if (selectAllCheckbox.checked) {
      orderCheckboxes.forEach(checkbox => {
        checkbox.checked = true;
        this.selectedOrders.add(checkbox.value);
      });
    } else {
      orderCheckboxes.forEach(checkbox => {
        checkbox.checked = false;
        this.selectedOrders.delete(checkbox.value);
      });
    }
    this.updateBulkActionsVisibility();
  }

  updateBulkActionsVisibility() {
    const bulkActionsBtn = document.querySelector('[onclick="toggleBulkActions()"]');
    if (bulkActionsBtn) {
      if (this.selectedOrders.size > 0) {
        bulkActionsBtn.classList.remove('btn-outline');
        bulkActionsBtn.classList.add('btn-primary');
        bulkActionsBtn.innerHTML = `<i class="fas fa-check-square"></i> ${this.selectedOrders.size} Selected`;
      } else {
        bulkActionsBtn.classList.add('btn-outline');
        bulkActionsBtn.classList.remove('btn-primary');
        bulkActionsBtn.innerHTML = '<i class="fas fa-check-square"></i> Bulk Actions';
      }
    }
  }

  applyFilters() {
    const formData = new FormData(document.getElementById('order-filters'));
    this.filters = Object.fromEntries(formData.entries());
    this.currentPage = 1;
    this.loadOrders();
  }

  clearFilters() {
    document.getElementById('order-filters').reset();
    this.filters = {};
    this.currentPage = 1;
    this.loadOrders();
  }

  updatePagination(data) {
    const paginationControls = document.getElementById('pagination-controls');
    if (!paginationControls) return;

    const totalPages = data.totalPages || Math.ceil(data.length / this.pageSize);
    const currentPage = data.number + 1 || this.currentPage;

    let paginationHTML = '';

    // Previous button
    if (currentPage > 1) {
      paginationHTML += `
                <button class="btn btn-outline btn-sm" onclick="ordersManager.goToPage(${currentPage - 1})">
                    <i class="fas fa-chevron-left"></i>
                </button>
            `;
    }

    // Page numbers
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
      const isActive = i === currentPage;
      paginationHTML += `
                <button class="btn ${isActive ? 'btn-primary' : 'btn-outline'} btn-sm" 
                        onclick="ordersManager.goToPage(${i})">
                    ${i}
                </button>
            `;
    }

    // Next button
    if (currentPage < totalPages) {
      paginationHTML += `
                <button class="btn btn-outline btn-sm" onclick="ordersManager.goToPage(${currentPage + 1})">
                    <i class="fas fa-chevron-right"></i>
                </button>
            `;
    }

    paginationControls.innerHTML = paginationHTML;
  }

  goToPage(page) {
    this.currentPage = page;
    this.loadOrders();
  }

  updateOrdersCount(total) {
    this.totalOrders = total;
    const countElement = document.getElementById('orders-count');
    if (countElement) {
      countElement.textContent = `${total} orders found`;
    }

    // Update showing info
    const showingFrom = document.getElementById('showing-from');
    const showingTo = document.getElementById('showing-to');
    const totalOrdersSpan = document.getElementById('total-orders');

    if (showingFrom && showingTo && totalOrdersSpan) {
      const from = (this.currentPage - 1) * this.pageSize + 1;
      const to = Math.min(this.currentPage * this.pageSize, total);

      showingFrom.textContent = from;
      showingTo.textContent = to;
      totalOrdersSpan.textContent = total;
    }
  }

  showLoading() {
    const loadingState = document.getElementById('loading-state');
    const ordersTable = document.getElementById('orders-table');
    const emptyState = document.getElementById('empty-state');

    if (loadingState) loadingState.classList.remove('hidden');
    if (ordersTable) ordersTable.classList.add('hidden');
    if (emptyState) emptyState.classList.add('hidden');
  }

  hideLoading() {
    const loadingState = document.getElementById('loading-state');
    if (loadingState) loadingState.classList.add('hidden');
  }

  showEmptyState() {
    const emptyState = document.getElementById('empty-state');
    const ordersTable = document.getElementById('orders-table');

    if (emptyState) emptyState.classList.remove('hidden');
    if (ordersTable) ordersTable.classList.add('hidden');
  }

  showError(message) {
    this.showToast(message, 'error');
  }

  showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
            <div class="flex items-center gap-3">
                <i class="fas fa-${type === 'error' ? 'exclamation-triangle' : 'info-circle'}"></i>
                <span>${message}</span>
                <button onclick="this.parentElement.parentElement.remove()" class="ml-auto">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;

    const container = document.getElementById('toast-container');
    if (container) {
      container.appendChild(toast);
      setTimeout(() => toast.remove(), 5000);
    }
  }

  formatDate(dateString) {
    if (!dateString) return 'N/A';
    // Handle array format [year, month, day, hour, minute, second] from Jackson
    if (Array.isArray(dateString)) {
      const [y, m, d, h = 0, min = 0, s = 0] = dateString;
      return new Date(y, m - 1, d, h, min, s).toLocaleDateString();
    }
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? 'N/A' : date.toLocaleDateString();
  }

  formatTime(dateString) {
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    if (Array.isArray(dateString)) {
      const [y, m, d, h = 0, min = 0, s = 0] = dateString;
      return new Date(y, m - 1, d, h, min, s).toLocaleString();
    }
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? 'N/A' : date.toLocaleString();
  }
}

// Global functions
function refreshOrders() {
  if (window.ordersManager) {
    window.ordersManager.loadOrders();
  }
}

function closeOrderModal() {
  if (window.ordersManager) {
    window.ordersManager.closeOrderModal();
  }
}

function clearFilters() {
  if (window.ordersManager) {
    window.ordersManager.clearFilters();
  }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  window.ordersManager = new OrdersManager();
});