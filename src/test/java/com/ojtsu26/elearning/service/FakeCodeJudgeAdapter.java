package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;

import java.util.List;

class FakeCodeJudgeAdapter implements CodeJudgeAdapter {
    private JudgeOutcome outcome = new JudgeOutcome(
            CodeJudgeStatus.UNAVAILABLE,
            0,
            0,
            List.of(),
            "Fake adapter unavailable by default.",
            0L
    );

    void setOutcome(JudgeOutcome outcome) {
        this.outcome = outcome;
    }

    @Override
    public JudgeOutcome judge(CodingAssignment assignment, Submission submission) {
        return outcome;
    }
}
