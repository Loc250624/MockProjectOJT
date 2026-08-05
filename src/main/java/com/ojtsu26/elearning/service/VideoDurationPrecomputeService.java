package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoDurationPrecomputeService {

    private final VideoRepository videoRepository;
    private final VideoMetadataDurationResolver durationResolver;

    @Transactional
    public void refreshCourseDurations(Collection<Integer> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }
        List<Video> videos = videoRepository.findVideoLessonsByCourseIds(courseIds);
        Map<String, Integer> resolvedByUrl = videos.stream()
                .map(Video::getVideoUrl)
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .distinct()
                .parallel()
                .map(url -> Map.entry(url, durationResolver.resolveDurationSeconds(url)))
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getAsInt(),
                        (left, right) -> left));

        List<Video> changed = videos.stream()
                .filter(video -> resolvedByUrl.containsKey(video.getVideoUrl()))
                .filter(video -> !Objects.equals(
                        video.getDurationSeconds(), resolvedByUrl.get(video.getVideoUrl())))
                .peek(video -> video.setDurationSeconds(resolvedByUrl.get(video.getVideoUrl())))
                .toList();
        if (!changed.isEmpty()) {
            videoRepository.saveAll(changed);
            videoRepository.flush();
        }
    }
}
