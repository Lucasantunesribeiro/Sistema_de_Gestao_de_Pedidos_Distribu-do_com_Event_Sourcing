// Global Application JavaScript

// Toast notification system
function showToast(message, type) {
  type = type || 'info';
  var container = document.getElementById('toast-container');
  if (!container) return;

  var iconMap = {
    success: 'fa-check-circle',
    error: 'fa-exclamation-triangle',
    warning: 'fa-exclamation-circle',
    info: 'fa-info-circle'
  };

  var toast = document.createElement('div');
  toast.className = 'toast toast-' + type;
  toast.innerHTML =
    '<div class="toast-content">' +
      '<i class="fas ' + (iconMap[type] || iconMap.info) + '"></i>' +
      '<span>' + message + '</span>' +
      '<button class="toast-close" onclick="this.closest(\'.toast\').remove()">' +
        '<i class="fas fa-times"></i>' +
      '</button>' +
    '</div>';

  container.appendChild(toast);

  // Auto-remove after 5 seconds
  setTimeout(function() {
    if (toast.parentNode) {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      setTimeout(function() { toast.remove(); }, 300);
    }
  }, 5000);
}

// Toggle sidebar
function toggleSidebar() {
  var sidebar = document.getElementById('sidebar');
  if (sidebar) {
    sidebar.classList.toggle('sidebar-open');
  }
}

// Refresh page data
function refreshPage() {
  if (typeof initializeDashboard === 'function') {
    initializeDashboard();
  }
  showToast('Data refreshed', 'success');
}

// Initialize global app
function initializeApp() {
  // Close sidebar when clicking outside on mobile
  document.addEventListener('click', function(e) {
    var sidebar = document.getElementById('sidebar');
    if (sidebar && sidebar.classList.contains('sidebar-open')) {
      if (!sidebar.contains(e.target) && !e.target.closest('.sidebar-toggle')) {
        sidebar.classList.remove('sidebar-open');
      }
    }
  });
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', initializeApp);
