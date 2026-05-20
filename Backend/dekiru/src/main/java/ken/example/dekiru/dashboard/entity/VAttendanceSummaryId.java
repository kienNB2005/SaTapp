package ken.example.dekiru.dashboard.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite PK cho VAttendanceSummary.
 *
 * @EqualsAndHashCode — Lombok tự sinh equals() + hashCode()
 *                      bắt buộc phải có cho @IdClass.
 * @NoArgsConstructor — JPA yêu cầu constructor không tham số.
 * @AllArgsConstructor — tiện dùng khi tạo thủ công nếu cần.
 */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class VAttendanceSummaryId implements Serializable {

    private Long studentId;
    private Long semesterId;
    private Long subjectId;
}
