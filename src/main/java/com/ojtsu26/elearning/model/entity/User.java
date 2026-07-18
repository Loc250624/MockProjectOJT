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
@Table(
        name = "Users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_provider_identity",
                        columnNames = {"auth_provider", "provider_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String passwordHash;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    /**
     * The opaque ID issued by the OAuth provider (e.g. Google "sub", GitHub numeric id).
     * Used as a secondary key to locate the account when the user changes their email
     * on the provider side, preventing duplicate row creation.
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    /**
     * Timestamp of the most recent successful login, updated by OAuth2LoginSuccessHandler
     * and AuthServiceImpl on every login.
     */
    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    @Convert(converter = RoleConverter.class)
    private Role role;

    @Convert(converter = AuthProviderConverter.class)
    @Column(name = "auth_provider")
    private AuthProvider authProvider;

    @Convert(converter = UserStatusConverter.class)
    private UserStatus status;

    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("roadmap-instructor")
    private List<Roadmap> roadmaps;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("course-instructor")
    private List<Course> courses;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("courseenrollment-student")
    private List<CourseEnrollment> courseenrollments;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("submission-student")
    private List<Submission> submissions;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("certificate-student")
    private List<Certificate> certificates;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("transaction-student")
    private List<Transaction> transactions;

}
