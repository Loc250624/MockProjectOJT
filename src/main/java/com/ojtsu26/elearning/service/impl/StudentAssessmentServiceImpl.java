package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.request.StudentCodeSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentCodeExampleDTO;
import com.ojtsu26.elearning.dto.response.StudentCodingAssignmentDTO;
import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizQuestionDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.CodeJudgeResult;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizAttemptQuestion;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.CodeJudgeResultRepository;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.TestcaseRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.CodeJudgeAdapter;
import com.ojtsu26.elearning.service.NotificationService;
import com.ojtsu26.elearning.service.StudentAssessmentService;
import com.ojtsu26.elearning.service.StudentLearningService;
import com.ojtsu26.elearning.service.quiz.QuizAttemptApplicationService;
import com.ojtsu26.elearning.service.quiz.QuizQuestionTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final int MAX_ANSWER_LENGTH = 2000;
    private static final int QUIZ_QUESTION_COUNT = 10;

    private final StudentLearningService studentLearningService;
    private final CurrentUserService currentUserService;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CodingAssignmentRepository codingAssignmentRepository;
    private final TestcaseRepository testcaseRepository;
    private final SubmissionRepository submissionRepository;
    private final CodeJudgeResultRepository codeJudgeResultRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CodeJudgeAdapter codeJudgeAdapter;
    private final NotificationService notificationService;
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
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.QUIZ);
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

        User student = currentUserService.getCurrentUser();
        List<Question> questionBank = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId());
        if (questionBank.isEmpty()) {
            return unavailableQuiz(courseId, lessonId, "This quiz has no questions yet.");
        }

        Submission attempt = currentDraftAttempt(student.getId(), lessonId)
                .orElseGet(() -> createQuizAttempt(student, lesson, questionBank));
        List<Question> questions = ensureAssignedQuestions(attempt, questionBank);
        return toQuizDto(courseId, lessonId, quiz, questions, attempt);
    }

    @Override
    @Transactional
    public StudentQuizAttemptDTO saveQuizDraft(Integer courseId, Integer lessonId, StudentQuizSubmissionRequestDTO request) {
        requireAccessibleLesson(courseId, lessonId, LessonType.QUIZ);
        Quiz quiz = requireQuiz(lessonId);
        if (quizAttemptApplicationService != null) {
            requireRequest(request);
            QuizAttemptApplicationService.AttemptSession session =
                    quizAttemptApplicationService.saveTextAnswers(
                            request.getAttemptId(), sanitizeAnswers(request.getAnswers()));
            requireAttemptQuiz(session, quiz);
            return toCanonicalQuizDto(courseId, lessonId, session, null);
        }

        List<Question> questionBank = requireQuizQuestions(quiz);
        Submission attempt = requirePendingAttempt(request, lessonId);
        List<Question> questions = ensureAssignedQuestions(attempt, questionBank);
        Map<Integer, String> answers = sanitizeAnswers(request.getAnswers());
        validateQuestionMembership(answers, questions);

        attempt.setSubmittedContent(writeQuizPayload(STATE_DRAFT, questions, answers));
        submissionRepository.save(attempt);
        return toQuizDto(courseId, lessonId, quiz, questions, attempt);
    }

    @Override
    @Transactional
    public StudentQuizAttemptDTO submitQuiz(Integer courseId, Integer lessonId, StudentQuizSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.QUIZ);
        Quiz quiz = requireQuiz(lessonId);
        if (quizAttemptApplicationService != null) {
            requireRequest(request);
            QuizAttemptApplicationService.AttemptSession session =
                    quizAttemptApplicationService.submitTextAnswers(
                            request.getAttemptId(), sanitizeAnswers(request.getAnswers()));
            requireAttemptQuiz(session, quiz);
            boolean passed = session.attempt().getScore() != null
                    && session.attempt().getScore().compareTo(defaultPassingScore(quiz)) >= 0;
            LearningProgressDTO learningProgress = passed
                    ? markAssessmentProgressCompleted(courseId, lesson)
                    : null;
            return toCanonicalQuizDto(courseId, lessonId, session, learningProgress);
        }

        List<Question> questionBank = requireQuizQuestions(quiz);
        Submission attempt = requirePendingAttempt(request, lessonId);
        List<Question> questions = ensureAssignedQuestions(attempt, questionBank);
        Map<Integer, String> answers = sanitizeAnswers(request.getAnswers());
        validateQuestionMembership(answers, questions);

        BigDecimal score = scoreQuiz(answers, questions);
        boolean passed = score.compareTo(defaultPassingScore(quiz)) >= 0;
        attempt.setScore(score);
        attempt.setStatus(passed ? SubmissionStatus.PASSED : SubmissionStatus.FAILED);
        attempt.setSubmittedContent(writeQuizPayload(STATE_SUBMITTED, questions, answers));
        submissionRepository.save(attempt);
        LearningProgressDTO learningProgress = passed ? markAssessmentProgressCompleted(courseId, lesson) : null;
        return toQuizDto(courseId, lessonId, quiz, questions, attempt, learningProgress);
    }

    @Override
    @Transactional
    public StudentCodingAssignmentDTO getCodingAssignment(Integer courseId, Integer lessonId) {
        requireAccessibleLesson(courseId, lessonId, LessonType.CODING);
        CodingAssignment assignment = codingAssignmentRepository.findByLessonId(lessonId).orElse(null);
        if (assignment == null) {
            return unavailableAssignment(courseId, lessonId, "This coding exercise is not configured yet.");
        }
        List<String> languages = allowedLanguages(assignment);
        if (languages.isEmpty()) {
            return unavailableAssignment(courseId, lessonId, "This coding exercise has no allowed languages yet.");
        }
        Submission submission = submissionRepository
                .findTopByAssignmentIdAndStudentIdOrderByUpdatedAtDesc(assignment.getId(), currentUserService.getCurrentUser().getId())
                .orElseGet(() -> submissionRepository
                        .findTopByStudentIdAndLessonIdOrderByIdDesc(currentUserService.getCurrentUser().getId(), lessonId)
                        .orElse(null));
        return toCodingDto(courseId, lessonId, assignment, submission, false, null);
    }

    @Override
    @Transactional
    public StudentCodingAssignmentDTO saveCodeDraft(Integer courseId, Integer lessonId, StudentCodeSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.CODING);
        CodingAssignment assignment = requireAssignment(lessonId);
        String language = validateCodeRequest(assignment, request);
        Submission submission = writableCodeSubmission(request.getSubmissionId(), assignment, false);
        applyCodeSubmission(submission, assignment, language, request.getCode(), STATE_DRAFT, SubmissionStatus.DRAFT);
        submissionRepository.save(submission);
        return toCodingDto(courseId, lessonId, assignment, submission, false, null);
    }

    @Override
    @Transactional
    public StudentCodingAssignmentDTO runCode(Integer courseId, Integer lessonId, StudentCodeSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.CODING);
        CodingAssignment assignment = requireAssignment(lessonId);
        String language = validateCodeRequest(assignment, request);
        Submission submission = writableCodeSubmission(request.getSubmissionId(), assignment, true);
        applyCodeSubmission(submission, assignment, language, request.getCode(), STATE_DRAFT, SubmissionStatus.DRAFT);
        Submission saved = submissionRepository.save(submission);
        CodeJudgeResult result = judge(saved, assignment);
        return toCodingDto(courseId, lessonId, assignment, saved, false, null, result);
    }

    @Override
    @Transactional
    public StudentCodingAssignmentDTO submitCode(Integer courseId, Integer lessonId, StudentCodeSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.CODING);
        CodingAssignment assignment = requireAssignment(lessonId);
        String language = validateCodeRequest(assignment, request);
        Submission submission = writableCodeSubmission(request.getSubmissionId(), assignment, true);
        if (STATE_SUBMITTED.equals(payloadState(submission)) && submission.getStatus() == SubmissionStatus.PENDING_REVIEW) {
            return toCodingDto(courseId, lessonId, assignment, submission, false, null);
        }
        applyCodeSubmission(submission, assignment, language, request.getCode(), STATE_SUBMITTED, SubmissionStatus.PENDING_REVIEW);
        Submission saved = submissionRepository.save(submission);
        createTeacherSubmissionNotification(saved);
        CodeJudgeResult result = judge(saved, assignment);
        LearningProgressDTO learningProgress = applyStudentJudgeOutcome(courseId, lesson, saved, result);
        return toCodingDto(courseId, lessonId, assignment, saved, false, null, result, learningProgress);
    }

    private Lesson requireAccessibleLesson(Integer courseId, Integer lessonId, LessonType expectedType) {
        StudentLearningLessonDTO opened = studentLearningService.openLesson(courseId, lessonId);
        if (opened == null || opened.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Lesson is not available in this course.");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Lesson is not available in this course."));
        if (lesson.getCourse() == null || !Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Lesson is not available in this course.");
        }
        if (expectedType == LessonType.QUIZ && lesson.getType() != LessonType.QUIZ && lesson.getQuiz() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This lesson is not a quiz.");
        }
        if (expectedType == LessonType.CODING && lesson.getType() != LessonType.CODING && lesson.getCodingassignment() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This lesson is not a coding exercise.");
        }
        return lesson;
    }

    private Quiz requireQuiz(Integer lessonId) {
        return quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "This quiz is not configured yet."));
    }

    private List<Question> requireQuizQuestions(Quiz quiz) {
        List<Question> questions = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId());
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This quiz has no questions yet.");
        }
        return questions;
    }

    private Submission createQuizAttempt(User student, Lesson lesson, List<Question> questionBank) {
        List<Question> assignedQuestions = selectRandomQuestions(questionBank);
        Submission attempt = Submission.builder()
                .student(student)
                .lesson(lesson)
                .status(SubmissionStatus.PENDING_REVIEW)
                .submittedContent(writeQuizPayload(STATE_DRAFT, assignedQuestions, Map.of()))
                .build();
        return submissionRepository.save(attempt);
    }

    private List<Question> ensureAssignedQuestions(Submission attempt, List<Question> questionBank) {
        Map<String, Object> payload = readPayload(attempt.getSubmittedContent());
        List<Integer> assignedIds = readQuestionIds(payload);
        if (assignedIds.isEmpty()) {
            List<Question> selected = selectRandomQuestions(questionBank);
            attempt.setSubmittedContent(writeQuizPayload(
                    payloadState(payload),
                    selected,
                    readAnswers(payload)
            ));
            submissionRepository.save(attempt);
            return selected;
        }

        Map<Integer, Question> questionsById = questionBank.stream()
                .collect(Collectors.toMap(Question::getId, question -> question));
        List<Question> assigned = assignedIds.stream()
                .map(questionsById::get)
                .filter(Objects::nonNull)
                .toList();
        if (assigned.size() != assignedIds.size()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "One or more assigned quiz questions are no longer available."
            );
        }
        return assigned;
    }

    private List<Question> selectRandomQuestions(List<Question> questionBank) {
        List<Question> shuffled = new ArrayList<>(questionBank);
        Collections.shuffle(shuffled);
        return new ArrayList<>(shuffled.subList(0, Math.min(QUIZ_QUESTION_COUNT, shuffled.size())));
    }

    private String writeQuizPayload(String state, List<Question> questions, Map<Integer, String> answers) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "QUIZ");
        payload.put("state", state == null ? STATE_DRAFT : state);
        payload.put("questionIds", questions.stream().map(Question::getId).toList());
        payload.put("answers", answers == null ? Map.of() : answers);
        return writePayload(payload);
    }

    private java.util.Optional<Submission> currentDraftAttempt(Integer studentId, Integer lessonId) {
        return submissionRepository.findDraftsForUpdate(studentId, lessonId, SubmissionStatus.PENDING_REVIEW)
                .stream()
                .findFirst();
    }

    private Submission requirePendingAttempt(StudentQuizSubmissionRequestDTO request, Integer lessonId) {
        if (request == null || request.getAttemptId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Attempt is required.");
        }
        Submission attempt = submissionRepository
                .findOwnedLessonSubmission(request.getAttemptId(), currentUserService.getCurrentUser().getId(), lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz attempt is not available."));
        if (attempt.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This quiz attempt has already been submitted.");
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
            String normalizedAnswer = answer == null ? "" : answer.trim();
            if (normalizedAnswer.length() > MAX_ANSWER_LENGTH) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Answer is too long.");
            }
            sanitized.put(questionId, normalizedAnswer);
        });
        return sanitized;
    }

    private void validateQuestionMembership(Map<Integer, String> answers, List<Question> questions) {
        Set<Integer> questionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        for (Integer answeredQuestionId : answers.keySet()) {
            if (!questionIds.contains(answeredQuestionId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Submitted answer does not belong to this quiz.");
            }
        }
    }

    private BigDecimal scoreQuiz(Map<Integer, String> answers, List<Question> questions) {
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
        List<AssessmentOptionCodec.ParsedOption> options = AssessmentOptionCodec.readOptions(question, objectMapper);
        Set<String> tokens = options.stream()
                .filter(AssessmentOptionCodec.ParsedOption::correct)
                .flatMap(option -> java.util.stream.Stream.of(String.valueOf(option.id()), option.content()))
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

    private LearningProgressDTO markAssessmentProgressCompleted(Integer courseId, Lesson lesson) {
        User student = currentUserService.getCurrentUser();
        CourseEnrollment enrollment = enrollmentRepository.findByStudentIdAndCourseIdForUpdate(student.getId(), courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "You are not enrolled in this course."));
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

    private LearningProgressDTO assessmentLearningProgress(Integer courseId, Lesson lesson, LessonProgress progress) {
        StudentLearningLessonDTO lessonState = studentLearningService.openLesson(courseId, lesson.getId());
        LearningProgressDTO courseProgress = lessonState == null ? null : lessonState.getCourseProgress();
        return LearningProgressDTO.builder()
                .courseId(courseId)
                .lessonId(lesson.getId())
                .enrollmentId(progress.getEnrollment() == null ? null : progress.getEnrollment().getId())
                .completedLessons(courseProgress == null ? null : courseProgress.getCompletedLessons())
                .totalLessons(courseProgress == null ? null : courseProgress.getTotalLessons())
                .progressPercentage(courseProgress == null ? null : courseProgress.getProgressPercentage())
                .completed(Boolean.TRUE.equals(progress.getIsCompleted()))
                .lessonCompleted(Boolean.TRUE.equals(progress.getIsCompleted()))
                .courseCompleted(courseProgress == null ? null : courseProgress.getCourseCompleted())
                .courseStatus(courseProgress == null ? null : courseProgress.getCourseStatus())
                .nextLessonId(lessonState == null ? null : lessonState.getNextLessonId())
                .nextLessonAccessible(lessonState == null ? null : lessonState.getNextLessonAccessible())
                .nextLessonLockReason(lessonState == null ? null : lessonState.getNextLessonLockReason())
                .completedAt(progress.getCompletedAt())
                .lastAccessedAt(progress.getLastAccessedAt())
                .build();
    }

    private StudentQuizAttemptDTO unavailableQuiz(Integer courseId, Integer lessonId, String message) {
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

    private StudentQuizAttemptDTO toQuizDto(Integer courseId, Integer lessonId, Quiz quiz, List<Question> questions, Submission attempt) {
        return toQuizDto(courseId, lessonId, quiz, questions, attempt, null);
    }

    private StudentQuizAttemptDTO toQuizDto(Integer courseId, Integer lessonId, Quiz quiz, List<Question> questions,
                                            Submission attempt, LearningProgressDTO learningProgress) {
        Map<String, Object> payload = readPayload(attempt.getSubmittedContent());
        Map<Integer, String> answers = readAnswers(payload);
        boolean submitted = attempt.getStatus() != SubmissionStatus.PENDING_REVIEW || STATE_SUBMITTED.equals(payloadState(payload));
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
        List<Map<String, Object>> options = AssessmentOptionCodec.readOptions(question, objectMapper).stream()
                .map(option -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("content", option.content());
                    view.put("correct", option.correct());
                    return view;
                })
                .toList();
        return AssessmentOptionCodec.writeJson(options, objectMapper);
    }

    private CodingAssignment requireAssignment(Integer lessonId) {
        CodingAssignment assignment = codingAssignmentRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "This coding exercise is not configured yet."));
        if (allowedLanguages(assignment).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This coding exercise has no allowed languages yet.");
        }
        return assignment;
    }

    private String validateCodeRequest(CodingAssignment assignment, StudentCodeSubmissionRequestDTO request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Code submission is required.");
        }
        String language = request.getLanguage() == null ? "" : request.getLanguage().trim();
        String code = request.getCode() == null ? "" : request.getCode().trim();
        if (language.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Programming language is required.");
        }
        if (code.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Code is required.");
        }
        List<String> languages = allowedLanguages(assignment);
        boolean allowed = languages.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(language));
        if (!allowed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported programming language.");
        }
        return languages.stream()
                .filter(candidate -> candidate.equalsIgnoreCase(language))
                .findFirst()
                .orElse(language);
    }

    private Submission writableCodeSubmission(Integer requestedSubmissionId, CodingAssignment assignment, boolean allowSubmittedReturn) {
        User student = currentUserService.getCurrentUser();
        Lesson lesson = assignment.getLesson();
        Submission submission;
        if (requestedSubmissionId != null) {
            submission = submissionRepository
                    .findOwnedLessonSubmission(requestedSubmissionId, student.getId(), lesson.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "Coding submission is not available."));
        } else {
            submission = submissionRepository
                    .findTopByAssignmentIdAndStudentIdOrderByUpdatedAtDesc(assignment.getId(), student.getId())
                    .filter(existing -> existing.getStatus() == SubmissionStatus.DRAFT
                            || existing.getStatus() == SubmissionStatus.PENDING_REVIEW)
                    .orElseGet(() -> Submission.builder()
                            .assignment(assignment)
                            .student(student)
                            .lesson(lesson)
                            .status(SubmissionStatus.DRAFT)
                            .build());
        }

        boolean submitted = STATE_SUBMITTED.equals(payloadState(submission));
        if (submission.getAssignment() == null) {
            submission.setAssignment(assignment);
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW && submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This coding submission has already been graded.");
        }
        if (submitted && !allowSubmittedReturn) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This coding submission has already been submitted.");
        }
        return submission;
    }

    private void applyCodeSubmission(Submission submission, CodingAssignment assignment, String language, String code,
                                     String state, SubmissionStatus status) {
        submission.setAssignment(assignment);
        submission.setLesson(assignment.getLesson());
        submission.setStudent(currentUserService.getCurrentUser());
        submission.setSubmittedContent(writeCodePayload(language, code, state));
        submission.setCodeLanguage(language);
        submission.setCodeContent(code);
        submission.setStatus(status);
        if (STATE_SUBMITTED.equals(state)) {
            submission.setScore(null);
        }
    }

    private CodeJudgeResult judge(Submission submission, CodingAssignment assignment) {
        List<Testcase> testcases = testcaseRepository.findByAssignmentIdOrderByIdAsc(assignment.getId());
        CodeJudgeAdapter.JudgeOutcome outcome;
        if (testcases.isEmpty()) {
            outcome = new CodeJudgeAdapter.JudgeOutcome(
                    com.ojtsu26.elearning.model.enums.CodeJudgeStatus.ERROR,
                    0,
                    0,
                    List.of(),
                    "No testcases are configured for this assignment.",
                    0L);
        } else {
            CodingAssignment judgeAssignment = CodingAssignment.builder()
                    .id(assignment.getId())
                    .title(assignment.getTitle())
                    .timeLimitMs(assignment.getTimeLimitMs())
                    .maxScore(assignment.getMaxScore())
                    .testcases(testcases)
                    .build();
            outcome = codeJudgeAdapter.judge(judgeAssignment, submission);
        }
        CodeJudgeResult result = CodeJudgeResult.builder()
                .submission(submission)
                .status(outcome.status())
                .totalTests(outcome.totalTests())
                .passedTests(outcome.passedTests())
                .outputLog(outcome.outputLog())
                .executionTimeMs(outcome.executionTimeMs())
                .build();
        return codeJudgeResultRepository.save(result);
    }

    private LearningProgressDTO applyStudentJudgeOutcome(Integer courseId, Lesson lesson, Submission submission, CodeJudgeResult result) {
        if (result == null || result.getStatus() == null) {
            return null;
        }
        if (result.getStatus() == com.ojtsu26.elearning.model.enums.CodeJudgeStatus.PASSED) {
            submission.setStatus(SubmissionStatus.PASSED);
            submission.setScore(BigDecimal.valueOf(100));
            return markAssessmentProgressCompleted(courseId, lesson);
        } else if (result.getStatus() == com.ojtsu26.elearning.model.enums.CodeJudgeStatus.FAILED) {
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setScore(judgePercent(result));
        }
        return null;
    }

    private BigDecimal judgePercent(CodeJudgeResult result) {
        if (result.getTotalTests() == null || result.getTotalTests() <= 0 || result.getPassedTests() == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(result.getPassedTests())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(result.getTotalTests()), 2, RoundingMode.HALF_UP);
    }

    private void createTeacherSubmissionNotification(Submission submission) {
        if (submission == null || submission.getId() == null || submission.getAssignment() == null
                || submission.getAssignment().getLesson() == null
                || submission.getAssignment().getLesson().getCourse() == null
                || submission.getAssignment().getLesson().getCourse().getInstructor() == null) {
            return;
        }
        CodingAssignment assignment = submission.getAssignment();
        User teacher = assignment.getLesson().getCourse().getInstructor();
        String studentName = submission.getStudent() == null || submission.getStudent().getFullName() == null
                ? "A student"
                : submission.getStudent().getFullName();
        notificationService.createNotification(
                teacher,
                NotificationType.COURSE_SUBMITTED_FOR_REVIEW,
                "New coding submission",
                studentName + " submitted " + assignment.getTitle() + ".",
                "/teacher/grading?courseId=" + assignment.getLesson().getCourse().getId()
                        + "&assignmentId=" + assignment.getId()
                        + "&submissionId=" + submission.getId(),
                "TEACHER_CODING_SUBMISSION:submission:" + submission.getId());
    }

    private String writeCodePayload(String language, String code, String state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "CODE");
        payload.put("state", state);
        payload.put("language", language);
        payload.put("code", code);
        return writePayload(payload);
    }

    private StudentCodingAssignmentDTO unavailableAssignment(Integer courseId, Integer lessonId, String message) {
        return StudentCodingAssignmentDTO.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .unavailable(true)
                .unavailableMessage(message)
                .allowedLanguages(List.of())
                .examples(List.of())
                .submitted(false)
                .build();
    }

    private StudentCodingAssignmentDTO toCodingDto(Integer courseId, Integer lessonId, CodingAssignment assignment,
                                                   Submission submission, boolean unavailable, String unavailableMessage) {
        return toCodingDto(courseId, lessonId, assignment, submission, unavailable, unavailableMessage,
                submission == null || submission.getId() == null
                        ? null
                        : codeJudgeResultRepository.findTopBySubmissionIdOrderByCreatedAtDesc(submission.getId()).orElse(null));
    }

    private StudentCodingAssignmentDTO toCodingDto(Integer courseId, Integer lessonId, CodingAssignment assignment,
                                                   Submission submission, boolean unavailable, String unavailableMessage,
                                                   CodeJudgeResult latestResult) {
        return toCodingDto(courseId, lessonId, assignment, submission, unavailable, unavailableMessage, latestResult, null);
    }

    private StudentCodingAssignmentDTO toCodingDto(Integer courseId, Integer lessonId, CodingAssignment assignment,
                                                   Submission submission, boolean unavailable, String unavailableMessage,
                                                   CodeJudgeResult latestResult, LearningProgressDTO learningProgress) {
        Map<String, Object> payload = submission == null ? Map.of() : readPayload(submission.getSubmittedContent());
        String state = payloadState(payload);
        String submittedLanguage = submission != null && submission.getCodeLanguage() != null
                ? submission.getCodeLanguage()
                : stringValue(payload.get("language"));
        String submittedCode = submission != null && submission.getCodeContent() != null
                ? submission.getCodeContent()
                : stringValue(payload.get("code"));
        return StudentCodingAssignmentDTO.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .assignmentId(assignment.getId())
                .title(assignment.getTitle())
                .problemStatement(assignment.getProblemStatement())
                .starterCode(assignment.getStarterCode() == null ? "" : assignment.getStarterCode())
                .allowedLanguages(allowedLanguages(assignment))
                .timeLimitMs(assignment.getTimeLimitMs())
                .examples(visibleExamples(assignment.getId()))
                .submissionId(submission == null ? null : submission.getId())
                .submittedLanguage(submittedLanguage)
                .submittedCode(submittedCode)
                .status(submission == null ? null : submission.getStatus())
                .submissionState(state)
                .submitted(STATE_SUBMITTED.equals(state) || (submission != null
                        && submission.getStatus() != SubmissionStatus.PENDING_REVIEW
                        && submission.getStatus() != SubmissionStatus.DRAFT))
                .unavailable(unavailable)
                .unavailableMessage(unavailableMessage)
                .submittedAt(submission == null ? null : submission.getSubmittedAt())
                .judgeStatus(latestResult == null ? null : latestResult.getStatus())
                .totalTests(latestResult == null ? null : latestResult.getTotalTests())
                .passedTests(latestResult == null ? null : latestResult.getPassedTests())
                .outputLog(studentJudgeSummary(latestResult))
                .learningProgress(learningProgress)
                .build();
    }

    private String studentJudgeSummary(CodeJudgeResult result) {
        if (result == null || result.getStatus() == null) {
            return null;
        }
        int passed = result.getPassedTests() == null ? 0 : result.getPassedTests();
        int total = result.getTotalTests() == null ? 0 : result.getTotalTests();
        return "Judge status: " + result.getStatus() + ". Passed " + passed + " of " + total + " tests.";
    }

    private List<String> allowedLanguages(CodingAssignment assignment) {
        if (assignment.getAllowedLanguages() == null || assignment.getAllowedLanguages().isBlank()) {
            return List.of();
        }
        List<String> languages = new ArrayList<>();
        for (String language : assignment.getAllowedLanguages().split(",")) {
            String trimmed = language.trim();
            if (!trimmed.isBlank()) {
                languages.add(trimmed);
            }
        }
        return languages;
    }

    private List<StudentCodeExampleDTO> visibleExamples(Integer assignmentId) {
        return testcaseRepository.findByAssignmentIdOrderByIdAsc(assignmentId).stream()
                .filter(testcase -> !Boolean.TRUE.equals(testcase.getIsHidden()))
                .map(this::toVisibleExample)
                .toList();
    }

    private StudentCodeExampleDTO toVisibleExample(Testcase testcase) {
        return StudentCodeExampleDTO.builder()
                .id(testcase.getId())
                .inputData(testcase.getInputData())
                .build();
    }

    private String writePayload(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Unable to save assessment state.");
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
        Object idsValue = payload.get("questionIds");
        if (!(idsValue instanceof List<?> rawIds)) {
            return List.of();
        }
        return rawIds.stream()
                .map(this::parseInteger)
                .filter(Objects::nonNull)
                .distinct()
                .limit(QUIZ_QUESTION_COUNT)
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

    private String payloadState(Submission submission) {
        if (submission == null) {
            return null;
        }
        return payloadState(readPayload(submission.getSubmittedContent()));
    }

    private String payloadState(Map<String, Object> payload) {
        String state = stringValue(payload.get("state"));
        return state == null ? STATE_DRAFT : state.toUpperCase(Locale.ROOT);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value);
        return string.isBlank() ? null : string;
    }
}
