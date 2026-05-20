package ken.example.dekiru.academic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "administrative_class")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdministrativeClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /**
     * ON DELETE SET NULL — GV bị xóa thì lớp vẫn tồn tại, chỉ mất chủ nhiệm.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeroom_teacher_id")
    private Lecturer homeroomTeacher;

    @Column(length = 30, nullable = false, unique = true)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(name = "cohort_year", nullable = false)
    private String cohortYear;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}