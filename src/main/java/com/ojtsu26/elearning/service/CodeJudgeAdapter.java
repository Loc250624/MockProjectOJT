package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;

import java.util.List;

public interface CodeJudgeAdapter {
    JudgeOutcome judge(CodingAssignment assignment, Submission submission);

    record JudgeOutcome(CodeJudgeStatus status, int totalTests, int passedTests, List<JudgeCaseOutcome> cases,
                        String outputLog, long executionTimeMs) {
        public boolean passed() {
            return status == CodeJudgeStatus.PASSED;
        }
    }

    record JudgeCaseOutcome(Integer testcaseId, boolean passed) {
    }
}
