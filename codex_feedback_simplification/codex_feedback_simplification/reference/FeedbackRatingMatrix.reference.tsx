import React from "react";

// REFERENCE ONLY: adapt names, design system and form library to the real project.

type RatingKey =
  | "courseContent"
  | "instructorSupport"
  | "learningExperience"
  | "platformUsability"
  | "assessmentExperience"
  | "overallSatisfaction";

type RatingValues = Record<RatingKey, number>;

type Props = {
  value: RatingValues;
  onChange: (key: RatingKey, score: number) => void;
  disabled?: boolean;
};

const CRITERIA: Array<{ key: RatingKey; label: string; emphasized?: boolean }> = [
  { key: "courseContent", label: "Course Content" },
  { key: "instructorSupport", label: "Instructor Support" },
  { key: "learningExperience", label: "Learning Experience" },
  { key: "platformUsability", label: "Platform Usability" },
  { key: "assessmentExperience", label: "Assessment Experience" },
  { key: "overallSatisfaction", label: "Overall Satisfaction", emphasized: true },
];

export function FeedbackRatingMatrix({ value, onChange, disabled = false }: Props) {
  return (
    <section className="feedback-rating-card" aria-labelledby="feedback-rating-title">
      <header className="feedback-rating-card__header">
        <div>
          <p className="feedback-rating-card__eyebrow">RATING MATRIX</p>
          <h2 id="feedback-rating-title">Rate your experience</h2>
        </div>
        <span className="feedback-rating-card__badge" aria-label="Rating scale from 1 to 5">
          1–5
        </span>
      </header>

      <div className="feedback-rating-grid">
        {CRITERIA.map(({ key, label, emphasized }) => (
          <fieldset
            key={key}
            className={`feedback-rating-item${emphasized ? " feedback-rating-item--wide" : ""}`}
          >
            <legend>{label}</legend>
            <div className="feedback-stars" role="group" aria-label={`${label} rating`}>
              {[1, 2, 3, 4, 5].map((score) => {
                const selected = score <= value[key];

                return (
                  <button
                    key={score}
                    type="button"
                    className="feedback-star"
                    aria-label={`${score} out of 5 for ${label}`}
                    aria-pressed={value[key] === score}
                    data-active={selected || undefined}
                    disabled={disabled}
                    onClick={() => onChange(key, score)}
                  >
                    <span aria-hidden="true">★</span>
                  </button>
                );
              })}
            </div>
          </fieldset>
        ))}
      </div>
    </section>
  );
}
