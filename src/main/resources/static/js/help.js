(function () {
  'use strict';

  if (window.LuminaHelpInitialized) {
    return;
  }

  window.LuminaHelpInitialized = true;

  var focusableSelector = [
    'a[href]',
    'button:not([disabled])',
    'textarea:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    '[tabindex]:not([tabindex="-1"])'
  ].join(',');

  var activeDrawer = null;
  var activeTrigger = null;

  function getDrawerFromTrigger(trigger) {
    var drawerId = trigger.getAttribute('aria-controls');
    return drawerId ? document.getElementById(drawerId) : null;
  }

  function getFocusableElements(container) {
    return Array.prototype.slice.call(container.querySelectorAll(focusableSelector))
      .filter(function (element) {
        return element.getClientRects().length > 0 || element === document.activeElement;
      });
  }

  function setTriggerState(trigger, expanded) {
    if (trigger) {
      trigger.setAttribute('aria-expanded', expanded ? 'true' : 'false');
    }
  }

  function closeDrawer(restoreFocus) {
    if (!activeDrawer) {
      return;
    }

    var drawer = activeDrawer;
    var trigger = activeTrigger;
    drawer.hidden = true;
    activeDrawer = null;
    activeTrigger = null;
    document.body.classList.remove('help-drawer-open');
    setTriggerState(trigger, false);

    if (restoreFocus && trigger && typeof trigger.focus === 'function') {
      trigger.focus();
    }
  }

  function openDrawer(trigger) {
    var drawer = getDrawerFromTrigger(trigger);
    if (!drawer) {
      return;
    }

    if (activeDrawer && activeDrawer !== drawer) {
      closeDrawer(false);
    }

    activeDrawer = drawer;
    activeTrigger = trigger;
    drawer.hidden = false;
    document.body.classList.add('help-drawer-open');
    setTriggerState(trigger, true);

    window.requestAnimationFrame(function () {
      var panel = drawer.querySelector('.help-drawer-panel');
      var closeButton = drawer.querySelector('[data-help-close]:not(.help-drawer-backdrop)');
      (closeButton || panel || drawer).focus();
    });
  }

  function activateTopic(tab) {
    var drawer = tab.closest('[data-help-drawer]');
    if (!drawer) {
      return;
    }

    var topic = tab.getAttribute('data-help-topic');
    var tabs = drawer.querySelectorAll('[data-help-topic]');
    var panels = drawer.querySelectorAll('[data-help-topic-panel]');

    Array.prototype.forEach.call(tabs, function (item) {
      item.setAttribute('aria-selected', item === tab ? 'true' : 'false');
      item.setAttribute('tabindex', item === tab ? '0' : '-1');
    });

    Array.prototype.forEach.call(panels, function (panel) {
      panel.hidden = panel.getAttribute('data-help-topic-panel') !== topic;
    });
  }

  function moveTopicFocus(tab, direction) {
    var drawer = tab.closest('[data-help-drawer]');
    var tabs = drawer ? Array.prototype.slice.call(drawer.querySelectorAll('[data-help-topic]')) : [];
    var index = tabs.indexOf(tab);
    var nextIndex;

    if (!tabs.length || index < 0) {
      return;
    }

    if (direction === 'first') {
      nextIndex = 0;
    } else if (direction === 'last') {
      nextIndex = tabs.length - 1;
    } else {
      nextIndex = (index + direction + tabs.length) % tabs.length;
    }

    tabs[nextIndex].focus();
    activateTopic(tabs[nextIndex]);
  }

  function trapFocus(event) {
    if (!activeDrawer || event.key !== 'Tab') {
      return;
    }

    var focusable = getFocusableElements(activeDrawer);
    var first = focusable[0];
    var last = focusable[focusable.length - 1];

    if (!first || !last) {
      event.preventDefault();
      activeDrawer.querySelector('.help-drawer-panel').focus();
      return;
    }

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  function initializeTopics(drawer) {
    var tabs = drawer.querySelectorAll('[data-help-topic]');
    Array.prototype.forEach.call(tabs, function (tab, index) {
      tab.setAttribute('tabindex', index === 0 ? '0' : '-1');
    });
  }

  document.addEventListener('click', function (event) {
    var trigger = event.target.closest('[data-help-trigger]');
    var closeButton = event.target.closest('[data-help-close]');
    var topic = event.target.closest('[data-help-topic]');

    if (trigger) {
      event.preventDefault();
      if (activeDrawer === getDrawerFromTrigger(trigger)) {
        closeDrawer(true);
      } else {
        openDrawer(trigger);
      }
      return;
    }

    if (closeButton && closeButton.closest('[data-help-drawer]')) {
      event.preventDefault();
      closeDrawer(true);
      return;
    }

    if (topic) {
      event.preventDefault();
      activateTopic(topic);
    }
  });

  document.addEventListener('keydown', function (event) {
    var topic = event.target.closest ? event.target.closest('[data-help-topic]') : null;

    if (event.key === 'Escape' && activeDrawer) {
      event.preventDefault();
      closeDrawer(true);
      return;
    }

    trapFocus(event);

    if (!topic) {
      return;
    }

    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault();
      moveTopicFocus(topic, 1);
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault();
      moveTopicFocus(topic, -1);
    } else if (event.key === 'Home') {
      event.preventDefault();
      moveTopicFocus(topic, 'first');
    } else if (event.key === 'End') {
      event.preventDefault();
      moveTopicFocus(topic, 'last');
    }
  });

  document.addEventListener('DOMContentLoaded', function () {
    Array.prototype.forEach.call(document.querySelectorAll('[data-help-drawer]'), initializeTopics);
  });
})();
