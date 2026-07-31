package com.ojtsu26.elearning.service.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizAttemptApplicationService {
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;
    private final QuizQuestionAssignmentService assignmentService;
    private final QuizGradingService gradingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AttemptSession startOrResume(Integer quizId) {
        User student = currentUserService.getCurrentUser();
        Quiz quiz = quizRepository.findByIdForAttemptStart(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Quiz not found"));
        requireEnrollment(student, quiz);
        if (quiz.getStatus() != null && quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz is not published");
        }
        quiz.setDurationMinutes(QuizRules.DURATION_MINUTES);

        Optional<QuizAttempt> draft = attemptRepository
                .findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                        quizId, student.getId(), QuizAttemptStatus.DRAFT);
        if (draft.isPresent()) {
            QuizAttempt existing = draft.get();
            List<QuizAttemptQuestion> assigned = assignmentService.loadAssigned(existing.getId());
            if (assigned.isEmpty()) {
                assigned = assignmentService.assign(existing);
            }
            return session(existing, assigned);
        }

        QuizAttempt attempt = attemptRepository.save(QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.DRAFT)
                .build());
        List<QuizAttemptQuestion> assigned = assignmentService.assign(attempt);
        return session(attempt, assigned);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttemptSession getOwnedAttempt(Integer attemptId) {
        QuizAttempt attempt = requireOwnedAttempt(attemptId);
        List<QuizAttemptQuestion> assigned = assignmentService.loadAssigned(attemptId);
        List<QuizAnswer> answers = answerRepository.findByAttemptId(attemptId);
        reconcileStoredGrade(attempt, assigned, answers);
        return session(attempt, assigned, answers);
    }

    @Transactional
    public AttemptSession saveTextAnswers(Integer attemptId, Map<Integer, String> answers) {
        QuizAttempt attempt = requireDraft(attemptId);
        List<QuizAnswer> persistedAnswers = upsert(attempt, textAnswerInputs(answers));
        return session(attempt, assignmentService.loadAssigned(attemptId), persistedAnswers);
    }

    @Transactional
    public AttemptSession saveSelectedAnswers(Integer attemptId, Map<Integer, List<Integer>> answers) {
        QuizAttempt attempt = requireDraft(attemptId);
        List<QuizAnswer> persistedAnswers = upsert(attempt, selectedAnswerInputs(answers));
        return session(attempt, assignmentService.loadAssigned(attemptId), persistedAnswers);
    }

    @Transactional
    public AttemptSession submitTextAnswers(Integer attemptId, Map<Integer, String> answers) {
        QuizAttempt attempt = requireOwnedAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            return session(attempt, assignmentService.loadAssigned(attemptId));
        }
        List<QuizAnswer> persistedAnswers = upsert(attempt, textAnswerInputs(answers));
        return gradeAndSubmit(attempt, persistedAnswers);
    }

    @Transactional
    public AttemptSession submitSelectedAnswers(Integer attemptId, Map<Integer, List<Integer>> answers) {
        QuizAttempt attempt = requireOwnedAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            return session(attempt, assignmentService.loadAssigned(attemptId));
        }
        List<QuizAnswer> persistedAnswers = upsert(attempt, selectedAnswerInputs(answers));
        return gradeAndSubmit(attempt, persistedAnswers);
    }

    private AttemptSession gradeAndSubmit(QuizAttempt attempt, List<QuizAnswer> answers) {
        List<QuizAttemptQuestion> assigned = assignmentService.loadAssigned(attempt.getId());
        requireCompleteAnswersOrExpired(attempt, assigned, answers);
        QuizGradingService.GradeResult grade = gradingService.grade(assigned, answers);
        attempt.setScore(grade.percentage());
        attempt.setTotalPoints(grade.totalPoints());
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(QuizAttemptStatus.GRADED);
        attemptRepository.save(attempt);
        return session(attempt, assigned, answers);
    }

    private void requireCompleteAnswersOrExpired(
            QuizAttempt attempt,
            List<QuizAttemptQuestion> assigned,
            List<QuizAnswer> answers) {
        Set<Integer> answeredQuestionIds = answers.stream()
                .filter(this::hasSubmittedAnswer)
                .map(answer -> answer.getQuestion().getId())
                .collect(Collectors.toSet());
        boolean complete = assigned.stream()
                .map(item -> item.getQuestion().getId())
                .allMatch(answeredQuestionIds::contains);
        if (!complete && !submissionWindowExpired(attempt)) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "All questions must be answered before submitting the quiz");
        }
    }

    private boolean hasSubmittedAnswer(QuizAnswer answer) {
        if (answer.getAnswerText() != null && !answer.getAnswerText().isBlank()) {
            return true;
        }
        String selected = answer.getSelectedOptionsJson();
        if (selected == null || selected.isBlank()) {
            return false;
        }
        try {
            var selectedOptions = objectMapper.readTree(selected);
            return selectedOptions.isArray() && !selectedOptions.isEmpty();
        } catch (JsonProcessingException ignored) {
            return false;
        }
    }

    private boolean submissionWindowExpired(QuizAttempt attempt) {
        if (attempt.getStartedAt() == null) {
            return false;
        }
        return !LocalDateTime.now().isBefore(
                attempt.getStartedAt().plusMinutes(QuizRules.DURATION_MINUTES));
    }

    private void reconcileStoredGrade(QuizAttempt attempt,
                                      List<QuizAttemptQuestion> assigned,
                                      List<QuizAnswer> answers) {
        if (attempt.getStatus() != QuizAttemptStatus.GRADED || assigned.isEmpty()) {
            return;
        }
        QuizGradingService.GradeResult grade = gradingService.grade(assigned, answers);
        if (sameValue(attempt.getScore(), grade.percentage())
                && sameValue(attempt.getTotalPoints(), grade.totalPoints())) {
            return;
        }
        attempt.setScore(grade.percentage());
        attempt.setTotalPoints(grade.totalPoints());
        attemptRepository.save(attempt);
    }

    private boolean sameValue(java.math.BigDecimal left, java.math.BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private List<QuizAnswer> upsert(QuizAttempt attempt, Map<Integer, AnswerInput> inputs) {
        List<QuizAttemptQuestion> assigned = assignmentService.loadAssigned(attempt.getId());
        Set<Integer> assignedIds = assigned.stream()
                .map(item -> item.getQuestion().getId())
                .collect(Collectors.toSet());
        for (Integer questionId : inputs.keySet()) {
            if (!assignedIds.contains(questionId)) {
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "Submitted answer was not assigned to this attempt");
            }
        }

        Map<Integer, QuizAnswer> existing = answerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));
        Map<Integer, Question> assignedQuestions = assigned.stream()
                .collect(Collectors.toMap(item -> item.getQuestion().getId(), QuizAttemptQuestion::getQuestion));
        for (Map.Entry<Integer, AnswerInput> entry : inputs.entrySet()) {
            QuizAnswer answer = existing.getOrDefault(entry.getKey(), QuizAnswer.builder()
                    .attempt(attempt)
                    .question(assignedQuestions.get(entry.getKey()))
                    .build());
            answer.setAnswerText(entry.getValue().text());
            answer.setSelectedOptionsJson(entry.getValue().selectedJson());
            existing.put(entry.getKey(), answerRepository.save(answer));
        }
        return List.copyOf(existing.values());
    }

    private Map<Integer, AnswerInput> textAnswerInputs(Map<Integer, String> answers) {
        Map<Integer, AnswerInput> inputs = new LinkedHashMap<>();
        Optional.ofNullable(answers).orElse(Map.of()).forEach((id, value) -> {
            if (id != null) {
                String normalized = value == null ? "" : value.trim();
                if (normalized.length() > 2000) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Answer is too long");
                }
                inputs.put(id, new AnswerInput(normalized, null));
            }
        });
        return inputs;
    }

    private Map<Integer, AnswerInput> selectedAnswerInputs(Map<Integer, List<Integer>> answers) {
        Map<Integer, AnswerInput> inputs = new LinkedHashMap<>();
        Optional.ofNullable(answers).orElse(Map.of()).forEach((id, value) -> {
            if (id != null) {
                inputs.put(id, new AnswerInput(null, writeJson(
                        Optional.ofNullable(value).orElse(List.of()))));
            }
        });
        return inputs;
    }

    private AttemptSession session(QuizAttempt attempt, List<QuizAttemptQuestion> assigned) {
        return session(attempt, assigned, answerRepository.findByAttemptId(attempt.getId()));
    }

    private AttemptSession session(QuizAttempt attempt,
                                   List<QuizAttemptQuestion> assigned,
                                   List<QuizAnswer> persistedAnswers) {
        Map<Integer, QuizAnswer> answers = persistedAnswers.stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getId(),
                        Function.identity(),
                        (first, second) -> second,
                        LinkedHashMap::new));
        return new AttemptSession(attempt, List.copyOf(assigned), Collections.unmodifiableMap(answers));
    }

    private QuizAttempt requireDraft(Integer attemptId) {
        QuizAttempt attempt = requireOwnedAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Submitted attempts cannot be edited");
        }
        return attempt;
    }

    private QuizAttempt requireOwnedAttempt(Integer attemptId) {
        QuizAttempt attempt = attemptRepository.findByIdWithQuizCourse(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Quiz attempt not found"));
        User student = currentUserService.getCurrentUser();
        if (attempt.getStudent() == null || !Objects.equals(attempt.getStudent().getId(), student.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz attempt access denied");
        }
        return attempt;
    }

    private void requireEnrollment(User student, Quiz quiz) {
        Integer courseId = quiz.getLesson().getCourse().getId();
        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "You are not enrolled in this course");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid quiz answer");
        }
    }

    private record AnswerInput(String text, String selectedJson) {
    }

    public record AttemptSession(QuizAttempt attempt,
                                 List<QuizAttemptQuestion> questions,
                                 Map<Integer, QuizAnswer> answers) {
    }
}
