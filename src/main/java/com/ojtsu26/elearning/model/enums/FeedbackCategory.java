package com.ojtsu26.elearning.model.enums;

public enum FeedbackCategory {
    COURSE_CONTENT("Course Content"),
    INSTRUCTOR("Instructor"),
    ASSESSMENT("Assessment"),
    PLATFORM_UI("Platform/UI"),
    TECHNICAL_ISSUE("Technical Issue"),
    OTHER("Other");

    private final String displayName;

    FeedbackCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
