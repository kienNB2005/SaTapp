package ken.example.dekiru.academic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "semester")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String name;

    /**
     * Ngày đầu tuần 1 — Stored Procedure dùng làm mốc tính ngày buổi học.
     * Phải là Thứ Hai — validate ở Service trước khi lưu.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_week", nullable = false)
    @Builder.Default
    private Byte startWeek = 1;

    /**
     * Chỉ 1 học kỳ được active tại 1 thời điểm.
     * Không có DB constraint — enforce ở Service khi kích hoạt.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}