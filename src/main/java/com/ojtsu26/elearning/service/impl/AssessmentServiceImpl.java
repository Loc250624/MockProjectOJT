package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.AnswerPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.OptionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.OptionView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizAttemptView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizDraftPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.ResultSummaryView;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizAnswer;
import com.ojtsu26.elearning.model.entity.QuizAttempt;
import com.ojtsu26.elearning.model.entity.QuizAttemptQuestion;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.model.enums.QuestionType;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizAnswerRepository;
import com.ojtsu26.elearning.repository.QuizAttemptRepository;
import com.ojtsu26.elearning.repository.QuizAttemptQuestionRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.service.AssessmentService;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.NotificationService;
import com.ojtsu26.elearning.service.quiz.QuizAttemptApplicationService;
import com.ojtsu26.elearning.service.quiz.QuizQuestionTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentServiceImpl implements AssessmentService {
    private static final int QUIZ_QUESTION_LIMIT = 10;

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    @Autowired(required = false)
    private QuizAttemptApplicationService quizAttemptApplicationService;

    @Autowired(required = false)
    private QuizAttemptQuestionRepository attemptQuestionRepository;

    @Autowired(required = false)
    private com.ojtsu26.elearning.service.quiz.QuizQuestionAssignmentService quizQuestionAssignmentService;

    @Override
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
        QuizAttempt attempt = getOrCreateDraftAttempt(quiz, student);
        QuizView view = toQuizView(
                quiz, false, selectedQuestionsForAttempt(attempt));
        applyDraftAnswers(view, attempt);
        return view;
    }

    @Override
    public QuizAttemptView startQuizAttempt(Integer quizId) {
        if (quizAttemptApplicationService != null) {
            return toCanonicalAttemptView(quizAttemptApplicationService.startOrResume(quizId));
        }
        User student = currentUserService.getCurrentUser();
        Quiz quiz = getQuiz(quizId);
        requireEnrollment(student.getId(), quiz.getLesson().getCourse().getId());
        if (quiz.getStatus() != null && quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz is not published");
        }
        return toAttemptView(getOrCreateDraftAttempt(quiz, student), false);
    }

    @Override
    public QuizAttemptView saveQuizDraft(Integer attemptId, QuizDraftPayload payload) {
        if (quizAttemptApplicationService != null) {
            return toCanonicalAttemptView(quizAttemptApplicationService.saveSelectedAnswers(
                    attemptId, selectedAnswerMap(payload)));
        }
        QuizAttempt attempt = requireStudentAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Submitted attempts cannot be edited");
        }
        persistAnswers(attempt, payload);
        return toAttemptView(attempt, false);
    }

    @Override
    public QuizAttemptView submitQuizAttempt(Integer attemptId, QuizDraftPayload payload) {
        if (quizAttemptApplicationService != null) {
            return toCanonicalAttemptView(quizAttemptApplicationService.submitSelectedAnswers(
                    attemptId, selectedAnswerMap(payload)));
        }
        QuizAttempt attempt = requireStudentAttempt(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Attempt already submitted");
        }
        persistAnswers(attempt, payload);
        List<Question> selectedQuestions = selectedQuestionsForAttempt(attempt);
        BigDecimal total = totalPoints(selectedQuestions);
        BigDecimal raw = gradeAttempt(attempt, selectedQuestions);
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
                .filter(attempt -> attempt.getStatus() != QuizAttemptStatus.DRAFT)
                .map(attempt -> toAttemptView(attempt, true))
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
    public List<QuizView> getTeacherCourseQuizzes(Integer courseId) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacherCourse(courseId, teacher.getId());
        return quizRepository.findByCourseId(courseId).stream()
                .map(quiz -> toQuizView(quiz, true))
                .toList();
    }

    @Override
    public QuizView createTeacherQuiz(Integer courseId, QuizPayload payload) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacherCourse(courseId, teacher.getId());
        validateQuizPayload(payload);
        if (payload.getStatus() == QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Create quiz as draft, then publish after adding questions");
        }
        Lesson lesson = requireLessonForTeacher(payload.getLessonId(), courseId, teacher.getId());
        if (quizRepository.existsByLessonId(lesson.getId())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_CONTENT_ALREADY_EXISTS,
                    "This quiz lesson already has a quiz");
        }
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
            Lesson lesson = requireLessonForTeacher(
                    payload.getLessonId(), quiz.getLesson().getCourse().getId(), teacher.getId());
            quizRepository.findByLessonId(lesson.getId())
                    .filter(existing -> !existing.getId().equals(quiz.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException(ErrorCode.ASSESSMENT_CONTENT_ALREADY_EXISTS,
                                "This quiz lesson already has a quiz");
                    });
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
    public void archiveTeacherQuiz(Integer quizId) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacherQuiz(quizId, teacher.getId()).setStatus(QuizStatus.ARCHIVED);
    }

    @Override
    public void deleteTeacherQuiz(Integer quizId) {
        User teacher = currentUserService.getCurrentUser();
        if (quizAttemptRepository.countByQuizId(quizId) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Quiz has attempt history; archive it instead of deleting it");
        }
        requireTeacherQuiz(quizId, teacher.getId());
        quizRepository.deleteById(quizId);
        quizRepository.flush();
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
        if (question.getQuiz() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Quiz question not found");
        }
        requireTeacherQuiz(question.getQuiz().getId(), teacher.getId());
        validateQuestionPayload(payload);
        boolean hasSnapshot = attemptQuestionRepository != null
                && attemptQuestionRepository.countByQuestionId(questionId) > 0;
        if (quizAnswerRepository.countByQuestionId(questionId) > 0
                && !hasSnapshot
                && questionContentOrScoringChanged(question, payload)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Question has submitted answers; create a new question instead of changing scored content");
        }
        question.setVersion(Objects.requireNonNullElse(question.getVersion(), 1) + 1);
        applyQuestion(question, payload);
        return toQuestionView(question, true);
    }

    @Override
    public void deleteTeacherQuestion(Integer questionId) {
        User teacher = currentUserService.getCurrentUser();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Question not found"));
        if (question.getQuiz() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Quiz question not found");
        }
        requireTeacherQuiz(question.getQuiz().getId(), teacher.getId());
        if (quizAnswerRepository.countByQuestionId(questionId) > 0
                || (attemptQuestionRepository != null
                && attemptQuestionRepository.countByQuestionId(questionId) > 0)) {
            question.setActive(false);
            question.setReviewStatus(QuestionReviewStatus.ARCHIVED);
            return;
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
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Question order must include every question exactly once");
        }
        for (int index = 0; index < requestedIds.size(); index++) {
            byId.get(requestedIds.get(index)).setDisplayOrder(index + 1);
        }
        return questions.stream()
                .sorted(Comparator.comparing(
                                (Question question) -> Optional.ofNullable(question.getDisplayOrder()).orElse(0))
                        .thenComparing(Question::getId))
                .map(question -> toQuestionView(question, true))
                .toList();
    }

    private void persistAnswers(QuizAttempt attempt, QuizDraftPayload payload) {
        Map<Integer, QuizAnswer> existing = quizAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));
        Set<Integer> quizQuestionIds = selectedQuestionsForAttempt(attempt).stream()
                .map(Question::getId)
                .collect(Collectors.toSet());
        List<AnswerPayload> answers = payload == null
                ? List.of()
                : Optional.ofNullable(payload.getAnswers()).orElse(List.of());
        for (AnswerPayload answerPayload : answers) {
            if (!quizQuestionIds.contains(answerPayload.getQuestionId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Question does not belong to this quiz");
            }
            Question question = questionRepository.findById(answerPayload.getQuestionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Question not found"));
            QuizAnswer answer = existing.getOrDefault(question.getId(), QuizAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .build());
            answer.setSelectedOptionsJson(writeJson(
                    Optional.ofNullable(answerPayload.getSelectedOptionIds()).orElse(List.of())));
            answer.setAnswerText(trim(answerPayload.getAnswerText()));
            quizAnswerRepository.save(answer);
        }
    }

    private BigDecimal gradeAttempt(QuizAttempt attempt,
                                    List<Question> selectedQuestions) {
        Map<Integer, QuizAnswer> answers = quizAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));
        BigDecimal earned = BigDecimal.ZERO;
        for (Question question : selectedQuestions) {
            QuizAnswer answer = answers.get(question.getId());
            Set<Integer> selected = new HashSet<>(readIntegerList(
                    answer == null ? null : answer.getSelectedOptionsJson()));
            Set<Integer> correct = new HashSet<>(
                    AssessmentOptionCodec.correctIndexes(question, objectMapper));
            if (!correct.isEmpty() && selected.equals(correct)) {
                earned = earned.add(points(question));
            }
        }
        return earned;
    }

    private void applyQuestion(Question question, QuestionPayload payload) {
        question.setQuestionText(payload.getContent().trim());
        question.setQuestionType(payload.getQuestionType() == null
                ? QuestionType.SINGLE_CHOICE
                : payload.getQuestionType());
        question.setPoints(payload.getPoints() == null ? BigDecimal.ONE : payload.getPoints());
        question.setDisplayOrder(payload.getDisplayOrder());
        question.setOptionsJson(writeJson(payload.getOptions()));
        question.setCorrectAnswer(AssessmentOptionCodec.correctAnswerValue(payload.getOptions()));
        question.setTopicCode(com.ojtsu26.elearning.service.quiz.StratifiedQuestionSampler.normalizeTopic(
                payload.getTopicCode()));
        question.setDifficulty(Objects.requireNonNullElse(payload.getDifficulty(), QuestionDifficulty.MEDIUM));
        question.setReviewStatus(Objects.requireNonNullElse(
                payload.getReviewStatus(), QuestionReviewStatus.APPROVED));
        question.setActive(!Boolean.FALSE.equals(payload.getActive()));
        question.setGenerationSource(Objects.requireNonNullElse(
                payload.getGenerationSource(), QuestionGenerationSource.MANUAL));
        if (question.getVersion() == null) {
            question.setVersion(1);
        }
    }

    private void validateQuizPayload(QuizPayload payload) {
        if (payload == null || payload.getTitle() == null || payload.getTitle().trim().isBlank()) {
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
        if (quizQuestionAssignmentService != null) {
            var readiness = quizQuestionAssignmentService.readiness(quiz.getId());
            if (!readiness.ready()) {
                String shortages = readiness.buckets().stream()
                        .filter(bucket -> !bucket.ready())
                        .map(bucket -> bucket.topicCode() + "/" + bucket.difficulty()
                                + " required " + bucket.required() + ", available " + bucket.available())
                        .collect(Collectors.joining("; "));
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "Quiz question bank is not ready: " + shortages);
            }
        }
        List<Question> questions = questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId());
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Quiz must have at least one question before publishing");
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
        if (payload == null || payload.getContent() == null || payload.getContent().trim().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question content is required");
        }
        QuestionType type = payload.getQuestionType() == null
                ? QuestionType.SINGLE_CHOICE
                : payload.getQuestionType();
        if (payload.getPoints() != null && payload.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question points must be greater than zero");
        }
        List<OptionPayload> options = Optional.ofNullable(payload.getOptions()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .toList();
        if (options.size() < 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Multiple-choice questions require at least two options");
        }
        for (OptionPayload option : options) {
            if (option.getContent() == null || option.getContent().trim().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Answer option content is required");
            }
        }
        long correctCount = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getCorrect()))
                .count();
        if (correctCount == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one correct answer is required");
        }
        if (type == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Single choice questions must have exactly one correct answer");
        }
    }

    private boolean questionContentOrScoringChanged(Question question, QuestionPayload payload) {
        QuestionType type = payload.getQuestionType() == null
                ? QuestionType.SINGLE_CHOICE
                : payload.getQuestionType();
        BigDecimal newPoints = payload.getPoints() == null ? BigDecimal.ONE : payload.getPoints();
        String correct = AssessmentOptionCodec.correctAnswerValue(payload.getOptions());
        return !Objects.equals(question.getQuestionText(), payload.getContent().trim())
                || !Objects.equals(question.getQuestionType() == null
                        ? QuestionType.SINGLE_CHOICE
                        : question.getQuestionType(), type)
                || points(question).compareTo(newPoints) != 0
                || !Objects.equals(Optional.ofNullable(question.getOptionsJson()).orElse(""),
                        writeJson(payload.getOptions()))
                || !Objects.equals(Optional.ofNullable(question.getCorrectAnswer()).orElse(""), correct);
    }

    private QuestionPayload toQuestionPayload(Question question) {
        QuestionPayload payload = new QuestionPayload();
        payload.setContent(question.getQuestionText());
        payload.setQuestionType(question.getQuestionType());
        payload.setPoints(points(question));
        payload.setDisplayOrder(question.getDisplayOrder());
        payload.setOptions(readOptions(question).stream().map(option -> {
            OptionPayload item = new OptionPayload();
            item.setContent(option.content());
            item.setCorrect(option.correct());
            return item;
        }).toList());
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

    private Quiz requireTeacherQuiz(Integer quizId, Integer teacherId) {
        Quiz quiz = getQuiz(quizId);
        requireTeacherCourse(quiz.getLesson().getCourse().getId(), teacherId);
        return quiz;
    }

    private Lesson requireLessonForTeacher(Integer lessonId, Integer courseId, Integer teacherId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lesson is required");
        }
        Lesson lesson = lessonRepository.findByIdWithCourseAndAssessment(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Lesson not found"));
        if (lesson.getCourse() == null || !lesson.getCourse().getId().equals(courseId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lesson does not belong to this course");
        }
        requireTeacherCourse(courseId, teacherId);
        if (lesson.getType() != LessonType.QUIZ) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lesson type must be QUIZ for this assessment");
        }
        return lesson;
    }

    private void requireTeacherCourse(Integer courseId, Integer teacherId) {
        if (courseId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Course is required");
        }
        courseRepository.findById(courseId)
                .filter(course -> course.getInstructor() != null
                        && course.getInstructor().getId().equals(teacherId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "Teacher can only manage their own course"));
    }

    private void requireEnrollment(Integer studentId, Integer courseId) {
        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ACCESS_DENIED,
                    "Student is not enrolled in this course");
        }
    }

    private Quiz getQuiz(Integer quizId) {
        return quizRepository.findByIdWithCourse(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Quiz not found"));
    }

    private QuizView toQuizView(Quiz quiz, boolean includeCorrect) {
        return toQuizView(
                quiz,
                includeCorrect,
                questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quiz.getId()));
    }

    private QuizView toQuizView(Quiz quiz,
                                boolean includeCorrect,
                                List<Question> selectedQuestions) {
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
        List<QuestionView> questions = selectedQuestions.stream()
                .map(question -> toQuestionView(question, includeCorrect))
                .toList();
        view.setQuestions(questions);
        view.setTotalPoints(questions.stream()
                .map(question -> question.getPoints() == null ? BigDecimal.ZERO : question.getPoints())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return view;
    }

    private QuestionView toQuestionView(Question question, boolean includeCorrect) {
        QuestionView view = new QuestionView();
        view.setId(question.getId());
        view.setContent(question.getQuestionText());
        view.setQuestionType(question.getQuestionType() == null
                ? QuestionType.SINGLE_CHOICE
                : question.getQuestionType());
        view.setPoints(points(question));
        view.setDisplayOrder(question.getDisplayOrder());
        view.setTopicCode(question.getTopicCode());
        view.setDifficulty(question.getDifficulty());
        view.setReviewStatus(question.getReviewStatus());
        view.setVersion(question.getVersion());
        view.setActive(question.getActive());
        view.setGenerationSource(question.getGenerationSource());
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
        if (quizAttemptApplicationService != null) {
            QuizAttemptApplicationService.AttemptSession session =
                    quizAttemptApplicationService.getOwnedAttempt(attempt.getId());
            if (!session.questions().isEmpty()) {
                return toCanonicalAttemptView(session);
            }
        }
        QuizAttemptView view = new QuizAttemptView();
        view.setId(attempt.getId());
        view.setStatus(attempt.getStatus());
        view.setStartedAt(attempt.getStartedAt());
        view.setSubmittedAt(attempt.getSubmittedAt());
        view.setScore(attempt.getScore());
        view.setTotalPoints(attempt.getTotalPoints());
        QuizView quizView = toQuizView(
                attempt.getQuiz(),
                includeCorrect,
                selectedQuestionsForAttempt(attempt));
        applyDraftAnswers(quizView, attempt);
        view.setQuiz(quizView);
        return view;
    }

    private Map<Integer, List<Integer>> selectedAnswerMap(QuizDraftPayload payload) {
        Map<Integer, List<Integer>> answers = new LinkedHashMap<>();
        if (payload == null || payload.getAnswers() == null) {
            return answers;
        }
        for (AnswerPayload answer : payload.getAnswers()) {
            if (answer != null && answer.getQuestionId() != null) {
                answers.put(answer.getQuestionId(),
                        Optional.ofNullable(answer.getSelectedOptionIds()).orElse(List.of()));
            }
        }
        return answers;
    }

    private QuizAttemptView toCanonicalAttemptView(
            QuizAttemptApplicationService.AttemptSession session) {
        QuizAttempt attempt = session.attempt();
        boolean includeCorrect = attempt.getStatus() != QuizAttemptStatus.DRAFT;
        QuizView quizView = toQuizView(attempt.getQuiz(), false);
        List<QuestionView> questions = session.questions().stream()
                .map(assignment -> toSnapshotQuestionView(
                        assignment,
                        session.answers().get(assignment.getQuestion().getId()),
                        includeCorrect))
                .toList();
        quizView.setQuestions(questions);
        quizView.setTotalPoints(attempt.getTotalPoints());

        QuizAttemptView view = new QuizAttemptView();
        view.setId(attempt.getId());
        view.setStatus(attempt.getStatus());
        view.setStartedAt(attempt.getStartedAt());
        view.setSubmittedAt(attempt.getSubmittedAt());
        view.setScore(attempt.getScore());
        view.setTotalPoints(attempt.getTotalPoints());
        view.setQuiz(quizView);
        return view;
    }

    private QuestionView toSnapshotQuestionView(QuizAttemptQuestion assignment,
                                                QuizAnswer answer,
                                                boolean includeCorrect) {
        Question snapshot = Question.builder()
                .id(assignment.getQuestion().getId())
                .questionText(QuizQuestionTextNormalizer.stripCheckpointPrefix(
                        assignment.getQuestionTextSnapshot()))
                .optionsJson(assignment.getOptionsJsonSnapshot())
                .correctAnswer(assignment.getCorrectAnswerSnapshot())
                .points(assignment.getPointsSnapshot())
                .displayOrder(assignment.getDisplayOrder())
                .build();
        if (assignment.getQuestionTypeSnapshot() != null) {
            try {
                snapshot.setQuestionType(QuestionType.valueOf(assignment.getQuestionTypeSnapshot()));
            } catch (IllegalArgumentException ignored) {
                snapshot.setQuestionType(QuestionType.SINGLE_CHOICE);
            }
        }
        QuestionView view = toQuestionView(snapshot, includeCorrect);
        view.setSelectedOptionIds(answer == null
                ? List.of()
                : readIntegerList(answer.getSelectedOptionsJson()));
        return view;
    }

    private void applyDraftAnswers(QuizView quizView, QuizAttempt attempt) {
        Map<Integer, List<Integer>> answers = quizAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getId(),
                        answer -> readIntegerList(answer.getSelectedOptionsJson())));
        quizView.getQuestions().forEach(question ->
                question.setSelectedOptionIds(answers.getOrDefault(question.getId(), List.of())));
    }

    private BigDecimal totalPoints(List<Question> questions) {
        return questions.stream()
                .map(this::points)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private QuizAttempt getOrCreateDraftAttempt(Quiz quiz, User student) {
        Optional<QuizAttempt> existing = quizAttemptRepository
                .findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                        quiz.getId(), student.getId(), QuizAttemptStatus.DRAFT);
        if (existing.isPresent()) {
            selectedQuestionsForAttempt(existing.get());
            return existing.get();
        }

        List<Question> selectedQuestions = randomQuizQuestions(quiz.getId());
        if (selectedQuestions.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST, "Quiz must have at least one question");
        }
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.DRAFT)
                .selectedQuestionIdsJson(writeJson(
                        selectedQuestions.stream().map(Question::getId).toList()))
                .totalPoints(totalPoints(selectedQuestions))
                .build();
        return quizAttemptRepository.save(attempt);
    }

    private List<Question> selectedQuestionsForAttempt(QuizAttempt attempt) {
        List<Integer> selectedIds =
                readIntegerList(attempt.getSelectedQuestionIdsJson()).stream()
                        .distinct()
                        .limit(QUIZ_QUESTION_LIMIT)
                        .toList();
        if (!selectedIds.isEmpty()) {
            Map<Integer, Question> questionsById = questionRepository
                    .findByQuizIdAndIdIn(attempt.getQuiz().getId(), selectedIds)
                    .stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));
            List<Question> selected = selectedIds.stream()
                    .map(questionsById::get)
                    .filter(Objects::nonNull)
                    .toList();
            if (!selected.isEmpty()) {
                return selected;
            }
        }

        if (attempt.getStatus() != QuizAttemptStatus.DRAFT) {
            return questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(
                    attempt.getQuiz().getId());
        }

        List<Question> selected = randomQuizQuestions(attempt.getQuiz().getId());
        if (selected.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST, "Quiz must have at least one question");
        }
        attempt.setSelectedQuestionIdsJson(writeJson(
                selected.stream().map(Question::getId).toList()));
        attempt.setTotalPoints(totalPoints(selected));
        quizAttemptRepository.save(attempt);
        return selected;
    }

    private List<Question> randomQuizQuestions(Integer quizId) {
        return questionRepository
                .findRandomByQuizId(
                        quizId, PageRequest.of(0, QUIZ_QUESTION_LIMIT))
                .stream()
                .limit(QUIZ_QUESTION_LIMIT)
                .toList();
    }

    private BigDecimal points(Question question) {
        return question.getPoints() == null ? BigDecimal.ONE : question.getPoints();
    }

    private List<AssessmentOptionCodec.ParsedOption> readOptions(Question question) {
        return AssessmentOptionCodec.readOptions(question, objectMapper);
    }

    private List<Integer> readIntegerList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid JSON payload");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
