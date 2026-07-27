// REFERENCE ONLY: inspect the real backend contract before applying.

type FeedbackRatings = {
  courseContent: number;
  instructorSupport: number;
  learningExperience: number;
  platformUsability: number;
  assessmentExperience: number;
  overallSatisfaction: number;
};

export type FeedbackFormValues = FeedbackRatings & {
  comment?: string;
};

export type FeedbackRequest = FeedbackFormValues & {
  // Keep this only when the current backend still requires it.
  category?: string;
};

export function buildFeedbackRequest(
  values: FeedbackFormValues,
  backendRequiredCategory?: string,
): FeedbackRequest {
  const request: FeedbackRequest = {
    ...values,
    comment: values.comment?.trim() || undefined,
  };

  if (backendRequiredCategory) {
    request.category = backendRequiredCategory;
  }

  return request;
}
