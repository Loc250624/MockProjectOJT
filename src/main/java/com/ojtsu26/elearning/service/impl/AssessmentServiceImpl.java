package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.*;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.service.AssessmentService;
import com.ojtsu26.elearning.service.CodeJudgeAdapter;
import com.ojtsu26.elearning.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentServiceImpl implements AssessmentService {
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final CodingAssignmentRepository codingAssignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TestcaseRepository testcaseRepository;
    private final CodeJudgeResultRepository codeJudgeResultRepository;
    private final GradeFeedbackRepository gradeFeedbackRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;
    private final CodeJudgeAdapter codeJudgeAdapter;
    private final ObjectMapper objectMapper;
    private static final List<SubmissionStatus> PENDING_GRADE_STATUSES = List.of(
            SubmissionStatus.SUBMITTED,
            SubmissionStatus.PENDING_REVIEW,
            SubmissionStatus.RETURNED,
            SubmissionStatus.FAILED
    );
    private static final List<SubmissionStatus> GRADED_STATUSES = List.of(
            SubmissionStatus.GRADED,
            SubmissionStatus.PASSED,
            SubmissionStatus.AUTO_GRADED
    );
    private static final List<SubmissionStatus> FAILED_STATUSES = List.of(
            SubmissionStatus.RETURNED,
            SubmissionStatus.FAILED
    );
    private static final Set<SubmissionStatus> RELEASED_SUBMISSION_STATUSES = EnumSet.of(
            SubmissionStatus.GRADED,
            SubmissionStatus.PASSED,
            SubmissionStatus.AUTO_GRADED,
            SubmissionStatus.RETURNED,
            SubmissionStatus.FAILED
    );

    @Override
    @Transactional(readOnly = true)
    public QuizView getStudentQuiz(Integer courseId, Integer quizId) {
        User student = currentUserService.getCurrentUser();
        Quiz quiz = getQuiz(quizId);
        if (!quiz.getLesson().getCourse().getId().equals(courseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Quiz does not belong to this course");
        }
        requireEnrollment(student.getId(), courseId);
        if (quiz.getStatus() != null && quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz is not published");
        }
        QuizView view = toQuizView(quiz, false);
        quizAttemptRepository.findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                quizId, student.getId(), QuizAttemptStatus.DRAFT
        ).ifPresent(attempt -> applyDraftAnswers(view, attempt));
        return view;
    }

    @Override
    public QuizAttemptView startQuizAttempt(Integer quizId) {
        User student = currentUserService.getCurrentUser();
        Quiz quiz = getQuiz(quizId);
        requireEnrollment(student.getId(), quiz.getLesson().getCourse().getId());
        if (quiz.getStatus() != null && quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz is not published");
        }
        Optional<QuizAttempt> existingDraft = quizAttemptRepository.findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                quizId, student.getId(), QuizAttemptStatus.DRAFT);
        if (existingDraft.isPresent()) {
            return toAttemptView(existingDraft.get(), false);
        }
        int maxAttempts = quiz.getMaxAttempts() == null || quiz.getMaxAttempts() < 1 ? 1 : quiz.getMaxAttempts();
        long usedAttempts = quizAttemptRepository.countByQuizIdAndStudentIdAndStatusIn(
                quizId, student.getId(), List.of(QuizAttemptStatus.SUBMITTED, QuizAttemptStatus.GRADED));
        if (usedAttempts >= maxAttempts) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Maximum attempts reached");
        }
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.DRAFT)
                .totalPoints(totalPoints(quiz))
                .build();
        return toAttemptView(quizAttemptRepository.save(attempt), false);
    }

    @Override
    public QuizAttemptView saveQuizDraft(Integer attemptId, QuizDraftPayload payload) {
        QuizAttempt attempt = requireStudentAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Submitted attempts cannot be edited");
        }
        persistAnswers(attempt, payload);
        return toAttemptView(attempt, false);
    }

    @Override
    public QuizAttemptView submitQuizAttempt(Integer attemptId, QuizDraftPayload payload) {
        QuizAttempt attempt = requireStudentAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Attempt already submitted");
        }
        persistAnswers(attempt, payload);
        BigDecimal total = totalPoints(attempt.getQuiz());
        BigDecimal raw = gradeAttempt(attempt);
        BigDecimal percent = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : raw.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
        attempt.setScore(percent);
        attempt.setTotalPoints(total);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(QuizAttemptStatus.GRADED);
        return toAttemptView(attempt, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultSummaryView getStudentResults() {
        User student = currentUserService.getCurrentUser();
        ResultSummaryView view = new ResultSummaryView();
        view.setQuizAttempts(quizAttemptRepository.findByStudentIdOrderByStartedAtDesc(student.getId()).stream()
                .filter(a -> a.getStatus() != QuizAttemptStatus.DRAFT)
                .map(a -> toAttemptView(a, true))
                .toList());
        view.setSubmissions(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()).stream()
                .map(this::toStudentSubmissionView)
                .toList());
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public QuizAttemptView getStudentQuizResult(Integer attemptId) {
        return toAttemptView(requireStudentAttempt(attemptId), true);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionView getStudentSubmissionResult(Integer submissionId) {
        Submission submission = submissionRepository.findByIdWithAssignmentCourse(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Submission not found"));
        User student = currentUserService.getCurrentUser();
        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Submission access denied");
        }
        return toStudentSubmissionView(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionView getAssignmentForSubmission(Integer assignmentId) {
        User student = currentUserService.getCurrentUser();
        CodingAssignment assignment = getAssignment(assignmentId);
        requireEnrollment(student.getId(), assignment.getLesson().getCourse().getId());
        return submissionRepository.findTopByAssignmentIdAndStudentIdOrderByUpdatedAtDesc(assignmentId, student.getId())
                .map(this::toSubmissionView)
                .orElseGet(() -> toAssignmentShell(assignment));
    }

    @Override
    public SubmissionView saveSubmissionDraft(Integer assignmentId, AssignmentSubmissionPayload payload) {
        return saveSubmission(assignmentId, payload, SubmissionStatus.DRAFT);
    }

    @Override
    public SubmissionView submitAssignment(Integer assignmentId, AssignmentSubmissionPayload payload) {
        return saveSubmission(assignmentId, payload, SubmissionStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizView> getTeacherCourseQuizzes(Integer courseId) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacherCourse(courseId, teacher.getId());
        return quizRepository.findByCourseId(courseId).stream()
                .map(q -> toQuizView(q, true))
                .toList();
    }

    @Override
    public QuizView createTeacherQuiz(Integer courseId, QuizPayload payload) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacherCourse(courseId, teacher.getId());
        validateQuizPayload(payload);
        if (payload.getStatus() == QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Create quiz as draft, then publish after adding questions");
        }
        Lesson lesson = requireLessonForTeacher(payload.getLessonId(), courseId, teacher.getId());
        Quiz quiz = Quiz.builder()
                .title(payload.getTitle().trim())
                .description(trim(payload.getDescription()))
                .durationMinutes(payload.getDurationMinutes())
                .maxAttempts(payload.getMaxAttempts())
                .passingScore(payload.getPassingScore())
                .status(payload.getStatus() == null ? QuizStatus.DRAFT : payload.getStatus())
                .lesson(lesson)
                .createdBy(teacher)
                .build();
        return toQuizView(quizRepository.save(quiz), true);
    }

    @Override
    public QuizView updateTeacherQuiz(Integer quizId, QuizPayload payload) {
        User teacher = currentUserService.getCurrentUser();
        Quiz quiz = requireTeacherQuiz(quizId, teacher.getId());
        validateQuizPayload(payload);
        if (payload.getStatus() == QuizStatus.PUBLISHED) {
            validateQuizCanPublish(quiz);
        }
        if (payload.getLessonId() != null && !payload.getLessonId().equals(quiz.getLesson().getId())) {
            Lesson lesson = requireLessonForTeacher(payload.getLessonId(), quiz.getLesson().getCourse().getId(), teacher.getId());
            quiz.setLesson(lesson);
        }
        quiz.setTitle(payload.getTitle().trim());
        quiz.setDescription(trim(payload.getDescription()));
        quiz.setDurationMinutes(payload.getDurationMinutes());
        quiz.setMaxAttempts(payload.getMaxAttempts());
        quiz.setPassingScore(payload.getPassingScore());
        quiz.setStatus(payload.getStatus() == null ? QuizStatus.DRAFT : payload.getStatus());
        return toQuizView(quiz, true);
    }

    @Override
    public void deleteTeacherQuiz(Integer quizId) {
        User teacher = currentUserService.getCurrentUser();
        Quiz quiz = requireTeacherQuiz(quizId, teacher.getId());
        if (quizAttemptRepository.countByQuizId(quizId) > 0) {
            quiz.setStatus(QuizStatus.ARCHIVED);
            return;
        }
        quizRepository.delete(quiz);
    }

    @Override
    public QuestionView createTeacherQuestion(Integer quizId, QuestionPayload payload) {
        User teacher = currentUserService.getCurrentUser();
        Quiz quiz = requireTeacherQuiz(quizId, teacher.getId());
        validateQuestionPayload(payload);
        Question question = Question.builder().quiz(quiz).build();
        applyQuestion(question, payload);
        return toQuestionView(questionRepository.save(question), true);
    }

    @Override
    public QuestionView updateTeacherQuestion(Integer questionId, QuestionPayload payload) {
        User teacher = currentUserService.getCurrentUser();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Question not found"));
        requireTeacherQuiz(question.getQuiz().getId(), teacher.getId());
        validateQuestionPayload(payload);
        if (quizAnswerRepository.countByQuestionId(questionId) > 0 && questionContentOrScoringChanged(question, payload)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Question has submitted answers; create a new question instead of changing scored content");
        }
        applyQuestion(question, payload);
        return toQuestionView(question, true);
    }

    @Override
    public void deleteTeacherQuestion(Integer questionId) {
        User teacher = currentUserService.getCurrentUser();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Question not found"));
        requireTeacherQuiz(question.getQuiz().getId(), teacher.getId());
        if (quizAnswerRepository.countByQuestionId(questionId) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Question has submitted answers and cannot be deleted without losing attempt history");
        }
        questionRepository.delete(question);
    }

    @Override
    public List<QuestionView> reorderTeacherQuestions(Integer quizId, List<Integer> questionIdsInOrder) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacherQuiz(quizId, teacher.getId());
        List<Question> questions = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quizId);
        Map<Integer, Question> byId = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<Integer> requestedIds = Optional.ofNullable(questionIdsInOrder).orElse(List.of());
        if (requestedIds.size() != byId.size() || !byId.keySet().equals(new HashSet<>(requestedIds))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question order must include every question exactly once");
        }
        for (int i = 0; i < requestedIds.size(); i++) {
            byId.get(requestedIds.get(i)).setDisplayOrder(i + 1);
        }
        return questions.stream()
                .sorted(Comparator.comparing((Question q) -> Optional.ofNullable(q.getDisplayOrder()).orElse(0))
                        .thenComparing(Question::getId))
                .map(q -> toQuestionView(q, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionView> getTeacherAssignmentSubmissions(Integer assignmentId) {
        User teacher = currentUserService.getCurrentUser();
        CodingAssignment assignment = getAssignment(assignmentId);
        requireTeacherCourse(assignment.getLesson().getCourse().getId(), teacher.getId());
        return submissionRepository.findByAssignmentIdWithStudent(assignmentId).stream()
                .map(this::toSubmissionView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentView> getTeacherAssignments(Integer courseId, String status, String search, Pageable pageable) {
        User teacher = currentUserService.getCurrentUser();
        if (courseId != null) {
            requireTeacherCourse(courseId, teacher.getId());
        }
        String cleanStatus = normalize(status);
        String cleanSearch = normalize(search);
        Page<CodingAssignment> assignmentPage = codingAssignmentRepository.findTeacherAssignments(
                teacher.getId(), courseId, cleanStatus, cleanSearch, pageable);
        Map<Integer, SubmissionRepository.AssignmentSubmissionStats> statsByAssignmentId =
                assignmentStats(assignmentPage.getContent());
        return assignmentPage.map(assignment -> toAssignmentView(
                assignment,
                statsByAssignmentId.get(assignment.getId())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubmissionView> getTeacherSubmissions(Integer courseId, Integer assignmentId, SubmissionStatus status,
                                                      String search, Pageable pageable) {
        User teacher = currentUserService.getCurrentUser();
        validateTeacherSubmissionFilters(courseId, assignmentId, teacher.getId());
        Page<Submission> submissionPage = submissionRepository.findTeacherSubmissions(
                        teacher.getId(),
                        courseId,
                        assignmentId,
                        status,
                        normalize(search),
                        pageable);
        Map<Integer, CodeJudgeResult> latestJudgeResults = latestJudgeResults(submissionPage.getContent());
        return submissionPage.map(submission -> toSubmissionView(
                submission,
                latestJudgeResults.get(submission.getId())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public GradingSummaryView getTeacherGradingSummary(Integer courseId, Integer assignmentId, String search) {
        User teacher = currentUserService.getCurrentUser();
        validateTeacherSubmissionFilters(courseId, assignmentId, teacher.getId());
        String cleanSearch = normalize(search);
        GradingSummaryView view = new GradingSummaryView();
        view.setPendingCount(submissionRepository.countTeacherSubmissionsByStatuses(
                teacher.getId(), courseId, assignmentId, PENDING_GRADE_STATUSES, cleanSearch));
        view.setGradedCount(submissionRepository.countTeacherSubmissionsByStatuses(
                teacher.getId(), courseId, assignmentId, GRADED_STATUSES, cleanSearch));
        view.setFailedCount(submissionRepository.countTeacherSubmissionsByStatuses(
                teacher.getId(), courseId, assignmentId, FAILED_STATUSES, cleanSearch));
        return view;
    }

    @Override
    public SubmissionView gradeSubmission(Integer submissionId, GradePayload payload) {
        User teacher = currentUserService.getCurrentUser();
        Submission submission = requireTeacherSubmission(submissionId, teacher.getId());
        validateGrade(submission, payload);
        submission.setScore(payload.getScore());
        submission.setTeacherFeedback(payload.getFeedback().trim());
        submission.setStatus(Boolean.TRUE.equals(payload.getPublish())
                ? SubmissionStatus.GRADED
                : SubmissionStatus.PENDING_REVIEW);
        GradeFeedback feedback = GradeFeedback.builder()
                .submission(submission)
                .teacher(teacher)
                .score(payload.getScore())
                .feedback(payload.getFeedback().trim())
                .build();
        gradeFeedbackRepository.save(feedback);
        return toSubmissionView(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestcaseView> getTeacherTestcases(Integer assignmentId) {
        User teacher = currentUserService.getCurrentUser();
        CodingAssignment assignment = getAssignment(assignmentId);
        requireTeacherCourse(assignment.getLesson().getCourse().getId(), teacher.getId());
        return testcaseRepository.findByAssignmentIdOrderByIdAsc(assignmentId).stream()
                .sorted(Comparator.comparing((Testcase t) -> Optional.ofNullable(t.getDisplayOrder()).orElse(0)).thenComparing(Testcase::getId))
                .map(this::toTestcaseView)
                .toList();
    }

    @Override
    public TestcaseView createTeacherTestcase(Integer assignmentId, TestcasePayload payload) {
        User teacher = currentUserService.getCurrentUser();
        CodingAssignment assignment = getAssignment(assignmentId);
        requireTeacherCourse(assignment.getLesson().getCourse().getId(), teacher.getId());
        validateTestcasePayload(payload);
        Testcase testcase = Testcase.builder()
                .assignment(assignment)
                .inputData(payload.getInput().trim())
                .expectedOutput(payload.getExpectedOutput().trim())
                .isHidden(Boolean.TRUE.equals(payload.getHidden()))
                .points(payload.getPoints())
                .displayOrder(payload.getDisplayOrder())
                .build();
        return toTestcaseView(testcaseRepository.save(testcase));
    }

    @Override
    public TestcaseView updateTeacherTestcase(Integer testcaseId, TestcasePayload payload) {
        User teacher = currentUserService.getCurrentUser();
        Testcase testcase = requireTeacherTestcase(testcaseId, teacher.getId());
        validateTestcasePayload(payload);
        testcase.setInputData(payload.getInput().trim());
        testcase.setExpectedOutput(payload.getExpectedOutput().trim());
        testcase.setIsHidden(Boolean.TRUE.equals(payload.getHidden()));
        testcase.setPoints(payload.getPoints());
        testcase.setDisplayOrder(payload.getDisplayOrder());
        return toTestcaseView(testcaseRepository.save(testcase));
    }

    @Override
    public void deleteTeacherTestcase(Integer testcaseId) {
        User teacher = currentUserService.getCurrentUser();
        Testcase testcase = requireTeacherTestcase(testcaseId, teacher.getId());
        testcaseRepository.delete(testcase);
    }

    @Override
    public SubmissionView judgeSubmission(Integer submissionId) {
        User teacher = currentUserService.getCurrentUser();
        Submission submission = requireTeacherSubmission(submissionId, teacher.getId());
        CodingAssignment assignment = submission.getAssignment();
        List<Testcase> testcases = testcaseRepository.findByAssignmentIdOrderByIdAsc(assignment.getId());
        CodingAssignment judgeAssignment = CodingAssignment.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .timeLimitMs(assignment.getTimeLimitMs())
                .maxScore(assignment.getMaxScore())
                .testcases(testcases)
                .build();
        if (testcases.isEmpty()) {
            CodeJudgeResult result = CodeJudgeResult.builder()
                    .submission(submission)
                    .status(CodeJudgeStatus.ERROR)
                    .totalTests(0)
                    .passedTests(0)
                    .outputLog("No testcases are configured for this assignment.")
                    .executionTimeMs(0L)
                    .build();
            codeJudgeResultRepository.save(result);
            return toSubmissionView(submission, result);
        }
        CodeJudgeAdapter.JudgeOutcome outcome = codeJudgeAdapter.judge(judgeAssignment, submission);
        CodeJudgeResult result = CodeJudgeResult.builder()
                .submission(submission)
                .status(outcome.status())
                .totalTests(outcome.totalTests())
                .passedTests(outcome.passedTests())
                .outputLog(outcome.outputLog())
                .executionTimeMs(outcome.executionTimeMs())
                .build();
        codeJudgeResultRepository.save(result);
        if (outcome.status() == CodeJudgeStatus.PASSED || outcome.status() == CodeJudgeStatus.FAILED) {
            submission.setStatus(outcome.status() == CodeJudgeStatus.PASSED
                    ? SubmissionStatus.AUTO_GRADED
                    : SubmissionStatus.RETURNED);
            submission.setScore(calculateJudgeScore(judgeAssignment, outcome, testcases));
        }
        return toSubmissionView(submission, result);
    }

    private SubmissionView saveSubmission(Integer assignmentId, AssignmentSubmissionPayload payload, SubmissionStatus status) {
        User student = currentUserService.getCurrentUser();
        CodingAssignment assignment = getAssignment(assignmentId);
        requireEnrollment(student.getId(), assignment.getLesson().getCourse().getId());
        Submission submission = submissionRepository.findTopByAssignmentIdAndStudentIdOrderByUpdatedAtDesc(assignmentId, student.getId())
                .filter(s -> s.getStatus() == SubmissionStatus.DRAFT)
                .orElseGet(() -> Submission.builder()
                        .assignment(assignment)
                        .lesson(assignment.getLesson())
                        .student(student)
                        .build());
        submission.setSubmittedContent(trim(payload.getContentText()));
        submission.setCodeLanguage(trim(payload.getCodeLanguage()));
        submission.setCodeContent(trim(payload.getCodeContent()));
        submission.setFilePath(trim(payload.getFilePath()));
        submission.setStatus(status);
        if (status == SubmissionStatus.SUBMITTED) {
            submission.setSubmittedAt(LocalDateTime.now());
        }
        return toSubmissionView(submissionRepository.save(submission));
    }

    private void persistAnswers(QuizAttempt attempt, QuizDraftPayload payload) {
        Map<Integer, QuizAnswer> existing = quizAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));
        Set<Integer> quizQuestionIds = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(attempt.getQuiz().getId()).stream()
                .map(Question::getId)
                .collect(Collectors.toSet());
        for (AnswerPayload answerPayload : Optional.ofNullable(payload.getAnswers()).orElse(List.of())) {
            if (!quizQuestionIds.contains(answerPayload.getQuestionId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Question does not belong to this quiz");
            }
            Question question = questionRepository.findById(answerPayload.getQuestionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Question not found"));
            QuizAnswer answer = existing.getOrDefault(question.getId(), QuizAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .build());
            answer.setSelectedOptionsJson(writeJson(Optional.ofNullable(answerPayload.getSelectedOptionIds()).orElse(List.of())));
            answer.setAnswerText(trim(answerPayload.getAnswerText()));
            quizAnswerRepository.save(answer);
        }
    }

    private BigDecimal gradeAttempt(QuizAttempt attempt) {
        Map<Integer, QuizAnswer> answers = quizAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));
        BigDecimal earned = BigDecimal.ZERO;
        for (Question question : questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(attempt.getQuiz().getId())) {
            Set<Integer> selected = new HashSet<>(readIntegerList(answers.get(question.getId()) == null ? null : answers.get(question.getId()).getSelectedOptionsJson()));
            Set<Integer> correct = new HashSet<>(correctOptionIndexes(question));
            if (!correct.isEmpty() && selected.equals(correct)) {
                earned = earned.add(points(question));
            }
        }
        return earned;
    }

    private void applyQuestion(Question question, QuestionPayload payload) {
        question.setQuestionText(payload.getContent().trim());
        question.setQuestionType(payload.getQuestionType() == null ? QuestionType.SINGLE_CHOICE : payload.getQuestionType());
        question.setPoints(payload.getPoints() == null ? BigDecimal.ONE : payload.getPoints());
        question.setDisplayOrder(payload.getDisplayOrder());
        question.setOptionsJson(writeJson(payload.getOptions()));
        question.setCorrectAnswer(correctOptionIndexes(payload.getOptions()).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
    }

    private void validateQuizPayload(QuizPayload payload) {
        if (payload == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz payload is required");
        }
        if (payload.getTitle() == null || payload.getTitle().trim().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz title is required");
        }
        if (payload.getDurationMinutes() != null && payload.getDurationMinutes() < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz duration must be at least 1 minute");
        }
        if (payload.getMaxAttempts() == null || payload.getMaxAttempts() < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz max attempts must be at least 1");
        }
        if (payload.getPassingScore() != null
                && (payload.getPassingScore().compareTo(BigDecimal.ZERO) < 0
                || payload.getPassingScore().compareTo(new BigDecimal("100")) > 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Passing score must be between 0 and 100");
        }
    }

    private void validateQuizCanPublish(Quiz quiz) {
        List<Question> questions = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId());
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz must have at least one question before publishing");
        }
        BigDecimal total = questions.stream()
                .map(this::points)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz total points must be greater than zero");
        }
        questions.forEach(question -> validateQuestionPayload(toQuestionPayload(question)));
    }

    private void validateQuestionPayload(QuestionPayload payload) {
        if (payload == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question payload is required");
        }
        if (payload.getContent() == null || payload.getContent().trim().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question content is required");
        }
        QuestionType type = payload.getQuestionType() == null ? QuestionType.SINGLE_CHOICE : payload.getQuestionType();
        if (payload.getPoints() != null && payload.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question points must be greater than zero");
        }
        List<OptionPayload> options = Optional.ofNullable(payload.getOptions()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .toList();
        if (options.size() < 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Multiple-choice questions require at least two options");
        }
        for (OptionPayload option : options) {
            if (option.getContent() == null || option.getContent().trim().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Answer option content is required");
            }
        }
        long correctCount = options.stream().filter(option -> Boolean.TRUE.equals(option.getCorrect())).count();
        if (correctCount == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one correct answer is required");
        }
        if (type == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Single choice questions must have exactly one correct answer");
        }
    }

    private boolean questionContentOrScoringChanged(Question question, QuestionPayload payload) {
        QuestionType type = payload.getQuestionType() == null ? QuestionType.SINGLE_CHOICE : payload.getQuestionType();
        BigDecimal points = payload.getPoints() == null ? BigDecimal.ONE : payload.getPoints();
        String correct = correctOptionIndexes(payload.getOptions()).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return !Objects.equals(question.getQuestionText(), payload.getContent().trim())
                || !Objects.equals(question.getQuestionType() == null ? QuestionType.SINGLE_CHOICE : question.getQuestionType(), type)
                || points(question).compareTo(points) != 0
                || !Objects.equals(Optional.ofNullable(question.getOptionsJson()).orElse(""), writeJson(payload.getOptions()))
                || !Objects.equals(Optional.ofNullable(question.getCorrectAnswer()).orElse(""), correct);
    }

    private QuestionPayload toQuestionPayload(Question question) {
        QuestionPayload payload = new QuestionPayload();
        payload.setContent(question.getQuestionText());
        payload.setQuestionType(question.getQuestionType());
        payload.setPoints(points(question));
        payload.setDisplayOrder(question.getDisplayOrder());
        List<OptionPayload> options = readOptions(question).stream().map(option -> {
            OptionPayload optionPayload = new OptionPayload();
            optionPayload.setContent(option.content());
            optionPayload.setCorrect(option.correct());
            return optionPayload;
        }).toList();
        payload.setOptions(options);
        return payload;
    }

    private QuizAttempt requireStudentAttempt(Integer attemptId) {
        User student = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdWithQuizCourse(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Quiz attempt not found"));
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Attempt access denied");
        }
        return attempt;
    }

    private Submission requireTeacherSubmission(Integer submissionId, Integer teacherId) {
        Submission submission = submissionRepository.findByIdWithAssignmentCourse(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Submission not found"));
        requireTeacherCourse(submission.getAssignment().getLesson().getCourse().getId(), teacherId);
        return submission;
    }

    private Testcase requireTeacherTestcase(Integer testcaseId, Integer teacherId) {
        Testcase testcase = testcaseRepository.findByIdWithAssignmentCourse(testcaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Testcase not found"));
        requireTeacherCourse(testcase.getAssignment().getLesson().getCourse().getId(), teacherId);
        return testcase;
    }

    private Quiz requireTeacherQuiz(Integer quizId, Integer teacherId) {
        Quiz quiz = getQuiz(quizId);
        requireTeacherCourse(quiz.getLesson().getCourse().getId(), teacherId);
        return quiz;
    }

    private Lesson requireLessonForTeacher(Integer lessonId, Integer courseId, Integer teacherId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lesson is required");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Lesson not found"));
        if (lesson.getCourse() == null || !lesson.getCourse().getId().equals(courseId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lesson does not belong to this course");
        }
        requireTeacherCourse(courseId, teacherId);
        return lesson;
    }

    private void requireTeacherCourse(Integer courseId, Integer teacherId) {
        courseRepository.findById(courseId)
                .filter(course -> course.getInstructor() != null && course.getInstructor().getId().equals(teacherId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "Teacher can only manage their own course"));
    }

    private void validateTeacherSubmissionFilters(Integer courseId, Integer assignmentId, Integer teacherId) {
        if (courseId != null) {
            requireTeacherCourse(courseId, teacherId);
        }
        if (assignmentId != null) {
            CodingAssignment assignment = getAssignment(assignmentId);
            requireTeacherCourse(assignment.getLesson().getCourse().getId(), teacherId);
            if (courseId != null && !assignment.getLesson().getCourse().getId().equals(courseId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Assignment does not belong to this course");
            }
        }
    }

    private void validateGrade(Submission submission, GradePayload payload) {
        BigDecimal score = payload.getScore();
        if (score == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Score is required");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Score cannot be negative");
        }
        BigDecimal maxScore = Optional.ofNullable(submission.getAssignment())
                .map(CodingAssignment::getMaxScore)
                .orElse(new BigDecimal("100.00"));
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            maxScore = new BigDecimal("100.00");
        }
        if (score.compareTo(maxScore) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Score cannot exceed assignment max score");
        }
    }

    private void validateTestcasePayload(TestcasePayload payload) {
        if (payload.getInput() == null || payload.getInput().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Testcase input is required");
        }
        if (payload.getExpectedOutput() == null || payload.getExpectedOutput().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Expected output is required");
        }
        if (payload.getPoints() == null || payload.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Testcase weight must be greater than zero");
        }
        if (payload.getDisplayOrder() != null && payload.getDisplayOrder() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Display order cannot be negative");
        }
    }

    private BigDecimal calculateJudgeScore(CodingAssignment assignment, CodeJudgeAdapter.JudgeOutcome outcome,
                                           List<Testcase> testcases) {
        BigDecimal maxScore = Optional.ofNullable(assignment.getMaxScore())
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .orElse(new BigDecimal("100.00"));
        BigDecimal totalWeight = testcases.stream()
                .map(this::testcasePoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal earnedWeight = earnedJudgeWeight(outcome, testcases, totalWeight);
        return earnedWeight.multiply(maxScore).divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal earnedJudgeWeight(CodeJudgeAdapter.JudgeOutcome outcome, List<Testcase> testcases,
                                         BigDecimal totalWeight) {
        if (outcome.cases() != null && !outcome.cases().isEmpty()) {
            Map<Integer, Testcase> byId = testcases.stream()
                    .filter(testcase -> testcase.getId() != null)
                    .collect(Collectors.toMap(Testcase::getId, Function.identity(), (left, right) -> left));
            return outcome.cases().stream()
                    .filter(CodeJudgeAdapter.JudgeCaseOutcome::passed)
                    .map(result -> byId.get(result.testcaseId()))
                    .filter(Objects::nonNull)
                    .map(this::testcasePoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (outcome.totalTests() <= 0) {
            return BigDecimal.ZERO;
        }
        return totalWeight
                .multiply(BigDecimal.valueOf(Math.max(0, outcome.passedTests())))
                .divide(BigDecimal.valueOf(outcome.totalTests()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal testcasePoints(Testcase testcase) {
        return Optional.ofNullable(testcase.getPoints())
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.ONE);
    }

    private void requireEnrollment(Integer studentId, Integer courseId) {
        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ACCESS_DENIED, "Student is not enrolled in this course");
        }
    }

    private Quiz getQuiz(Integer quizId) {
        return quizRepository.findByIdWithCourse(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Quiz not found"));
    }

    private CodingAssignment getAssignment(Integer assignmentId) {
        return codingAssignmentRepository.findByIdWithCourse(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Assignment not found"));
    }

    private QuizView toQuizView(Quiz quiz, boolean includeCorrect) {
        QuizView view = new QuizView();
        view.setId(quiz.getId());
        view.setTitle(quiz.getTitle());
        view.setDescription(quiz.getDescription());
        view.setDurationMinutes(quiz.getDurationMinutes());
        view.setMaxAttempts(quiz.getMaxAttempts());
        view.setStatus(quiz.getStatus());
        view.setPassingScore(quiz.getPassingScore());
        if (quiz.getLesson() != null) {
            view.setLessonId(quiz.getLesson().getId());
            view.setLessonTitle(quiz.getLesson().getTitle());
            if (quiz.getLesson().getCourse() != null) {
                view.setCourseId(quiz.getLesson().getCourse().getId());
                view.setCourseTitle(quiz.getLesson().getCourse().getTitle());
            }
        }
        List<QuestionView> questions = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId()).stream()
                .map(q -> toQuestionView(q, includeCorrect))
                .toList();
        view.setQuestions(questions);
        view.setTotalPoints(questions.stream()
                .map(q -> q.getPoints() == null ? BigDecimal.ZERO : q.getPoints())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return view;
    }

    private QuestionView toQuestionView(Question question, boolean includeCorrect) {
        QuestionView view = new QuestionView();
        view.setId(question.getId());
        view.setContent(question.getQuestionText());
        view.setQuestionType(question.getQuestionType() == null ? QuestionType.SINGLE_CHOICE : question.getQuestionType());
        view.setPoints(points(question));
        view.setDisplayOrder(question.getDisplayOrder());
        List<OptionView> options = readOptions(question).stream().map(option -> {
            OptionView optionView = new OptionView();
            optionView.setId(option.id());
            optionView.setContent(option.content());
            optionView.setCorrect(includeCorrect ? option.correct() : null);
            return optionView;
        }).toList();
        view.setOptions(options);
        return view;
    }

    private QuizAttemptView toAttemptView(QuizAttempt attempt, boolean includeCorrect) {
        QuizAttemptView view = new QuizAttemptView();
        view.setId(attempt.getId());
        view.setStatus(attempt.getStatus());
        view.setStartedAt(attempt.getStartedAt());
        view.setSubmittedAt(attempt.getSubmittedAt());
        view.setScore(attempt.getScore());
        view.setTotalPoints(attempt.getTotalPoints());
        QuizView quizView = toQuizView(attempt.getQuiz(), includeCorrect);
        applyDraftAnswers(quizView, attempt);
        view.setQuiz(quizView);
        return view;
    }

    private void applyDraftAnswers(QuizView quizView, QuizAttempt attempt) {
        Map<Integer, List<Integer>> answers = quizAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> readIntegerList(a.getSelectedOptionsJson())));
        quizView.getQuestions().forEach(q -> q.setSelectedOptionIds(answers.getOrDefault(q.getId(), List.of())));
    }

    private SubmissionView toAssignmentShell(CodingAssignment assignment) {
        SubmissionView view = new SubmissionView();
        view.setAssignmentId(assignment.getId());
        view.setAssignmentTitle(assignment.getTitle());
        view.setLessonId(assignment.getLesson().getId());
        view.setCourseId(assignment.getLesson().getCourse().getId());
        view.setCourseTitle(assignment.getLesson().getCourse().getTitle());
        return view;
    }

    private SubmissionView toSubmissionView(Submission submission) {
        CodeJudgeResult latestResult = submission.getId() == null
                ? null
                : codeJudgeResultRepository.findTopBySubmissionIdOrderByCreatedAtDesc(submission.getId()).orElse(null);
        return toSubmissionView(submission, latestResult);
    }

    private SubmissionView toStudentSubmissionView(Submission submission) {
        SubmissionView view = toSubmissionView(submission);
        view.setOutputLog(null);
        if (!Boolean.TRUE.equals(view.getReleased())) {
            view.setScore(null);
            view.setFeedback(null);
            view.setGradedAt(null);
            view.setGradedByName(null);
        }
        return view;
    }

    private SubmissionView toSubmissionView(Submission submission, CodeJudgeResult latestResult) {
        SubmissionView view = submission.getAssignment() != null
                ? toAssignmentShell(submission.getAssignment())
                : toLegacySubmissionShell(submission);
        view.setId(submission.getId());
        view.setStudentName(submission.getStudent() == null ? null : submission.getStudent().getFullName());
        view.setContentText(submission.getSubmittedContent());
        view.setCodeLanguage(submission.getCodeLanguage());
        view.setCodeContent(submission.getCodeContent());
        view.setFilePath(submission.getFilePath());
        view.setStatus(submission.getStatus());
        view.setScore(submission.getScore());
        view.setMaxScore(Optional.ofNullable(submission.getAssignment())
                .map(CodingAssignment::getMaxScore)
                .orElse(new BigDecimal("100.00")));
        view.setFeedback(submission.getTeacherFeedback());
        view.setSubmittedAt(submission.getSubmittedAt());
        view.setUpdatedAt(submission.getUpdatedAt());
        view.setReleased(isReleasedSubmission(submission));
        if (submission.getId() != null) {
            gradeFeedbackRepository.findTopBySubmissionIdOrderByGradedAtDesc(submission.getId()).ifPresent(feedback -> {
                view.setGradedAt(feedback.getGradedAt());
                view.setGradedByName(feedback.getTeacher() == null ? null : feedback.getTeacher().getFullName());
            });
        }
        if (latestResult != null) {
            view.setJudgeStatus(latestResult.getStatus());
            view.setTotalTests(latestResult.getTotalTests());
            view.setPassedTests(latestResult.getPassedTests());
            view.setOutputLog(latestResult.getOutputLog());
        }
        return view;
    }

    private boolean isReleasedSubmission(Submission submission) {
        return submission.getStatus() != null && RELEASED_SUBMISSION_STATUSES.contains(submission.getStatus());
    }

    private SubmissionView toLegacySubmissionShell(Submission submission) {
        SubmissionView view = new SubmissionView();
        if (submission.getLesson() != null) {
            view.setLessonId(submission.getLesson().getId());
            if (submission.getLesson().getCourse() != null) {
                view.setCourseId(submission.getLesson().getCourse().getId());
            }
            if (submission.getLesson().getCodingassignment() != null) {
                view.setAssignmentId(submission.getLesson().getCodingassignment().getId());
                view.setAssignmentTitle(submission.getLesson().getCodingassignment().getTitle());
            } else {
                view.setAssignmentTitle(submission.getLesson().getTitle());
            }
        }
        return view;
    }

    private AssignmentView toAssignmentView(CodingAssignment assignment,
                                            SubmissionRepository.AssignmentSubmissionStats stats) {
        AssignmentView view = new AssignmentView();
        view.setId(assignment.getId());
        view.setTitle(assignment.getTitle());
        view.setType("Coding");
        view.setDueDate(assignment.getDueDate());
        view.setStatus(assignment.getStatus());
        view.setMaxScore(assignment.getMaxScore());
        if (assignment.getLesson() != null) {
            view.setLessonId(assignment.getLesson().getId());
            if (assignment.getLesson().getCourse() != null) {
                view.setCourseId(assignment.getLesson().getCourse().getId());
                view.setCourseTitle(assignment.getLesson().getCourse().getTitle());
            }
        }
        view.setSubmissionCount(stats == null || stats.getSubmissionCount() == null ? 0L : stats.getSubmissionCount());
        view.setPendingCount(stats == null || stats.getPendingCount() == null ? 0L : stats.getPendingCount());
        return view;
    }

    private Map<Integer, SubmissionRepository.AssignmentSubmissionStats> assignmentStats(List<CodingAssignment> assignments) {
        List<Integer> assignmentIds = assignments.stream()
                .map(CodingAssignment::getId)
                .toList();
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return submissionRepository.countSubmissionStatsByAssignmentIds(assignmentIds, PENDING_GRADE_STATUSES).stream()
                .collect(Collectors.toMap(SubmissionRepository.AssignmentSubmissionStats::getAssignmentId, Function.identity()));
    }

    private Map<Integer, CodeJudgeResult> latestJudgeResults(List<Submission> submissions) {
        List<Integer> submissionIds = submissions.stream()
                .map(Submission::getId)
                .filter(Objects::nonNull)
                .toList();
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, CodeJudgeResult> latestBySubmissionId = new HashMap<>();
        for (CodeJudgeResult result : codeJudgeResultRepository.findBySubmissionIdInOrderByCreatedAtDescIdDesc(submissionIds)) {
            if (result.getSubmission() != null) {
                latestBySubmissionId.putIfAbsent(result.getSubmission().getId(), result);
            }
        }
        return latestBySubmissionId;
    }

    private TestcaseView toTestcaseView(Testcase testcase) {
        TestcaseView view = new TestcaseView();
        view.setId(testcase.getId());
        view.setInput(testcase.getInputData());
        view.setExpectedOutput(testcase.getExpectedOutput());
        view.setHidden(testcase.getIsHidden());
        view.setPoints(testcase.getPoints());
        view.setDisplayOrder(testcase.getDisplayOrder());
        return view;
    }

    private BigDecimal totalPoints(Quiz quiz) {
        return questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId()).stream()
                .map(this::points)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal points(Question question) {
        return question.getPoints() == null ? BigDecimal.ONE : question.getPoints();
    }

    private List<Integer> correctOptionIndexes(Question question) {
        if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
            return readOptions(question).stream().filter(ParsedOption::correct).map(ParsedOption::id).toList();
        }
        List<Integer> indexes = new ArrayList<>();
        for (String part : question.getCorrectAnswer().split(",")) {
            try {
                indexes.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                List<ParsedOption> options = readOptions(question);
                for (ParsedOption option : options) {
                    if (option.content().equalsIgnoreCase(part.trim())) {
                        indexes.add(option.id());
                    }
                }
            }
        }
        return indexes;
    }

    private List<Integer> correctOptionIndexes(List<OptionPayload> options) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            if (Boolean.TRUE.equals(options.get(i).getCorrect())) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private List<ParsedOption> readOptions(Question question) {
        if (question.getOptionsJson() == null || question.getOptionsJson().isBlank()) {
            return List.of();
        }
        try {
            List<OptionPayload> payloads = objectMapper.readValue(question.getOptionsJson(), new TypeReference<>() {});
            List<Integer> correct = question.getCorrectAnswer() == null ? List.of() : correctOptionIndexes(question);
            List<ParsedOption> parsed = new ArrayList<>();
            for (int i = 0; i < payloads.size(); i++) {
                parsed.add(new ParsedOption(i, payloads.get(i).getContent(), Boolean.TRUE.equals(payloads.get(i).getCorrect()) || correct.contains(i)));
            }
            return parsed;
        } catch (Exception ignored) {
            try {
                List<String> values = objectMapper.readValue(question.getOptionsJson(), new TypeReference<>() {});
                List<ParsedOption> parsed = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) {
                    parsed.add(new ParsedOption(i, values.get(i), values.get(i).equalsIgnoreCase(Optional.ofNullable(question.getCorrectAnswer()).orElse(""))));
                }
                return parsed;
            } catch (Exception ex) {
                return List.of();
            }
        }
    }

    private List<Integer> readIntegerList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid JSON payload");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private record ParsedOption(Integer id, String content, Boolean correct) {
    }
}
