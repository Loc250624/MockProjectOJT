package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;
import com.ojtsu26.elearning.service.CodeJudgeAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnavailableCodeJudgeAdapter implements CodeJudgeAdapter {
    @Override
    public JudgeOutcome judge(CodingAssignment assignment, Submission submission) {
        return new JudgeOutcome(
                CodeJudgeStatus.UNAVAILABLE,
                testcaseCount(assignment),
                0,
                List.of(),
                "Code judge is not configured. Set app.code-judge.endpoint to an external sandbox service.",
                0L
        );
    }

    private int testcaseCount(CodingAssignment assignment) {
        return assignment == null || assignment.getTestcases() == null ? 0 : assignment.getTestcases().size();
    }
}
