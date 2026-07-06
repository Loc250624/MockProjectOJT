'use strict';
console.log('Admin JS loaded');
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a');
    sidebarLinks.forEach(function(link) {
        if (link.getAttribute('href') === currentPath) {
            link.style.background = 'rgba(233,69,96,0.2)';
            link.style.color = '#e94560';
        }
    });
});
