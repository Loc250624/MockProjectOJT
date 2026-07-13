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
                .map(this::toSubmissionView)
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
        return toSubmissionView(submission);
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
        quizRepository.delete(quiz);
    }

    @Override
    public QuestionView createTeacherQuestion(Integer quizId, QuestionPayload payload) {
        User teacher = currentUserService.getCurrentUser();
        Quiz quiz = requireTeacherQuiz(quizId, teacher.getId());
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
        applyQuestion(question, payload);
        return toQuestionView(question, true);
    }

    @Override
    public void deleteTeacherQuestion(Integer questionId) {
        User teacher = currentUserService.getCurrentUser();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Question not found"));
        requireTeacherQuiz(question.getQuiz().getId(), teacher.getId());
        questionRepository.delete(question);
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
    public SubmissionView gradeSubmission(Integer submissionId, GradePayload payload) {
        User teacher = currentUserService.getCurrentUser();
        Submission submission = requireTeacherSubmission(submissionId, teacher.getId());
        submission.setScore(payload.getScore());
        submission.setTeacherFeedback(payload.getFeedback().trim());
        submission.setStatus(SubmissionStatus.GRADED);
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
        return testcaseRepository.findAll().stream()
                .filter(t -> t.getAssignment() != null && t.getAssignment().getId().equals(assignmentId))
                .sorted(Comparator.comparing((Testcase t) -> Optional.ofNullable(t.getDisplayOrder()).orElse(0)).thenComparing(Testcase::getId))
                .map(this::toTestcaseView)
                .toList();
    }

    @Override
    public TestcaseView createTeacherTestcase(Integer assignmentId, TestcasePayload payload) {
        User teacher = currentUserService.getCurrentUser();
        CodingAssignment assignment = getAssignment(assignmentId);
        requireTeacherCourse(assignment.getLesson().getCourse().getId(), teacher.getId());
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
    public SubmissionView judgeSubmission(Integer submissionId) {
        User teacher = currentUserService.getCurrentUser();
        Submission submission = requireTeacherSubmission(submissionId, teacher.getId());
        CodeJudgeAdapter.JudgeOutcome outcome = codeJudgeAdapter.judge(submission.getAssignment(), submission);
        CodeJudgeResult result = CodeJudgeResult.builder()
                .submission(submission)
                .status(outcome.passed() ? CodeJudgeStatus.PASSED : CodeJudgeStatus.FAILED)
                .totalTests(outcome.totalTests())
                .passedTests(outcome.passedTests())
                .outputLog(outcome.outputLog())
                .executionTimeMs(outcome.executionTimeMs())
                .build();
        codeJudgeResultRepository.save(result);
        submission.setStatus(outcome.passed() ? SubmissionStatus.AUTO_GRADED : SubmissionStatus.RETURNED);
        submission.setScore(outcome.totalTests() == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(outcome.passedTests()).multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(outcome.totalTests()), 2, RoundingMode.HALF_UP));
        return toSubmissionView(submission);
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
        return view;
    }

    private SubmissionView toSubmissionView(Submission submission) {
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
        view.setFeedback(submission.getTeacherFeedback());
        view.setSubmittedAt(submission.getSubmittedAt());
        codeJudgeResultRepository.findTopBySubmissionIdOrderByCreatedAtDesc(submission.getId()).ifPresent(result -> {
            view.setJudgeStatus(result.getStatus());
            view.setTotalTests(result.getTotalTests());
            view.setPassedTests(result.getPassedTests());
            view.setOutputLog(result.getOutputLog());
        });
        return view;
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

    private record ParsedOption(Integer id, String content, Boolean correct) {
    }
}
