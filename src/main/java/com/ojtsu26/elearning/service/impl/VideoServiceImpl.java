package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.common.VideoUtils;
import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import com.ojtsu26.elearning.mapper.VideoMapper;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.model.enums.VideoSourceType;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.VideoRepository;
import com.ojtsu26.elearning.service.MediaDurationService;
import com.ojtsu26.elearning.service.RemoteVideoUrlValidator;
import com.ojtsu26.elearning.service.VideoMetadataDurationResolver;
import com.ojtsu26.elearning.service.VideoService;
import com.ojtsu26.elearning.service.VideoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final LessonRepository lessonRepository;
    private final VideoMapper videoMapper;
    private final VideoStorageService videoStorageService;
    private final MediaDurationService mediaDurationService;
    private final RemoteVideoUrlValidator remoteVideoUrlValidator;
    private final VideoMetadataDurationResolver durationResolver;

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
        resolveLesson(lessonId, instructorId);
        return videoRepository.findByLessonId(lessonId)
                .map(videoMapper::toDto);
    }

    @Override
    public VideoResponseDTO create(VideoRequestDTO requestDTO, Integer instructorId) {
        Lesson lesson = resolveLesson(requestDTO.getLessonId(), instructorId);
        if (videoRepository.findByLessonId(lesson.getId()).isPresent()) {
            throw new RuntimeException("This lesson already has a video. Please edit the existing video instead.");
        }

        ResolvedVideoSource source = resolveRequest(requestDTO, null);
        Video video = new Video();
        applyResolvedSource(video, source);
        video.setLesson(lesson);

        try {
            return videoMapper.toDto(videoRepository.save(video));
        } catch (RuntimeException ex) {
            cleanupNewUpload(source);
            throw ex;
        }
    }

    @Override
    public VideoResponseDTO update(Integer id, VideoRequestDTO requestDTO, Integer instructorId) {
        Video existing = resolveVideo(id, instructorId);
        ResolvedVideoSource source = resolveRequest(requestDTO, existing);
        String previousUploadUrl = existing.getVideoUrl();
        boolean shouldDeletePreviousUpload = isStoredUpload(existing)
                && (!VideoSourceType.UPLOAD.equals(source.sourceType())
                || !previousUploadUrl.equals(source.videoUrl()));

        applyResolvedSource(existing, source);

        try {
            VideoResponseDTO response = videoMapper.toDto(videoRepository.save(existing));
            if (shouldDeletePreviousUpload) {
                videoStorageService.deleteIfPresent(previousUploadUrl);
            }
            return response;
        } catch (RuntimeException ex) {
            cleanupNewUpload(source);
            throw ex;
        }
    }

    @Override
    public void delete(Integer id, Integer instructorId) {
        Video existing = resolveVideo(id, instructorId);
        videoRepository.delete(existing);
        if (isStoredUpload(existing)) {
            videoStorageService.deleteIfPresent(existing.getVideoUrl());
        }
    }

    private ResolvedVideoSource resolveRequest(VideoRequestDTO requestDTO, Video existing) {
        VideoSourceType requestedType = requestDTO.getSourceType();
        MultipartFile file = requestDTO.getVideoFile();
        if (requestedType == null) {
            requestedType = file != null && !file.isEmpty()
                    ? VideoSourceType.UPLOAD
                    : detectUrlSourceType(requestDTO.getVideoUrl());
        }
        return switch (requestedType) {
            case UPLOAD -> resolveUpload(requestDTO, existing);
            case YOUTUBE -> resolveYouTube(requestDTO);
            case DIRECT_URL -> resolveDirectUrl(requestDTO);
        };
    }

    private ResolvedVideoSource resolveUpload(VideoRequestDTO requestDTO, Video existing) {
        MultipartFile file = requestDTO.getVideoFile();
        if ((file == null || file.isEmpty()) && existing != null && isStoredUpload(existing)) {
            int seconds = mediaDurationService.clientFallback(requestDTO.getDurationSeconds())
                    .orElse(existing.getDurationSeconds() == null ? 0 : existing.getDurationSeconds());
            if (seconds <= 0) {
                throw new RuntimeException("We could not read the video duration. Choose another video.");
            }
            return new ResolvedVideoSource(
                    VideoSourceType.UPLOAD,
                    existing.getVideoUrl(),
                    seconds,
                    existing.getOriginalFilename(),
                    existing.getStoredFilename(),
                    existing.getContentType(),
                    existing.getFileSizeBytes(),
                    false);
        }

        VideoStorageService.StoredVideo storedVideo = videoStorageService.store(file);
        OptionalInt serverDuration = mediaDurationService.resolveLocalDurationSeconds(storedVideo.getStoredPath());
        OptionalInt fallbackDuration = mediaDurationService.clientFallback(requestDTO.getDurationSeconds());
        int durationSeconds = serverDuration.orElseGet(() -> fallbackDuration.orElse(0));
        if (durationSeconds <= 0) {
            videoStorageService.deleteIfPresent(storedVideo.getPublicUrl());
            throw new RuntimeException("We could not read the video duration. Choose another video.");
        }
        return new ResolvedVideoSource(
                VideoSourceType.UPLOAD,
                storedVideo.getPublicUrl(),
                durationSeconds,
                storedVideo.getOriginalFilename(),
                storedVideo.getStoredFilename(),
                storedVideo.getContentType(),
                storedVideo.getFileSizeBytes(),
                true);
    }

    private ResolvedVideoSource resolveYouTube(VideoRequestDTO requestDTO) {
        String videoId = VideoUtils.extractYouTubeVideoId(requestDTO.getVideoUrl());
        if (videoId == null) {
            throw new RuntimeException("This YouTube URL is not valid.");
        }
        String canonicalUrl = "https://www.youtube.com/watch?v=" + videoId;
        OptionalInt serverDuration = durationResolver.resolveDurationSeconds(canonicalUrl);
        OptionalInt fallbackDuration = mediaDurationService.clientFallback(requestDTO.getDurationSeconds());
        int durationSeconds = serverDuration.orElseGet(() -> fallbackDuration.orElse(0));
        if (durationSeconds <= 0) {
            throw new RuntimeException("We could not read the video duration. Choose another video or URL.");
        }
        return new ResolvedVideoSource(
                VideoSourceType.YOUTUBE,
                canonicalUrl,
                durationSeconds,
                null,
                null,
                null,
                null,
                false);
    }

    private ResolvedVideoSource resolveDirectUrl(VideoRequestDTO requestDTO) {
        VideoUtils.VideoSourceInfo sourceInfo = VideoUtils.classifyVideoSource(requestDTO.getVideoUrl());
        if (sourceInfo.type() != VideoUtils.VideoSourceType.DIRECT_MEDIA) {
            throw new RuntimeException(sourceInfo.message());
        }
        String canonicalUrl = remoteVideoUrlValidator.validateDirectVideoUrl(requestDTO.getVideoUrl());
        int durationSeconds = mediaDurationService.clientFallback(requestDTO.getDurationSeconds()).orElse(0);
        if (durationSeconds <= 0) {
            throw new RuntimeException("We could not read the video duration. Choose another video or URL.");
        }
        return new ResolvedVideoSource(
                VideoSourceType.DIRECT_URL,
                canonicalUrl,
                durationSeconds,
                null,
                null,
                null,
                null,
                false);
    }

    private VideoSourceType detectUrlSourceType(String videoUrl) {
        VideoUtils.VideoSourceInfo sourceInfo = VideoUtils.classifyVideoSource(videoUrl);
        if (sourceInfo.type() == VideoUtils.VideoSourceType.YOUTUBE) {
            return VideoSourceType.YOUTUBE;
        }
        if (sourceInfo.type() == VideoUtils.VideoSourceType.DIRECT_MEDIA) {
            return VideoSourceType.DIRECT_URL;
        }
        throw new RuntimeException(sourceInfo.message());
    }

    private void applyResolvedSource(Video video, ResolvedVideoSource source) {
        video.setSourceType(source.sourceType());
        video.setVideoUrl(source.videoUrl());
        video.setDurationSeconds(source.durationSeconds());
        video.setOriginalFilename(source.originalFilename());
        video.setStoredFilename(source.storedFilename());
        video.setContentType(source.contentType());
        video.setFileSizeBytes(source.fileSizeBytes());
    }

    private void cleanupNewUpload(ResolvedVideoSource source) {
        if (source != null && source.newUpload()) {
            videoStorageService.deleteIfPresent(source.videoUrl());
        }
    }

    private boolean isStoredUpload(Video video) {
        return video != null
                && video.getVideoUrl() != null
                && video.getVideoUrl().startsWith("/uploads/videos/");
    }

    private record ResolvedVideoSource(
            VideoSourceType sourceType,
            String videoUrl,
            Integer durationSeconds,
            String originalFilename,
            String storedFilename,
            String contentType,
            Long fileSizeBytes,
            boolean newUpload) {
    }
}
