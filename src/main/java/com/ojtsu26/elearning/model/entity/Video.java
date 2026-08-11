package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ojtsu26.elearning.model.enums.*;
import java.util.List;

@Entity
@Table(name = "Videos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private VideoSourceType sourceType;

    @Column(length = 2048)
    private String videoUrl;

    private Integer durationSeconds;

    @Column(length = 255)
    private String originalFilename;

    @Column(length = 255)
    private String storedFilename;

    @Column(length = 100)
    private String contentType;

    private Long fileSizeBytes;

    @OneToOne
    @JoinColumn(name = "lesson_id")
    @JsonBackReference("video-lesson")
    private Lesson lesson;
}
