package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.request.StudentCodeSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentCodeExampleDTO;
import com.ojtsu26.elearning.dto.response.StudentCodingAssignmentDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizQuestionDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.TestcaseRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.StudentAssessmentService;
import com.ojtsu26.elearning.service.StudentLearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private final StudentLearningService studentLearningService;
    private final CurrentUserService currentUserService;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CodingAssignmentRepository codingAssignmentRepository;
    private final TestcaseRepository testcaseRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StudentQuizAttemptDTO getOrStartQuiz(Integer courseId, Integer lessonId) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.QUIZ);
        User student = currentUserService.getCurrentUser();
        Quiz quiz = quizRepository.findByLessonId(lessonId).orElse(null);
        if (quiz == null) {
            return unavailableQuiz(courseId, lessonId, "This quiz is not configured yet.");
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId());
        if (questions.isEmpty()) {
            return unavailableQuiz(courseId, lessonId, "This quiz has no questions yet.");
        }

        Submission attempt = currentAttempt(student.getId(), lessonId)
                .orElseGet(() -> createQuizAttempt(student, lesson));
        return toQuizDto(courseId, lessonId, quiz, questions, attempt);
    }

    @Override
    @Transactional
    public StudentQuizAttemptDTO saveQuizDraft(Integer courseId, Integer lessonId, StudentQuizSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.QUIZ);
        Quiz quiz = requireQuiz(lessonId);
        List<Question> questions = requireQuizQuestions(quiz);
        Submission attempt = requirePendingAttempt(request, lessonId);
        Map<Integer, String> answers = sanitizeAnswers(request.getAnswers());
        validateQuestionMembership(answers, questions);

        attempt.setSubmittedContent(writePayload(Map.of(
                "type", "QUIZ",
                "state", STATE_DRAFT,
                "answers", answers
        )));
        submissionRepository.save(attempt);
        return toQuizDto(courseId, lessonId, quiz, questions, attempt);
    }

    @Override
    @Transactional
    public StudentQuizAttemptDTO submitQuiz(Integer courseId, Integer lessonId, StudentQuizSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.QUIZ);
        Quiz quiz = requireQuiz(lessonId);
        List<Question> questions = requireQuizQuestions(quiz);
        Submission attempt = requirePendingAttempt(request, lessonId);
        Map<Integer, String> answers = sanitizeAnswers(request.getAnswers());
        validateQuestionMembership(answers, questions);

        BigDecimal score = scoreQuiz(answers, questions);
        boolean passed = score.compareTo(defaultPassingScore(quiz)) >= 0;
        attempt.setScore(score);
        attempt.setStatus(passed ? SubmissionStatus.PASSED : SubmissionStatus.FAILED);
        attempt.setSubmittedContent(writePayload(Map.of(
                "type", "QUIZ",
                "state", STATE_SUBMITTED,
                "answers", answers
        )));
        submissionRepository.save(attempt);
        if (passed) {
            markAssessmentProgressCompleted(courseId, lesson);
        }
        return toQuizDto(courseId, lessonId, quiz, questions, attempt);
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
                .findTopByStudentIdAndLessonIdOrderByIdDesc(currentUserService.getCurrentUser().getId(), lessonId)
                .orElse(null);
        return toCodingDto(courseId, lessonId, assignment, submission, false, null);
    }

    @Override
    @Transactional
    public StudentCodingAssignmentDTO saveCodeDraft(Integer courseId, Integer lessonId, StudentCodeSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.CODING);
        CodingAssignment assignment = requireAssignment(lessonId);
        String language = validateCodeRequest(assignment, request);
        Submission submission = writableCodeSubmission(request.getSubmissionId(), lesson, false);
        submission.setSubmittedContent(writeCodePayload(language, request.getCode(), STATE_DRAFT));
        submissionRepository.save(submission);
        return toCodingDto(courseId, lessonId, assignment, submission, false, null);
    }

    @Override
    @Transactional
    public StudentCodingAssignmentDTO submitCode(Integer courseId, Integer lessonId, StudentCodeSubmissionRequestDTO request) {
        Lesson lesson = requireAccessibleLesson(courseId, lessonId, LessonType.CODING);
        CodingAssignment assignment = requireAssignment(lessonId);
        String language = validateCodeRequest(assignment, request);
        Submission submission = writableCodeSubmission(request.getSubmissionId(), lesson, true);
        if (STATE_SUBMITTED.equals(payloadState(submission)) && submission.getStatus() == SubmissionStatus.PENDING_REVIEW) {
            return toCodingDto(courseId, lessonId, assignment, submission, false, null);
        }
        submission.setSubmittedContent(writeCodePayload(language, request.getCode(), STATE_SUBMITTED));
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setScore(null);
        submissionRepository.save(submission);
        return toCodingDto(courseId, lessonId, assignment, submission, false, null);
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

    private Submission createQuizAttempt(User student, Lesson lesson) {
        Submission attempt = Submission.builder()
                .student(student)
                .lesson(lesson)
                .status(SubmissionStatus.PENDING_REVIEW)
                .submittedContent(writePayload(Map.of(
                        "type", "QUIZ",
                        "state", STATE_DRAFT,
                        "answers", Map.of()
                )))
                .build();
        return submissionRepository.save(attempt);
    }

    private java.util.Optional<Submission> currentAttempt(Integer studentId, Integer lessonId) {
        java.util.Optional<Submission> pending = submissionRepository
                .findTopByStudentIdAndLessonIdAndStatusOrderByIdDesc(studentId, lessonId, SubmissionStatus.PENDING_REVIEW);
        if (pending.isPresent()) {
            return pending;
        }
        return submissionRepository.findTopByStudentIdAndLessonIdOrderByIdDesc(studentId, lessonId);
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
                    String expected = question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer().trim();
                    String actual = answers.getOrDefault(question.getId(), "").trim();
                    return !expected.isEmpty() && expected.equalsIgnoreCase(actual);
                })
                .count();
        return BigDecimal.valueOf(correct)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultPassingScore(Quiz quiz) {
        return quiz.getPassingScore() == null ? BigDecimal.ZERO : quiz.getPassingScore();
    }

    private void markAssessmentProgressCompleted(Integer courseId, Lesson lesson) {
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

    private StudentQuizAttemptDTO toQuizDto(Integer courseId, Integer lessonId, Quiz quiz, List<Question> questions, Submission attempt) {
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
                                .optionsJson(question.getOptionsJson())
                                .build())
                        .toList())
                .answers(answers)
                .submittedAt(attempt.getSubmittedAt())
                .build();
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

    private Submission writableCodeSubmission(Integer requestedSubmissionId, Lesson lesson, boolean allowSubmittedReturn) {
        User student = currentUserService.getCurrentUser();
        Submission submission;
        if (requestedSubmissionId != null) {
            submission = submissionRepository
                    .findOwnedLessonSubmission(requestedSubmissionId, student.getId(), lesson.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "Coding submission is not available."));
        } else {
            submission = submissionRepository
                    .findTopByStudentIdAndLessonIdAndStatusOrderByIdDesc(student.getId(), lesson.getId(), SubmissionStatus.PENDING_REVIEW)
                    .orElseGet(() -> Submission.builder()
                            .student(student)
                            .lesson(lesson)
                            .status(SubmissionStatus.PENDING_REVIEW)
                            .build());
        }

        boolean submitted = STATE_SUBMITTED.equals(payloadState(submission));
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This coding submission has already been graded.");
        }
        if (submitted && !allowSubmittedReturn) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This coding submission has already been submitted.");
        }
        return submission;
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
        Map<String, Object> payload = submission == null ? Map.of() : readPayload(submission.getSubmittedContent());
        String state = payloadState(payload);
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
                .submittedLanguage(stringValue(payload.get("language")))
                .submittedCode(stringValue(payload.get("code")))
                .status(submission == null ? null : submission.getStatus())
                .submissionState(state)
                .submitted(STATE_SUBMITTED.equals(state) || (submission != null && submission.getStatus() != SubmissionStatus.PENDING_REVIEW))
                .unavailable(unavailable)
                .unavailableMessage(unavailableMessage)
                .submittedAt(submission == null ? null : submission.getSubmittedAt())
                .build();
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
