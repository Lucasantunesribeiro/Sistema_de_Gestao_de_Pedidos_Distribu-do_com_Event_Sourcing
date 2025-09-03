// Order Creation JavaScript
class OrderCreator {
  constructor() {
    this.orderItems = [];
    this.itemCounter = 0;
    this.subtotal = 0;
    this.tax = 0;
    this.shipping = 0;
    this.total = 0;
    this.init();
  }

  init() {
    this.bindEvents();
    this.addOrderItem(); // Add first item by default
    this.updateTotals();
  }

  bindEvents() {
    // Form submission
    const form = document.getElementById('create-order-form');
    if (form) {
      form.addEventListener('submit', (e) => {
        e.preventDefault();
        this.submitOrder();
      });
    }

    // Payment method change
    const paymentMethod = document.querySelector('[name="paymentMethod"]');
    if (paymentMethod) {
      paymentMethod.addEventListener('change', (e) => {
        this.showPaymentDetails(e.target.value);
      });
    }

    // Real-time validation
    this.setupRealTimeValidation();
  }

  setupRealTimeValidation() {
    const requiredFields = document.querySelectorAll('[required]');
    requiredFields.forEach(field => {
      field.addEventListener('blur', () => this.validateField(field));
      field.addEventListener('input', () => this.clearFieldError(field));
    });
  }

  validateField(field) {
    const value = field.value.trim();
    const isValid = field.checkValidity();

    if (!isValid || !value) {
      this.showFieldError(field, this.getFieldErrorMessage(field));
      return false;
    } else {
      this.clearFieldError(field);
      return true;
    }
  }

  getFieldErrorMessage(field) {
    const fieldName = field.name;
    const fieldType = field.type;

    if (!field.value.trim()) {
      return `${this.getFieldLabel(field)} is required`;
    }

    if (fieldType === 'email' && !field.validity.valid) {
      return 'Please enter a valid email address';
    }

    if (fieldName === 'customerPhone' && field.value && !this.isValidPhone(field.value)) {
      return 'Please enter a valid phone number';
    }

    return 'Please enter a valid value';
  }

  getFieldLabel(field) {
    const label = document.querySelector(`label[for="${field.name}"], label[for="${field.id}"]`);
    return label ? label.textContent.replace('*', '').trim() : field.name;
  }

  showFieldError(field, message) {
    field.classList.add('is-invalid');
    const feedback = field.parentElement.querySelector('.invalid-feedback');
    if (feedback) {
      feedback.textContent = message;
    }
  }

  clearFieldError(field) {
    field.classList.remove('is-invalid');
    const feedback = field.parentElement.querySelector('.invalid-feedback');
    if (feedback) {
      feedback.textContent = '';
    }
  }

  addOrderItem() {
    this.itemCounter++;
    const container = document.getElementById('order-items-container');

    const itemHTML = this.createOrderItemHTML(this.itemCounter);
    container.insertAdjacentHTML('beforeend', itemHTML);

    // Bind events for the new item
    this.bindItemEvents(this.itemCounter);
  }

  createOrderItemHTML(itemId) {
    return `
            <div class="order-item" data-item-id="${itemId}">
                <div class="grid md:grid-cols-6 gap-4 p-4 border border-gray-200 rounded-lg mb-4">
                    <div class="md:col-span-2">
                        <label class="form-label required">Product</label>
                        <div class="relative">
                            <input type="text" 
                                   class="form-control" 
                                   name="items[${itemId}].productName" 
                                   placeholder="Search or enter product name"
                                   required>
                            <button type="button" 
                                    class="absolute inset-y-0 right-0 px-3 text-gray-400 hover:text-gray-600"
                                    onclick="orderCreator.openProductSearch(${itemId})"
                                    title="Search Products">
                                <i class="fas fa-search"></i>
                            </button>
                        </div>
                        <input type="hidden" name="items[${itemId}].productId">
                    </div>
                    
                    <div>
                        <label class="form-label required">Quantity</label>
                        <input type="number" 
                               class="form-control" 
                               name="items[${itemId}].quantity" 
                               min="1" 
                               value="1"
                               onchange="orderCreator.updateItemTotal(${itemId})"
                               required>
                    </div>
                    
                    <div>
                        <label class="form-label required">Unit Price</label>
                        <div class="relative">
                            <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-500">$</span>
                            <input type="number" 
                                   class="form-control pl-8" 
                                   name="items[${itemId}].unitPrice" 
                                   min="0" 
                                   step="0.01"
                                   onchange="orderCreator.updateItemTotal(${itemId})"
                                   required>
                        </div>
                    </div>
                    
                    <div>
                        <label class="form-label">Total</label>
                        <div class="form-control bg-gray-50 flex items-center">
                            <span class="font-semibold" id="item-total-${itemId}">$0.00</span>
                        </div>
                    </div>
                    
                    <div class="flex items-end">
                        <button type="button" 
                                class="btn btn-outline btn-sm w-full"
                                onclick="orderCreator.removeOrderItem(${itemId})"
                                ${this.itemCounter === 1 ? 'disabled' : ''}>
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
  }

  bindItemEvents(itemId) {
    const item = document.querySelector(`[data-item-id="${itemId}"]`);
    if (!item) return;

    // Auto-calculate totals when inputs change
    const quantityInput = item.querySelector('[name*="quantity"]');
    const priceInput = item.querySelector('[name*="unitPrice"]');

    if (quantityInput) {
      quantityInput.addEventListener('input', () => this.updateItemTotal(itemId));
    }
    if (priceInput) {
      priceInput.addEventListener('input', () => this.updateItemTotal(itemId));
    }
  }

  updateItemTotal(itemId) {
    const item = document.querySelector(`[data-item-id="${itemId}"]`);
    if (!item) return;

    const quantity = parseFloat(item.querySelector('[name*="quantity"]').value) || 0;
    const unitPrice = parseFloat(item.querySelector('[name*="unitPrice"]').value) || 0;
    const total = quantity * unitPrice;

    const totalElement = document.getElementById(`item-total-${itemId}`);
    if (totalElement) {
      totalElement.textContent = `$${total.toFixed(2)}`;
    }

    this.updateTotals();
  }

  removeOrderItem(itemId) {
    const item = document.querySelector(`[data-item-id="${itemId}"]`);
    if (item) {
      item.remove();
      this.updateTotals();

      // Enable remove buttons if we have more than one item
      const remainingItems = document.querySelectorAll('.order-item');
      if (remainingItems.length === 1) {
        const removeBtn = remainingItems[0].querySelector('button[onclick*="removeOrderItem"]');
        if (removeBtn) removeBtn.disabled = true;
      }
    }
  }

  updateTotals() {
    this.subtotal = 0;

    // Calculate subtotal from all items
    const items = document.querySelectorAll('.order-item');
    items.forEach(item => {
      const quantity = parseFloat(item.querySelector('[name*="quantity"]').value) || 0;
      const unitPrice = parseFloat(item.querySelector('[name*="unitPrice"]').value) || 0;
      this.subtotal += quantity * unitPrice;
    });

    // Calculate tax (10%)
    this.tax = this.subtotal * 0.10;

    // Calculate shipping (free for orders over $100)
    this.shipping = this.subtotal > 100 ? 0 : 10;

    // Calculate total
    this.total = this.subtotal + this.tax + this.shipping;

    // Update display
    document.getElementById('subtotal-amount').textContent = `$${this.subtotal.toFixed(2)}`;
    document.getElementById('tax-amount').textContent = `$${this.tax.toFixed(2)}`;
    document.getElementById('shipping-amount').textContent = `$${this.shipping.toFixed(2)}`;
    document.getElementById('total-amount').textContent = `$${this.total.toFixed(2)}`;
  }

  openProductSearch(itemId) {
    this.currentItemId = itemId;
    const modal = document.getElementById('product-search-modal');
    modal.classList.remove('hidden');

    // Focus on search input
    const searchInput = document.getElementById('product-search-input');
    if (searchInput) {
      searchInput.focus();
      searchInput.addEventListener('input', (e) => this.searchProducts(e.target.value));
    }
  }

  async searchProducts(query) {
    if (!query || query.length < 2) {
      document.getElementById('product-search-results').innerHTML = '';
      return;
    }

    try {
      const response = await fetch(`/api/products/search?q=${encodeURIComponent(query)}`);
      const products = await response.json();
      this.displayProductResults(products);
    } catch (error) {
      console.error('Error searching products:', error);
      this.displayProductResults([]);
    }
  }

  displayProductResults(products) {
    const resultsContainer = document.getElementById('product-search-results');

    if (!products || products.length === 0) {
      resultsContainer.innerHTML = '<p class="text-center text-gray-500 py-4">No products found</p>';
      return;
    }

    resultsContainer.innerHTML = products.map(product => `
            <div class="flex justify-between items-center p-3 border border-gray-200 rounded hover:bg-gray-50 cursor-pointer"
                 onclick="orderCreator.selectProduct('${product.id}', '${product.name}', ${product.price})">
                <div>
                    <div class="font-medium">${product.name}</div>
                    <div class="text-sm text-gray-500">${product.description || ''}</div>
                </div>
                <div class="text-right">
                    <div class="font-semibold">$${product.price.toFixed(2)}</div>
                    <div class="text-xs text-gray-500">Stock: ${product.stock || 0}</div>
                </div>
            </div>
        `).join('');
  }

  selectProduct(productId, productName, price) {
    const item = document.querySelector(`[data-item-id="${this.currentItemId}"]`);
    if (item) {
      item.querySelector('[name*="productId"]').value = productId;
      item.querySelector('[name*="productName"]').value = productName;
      item.querySelector('[name*="unitPrice"]').value = price;
      this.updateItemTotal(this.currentItemId);
    }
    this.closeProductModal();
  }

  closeProductModal() {
    const modal = document.getElementById('product-search-modal');
    modal.classList.add('hidden');
    document.getElementById('product-search-input').value = '';
    document.getElementById('product-search-results').innerHTML = '';
  }

  showPaymentDetails(paymentMethod) {
    const detailsContainer = document.getElementById('payment-details');

    if (!paymentMethod) {
      detailsContainer.classList.add('hidden');
      return;
    }

    let detailsHTML = '';

    switch (paymentMethod) {
      case 'CREDIT_CARD':
      case 'DEBIT_CARD':
        detailsHTML = `
                    <div class="grid md:grid-cols-2 gap-4">
                        <div class="form-group">
                            <label class="form-label">Card Number</label>
                            <input type="text" class="form-control" placeholder="1234 5678 9012 3456">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Cardholder Name</label>
                            <input type="text" class="form-control" placeholder="John Doe">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Expiry Date</label>
                            <input type="text" class="form-control" placeholder="MM/YY">
                        </div>
                        <div class="form-group">
                            <label class="form-label">CVV</label>
                            <input type="text" class="form-control" placeholder="123">
                        </div>
                    </div>
                `;
        break;
      case 'PIX':
        detailsHTML = `
                    <div class="form-group">
                        <label class="form-label">PIX Key</label>
                        <input type="text" class="form-control" placeholder="Enter PIX key (email, phone, or random key)">
                    </div>
                `;
        break;
      case 'BANK_TRANSFER':
        detailsHTML = `
                    <div class="grid md:grid-cols-2 gap-4">
                        <div class="form-group">
                            <label class="form-label">Bank</label>
                            <input type="text" class="form-control" placeholder="Bank name">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Account Number</label>
                            <input type="text" class="form-control" placeholder="Account number">
                        </div>
                    </div>
                `;
        break;
    }

    if (detailsHTML) {
      detailsContainer.innerHTML = detailsHTML;
      detailsContainer.classList.remove('hidden');
    } else {
      detailsContainer.classList.add('hidden');
    }
  }

  async submitOrder() {
    if (!this.validateForm()) {
      return;
    }

    const submitBtn = document.getElementById('submit-btn');
    const originalText = submitBtn.innerHTML;

    try {
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creating Order...';

      const orderData = this.collectFormData();

      const response = await fetch('/api/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(orderData)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to create order');
      }

      const order = await response.json();
      this.showSuccess(`Order #${order.orderId} created successfully!`);

      // Redirect to order details or orders list
      setTimeout(() => {
        window.location.href = `/orders/${order.orderId}`;
      }, 2000);

    } catch (error) {
      console.error('Error creating order:', error);
      this.showError(error.message || 'Failed to create order');
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = originalText;
    }
  }

  validateForm() {
    let isValid = true;

    // Validate required fields
    const requiredFields = document.querySelectorAll('[required]');
    requiredFields.forEach(field => {
      if (!this.validateField(field)) {
        isValid = false;
      }
    });

    // Validate at least one order item
    const items = document.querySelectorAll('.order-item');
    if (items.length === 0) {
      this.showError('Please add at least one order item');
      isValid = false;
    }

    // Validate order items have valid data
    let hasValidItems = false;
    items.forEach(item => {
      const productName = item.querySelector('[name*="productName"]').value.trim();
      const quantity = parseFloat(item.querySelector('[name*="quantity"]').value) || 0;
      const unitPrice = parseFloat(item.querySelector('[name*="unitPrice"]').value) || 0;

      if (productName && quantity > 0 && unitPrice > 0) {
        hasValidItems = true;
      }
    });

    if (!hasValidItems) {
      this.showError('Please ensure all order items have valid product, quantity, and price');
      isValid = false;
    }

    return isValid;
  }

  collectFormData() {
    const formData = new FormData(document.getElementById('create-order-form'));

    // Collect basic order data
    const orderData = {
      customerId: formData.get('customerId') || this.generateCustomerId(),
      customerName: formData.get('customerName'),
      customerEmail: formData.get('customerEmail'),
      customerPhone: formData.get('customerPhone'),
      paymentMethod: formData.get('paymentMethod'),
      priority: formData.get('priority') || 'NORMAL',
      expectedDelivery: formData.get('expectedDelivery'),
      notes: formData.get('notes'),
      items: []
    };

    // Collect order items
    const items = document.querySelectorAll('.order-item');
    items.forEach(item => {
      const productId = item.querySelector('[name*="productId"]').value;
      const productName = item.querySelector('[name*="productName"]').value.trim();
      const quantity = parseFloat(item.querySelector('[name*="quantity"]').value) || 0;
      const unitPrice = parseFloat(item.querySelector('[name*="unitPrice"]').value) || 0;

      if (productName && quantity > 0 && unitPrice > 0) {
        orderData.items.push({
          productId: productId || null,
          productName: productName,
          quantity: quantity,
          unitPrice: unitPrice,
          totalPrice: quantity * unitPrice
        });
      }
    });

    return orderData;
  }

  generateCustomerId() {
    return 'CUST-' + Date.now().toString(36).toUpperCase();
  }

  saveDraft() {
    const orderData = this.collectFormData();
    localStorage.setItem('orderDraft', JSON.stringify(orderData));
    this.showSuccess('Order draft saved successfully!');
  }

  loadDraft() {
    const draft = localStorage.getItem('orderDraft');
    if (draft) {
      try {
        const orderData = JSON.parse(draft);
        this.populateForm(orderData);
        this.showSuccess('Order draft loaded successfully!');
      } catch (error) {
        console.error('Error loading draft:', error);
        this.showError('Failed to load order draft');
      }
    }
  }

  isValidPhone(phone) {
    const phoneRegex = /^[\+]?[1-9][\d]{0,15}$/;
    return phoneRegex.test(phone.replace(/[\s\-\(\)]/g, ''));
  }

  showSuccess(message) {
    this.showToast(message, 'success');
  }

  showError(message) {
    this.showToast(message, 'error');
  }

  showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
            <div class="flex items-center gap-3">
                <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-triangle' : 'info-circle'}"></i>
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
}

// Global functions
function generateCustomerId() {
  if (window.orderCreator) {
    const customerId = window.orderCreator.generateCustomerId();
    document.querySelector('[name="customerId"]').value = customerId;
  }
}

function saveDraft() {
  if (window.orderCreator) {
    window.orderCreator.saveDraft();
  }
}

function closeProductModal() {
  if (window.orderCreator) {
    window.orderCreator.closeProductModal();
  }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  window.orderCreator = new OrderCreator();
});