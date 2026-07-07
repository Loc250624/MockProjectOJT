package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import com.ojtsu26.elearning.mapper.VideoMapper;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.VideoRepository;
import com.ojtsu26.elearning.service.VideoService;
import com.ojtsu26.elearning.common.VideoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final LessonRepository lessonRepository;
    private final VideoMapper videoMapper;

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    /**
     * Resolves a Lesson by id and, when instructorId is non-null, verifies
     * that the lesson's course belongs to that instructor.
     */
    private Lesson resolveLesson(Integer lessonId, Integer instructorId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found with id: " + lessonId));

        if (instructorId != null) {
            Integer courseInstructorId = lesson.getCourse() != null
                    ? lesson.getCourse().getInstructor().getId()
                    : null;
            if (!instructorId.equals(courseInstructorId)) {
                throw new RuntimeException("Access Denied: You do not own the course this lesson belongs to.");
            }
        }
        return lesson;
    }

    /**
     * Resolves a Video by id and validates instructor ownership through its lesson.
     */
    private Video resolveVideo(Integer videoId, Integer instructorId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + videoId));

        if (instructorId != null && video.getLesson() != null) {
            Integer courseInstructorId = video.getLesson().getCourse() != null
                    ? video.getLesson().getCourse().getInstructor().getId()
                    : null;
            if (!instructorId.equals(courseInstructorId)) {
                throw new RuntimeException("Access Denied: You do not own the course this video belongs to.");
            }
        }
        return video;
    }

    // ----------------------------------------------------------------
    // Service methods
    // ----------------------------------------------------------------

    @Override
    public List<VideoResponseDTO> findAll() {
        return videoRepository.findAll().stream()
                .map(videoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public VideoResponseDTO findById(Integer id) {
        Video entity = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        return videoMapper.toDto(entity);
    }

    @Override
    public Optional<VideoResponseDTO> findByLessonId(Integer lessonId, Integer instructorId) {
        // Validate lesson exists and ownership (if instructorId provided)
        resolveLesson(lessonId, instructorId);
        return videoRepository.findByLessonId(lessonId)
                .map(videoMapper::toDto);
    }

    @Override
    public VideoResponseDTO create(VideoRequestDTO requestDTO, Integer instructorId) {
        // Validate video URL format
        VideoUtils.validateVideoUrl(requestDTO.getVideoUrl());

        // 1. Validate lesson exists and instructor owns it
        Lesson lesson = resolveLesson(requestDTO.getLessonId(), instructorId);

        // 2. Enforce OneToOne: prevent duplicate video on same lesson
        if (videoRepository.findByLessonId(lesson.getId()).isPresent()) {
            throw new RuntimeException(
                "This lesson already has a video. Please edit the existing video instead.");
        }

        // 3. Build and save — do NOT use mapper.toEntity because it only sets lesson.id (shallow),
        //    we want the full Lesson reference for the cascade/FK to work correctly.
        Video video = new Video();
        video.setVideoUrl(requestDTO.getVideoUrl());
        video.setDurationSeconds(requestDTO.getDurationSeconds());
        video.setLesson(lesson);

        return videoMapper.toDto(videoRepository.save(video));
    }

    @Override
    public VideoResponseDTO update(Integer id, VideoRequestDTO requestDTO, Integer instructorId) {
        // Validate video URL format
        VideoUtils.validateVideoUrl(requestDTO.getVideoUrl());

        // 1. Resolve existing video with ownership check
        Video existing = resolveVideo(id, instructorId);

        // 2. Only update mutable fields — never touch id or lesson
        existing.setVideoUrl(requestDTO.getVideoUrl());
        existing.setDurationSeconds(requestDTO.getDurationSeconds());

        return videoMapper.toDto(videoRepository.save(existing));
    }

    @Override
    public void delete(Integer id, Integer instructorId) {
        // 1. Resolve with ownership check
        Video existing = resolveVideo(id, instructorId);
        videoRepository.delete(existing);
    }
}
