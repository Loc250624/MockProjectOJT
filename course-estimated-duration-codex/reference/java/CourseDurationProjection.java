package reference;

/**
 * Mẫu Spring Data projection cho batch aggregation.
 * Đổi tên getter theo alias query thực tế.
 */
public interface CourseDurationProjection {
    Long getCourseId();
    Long getEstimatedDurationSeconds();
}
