'use strict';
console.log('Student JS loaded');
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a');
    sidebarLinks.forEach(function(link) {
        if (link.getAttribute('href') === currentPath) {
            link.style.background = 'rgba(52,152,219,0.2)';
            link.style.color = '#3498db';
        }
    });
});
