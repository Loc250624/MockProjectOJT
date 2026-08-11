package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import com.ojtsu26.elearning.mapper.VideoMapper;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.model.enums.VideoSourceType;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.VideoRepository;
import com.ojtsu26.elearning.service.impl.VideoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceImplTest {

    @Mock private VideoRepository videoRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private VideoMapper videoMapper;
    @Mock private VideoStorageService videoStorageService;
    @Mock private MediaDurationService mediaDurationService;
    @Mock private RemoteVideoUrlValidator remoteVideoUrlValidator;
    @Mock private VideoMetadataDurationResolver durationResolver;

    private VideoServiceImpl service;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        service = new VideoServiceImpl(
                videoRepository,
                lessonRepository,
                videoMapper,
                videoStorageService,
                mediaDurationService,
                remoteVideoUrlValidator,
                durationResolver);
        lesson = Lesson.builder()
                .id(99)
                .course(Course.builder()
                        .id(10)
                        .instructor(User.builder().id(7).build())
                        .build())
                .build();
        when(videoMapper.toDto(any(Video.class))).thenAnswer(invocation -> {
            Video video = invocation.getArgument(0);
            VideoResponseDTO dto = new VideoResponseDTO();
            dto.setSourceType(video.getSourceType());
            dto.setVideoUrl(video.getVideoUrl());
            dto.setDurationSeconds(video.getDurationSeconds());
            return dto;
        });
    }

    @Test
    void createsYouTubeVideoWithCanonicalUrlAndResolvedDuration() {
        VideoRequestDTO request = new VideoRequestDTO();
        request.setLessonId(99);
        request.setSourceType(VideoSourceType.YOUTUBE);
        request.setVideoUrl("https://youtu.be/qz0aGYrrIhU?t=10");
        request.setDurationSeconds(123);

        when(lessonRepository.findById(99)).thenReturn(Optional.of(lesson));
        when(videoRepository.findByLessonId(99)).thenReturn(Optional.empty());
        when(durationResolver.resolveDurationSeconds("https://www.youtube.com/watch?v=qz0aGYrrIhU"))
                .thenReturn(OptionalInt.of(615));
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoResponseDTO response = service.create(request, 7);

        assertEquals(VideoSourceType.YOUTUBE, response.getSourceType());
        assertEquals("https://www.youtube.com/watch?v=qz0aGYrrIhU", response.getVideoUrl());
        assertEquals(615, response.getDurationSeconds());
    }

    @Test
    void createsUploadedVideoWithGeneratedStorageUrlAndServerDuration() {
        VideoRequestDTO request = new VideoRequestDTO();
        request.setLessonId(99);
        request.setSourceType(VideoSourceType.UPLOAD);
        request.setDurationSeconds(10);

        VideoStorageService.StoredVideo stored = new VideoStorageService.StoredVideo(
                "/uploads/videos/generated.mp4",
                "generated.mp4",
                "lesson.mp4",
                "video/mp4",
                42L,
                Path.of("uploads/videos/generated.mp4"));

        when(lessonRepository.findById(99)).thenReturn(Optional.of(lesson));
        when(videoRepository.findByLessonId(99)).thenReturn(Optional.empty());
        when(videoStorageService.store(null)).thenReturn(stored);
        when(mediaDurationService.resolveLocalDurationSeconds(stored.getStoredPath())).thenReturn(OptionalInt.of(321));
        when(mediaDurationService.clientFallback(10)).thenReturn(OptionalInt.of(10));
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request, 7);

        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).save(captor.capture());
        Video saved = captor.getValue();
        assertEquals(VideoSourceType.UPLOAD, saved.getSourceType());
        assertEquals("/uploads/videos/generated.mp4", saved.getVideoUrl());
        assertEquals("generated.mp4", saved.getStoredFilename());
        assertEquals("lesson.mp4", saved.getOriginalFilename());
        assertEquals(321, saved.getDurationSeconds());
        verify(videoStorageService, never()).deleteIfPresent("/uploads/videos/generated.mp4");
    }
}
