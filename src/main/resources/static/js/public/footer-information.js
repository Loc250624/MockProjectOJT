(function () {
    "use strict";

    function openExistingChatbot() {
        var chatbotToggle = document.querySelector("[data-ai-chatbot-toggle]");
        if (!chatbotToggle) {
            return;
        }

        if (chatbotToggle.getAttribute("aria-expanded") !== "true") {
            chatbotToggle.click();
        }

        window.setTimeout(function () {
            var input = document.querySelector("[data-ai-chatbot-input]");
            if (input) {
                input.focus();
            }
        }, 0);
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-open-ai-chatbot]").forEach(function (button) {
            button.addEventListener("click", openExistingChatbot);
        });
    });
})();
