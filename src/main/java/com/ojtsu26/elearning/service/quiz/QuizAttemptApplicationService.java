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

        int maxAttempts = Objects.requireNonNullElse(quiz.getMaxAttempts(), 1);
        long usedAttempts = attemptRepository.countByQuizIdAndStudentIdAndStatusNot(
                quizId, student.getId(), QuizAttemptStatus.DRAFT);
        if (usedAttempts >= maxAttempts) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Maximum quiz attempts reached");
        }

        QuizAttempt attempt = attemptRepository.save(QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.DRAFT)
                .build());
        List<QuizAttemptQuestion> assigned = assignmentService.assign(attempt);
        return session(attempt, assigned);
    }

    @Transactional(readOnly = true)
    public AttemptSession getOwnedAttempt(Integer attemptId) {
        QuizAttempt attempt = requireOwnedAttempt(attemptId);
        return session(attempt, assignmentService.loadAssigned(attemptId));
    }

    @Transactional
    public AttemptSession saveTextAnswers(Integer attemptId, Map<Integer, String> answers) {
        QuizAttempt attempt = requireDraft(attemptId);
        upsert(attempt, textAnswerInputs(answers));
        return session(attempt, assignmentService.loadAssigned(attemptId));
    }

    @Transactional
    public AttemptSession saveSelectedAnswers(Integer attemptId, Map<Integer, List<Integer>> answers) {
        QuizAttempt attempt = requireDraft(attemptId);
        upsert(attempt, selectedAnswerInputs(answers));
        return session(attempt, assignmentService.loadAssigned(attemptId));
    }

    @Transactional
    public AttemptSession submitTextAnswers(Integer attemptId, Map<Integer, String> answers) {
        QuizAttempt attempt = requireOwnedAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            return session(attempt, assignmentService.loadAssigned(attemptId));
        }
        upsert(attempt, textAnswerInputs(answers));
        return gradeAndSubmit(attempt);
    }

    @Transactional
    public AttemptSession submitSelectedAnswers(Integer attemptId, Map<Integer, List<Integer>> answers) {
        QuizAttempt attempt = requireOwnedAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            return session(attempt, assignmentService.loadAssigned(attemptId));
        }
        upsert(attempt, selectedAnswerInputs(answers));
        return gradeAndSubmit(attempt);
    }

    private AttemptSession gradeAndSubmit(QuizAttempt attempt) {
        List<QuizAttemptQuestion> assigned = assignmentService.loadAssigned(attempt.getId());
        QuizGradingService.GradeResult grade =
                gradingService.grade(assigned, answerRepository.findByAttemptId(attempt.getId()));
        attempt.setScore(grade.percentage());
        attempt.setTotalPoints(grade.totalPoints());
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(QuizAttemptStatus.GRADED);
        attemptRepository.save(attempt);
        return session(attempt, assigned);
    }

    private void upsert(QuizAttempt attempt, Map<Integer, AnswerInput> inputs) {
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
            answerRepository.save(answer);
        }
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
        Map<Integer, QuizAnswer> answers = answerRepository.findByAttemptId(attempt.getId()).stream()
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
