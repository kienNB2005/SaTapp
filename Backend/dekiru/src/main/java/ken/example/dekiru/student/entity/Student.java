package ken.example.dekiru.student.entity;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.academic.entity.AdministrativeClass;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    /**
     * ON DELETE CASCADE — xóa User thì Student profile xóa theo.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * ON DELETE RESTRICT — không xóa được lớp nếu còn SV.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_class_id", nullable = false)
    private AdministrativeClass adminClass;

    @Column(name = "student_code", length = 20, nullable = false, unique = true)
    private String studentCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}