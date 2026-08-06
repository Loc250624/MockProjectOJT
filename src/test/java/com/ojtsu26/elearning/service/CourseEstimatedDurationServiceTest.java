package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.mapper.CourseMapper;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.VideoRepository;
import com.ojtsu26.elearning.repository.projection.CourseDurationProjection;
import com.ojtsu26.elearning.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CourseEstimatedDurationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VideoDurationPrecomputeService videoDurationPrecomputeService;

    private CourseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseServiceImpl(
                courseRepository,
                videoRepository,
                videoDurationPrecomputeService,
                courseMapper,
                notificationService);
    }

    @Test
    void approvedCourseListUsesOneBatchAggregationAndFormatsMissingDurations() {
        Course first = Course.builder().id(10).title("First").build();
        Course second = Course.builder().id(20).title("Second").build();
        CourseResponseDTO firstDto = new CourseResponseDTO();
        CourseResponseDTO secondDto = new CourseResponseDTO();

        when(courseRepository.findApprovedCourses(isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(first, second));
        when(courseMapper.toDto(first)).thenReturn(firstDto);
        when(courseMapper.toDto(second)).thenReturn(secondDto);
        when(videoRepository.sumDurationSecondsByCourseIds(anyCollection()))
                .thenReturn(List.of(new DurationRow(10, 2700L)));

        List<CourseResponseDTO> result = service.findApprovedCourses(null, null, "newest");

        assertEquals(2700L, result.get(0).getEstimatedDurationSeconds());
        assertEquals("45 mins", result.get(0).getEstimatedDurationDisplay());
        assertEquals(0L, result.get(1).getEstimatedDurationSeconds());
        assertEquals("No video duration", result.get(1).getEstimatedDurationDisplay());
        var orderedCalls = inOrder(videoDurationPrecomputeService, videoRepository);
        orderedCalls.verify(videoDurationPrecomputeService).refreshCourseDurations(anyCollection());
        orderedCalls.verify(videoRepository).sumDurationSecondsByCourseIds(anyCollection());
    }

    private record DurationRow(Integer courseId, Long estimatedDurationSeconds)
            implements CourseDurationProjection {

        @Override
        public Integer getCourseId() {
            return courseId;
        }

        @Override
        public Long getEstimatedDurationSeconds() {
            return estimatedDurationSeconds;
        }
    }
}
