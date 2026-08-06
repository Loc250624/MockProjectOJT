(() => {
  'use strict';

  const slider = document.querySelector('[data-auth-hero-slider]');
  if (!slider) return;

  const slides = Array.from(slider.querySelectorAll('.auth-hero-slide'));
  const dots = Array.from(slider.querySelectorAll('.auth-hero-dot'));
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

  if (slides.length < 2) return;

  const SLIDE_INTERVAL_MS = 6500;
  const TRANSITION_CLEANUP_MS = 1200;

  let activeIndex = Math.max(0, slides.findIndex((slide) => slide.classList.contains('is-active')));
  let intervalId = null;
  let cleanupId = null;

  const updateDots = (nextIndex) => {
    dots.forEach((dot, index) => {
      dot.classList.toggle('is-active', index === nextIndex);
    });
  };

  const resetToFirstSlide = () => {
    activeIndex = 0;
    slides.forEach((slide, index) => {
      slide.classList.toggle('is-active', index === 0);
      slide.classList.remove('is-exiting-right');
    });
    updateDots(0);
  };

  const showNextSlide = () => {
    const currentSlide = slides[activeIndex];
    const nextIndex = (activeIndex + 1) % slides.length;
    const nextSlide = slides[nextIndex];

    window.clearTimeout(cleanupId);

    currentSlide.classList.add('is-exiting-right');
    currentSlide.classList.remove('is-active');

    nextSlide.classList.remove('is-exiting-right');
    nextSlide.classList.add('is-active');

    activeIndex = nextIndex;
    updateDots(activeIndex);

    cleanupId = window.setTimeout(() => {
      slides.forEach((slide, index) => {
        if (index !== activeIndex) {
          slide.classList.remove('is-active', 'is-exiting-right');
        }
      });
    }, TRANSITION_CLEANUP_MS);
  };

  const start = () => {
    if (intervalId || document.hidden || reduceMotion.matches) return;
    intervalId = window.setInterval(showNextSlide, SLIDE_INTERVAL_MS);
  };

  const stop = () => {
    window.clearInterval(intervalId);
    intervalId = null;
  };

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) stop();
    else start();
  });

  reduceMotion.addEventListener?.('change', (event) => {
    if (event.matches) {
      stop();
      window.clearTimeout(cleanupId);
      resetToFirstSlide();
    } else {
      start();
    }
  });

  updateDots(activeIndex);
  start();
})();
