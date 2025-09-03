// Mobile Optimization and Touch Interactions
class MobileOptimizer {
  constructor() {
    this.isMobile = this.detectMobile();
    this.isTouch = this.detectTouch();
    this.swipeThreshold = 50;
    this.swipeTimeout = 300;
    this.init();
  }

  init() {
    this.setupViewport();
    this.setupTouchEvents();
    this.setupMobileNavigation();
    this.setupSwipeGestures();
    this.setupFormOptimizations();
    this.setupPerformanceOptimizations();
    this.bindEvents();
  }

  detectMobile() {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  }

  detectTouch() {
    return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
  }

  setupViewport() {
    // Prevent zoom on input focus for iOS
    if (this.isMobile) {
      const viewport = document.querySelector('meta[name="viewport"]');
      if (viewport) {
        viewport.setAttribute('content',
          'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'
        );
      }
    }

    // Handle orientation changes
    window.addEventListener('orientationchange', () => {
      setTimeout(() => {
        this.handleOrientationChange();
      }, 100);
    });
  }

  setupTouchEvents() {
    if (!this.isTouch) return;

    // Add touch-friendly classes
    document.body.classList.add('touch-device');

    // Improve touch responsiveness
    document.addEventListener('touchstart', () => { }, { passive: true });

    // Handle touch feedback
    this.setupTouchFeedback();
  }

  setupTouchFeedback() {
    const touchElements = document.querySelectorAll('.btn, .nav-link, .card-hover');

    touchElements.forEach(element => {
      element.addEventListener('touchstart', (e) => {
        element.classList.add('touch-active');
      }, { passive: true });

      element.addEventListener('touchend', (e) => {
        setTimeout(() => {
          element.classList.remove('touch-active');
        }, 150);
      }, { passive: true });

      element.addEventListener('touchcancel', (e) => {
        element.classList.remove('touch-active');
      }, { passive: true });
    });
  }

  setupMobileNavigation() {
    const navToggle = document.querySelector('.mobile-nav-toggle');
    const nav = document.querySelector('.nav');
    const navOverlay = document.querySelector('.nav-overlay');

    if (!navToggle || !nav) return;

    // Create overlay if it doesn't exist
    if (!navOverlay) {
      const overlay = document.createElement('div');
      overlay.className = 'nav-overlay';
      document.body.appendChild(overlay);
    }

    navToggle.addEventListener('click', () => {
      this.toggleMobileNav();
    });

    // Close nav when clicking overlay
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('nav-overlay')) {
        this.closeMobileNav();
      }
    });

    // Close nav when clicking nav links
    const navLinks = nav.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
      link.addEventListener('click', () => {
        this.closeMobileNav();
      });
    });
  }

  toggleMobileNav() {
    const nav = document.querySelector('.nav');
    const overlay = document.querySelector('.nav-overlay');

    if (nav && overlay) {
      nav.classList.toggle('show');
      overlay.classList.toggle('show');
      document.body.classList.toggle('nav-open');
    }
  }

  closeMobileNav() {
    const nav = document.querySelector('.nav');
    const overlay = document.querySelector('.nav-overlay');

    if (nav && overlay) {
      nav.classList.remove('show');
      overlay.classList.remove('show');
      document.body.classList.remove('nav-open');
    }
  }

  setupSwipeGestures() {
    if (!this.isTouch) return;

    const swipeContainers = document.querySelectorAll('.swipe-container');

    swipeContainers.forEach(container => {
      this.initSwipeGestures(container);
    });
  }

  initSwipeGestures(element) {
    let startX = 0;
    let startY = 0;
    let startTime = 0;
    let isScrolling = false;

    element.addEventListener('touchstart', (e) => {
      const touch = e.touches[0];
      startX = touch.clientX;
      startY = touch.clientY;
      startTime = Date.now();
      isScrolling = false;

      element.classList.add('swiping');
    }, { passive: true });

    element.addEventListener('touchmove', (e) => {
      if (!startX || !startY) return;

      const touch = e.touches[0];
      const deltaX = touch.clientX - startX;
      const deltaY = touch.clientY - startY;

      // Determine if this is a scroll gesture
      if (Math.abs(deltaY) > Math.abs(deltaX)) {
        isScrolling = true;
        return;
      }

      // Prevent default if horizontal swipe
      if (Math.abs(deltaX) > 10) {
        e.preventDefault();
      }

      // Update visual feedback
      if (!isScrolling) {
        element.style.transform = `translateX(${deltaX}px)`;
      }
    }, { passive: false });

    element.addEventListener('touchend', (e) => {
      if (!startX || !startY || isScrolling) {
        this.resetSwipe(element);
        return;
      }

      const touch = e.changedTouches[0];
      const deltaX = touch.clientX - startX;
      const deltaY = touch.clientY - startY;
      const deltaTime = Date.now() - startTime;

      // Check if it's a valid swipe
      if (Math.abs(deltaX) > this.swipeThreshold &&
        deltaTime < this.swipeTimeout &&
        Math.abs(deltaX) > Math.abs(deltaY)) {

        const direction = deltaX > 0 ? 'right' : 'left';
        this.handleSwipe(element, direction, deltaX);
      } else {
        this.resetSwipe(element);
      }

      // Reset values
      startX = 0;
      startY = 0;
      startTime = 0;
    }, { passive: true });

    element.addEventListener('touchcancel', () => {
      this.resetSwipe(element);
      startX = 0;
      startY = 0;
      startTime = 0;
    }, { passive: true });
  }

  handleSwipe(element, direction, distance) {
    element.classList.remove('swiping');

    // Emit custom swipe event
    const swipeEvent = new CustomEvent('swipe', {
      detail: { direction, distance, element }
    });
    element.dispatchEvent(swipeEvent);

    // Handle common swipe actions
    if (element.classList.contains('swipe-to-delete') && direction === 'left') {
      this.handleSwipeToDelete(element);
    } else if (element.classList.contains('swipe-to-action') && direction === 'right') {
      this.handleSwipeToAction(element);
    } else {
      this.resetSwipe(element);
    }
  }

  resetSwipe(element) {
    element.classList.remove('swiping');
    element.style.transform = '';
  }

  handleSwipeToDelete(element) {
    element.style.transform = 'translateX(-100%)';
    element.style.opacity = '0';

    setTimeout(() => {
      element.remove();
    }, 300);
  }

  handleSwipeToAction(element) {
    // Show action buttons or trigger action
    const actionBtn = element.querySelector('.swipe-action');
    if (actionBtn) {
      actionBtn.click();
    }

    setTimeout(() => {
      this.resetSwipe(element);
    }, 1000);
  }

  setupFormOptimizations() {
    if (!this.isMobile) return;

    // Prevent zoom on input focus
    const inputs = document.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
      if (input.type !== 'file') {
        input.style.fontSize = '16px';
      }
    });

    // Optimize form submission
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
      form.addEventListener('submit', () => {
        // Hide keyboard on form submission
        document.activeElement.blur();
      });
    });

    // Sticky form actions
    this.setupStickyFormActions();
  }

  setupStickyFormActions() {
    const formActions = document.querySelectorAll('.form-actions');

    formActions.forEach(actions => {
      if (actions.classList.contains('sticky')) {
        this.makeStickyFormActions(actions);
      }
    });
  }

  makeStickyFormActions(element) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          element.classList.remove('stuck');
        } else {
          element.classList.add('stuck');
        }
      });
    }, { threshold: 0 });

    observer.observe(element);
  }

  setupPerformanceOptimizations() {
    // Lazy load images
    this.setupLazyLoading();

    // Optimize scroll performance
    this.optimizeScrolling();

    // Reduce animations on low-end devices
    this.optimizeAnimations();
  }

  setupLazyLoading() {
    if ('IntersectionObserver' in window) {
      const images = document.querySelectorAll('img[data-src]');
      const imageObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const img = entry.target;
            img.src = img.dataset.src;
            img.removeAttribute('data-src');
            imageObserver.unobserve(img);
          }
        });
      });

      images.forEach(img => imageObserver.observe(img));
    }
  }

  optimizeScrolling() {
    // Use passive event listeners for scroll
    let ticking = false;

    const handleScroll = () => {
      if (!ticking) {
        requestAnimationFrame(() => {
          this.updateScrollPosition();
          ticking = false;
        });
        ticking = true;
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
  }

  updateScrollPosition() {
    const scrollTop = window.pageYOffset;

    // Update scroll-dependent elements
    const parallaxElements = document.querySelectorAll('.parallax');
    parallaxElements.forEach(element => {
      const speed = element.dataset.speed || 0.5;
      element.style.transform = `translateY(${scrollTop * speed}px)`;
    });
  }

  optimizeAnimations() {
    // Check for low-end device indicators
    const isLowEnd = this.detectLowEndDevice();

    if (isLowEnd) {
      document.body.classList.add('reduced-motion');

      // Disable complex animations
      const style = document.createElement('style');
      style.textContent = `
                .reduced-motion * {
                    animation-duration: 0.01ms !important;
                    animation-iteration-count: 1 !important;
                    transition-duration: 0.01ms !important;
                }
            `;
      document.head.appendChild(style);
    }
  }

  detectLowEndDevice() {
    // Simple heuristics for low-end device detection
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection;
    const slowConnection = connection && (connection.effectiveType === 'slow-2g' || connection.effectiveType === '2g');
    const lowMemory = navigator.deviceMemory && navigator.deviceMemory < 4;
    const lowCores = navigator.hardwareConcurrency && navigator.hardwareConcurrency < 4;

    return slowConnection || lowMemory || lowCores;
  }

  handleOrientationChange() {
    // Recalculate layouts after orientation change
    const event = new Event('resize');
    window.dispatchEvent(event);

    // Fix viewport height issues on mobile
    if (this.isMobile) {
      const vh = window.innerHeight * 0.01;
      document.documentElement.style.setProperty('--vh', `${vh}px`);
    }
  }

  bindEvents() {
    // Handle back button on mobile
    if (this.isMobile) {
      window.addEventListener('popstate', (e) => {
        this.handleBackButton(e);
      });
    }

    // Handle app state changes
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.handleAppBackground();
      } else {
        this.handleAppForeground();
      }
    });
  }

  handleBackButton(event) {
    // Close modals or navigation when back button is pressed
    const openModal = document.querySelector('.modal-overlay:not(.hidden)');
    const openNav = document.querySelector('.nav.show');

    if (openModal) {
      event.preventDefault();
      const modal = openModal.querySelector('.modal');
      if (modal && modal._componentInstance) {
        modal._componentInstance.hide();
      }
    } else if (openNav) {
      event.preventDefault();
      this.closeMobileNav();
    }
  }

  handleAppBackground() {
    // Pause non-essential operations when app goes to background
    this.pauseAnimations();
    this.pauseAutoRefresh();
  }

  handleAppForeground() {
    // Resume operations when app comes to foreground
    this.resumeAnimations();
    this.resumeAutoRefresh();
  }

  pauseAnimations() {
    document.body.classList.add('paused-animations');
  }

  resumeAnimations() {
    document.body.classList.remove('paused-animations');
  }

  pauseAutoRefresh() {
    // Pause dashboard auto-refresh and other periodic updates
    if (window.dashboard && window.dashboard.stopAutoRefresh) {
      window.dashboard.stopAutoRefresh();
    }
  }

  resumeAutoRefresh() {
    // Resume dashboard auto-refresh
    if (window.dashboard && window.dashboard.startAutoRefresh) {
      window.dashboard.startAutoRefresh();
    }
  }

  // Public API methods
  enableMobileMode() {
    document.body.classList.add('mobile-mode');
  }

  disableMobileMode() {
    document.body.classList.remove('mobile-mode');
  }

  isMobileDevice() {
    return this.isMobile;
  }

  isTouchDevice() {
    return this.isTouch;
  }
}

// Touch gesture utilities
class TouchGestures {
  static addSwipeListener(element, callback) {
    const mobileOptimizer = window.mobileOptimizer;
    if (mobileOptimizer) {
      element.classList.add('swipe-container');
      element.addEventListener('swipe', callback);
    }
  }

  static addTouchFeedback(element) {
    if (!('ontouchstart' in window)) return;

    element.addEventListener('touchstart', () => {
      element.classList.add('touch-active');
    }, { passive: true });

    element.addEventListener('touchend', () => {
      setTimeout(() => {
        element.classList.remove('touch-active');
      }, 150);
    }, { passive: true });
  }
}

// Initialize mobile optimizer
document.addEventListener('DOMContentLoaded', () => {
  window.mobileOptimizer = new MobileOptimizer();
  window.TouchGestures = TouchGestures;
});

// Export for use in other modules
window.MobileOptimizer = MobileOptimizer;