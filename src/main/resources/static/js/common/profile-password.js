(() => {
  'use strict';

  const form = document.getElementById('profile-password-form');
  if (!form) return;

  const currentInput = form.querySelector('#current-password');
  const newInput = form.querySelector('#new-password');
  const confirmInput = form.querySelector('#confirm-password');
  const submitButton = form.querySelector('#change-password-submit');
  const feedback = form.querySelector('#profile-password-feedback');
  const ruleElements = new Map(
    Array.from(form.querySelectorAll('[data-password-rule]'))
      .map((element) => [element.dataset.passwordRule, element])
  );

  const evaluate = (current, next, confirmation) => ({
    length: next.length >= 8 && next.length <= 72,
    lowercase: /[a-z]/.test(next),
    uppercase: /[A-Z]/.test(next),
    number: /\d/.test(next),
    special: /[^A-Za-z0-9\s]/.test(next),
    'no-space': next.length > 0 && !/\s/.test(next),
    different: current.length > 0 && next.length > 0 && next !== current,
    match: next.length > 0 && confirmation.length > 0 && next === confirmation
  });

  const setRuleState = (name, passed, touched) => {
    const element = ruleElements.get(name);
    if (!element) return;
    element.classList.toggle('is-valid', touched && passed);
    element.classList.toggle('is-invalid', touched && !passed);
  };

  const validate = () => {
    const current = currentInput.value;
    const next = newInput.value;
    const confirmation = confirmInput.value;
    const states = evaluate(current, next, confirmation);
    const nextTouched = next.length > 0;

    Object.entries(states).forEach(([name, passed]) => {
      const touched = name === 'different'
        ? current.length > 0 && nextTouched
        : name === 'match'
          ? nextTouched && confirmation.length > 0
          : nextTouched;
      setRuleState(name, passed, touched);
    });

    const valid = current.length > 0
      && confirmation.length > 0
      && Object.values(states).every(Boolean);
    submitButton.disabled = !valid;
    return valid;
  };

  const showFeedback = (message, type) => {
    feedback.textContent = message || '';
    feedback.hidden = !message;
    feedback.className = `profile-password-feedback${type ? ` is-${type}` : ''}`;
    feedback.setAttribute('role', type === 'error' ? 'alert' : 'status');
  };

  [currentInput, newInput, confirmInput].forEach((input) => {
    input.addEventListener('input', () => {
      showFeedback('', '');
      validate();
    });
  });

  form.querySelectorAll('[data-profile-password-toggle]').forEach((button) => {
    button.addEventListener('click', () => {
      const input = form.querySelector(`#${button.dataset.profilePasswordToggle}`);
      if (!input) return;

      const showing = input.type === 'text';
      input.type = showing ? 'password' : 'text';
      button.textContent = showing ? 'Show' : 'Hide';
      button.setAttribute('aria-pressed', String(!showing));
      button.setAttribute('aria-label', `${showing ? 'Show' : 'Hide'} ${input.id.replaceAll('-', ' ')}`);
    });
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!validate() || submitButton.disabled) return;

    submitButton.disabled = true;
    submitButton.setAttribute('aria-busy', 'true');
    showFeedback('Changing password…', 'pending');

    try {
      const response = await fetch('/api/profile/password', {
        method: 'PUT',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
          currentPassword: currentInput.value,
          newPassword: newInput.value,
          confirmPassword: confirmInput.value
        })
      });
      const body = await response.json().catch(() => null);
      if (!response.ok || !body || body.code >= 400) {
        throw new Error(body?.message || 'Unable to change password.');
      }

      form.reset();
      ruleElements.forEach((element) => element.classList.remove('is-valid', 'is-invalid'));
      showFeedback(body.message || 'Password changed successfully', 'success');
      currentInput.focus();
    } catch (error) {
      showFeedback(error.message || 'Unable to change password.', 'error');
    } finally {
      submitButton.removeAttribute('aria-busy');
      validate();
    }
  });

  validate();
})();
