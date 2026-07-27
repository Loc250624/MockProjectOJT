(() => {
  const panel = document.getElementById("chat-panel");
  const launcher = document.getElementById("chat-launcher");
  const closeButton = document.getElementById("chat-close");
  const composer = document.getElementById("composer");
  const input = document.getElementById("message");
  const messages = document.getElementById("messages");
  const quickActions = document.getElementById("quick-actions");

  const conversationId =
    sessionStorage.getItem("ai-chatbot-conversation-id") || crypto.randomUUID();
  sessionStorage.setItem("ai-chatbot-conversation-id", conversationId);

  const consumedKey = `ai-chatbot:${conversationId}:quick-actions-consumed`;

  function consumeQuickActions() {
    sessionStorage.setItem(consumedKey, "true");
    quickActions?.remove();
  }

  function restoreQuickActionState() {
    if (sessionStorage.getItem(consumedKey) === "true") {
      quickActions?.remove();
    }
  }

  function appendUserMessage(text) {
    const article = document.createElement("article");
    article.className = "user-message";
    article.textContent = text;
    messages.append(article);
    messages.scrollTop = messages.scrollHeight;
  }

  function submitText(text) {
    const value = text.trim();
    if (!value) return;

    consumeQuickActions();
    appendUserMessage(value);
    input.value = "";

    // Reference behavior only:
    // call the project's existing chatbot API with message, conversationId
    // and allowlisted pageContext. Do not send raw DOM or client-declared role.
  }

  quickActions?.addEventListener("click", event => {
    const button = event.target.closest("button[data-prompt]");
    if (!button) return;
    submitText(button.dataset.prompt || "");
  });

  composer.addEventListener("submit", event => {
    event.preventDefault();
    submitText(input.value);
  });

  launcher.addEventListener("click", () => {
    panel.hidden = false;
    launcher.setAttribute("aria-expanded", "true");
    input.focus();
  });

  closeButton.addEventListener("click", () => {
    panel.hidden = true;
    launcher.setAttribute("aria-expanded", "false");
    launcher.focus();
  });

  document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !panel.hidden) {
      closeButton.click();
    }
  });

  restoreQuickActionState();
})();
