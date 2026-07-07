package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.service.CodeJudgeAdapter;
import org.springframework.stereotype.Component;

@Component
public class MockCodeJudgeAdapter implements CodeJudgeAdapter {
    @Override
    public JudgeOutcome judge(CodingAssignment assignment, Submission submission) {
        int total = assignment.getTestcases() == null ? 0 : assignment.getTestcases().size();
        boolean hasCode = submission.getCodeContent() != null && !submission.getCodeContent().isBlank();
        int passed = hasCode ? total : 0;
        String log = hasCode
                ? "Mock judge accepted the submission. TODO: integrate Judge0 or Docker sandbox before executing user code."
                : "Mock judge rejected the submission because code content is empty.";
        return new JudgeOutcome(hasCode, total, passed, log, 25L);
    }
}
