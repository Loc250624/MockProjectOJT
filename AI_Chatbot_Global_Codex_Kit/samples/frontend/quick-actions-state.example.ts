export function createQuickActionState(storage: Storage, conversationId: string) {
  const key = `ai-chatbot:${conversationId}:quick-actions-consumed`;

  return {
    isConsumed(): boolean {
      return storage.getItem(key) === "true";
    },
    consume(): void {
      storage.setItem(key, "true");
    },
    resetForNewConversation(): void {
      storage.removeItem(key);
    }
  };
}

/*
Use sessionStorage by default.
Consume before starting the API request so an API failure cannot make the
quick actions reappear.
*/
