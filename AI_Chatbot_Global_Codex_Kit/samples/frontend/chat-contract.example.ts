export type AiChatbotPageContext = {
  path: string;
  pageKey?: string;
  title?: string;
  entityType?: "course" | "lesson" | "quiz" | "coding" | "certificate" | "blog" | "profile";
  entityId?: string;
};

export type AiChatbotRequest = {
  message: string;
  conversationId: string;
  lessonId?: number;
  pageContext: AiChatbotPageContext;
  recentMessages?: Array<{
    role: "user" | "assistant";
    content: string;
  }>;
};

export type AiChatbotResponse = {
  conversationId: string;
  answer: string;
  scope?: "PUBLIC" | "SITE" | "LESSON";
  usedPageContext?: boolean;
};
