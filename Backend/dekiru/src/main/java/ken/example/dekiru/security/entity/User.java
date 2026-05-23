package ken.example.dekiru.security.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import ken.example.dekiru.common.enums.Gender;

@Entity
@Table(name = "user")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    public enum Role {
        admin, lecturer, student
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "google_id", length = 255, unique = true)
    private String googleId;

    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('admin','lecturer','student')", nullable = false)
    private Role role;

    /**
     * 1 = tài khoản hoạt động, 0 = bị khóa.
     * Dữ liệu lịch sử điểm danh giữ nguyên khi khóa tài khoản.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", columnDefinition = "ENUM('male', 'female', 'other')")
    private Gender gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "birth_place", length = 150)
    private String birthPlace;

}