package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface VideoService {

    /** Admin: fetch all videos across all lessons. */
    List<VideoResponseDTO> findAll();

    /** Fetch a single video by its own ID. */
    VideoResponseDTO findById(Integer id);

    /**
     * Fetch the video attached to a specific lesson.
     * instructorId is used for ownership validation (pass null for admin access).
     */
    Optional<VideoResponseDTO> findByLessonId(Integer lessonId, Integer instructorId);

    /**
     * Create a new video for a lesson.
     * Throws if the lesson already has a video, or if the teacher does not own the course.
     */
    VideoResponseDTO create(VideoRequestDTO requestDTO, Integer instructorId);

    /**
     * Update mutable fields of an existing video.
     * Never overwrites id or lesson relationship.
     */
    VideoResponseDTO update(Integer id, VideoRequestDTO requestDTO, Integer instructorId);

    /**
     * Delete a video by its ID.
     * Ownership is validated against the lesson's course instructor.
     */
    void delete(Integer id, Integer instructorId);
}
