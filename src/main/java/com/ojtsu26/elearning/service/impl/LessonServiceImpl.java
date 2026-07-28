package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.mapper.LessonMapper;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.service.LessonService;
import com.ojtsu26.elearning.model.enums.LessonType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final LessonMapper lessonMapper;

    @Override
    public List<LessonResponseDTO> findAll() {
        return lessonRepository.findAll().stream()
                .filter(lesson -> lesson.getType() != LessonType.RETIRED)
                .map(lessonMapper::toDto)
                .collect(Collectors.toList());
    }

    private void verifyCourseOwnership(Integer courseId, Integer instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("Access Denied: You do not own this course");
        }
    }

    @Override
    public List<LessonResponseDTO> findByCourseId(Integer courseId, Integer instructorId) {
        // Admin or Owner can view, but for now we enforce ownership or just return it.
        // The requirements state "Teachers may only manage lessons that belong to their own courses".
        if (instructorId != null) {
            verifyCourseOwnership(courseId, instructorId);
        }
        return lessonRepository.findByCourseIdWithAssessmentOrderByOrderIndexAsc(courseId).stream()
                .map(lessonMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public LessonResponseDTO findById(Integer id) {
        Lesson entity = lessonRepository.findByIdWithCourseAndAssessment(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        return lessonMapper.toDto(entity);
    }

    @Override
    public LessonResponseDTO findById(Integer id, Integer instructorId) {
        Lesson entity = lessonRepository.findByIdWithCourseAndAssessment(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        if (instructorId != null && entity.getCourse() != null) {
            verifyCourseOwnership(entity.getCourse().getId(), instructorId);
        }
        return lessonMapper.toDto(entity);
    }

    @Override
    public LessonResponseDTO create(LessonRequestDTO requestDTO, Integer instructorId) {
        verifyCourseOwnership(requestDTO.getCourseId(), instructorId);
        validateActiveType(requestDTO.getType());
        
        List<Lesson> existingLessons = lessonRepository
                .findByCourseIdWithAssessmentOrderByOrderIndexAsc(requestDTO.getCourseId());
        int targetIndex = requestDTO.getOrderIndex() != null ? requestDTO.getOrderIndex() : existingLessons.size() + 1;
        
        // Shift lessons down
        for (Lesson l : existingLessons) {
            if (l.getOrderIndex() >= targetIndex) {
                l.setOrderIndex(l.getOrderIndex() + 1);
                lessonRepository.save(l);
            }
        }
        
        Lesson entity = lessonMapper.toEntity(requestDTO);
        entity.setOrderIndex(targetIndex);
        Lesson saved = lessonRepository.save(entity);
        return lessonMapper.toDto(saved);
    }

    @Override
    public LessonResponseDTO update(Integer id, LessonRequestDTO requestDTO, Integer instructorId) {
        Lesson existing = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        if (existing.getType() == LessonType.RETIRED) {
            throw new RuntimeException("Lesson not found");
        }
        validateActiveType(requestDTO.getType());
        
        verifyCourseOwnership(existing.getCourse().getId(), instructorId);
        
        int oldIndex = existing.getOrderIndex();
        int newIndex = requestDTO.getOrderIndex();
        
        existing.setTitle(requestDTO.getTitle());
        existing.setContent(requestDTO.getContent());
        if (requestDTO.getType() != existing.getType()
                && ((existing.getQuiz() != null && requestDTO.getType() != com.ojtsu26.elearning.model.enums.LessonType.QUIZ)
                || (existing.getVideo() != null && requestDTO.getType() != com.ojtsu26.elearning.model.enums.LessonType.VIDEO))) {
            throw new RuntimeException("Cannot change lesson type while matching lesson content exists");
        }
        existing.setType(requestDTO.getType());
        
        if (oldIndex != newIndex) {
            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(existing.getCourse().getId());
            if (newIndex < oldIndex) {
                for (Lesson l : lessons) {
                    if (l.getId().equals(id)) continue;
                    if (l.getOrderIndex() >= newIndex && l.getOrderIndex() < oldIndex) {
                        l.setOrderIndex(l.getOrderIndex() + 1);
                        lessonRepository.save(l);
                    }
                }
            } else {
                for (Lesson l : lessons) {
                    if (l.getId().equals(id)) continue;
                    if (l.getOrderIndex() > oldIndex && l.getOrderIndex() <= newIndex) {
                        l.setOrderIndex(l.getOrderIndex() - 1);
                        lessonRepository.save(l);
                    }
                }
            }
            existing.setOrderIndex(newIndex);
        }
        
        Lesson updated = lessonRepository.save(existing);
        return lessonMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id, Integer instructorId) {
        Lesson existing = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        if (existing.getType() == LessonType.RETIRED) {
            throw new RuntimeException("Lesson not found");
        }
        verifyCourseOwnership(existing.getCourse().getId(), instructorId);
        Integer courseId = existing.getCourse().getId();
        
        lessonRepository.delete(existing);
        lessonRepository.flush(); // ensure delete is committed before normalizing
        
        // Normalize remaining
        List<Lesson> remaining = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        int idx = 1;
        for (Lesson l : remaining) {
            if (l.getOrderIndex() != idx) {
                l.setOrderIndex(idx);
                lessonRepository.save(l);
            }
            idx++;
        }
    }

    @Override
    public void reorderLessons(Integer courseId, List<Integer> lessonIdsInOrder, Integer instructorId) {
        verifyCourseOwnership(courseId, instructorId);
        int idx = 1;
        for (Integer id : lessonIdsInOrder) {
            Lesson lesson = lessonRepository.findById(id).orElse(null);
            if (lesson != null && lesson.getType() != LessonType.RETIRED
                    && lesson.getCourse().getId().equals(courseId)) {
                lesson.setOrderIndex(idx++);
                lessonRepository.save(lesson);
            }
        }
    }

    private void validateActiveType(LessonType type) {
        if (type != LessonType.VIDEO && type != LessonType.QUIZ) {
            throw new RuntimeException("Lesson type must be VIDEO or QUIZ");
        }
    }
}
