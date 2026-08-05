package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoDurationPrecomputeServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoMetadataDurationResolver durationResolver;

    private VideoDurationPrecomputeService service;

    @BeforeEach
    void setUp() {
        service = new VideoDurationPrecomputeService(videoRepository, durationResolver);
    }

    @Test
    void refreshesEveryVideoLessonWithoutEnrollmentOrUnlockChecks() {
        Course unpurchasedCourse = Course.builder().id(10).build();
        Course enrolledCourse = Course.builder().id(20).build();
        Video longVideo = video(101, unpurchasedCourse, "https://youtu.be/long", 3600);
        Video lockedVideo = video(202, enrolledCourse, "https://youtu.be/locked", 3600);

        when(videoRepository.findVideoLessonsByCourseIds(List.of(10, 20)))
                .thenReturn(List.of(longVideo, lockedVideo));
        when(durationResolver.resolveDurationSeconds("https://youtu.be/long"))
                .thenReturn(OptionalInt.of(8458));
        when(durationResolver.resolveDurationSeconds("https://youtu.be/locked"))
                .thenReturn(OptionalInt.of(5400));

        service.refreshCourseDurations(List.of(10, 20));

        assertEquals(8458, longVideo.getDurationSeconds());
        assertEquals(5400, lockedVideo.getDurationSeconds());
        verify(videoRepository).saveAll(List.of(longVideo, lockedVideo));
        verify(videoRepository).flush();
    }

    private Video video(Integer lessonId, Course course, String url, int durationSeconds) {
        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .course(course)
                .type(LessonType.VIDEO)
                .build();
        Video video = Video.builder()
                .lesson(lesson)
                .videoUrl(url)
                .durationSeconds(durationSeconds)
                .build();
        lesson.setVideo(video);
        return video;
    }
}
