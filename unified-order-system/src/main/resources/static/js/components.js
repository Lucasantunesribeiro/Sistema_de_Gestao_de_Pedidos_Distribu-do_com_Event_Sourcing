// Modern Component Library
class ComponentLibrary {
  constructor() {
    this.components = new Map();
    this.init();
  }

  init() {
    this.registerComponents();
    this.initializeComponents();
  }

  registerComponents() {
    // Register all available components
    this.components.set('modal', Modal);
    this.components.set('toast', Toast);
    this.components.set('button', Button);
    this.components.set('loading-spinner', LoadingSpinner);
    this.components.set('progress-bar', ProgressBar);
    this.components.set('dropdown', Dropdown);
    this.components.set('tabs', Tabs);
    this.components.set('accordion', Accordion);
    this.components.set('tooltip', Tooltip);
    this.components.set('datepicker', DatePicker);
  }

  initializeComponents() {
    // Auto-initialize components found in DOM
    document.addEventListener('DOMContentLoaded', () => {
      this.components.forEach((ComponentClass, name) => {
        const elements = document.querySelectorAll(`[data-component="${name}"]`);
        elements.forEach(element => {
          if (!element._componentInstance) {
            element._componentInstance = new ComponentClass(element);
          }
        });
      });
    });
  }

  create(componentName, options = {}) {
    const ComponentClass = this.components.get(componentName);
    if (ComponentClass) {
      return new ComponentClass(null, options);
    }
    throw new Error(`Component "${componentName}" not found`);
  }
}

// Base Component Class
class BaseComponent {
  constructor(element, options = {}) {
    this.element = element;
    this.options = { ...this.defaultOptions, ...options };
    this.isInitialized = false;

    if (this.element) {
      this.init();
    }
  }

  get defaultOptions() {
    return {};
  }

  init() {
    if (this.isInitialized) return;

    this.bindEvents();
    this.render();
    this.isInitialized = true;

    this.emit('initialized');
  }

  bindEvents() {
    // Override in subclasses
  }

  render() {
    // Override in subclasses
  }

  emit(eventName, data = {}) {
    const event = new CustomEvent(eventName, {
      detail: { component: this, ...data }
    });

    if (this.element) {
      this.element.dispatchEvent(event);
    } else {
      document.dispatchEvent(event);
    }
  }

  on(eventName, callback) {
    const target = this.element || document;
    target.addEventListener(eventName, callback);
    return this;
  }

  destroy() {
    if (this.element) {
      this.element._componentInstance = null;
    }
    this.isInitialized = false;
    this.emit('destroyed');
  }
}

// Modal Component
class Modal extends BaseComponent {
  get defaultOptions() {
    return {
      backdrop: true,
      keyboard: true,
      focus: true,
      show: false,
      size: 'md', // sm, md, lg, xl
      centered: false,
      scrollable: false,
      animation: true
    };
  }

  init() {
    if (!this.element) {
      this.createElement();
    }
    super.init();
  }

  createElement() {
    this.element = document.createElement('div');
    this.element.className = 'modal-overlay hidden';
    this.element.innerHTML = this.getTemplate();
    document.body.appendChild(this.element);
  }

  getTemplate() {
    return `
            <div class="modal ${this.getSizeClass()} ${this.options.centered ? 'modal-centered' : ''}">
                <div class="modal-header">
                    <h3 class="modal-title">${this.options.title || ''}</h3>
                    <button class="btn btn-ghost btn-sm modal-close">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="modal-body ${this.options.scrollable ? 'modal-scrollable' : ''}">
                    ${this.options.content || ''}
                </div>
                ${this.options.footer ? `<div class="modal-footer">${this.options.footer}</div>` : ''}
            </div>
        `;
  }

  getSizeClass() {
    const sizeMap = {
      sm: 'modal-sm',
      md: 'modal-md',
      lg: 'modal-lg',
      xl: 'modal-xl'
    };
    return sizeMap[this.options.size] || 'modal-md';
  }

  bindEvents() {
    // Close button
    const closeBtn = this.element.querySelector('.modal-close');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.hide());
    }

    // Backdrop click
    if (this.options.backdrop) {
      this.element.addEventListener('click', (e) => {
        if (e.target === this.element) {
          this.hide();
        }
      });
    }

    // Keyboard events
    if (this.options.keyboard) {
      document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && this.isVisible()) {
          this.hide();
        }
      });
    }
  }

  show() {
    this.element.classList.remove('hidden');

    if (this.options.animation) {
      this.element.style.opacity = '0';
      requestAnimationFrame(() => {
        this.element.style.opacity = '1';
      });
    }

    if (this.options.focus) {
      const focusable = this.element.querySelector('input, button, textarea, select');
      if (focusable) focusable.focus();
    }

    document.body.style.overflow = 'hidden';
    this.emit('shown');
    return this;
  }

  hide() {
    if (this.options.animation) {
      this.element.style.opacity = '0';
      setTimeout(() => {
        this.element.classList.add('hidden');
        document.body.style.overflow = '';
      }, 200);
    } else {
      this.element.classList.add('hidden');
      document.body.style.overflow = '';
    }

    this.emit('hidden');
    return this;
  }

  isVisible() {
    return !this.element.classList.contains('hidden');
  }

  setTitle(title) {
    const titleElement = this.element.querySelector('.modal-title');
    if (titleElement) {
      titleElement.textContent = title;
    }
    return this;
  }

  setContent(content) {
    const bodyElement = this.element.querySelector('.modal-body');
    if (bodyElement) {
      bodyElement.innerHTML = content;
    }
    return this;
  }
}

// Toast Component
class Toast extends BaseComponent {
  get defaultOptions() {
    return {
      type: 'info', // success, warning, error, info
      title: '',
      message: '',
      duration: 5000,
      position: 'top-right', // top-right, top-left, bottom-right, bottom-left
      closable: true,
      icon: true,
      animation: true
    };
  }

  init() {
    if (!this.element) {
      this.createElement();
    }
    super.init();

    if (this.options.duration > 0) {
      this.autoHide();
    }
  }

  createElement() {
    this.element = document.createElement('div');
    this.element.className = `toast toast-${this.options.type}`;
    this.element.innerHTML = this.getTemplate();

    this.getContainer().appendChild(this.element);
  }

  getContainer() {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = `toast-container toast-${this.options.position}`;
      document.body.appendChild(container);
    }
    return container;
  }

  getTemplate() {
    const icon = this.getIcon();
    return `
            <div class="toast-content">
                ${this.options.icon && icon ? `<i class="${icon}"></i>` : ''}
                <div class="toast-text">
                    ${this.options.title ? `<div class="toast-title">${this.options.title}</div>` : ''}
                    <div class="toast-message">${this.options.message}</div>
                </div>
                ${this.options.closable ? '<button class="toast-close"><i class="fas fa-times"></i></button>' : ''}
            </div>
        `;
  }

  getIcon() {
    const iconMap = {
      success: 'fas fa-check-circle',
      warning: 'fas fa-exclamation-triangle',
      error: 'fas fa-times-circle',
      info: 'fas fa-info-circle'
    };
    return iconMap[this.options.type];
  }

  bindEvents() {
    const closeBtn = this.element.querySelector('.toast-close');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.hide());
    }
  }

  show() {
    if (this.options.animation) {
      this.element.style.transform = 'translateX(100%)';
      this.element.style.opacity = '0';

      requestAnimationFrame(() => {
        this.element.style.transform = 'translateX(0)';
        this.element.style.opacity = '1';
      });
    }

    this.emit('shown');
    return this;
  }

  hide() {
    if (this.options.animation) {
      this.element.style.transform = 'translateX(100%)';
      this.element.style.opacity = '0';

      setTimeout(() => {
        this.element.remove();
      }, 300);
    } else {
      this.element.remove();
    }

    this.emit('hidden');
    return this;
  }

  autoHide() {
    setTimeout(() => {
      this.hide();
    }, this.options.duration);
  }

  static show(message, type = 'info', options = {}) {
    return new Toast(null, {
      message,
      type,
      ...options
    }).show();
  }

  static success(message, options = {}) {
    return Toast.show(message, 'success', options);
  }

  static error(message, options = {}) {
    return Toast.show(message, 'error', options);
  }

  static warning(message, options = {}) {
    return Toast.show(message, 'warning', options);
  }

  static info(message, options = {}) {
    return Toast.show(message, 'info', options);
  }
}

// Button Component
class Button extends BaseComponent {
  get defaultOptions() {
    return {
      variant: 'primary', // primary, secondary, success, warning, danger, outline, ghost
      size: 'md', // sm, md, lg
      loading: false,
      disabled: false,
      icon: null,
      iconPosition: 'left', // left, right
      ripple: true
    };
  }

  bindEvents() {
    if (this.options.ripple) {
      this.element.addEventListener('click', (e) => this.createRipple(e));
    }

    // Loading state
    this.element.addEventListener('loading:start', () => this.setLoading(true));
    this.element.addEventListener('loading:stop', () => this.setLoading(false));
  }

  render() {
    this.updateClasses();
    this.updateContent();
    this.updateState();
  }

  updateClasses() {
    const classes = ['btn'];

    classes.push(`btn-${this.options.variant}`);
    classes.push(`btn-${this.options.size}`);

    if (this.options.loading) classes.push('btn-loading');
    if (this.options.disabled) classes.push('btn-disabled');

    this.element.className = classes.join(' ');
  }

  updateContent() {
    if (this.options.icon) {
      const iconHtml = `<i class="${this.options.icon}"></i>`;
      const textContent = this.element.textContent.trim();

      if (this.options.iconPosition === 'left') {
        this.element.innerHTML = `${iconHtml} ${textContent}`;
      } else {
        this.element.innerHTML = `${textContent} ${iconHtml}`;
      }
    }
  }

  updateState() {
    this.element.disabled = this.options.disabled || this.options.loading;
  }

  createRipple(event) {
    const button = event.currentTarget;
    const rect = button.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    const x = event.clientX - rect.left - size / 2;
    const y = event.clientY - rect.top - size / 2;

    const ripple = document.createElement('span');
    ripple.className = 'btn-ripple';
    ripple.style.width = ripple.style.height = size + 'px';
    ripple.style.left = x + 'px';
    ripple.style.top = y + 'px';

    button.appendChild(ripple);

    setTimeout(() => {
      ripple.remove();
    }, 600);
  }

  setLoading(loading) {
    this.options.loading = loading;
    this.render();
    return this;
  }

  setDisabled(disabled) {
    this.options.disabled = disabled;
    this.render();
    return this;
  }

  setVariant(variant) {
    this.options.variant = variant;
    this.render();
    return this;
  }
}

// Loading Spinner Component
class LoadingSpinner extends BaseComponent {
  get defaultOptions() {
    return {
      size: 'md', // sm, md, lg
      color: 'primary',
      text: '',
      overlay: false,
      fullscreen: false
    };
  }

  init() {
    if (!this.element) {
      this.createElement();
    }
    super.init();
  }

  createElement() {
    this.element = document.createElement('div');
    this.element.className = this.getClasses();
    this.element.innerHTML = this.getTemplate();

    if (this.options.fullscreen) {
      document.body.appendChild(this.element);
    }
  }

  getClasses() {
    const classes = ['loading-spinner'];

    classes.push(`spinner-${this.options.size}`);
    classes.push(`spinner-${this.options.color}`);

    if (this.options.overlay) classes.push('spinner-overlay');
    if (this.options.fullscreen) classes.push('spinner-fullscreen');

    return classes.join(' ');
  }

  getTemplate() {
    return `
            <div class="spinner"></div>
            ${this.options.text ? `<div class="spinner-text">${this.options.text}</div>` : ''}
        `;
  }

  show() {
    this.element.classList.remove('hidden');
    this.emit('shown');
    return this;
  }

  hide() {
    this.element.classList.add('hidden');
    this.emit('hidden');
    return this;
  }

  setText(text) {
    const textElement = this.element.querySelector('.spinner-text');
    if (textElement) {
      textElement.textContent = text;
    } else if (text) {
      this.element.appendChild(document.createElement('div'));
      this.element.lastChild.className = 'spinner-text';
      this.element.lastChild.textContent = text;
    }
    return this;
  }
}

// Progress Bar Component
class ProgressBar extends BaseComponent {
  get defaultOptions() {
    return {
      value: 0,
      max: 100,
      min: 0,
      variant: 'primary',
      size: 'md',
      striped: false,
      animated: false,
      showLabel: false,
      labelFormat: '{value}%'
    };
  }

  render() {
    if (!this.element) {
      this.createElement();
    }

    this.updateProgress();
    this.updateClasses();
    this.updateLabel();
  }

  createElement() {
    this.element = document.createElement('div');
    this.element.className = 'progress';
    this.element.innerHTML = `
            <div class="progress-bar"></div>
            ${this.options.showLabel ? '<div class="progress-label"></div>' : ''}
        `;
  }

  updateClasses() {
    const bar = this.element.querySelector('.progress-bar');
    const classes = ['progress-bar'];

    classes.push(`progress-bar-${this.options.variant}`);
    classes.push(`progress-bar-${this.options.size}`);

    if (this.options.striped) classes.push('progress-bar-striped');
    if (this.options.animated) classes.push('progress-bar-animated');

    bar.className = classes.join(' ');
  }

  updateProgress() {
    const bar = this.element.querySelector('.progress-bar');
    const percentage = this.getPercentage();

    bar.style.width = `${percentage}%`;
    bar.setAttribute('aria-valuenow', this.options.value);
    bar.setAttribute('aria-valuemin', this.options.min);
    bar.setAttribute('aria-valuemax', this.options.max);
  }

  updateLabel() {
    if (!this.options.showLabel) return;

    const label = this.element.querySelector('.progress-label');
    if (label) {
      const text = this.options.labelFormat
        .replace('{value}', this.getPercentage())
        .replace('{current}', this.options.value)
        .replace('{max}', this.options.max);

      label.textContent = text;
    }
  }

  getPercentage() {
    const range = this.options.max - this.options.min;
    const value = Math.max(this.options.min, Math.min(this.options.max, this.options.value));
    return Math.round(((value - this.options.min) / range) * 100);
  }

  setValue(value) {
    this.options.value = value;
    this.updateProgress();
    this.updateLabel();
    this.emit('change', { value });
    return this;
  }

  increment(amount = 1) {
    return this.setValue(this.options.value + amount);
  }

  decrement(amount = 1) {
    return this.setValue(this.options.value - amount);
  }

  reset() {
    return this.setValue(this.options.min);
  }

  complete() {
    return this.setValue(this.options.max);
  }
}

// Initialize component library
const componentLibrary = new ComponentLibrary();

// Export for global use
window.ComponentLibrary = ComponentLibrary;
window.Modal = Modal;
window.Toast = Toast;
window.Button = Button;
window.LoadingSpinner = LoadingSpinner;
window.ProgressBar = ProgressBar;