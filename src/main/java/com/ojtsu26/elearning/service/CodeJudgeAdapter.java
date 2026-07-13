package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Submission;

public interface CodeJudgeAdapter {
    JudgeOutcome judge(CodingAssignment assignment, Submission submission);

    record JudgeOutcome(boolean passed, int totalTests, int passedTests, String outputLog, long executionTimeMs) {
    }
}
