'use strict';
console.log('Admin JS loaded');
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    sidebarLinks.forEach(function(link) {
        var linkPath = new URL(link.getAttribute('href'), window.location.origin).pathname;
        if (linkPath === currentPath) {
            link.classList.add('active');
        }
    });
});
