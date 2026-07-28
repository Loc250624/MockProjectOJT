package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizQuestionDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizAttemptQuestion;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.StudentAssessmentService;
import com.ojtsu26.elearning.service.StudentLearningService;
import com.ojtsu26.elearning.service.quiz.QuizAttemptApplicationService;
import com.ojtsu26.elearning.service.quiz.QuizQuestionTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAssessmentServiceImpl implements StudentAssessmentService {
    private static final String STATE_DRAFT = "DRAFT";
    private static final String STATE_SUBMITTED = "SUBMITTED";
    private static final String QUESTION_IDS_FIELD = "questionIds";
    private static final int QUIZ_QUESTION_LIMIT = 10;
    private static final int MAX_ANSWER_LENGTH = 2000;
    private static final int QUIZ_QUESTION_COUNT = 10;

    private final StudentLearningService studentLearningService;
    private final CurrentUserService currentUserService;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Optional only for direct-constructor legacy tests. In the Spring runtime
     * this bean is always injected and is the sole writer for new quiz attempts.
     */
    @Autowired(required = false)
    private QuizAttemptApplicationService quizAttemptApplicationService;

    @Override
    @Transactional
    public StudentQuizAttemptDTO getOrStartQuiz(Integer courseId, Integer lessonId) {
        Lesson lesson = requireAccessibleQuizLesson(courseId, lessonId);
        User student = currentUserService.getCurrentUser();
        Quiz quiz = quizRepository.findByLessonId(lessonId).orElse(null);
        if (quiz == null) {
            return unavailableQuiz(courseId, lessonId, "This quiz is not configured yet.");
        }
        if (quizAttemptApplicationService != null) {
            return toCanonicalQuizDto(
                    courseId,
                    lessonId,
                    quizAttemptApplicationService.startOrResume(quiz.getId()),
                    null);
        }

        java.util.Optional<Submission> currentAttempt =
                currentDraftAttempt(student.getId(), lessonId);
        List<Question> questions;
        Submission attempt;
        if (currentAttempt.isPresent()) {
            attempt = currentAttempt.get();
            questions = selectedQuestionsForAttempt(quiz, attempt);
        } else {
            questions = randomQuizQuestions(quiz.getId());
            if (questions.isEmpty()) {
                return unavailableQuiz(courseId, lessonId, "This quiz has no questions yet.");
            }
            attempt = createQuizAttempt(student, lesson, questions);
        }
        return toQuizDto(courseId, lessonId, quiz, questions, attempt);
    }

    @Override
    @Transactional
    public StudentQuizAttemptDTO saveQuizDraft(Integer courseId,
                                               Integer lessonId,
                                               StudentQuizSubmissionRequestDTO request) {
        requireAccessibleQuizLesson(courseId, lessonId);
        Quiz quiz = requireQuiz(lessonId);
        if (quizAttemptApplicationService != null) {
            requireRequest(request);
            QuizAttemptApplicationService.AttemptSession session =
                    quizAttemptApplicationService.saveTextAnswers(
                            request.getAttemptId(), sanitizeAnswers(request.getAnswers()));
            requireAttemptQuiz(session, quiz);
            return toCanonicalQuizDto(courseId, lessonId, session, null);
        }

        Submission attempt = requirePendingAttempt(request, lessonId);
        List<Question> questions = selectedQuestionsForAttempt(quiz, attempt);
        Map<Integer, String> answers = sanitizeAnswers(request.getAnswers());
        validateQuestionMembership(answers, questions);

        attempt.setSubmittedContent(writeQuizPayload(STATE_DRAFT, answers, questions));
        submissionRepository.save(attempt);
        return toQuizDto(courseId, lessonId, quiz, questions, attempt);
    }

    @Override
    @Transactional
    public StudentQuizAttemptDTO submitQuiz(Integer courseId,
                                            Integer lessonId,
                                            StudentQuizSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleQuizLesson(courseId, lessonId);
        Quiz quiz = requireQuiz(lessonId);
        Submission attempt = requirePendingAttempt(request, lessonId);
        List<Question> questions = selectedQuestionsForAttempt(quiz, attempt);
        Map<Integer, String> answers = sanitizeAnswers(request.getAnswers());
        validateQuestionMembership(answers, questions);

        BigDecimal score = scoreQuiz(answers, questions);
        boolean passed = score.compareTo(defaultPassingScore(quiz)) >= 0;
        attempt.setScore(score);
        attempt.setStatus(passed ? SubmissionStatus.PASSED : SubmissionStatus.FAILED);
        attempt.setSubmittedContent(writeQuizPayload(STATE_SUBMITTED, answers, questions));
        submissionRepository.save(attempt);
        LearningProgressDTO learningProgress =
                passed ? markAssessmentProgressCompleted(courseId, lesson) : null;
        return toQuizDto(courseId, lessonId, quiz, questions, attempt, learningProgress);
    }

    private Lesson requireAccessibleQuizLesson(Integer courseId, Integer lessonId) {
        StudentLearningLessonDTO opened = studentLearningService.openLesson(courseId, lessonId);
        if (opened == null || opened.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Lesson is not available in this course.");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Lesson is not available in this course."));
        if (lesson.getCourse() == null
                || !Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Lesson is not available in this course.");
        }
        if (lesson.getType() != LessonType.QUIZ && lesson.getQuiz() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This lesson is not a quiz.");
        }
        return lesson;
    }

    private Quiz requireQuiz(Integer lessonId) {
        return quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "This quiz is not configured yet."));
    }

    private Submission createQuizAttempt(User student,
                                         Lesson lesson,
                                         List<Question> questions) {
        Submission attempt = Submission.builder()
                .student(student)
                .lesson(lesson)
                .status(SubmissionStatus.PENDING_REVIEW)
                .submittedContent(writeQuizPayload(STATE_DRAFT, Map.of(), questions))
                .build();
        return submissionRepository.save(attempt);
    }

    private List<Question> selectedQuestionsForAttempt(Quiz quiz, Submission attempt) {
        Map<String, Object> payload = readPayload(attempt.getSubmittedContent());
        List<Integer> selectedIds = readQuestionIds(payload);
        if (!selectedIds.isEmpty()) {
            Map<Integer, Question> questionsById = questionRepository
                    .findByQuizIdAndIdIn(quiz.getId(), selectedIds)
                    .stream()
                    .collect(Collectors.toMap(Question::getId, question -> question));
            List<Question> selected = selectedIds.stream()
                    .map(questionsById::get)
                    .filter(Objects::nonNull)
                    .toList();
            if (!selected.isEmpty()) {
                return selected;
            }
        }

        List<Question> selected = randomQuizQuestions(quiz.getId());
        if (selected.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "This quiz has no questions yet.");
        }
        Map<Integer, String> selectedAnswers = filterAnswers(readAnswers(payload), selected);
        attempt.setSubmittedContent(writeQuizPayload(STATE_DRAFT, selectedAnswers, selected));
        submissionRepository.save(attempt);
        return selected;
    }

    private List<Question> randomQuizQuestions(Integer quizId) {
        return questionRepository
                .findRandomByQuizId(quizId, PageRequest.of(0, QUIZ_QUESTION_LIMIT))
                .stream()
                .limit(QUIZ_QUESTION_LIMIT)
                .toList();
    }

    private String writeQuizPayload(String state,
                                    Map<Integer, String> answers,
                                    List<Question> questions) {
        return writePayload(Map.of(
                "type", "QUIZ",
                "state", state,
                "answers", answers,
                QUESTION_IDS_FIELD, questions.stream().map(Question::getId).toList()
        ));
    }

    private Map<Integer, String> filterAnswers(Map<Integer, String> answers,
                                               List<Question> questions) {
        Set<Integer> selectedIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toSet());
        Map<Integer, String> filtered = new LinkedHashMap<>();
        answers.forEach((questionId, answer) -> {
            if (selectedIds.contains(questionId)) {
                filtered.put(questionId, answer);
            }
        });
        return filtered;
    }

    private java.util.Optional<Submission> currentDraftAttempt(Integer studentId,
                                                               Integer lessonId) {
        return submissionRepository
                .findDraftsForUpdate(studentId, lessonId, SubmissionStatus.PENDING_REVIEW)
                .stream()
                .findFirst();
    }

    private Submission requirePendingAttempt(StudentQuizSubmissionRequestDTO request,
                                             Integer lessonId) {
        if (request == null || request.getAttemptId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Attempt is required.");
        }
        Submission attempt = submissionRepository
                .findOwnedLessonSubmission(
                        request.getAttemptId(),
                        currentUserService.getCurrentUser().getId(),
                        lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "Quiz attempt is not available."));
        if (attempt.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "This quiz attempt has already been submitted.");
        }
        return attempt;
    }

    private Map<Integer, String> sanitizeAnswers(Map<Integer, String> answers) {
        Map<Integer, String> sanitized = new LinkedHashMap<>();
        if (answers == null) {
            return sanitized;
        }
        answers.forEach((questionId, answer) -> {
            if (questionId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Question is required.");
            }
            String normalized = answer == null ? "" : answer.trim();
            if (normalized.length() > MAX_ANSWER_LENGTH) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Answer is too long.");
            }
            sanitized.put(questionId, normalized);
        });
        return sanitized;
    }

    private void validateQuestionMembership(Map<Integer, String> answers,
                                            List<Question> questions) {
        Set<Integer> questionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toSet());
        for (Integer answeredQuestionId : answers.keySet()) {
            if (!questionIds.contains(answeredQuestionId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Submitted answer does not belong to this quiz.");
            }
        }
    }

    private BigDecimal scoreQuiz(Map<Integer, String> answers,
                                 List<Question> questions) {
        long correct = questions.stream()
                .filter(question -> {
                    String actual = answers.getOrDefault(question.getId(), "").trim();
                    return !actual.isEmpty() && correctAnswerTokens(question).stream()
                            .anyMatch(expected -> expected.equalsIgnoreCase(actual));
                })
                .count();
        return BigDecimal.valueOf(correct)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP);
    }

    private Set<String> correctAnswerTokens(Question question) {
        Set<String> tokens = AssessmentOptionCodec.readOptions(question, objectMapper).stream()
                .filter(AssessmentOptionCodec.ParsedOption::correct)
                .flatMap(option -> java.util.stream.Stream.of(
                        String.valueOf(option.id()), option.content()))
                .collect(Collectors.toSet());
        String rawCorrect = question.getCorrectAnswer();
        if (rawCorrect != null && !rawCorrect.isBlank()) {
            for (String part : rawCorrect.split(",")) {
                String token = part == null ? "" : part.trim();
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    private BigDecimal defaultPassingScore(Quiz quiz) {
        return quiz.getPassingScore() == null ? BigDecimal.ZERO : quiz.getPassingScore();
    }

    private LearningProgressDTO markAssessmentProgressCompleted(Integer courseId,
                                                                Lesson lesson) {
        User student = currentUserService.getCurrentUser();
        CourseEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdForUpdate(student.getId(), courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "You are not enrolled in this course."));
        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentIdAndLessonIdForUpdate(enrollment.getId(), lesson.getId())
                .orElseGet(() -> LessonProgress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .isCompleted(false)
                        .watchedSeconds(0)
                        .lastPositionSeconds(0)
                        .maxReachedSeconds(0)
                        .build());
        if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }
        progress.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);
        return assessmentLearningProgress(courseId, lesson, progress);
    }

    private LearningProgressDTO assessmentLearningProgress(Integer courseId,
                                                           Lesson lesson,
                                                           LessonProgress progress) {
        StudentLearningLessonDTO lessonState =
                studentLearningService.openLesson(courseId, lesson.getId());
        LearningProgressDTO courseProgress =
                lessonState == null ? null : lessonState.getCourseProgress();
        return LearningProgressDTO.builder()
                .courseId(courseId)
                .lessonId(lesson.getId())
                .enrollmentId(progress.getEnrollment() == null
                        ? null
                        : progress.getEnrollment().getId())
                .completedLessons(courseProgress == null
                        ? null
                        : courseProgress.getCompletedLessons())
                .totalLessons(courseProgress == null
                        ? null
                        : courseProgress.getTotalLessons())
                .progressPercentage(courseProgress == null
                        ? null
                        : courseProgress.getProgressPercentage())
                .completed(Boolean.TRUE.equals(progress.getIsCompleted()))
                .lessonCompleted(Boolean.TRUE.equals(progress.getIsCompleted()))
                .courseCompleted(courseProgress == null
                        ? null
                        : courseProgress.getCourseCompleted())
                .courseStatus(courseProgress == null
                        ? null
                        : courseProgress.getCourseStatus())
                .nextLessonId(lessonState == null ? null : lessonState.getNextLessonId())
                .nextLessonAccessible(lessonState == null
                        ? null
                        : lessonState.getNextLessonAccessible())
                .nextLessonLockReason(lessonState == null
                        ? null
                        : lessonState.getNextLessonLockReason())
                .completedAt(progress.getCompletedAt())
                .lastAccessedAt(progress.getLastAccessedAt())
                .build();
    }

    private StudentQuizAttemptDTO unavailableQuiz(Integer courseId,
                                                  Integer lessonId,
                                                  String message) {
        return StudentQuizAttemptDTO.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .unavailable(true)
                .unavailableMessage(message)
                .questions(List.of())
                .answers(Map.of())
                .submitted(false)
                .build();
    }

    private void requireRequest(StudentQuizSubmissionRequestDTO request) {
        if (request == null || request.getAttemptId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Attempt is required.");
        }
    }

    private void requireAttemptQuiz(QuizAttemptApplicationService.AttemptSession session, Quiz expectedQuiz) {
        if (session.attempt().getQuiz() == null
                || !Objects.equals(session.attempt().getQuiz().getId(), expectedQuiz.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz attempt does not belong to this lesson.");
        }
    }

    private StudentQuizAttemptDTO toCanonicalQuizDto(
            Integer courseId,
            Integer lessonId,
            QuizAttemptApplicationService.AttemptSession session,
            LearningProgressDTO learningProgress) {
        Quiz quiz = session.attempt().getQuiz();
        boolean submitted = session.attempt().getStatus() != QuizAttemptStatus.DRAFT;
        boolean passed = submitted
                && session.attempt().getScore() != null
                && session.attempt().getScore().compareTo(defaultPassingScore(quiz)) >= 0;
        Map<Integer, String> answers = new LinkedHashMap<>();
        session.answers().forEach((questionId, answer) ->
                answers.put(questionId, answer.getAnswerText() == null ? "" : answer.getAnswerText()));
        return StudentQuizAttemptDTO.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .attemptId(session.attempt().getId())
                .status(submitted
                        ? (passed ? SubmissionStatus.PASSED : SubmissionStatus.FAILED)
                        : SubmissionStatus.PENDING_REVIEW)
                .attemptState(submitted ? STATE_SUBMITTED : STATE_DRAFT)
                .passingScore(defaultPassingScore(quiz))
                .score(session.attempt().getScore())
                .passed(passed)
                .submitted(submitted)
                .unavailable(false)
                .questions(session.questions().stream()
                        .map(assignment -> StudentQuizQuestionDTO.builder()
                                .id(assignment.getQuestion().getId())
                                .questionText(QuizQuestionTextNormalizer.stripCheckpointPrefix(
                                        assignment.getQuestionTextSnapshot()))
                                .optionsJson(studentQuizOptionsJson(snapshotQuestion(assignment), submitted))
                                .build())
                        .toList())
                .answers(answers)
                .submittedAt(session.attempt().getSubmittedAt())
                .learningProgress(learningProgress)
                .build();
    }

    private Question snapshotQuestion(QuizAttemptQuestion assignment) {
        Question question = Question.builder()
                .id(assignment.getQuestion().getId())
                .questionText(QuizQuestionTextNormalizer.stripCheckpointPrefix(
                        assignment.getQuestionTextSnapshot()))
                .optionsJson(assignment.getOptionsJsonSnapshot())
                .correctAnswer(assignment.getCorrectAnswerSnapshot())
                .points(assignment.getPointsSnapshot())
                .build();
        if (assignment.getQuestionTypeSnapshot() != null) {
            try {
                question.setQuestionType(com.ojtsu26.elearning.model.enums.QuestionType.valueOf(
                        assignment.getQuestionTypeSnapshot()));
            } catch (IllegalArgumentException ignored) {
                // A historical snapshot may contain a retired type.
            }
        }
        return question;
    }

    private StudentQuizAttemptDTO toQuizDto(Integer courseId,
                                            Integer lessonId,
                                            Quiz quiz,
                                            List<Question> questions,
                                            Submission attempt) {
        return toQuizDto(courseId, lessonId, quiz, questions, attempt, null);
    }

    private StudentQuizAttemptDTO toQuizDto(Integer courseId,
                                            Integer lessonId,
                                            Quiz quiz,
                                            List<Question> questions,
                                            Submission attempt,
                                            LearningProgressDTO learningProgress) {
        Map<String, Object> payload = readPayload(attempt.getSubmittedContent());
        Map<Integer, String> answers = readAnswers(payload);
        boolean submitted = attempt.getStatus() != SubmissionStatus.PENDING_REVIEW
                || STATE_SUBMITTED.equals(payloadState(payload));
        return StudentQuizAttemptDTO.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .attemptId(attempt.getId())
                .status(attempt.getStatus())
                .attemptState(payloadState(payload))
                .passingScore(defaultPassingScore(quiz))
                .score(attempt.getScore())
                .passed(attempt.getStatus() == SubmissionStatus.PASSED)
                .submitted(submitted)
                .unavailable(false)
                .questions(questions.stream()
                        .map(question -> StudentQuizQuestionDTO.builder()
                                .id(question.getId())
                                .questionText(question.getQuestionText())
                                .optionsJson(studentQuizOptionsJson(question, submitted))
                                .build())
                        .toList())
                .answers(answers)
                .submittedAt(attempt.getSubmittedAt())
                .learningProgress(learningProgress)
                .build();
    }

    private String studentQuizOptionsJson(Question question, boolean includeCorrect) {
        if (!includeCorrect) {
            return AssessmentOptionCodec.studentOptionsJson(question, objectMapper);
        }
        List<Map<String, Object>> options =
                AssessmentOptionCodec.readOptions(question, objectMapper).stream()
                        .map(option -> {
                            Map<String, Object> view = new LinkedHashMap<>();
                            view.put("content", option.content());
                            view.put("correct", option.correct());
                            return view;
                        })
                        .toList();
        return AssessmentOptionCodec.writeJson(options, objectMapper);
    }

    private String writePayload(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unable to save assessment state.");
        }
    }

    private Map<String, Object> readPayload(String submittedContent) {
        if (submittedContent == null || submittedContent.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(submittedContent, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private Map<Integer, String> readAnswers(Map<String, Object> payload) {
        Object answersValue = payload.get("answers");
        if (!(answersValue instanceof Map<?, ?> rawAnswers)) {
            return Map.of();
        }
        Map<Integer, String> answers = new LinkedHashMap<>();
        rawAnswers.forEach((key, value) -> {
            Integer questionId = parseInteger(key);
            if (questionId != null) {
                answers.put(questionId, value == null ? "" : String.valueOf(value));
            }
        });
        return answers;
    }

    private List<Integer> readQuestionIds(Map<String, Object> payload) {
    private List<Integer> readQuestionIds(Map<String, Object> payload) {
        Object idsValue = payload.get("questionIds");
        if (!(idsValue instanceof List<?> rawIds)) {
            return List.of();
        }
        return rawIds.stream()
                .map(this::parseInteger)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String payloadState(Map<String, Object> payload) {
        Object stateValue = payload.get("state");
        String state = stateValue == null ? null : String.valueOf(stateValue);
        return state == null || state.isBlank()
                ? STATE_DRAFT
                : state.toUpperCase(Locale.ROOT);
    }
}
