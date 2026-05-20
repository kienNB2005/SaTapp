package ken.example.dekiru.student.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response tổng hợp cho toàn bộ màn hình chuyên cần.
 * Backend trả 1 lần duy nhất, frontend không cần gọi 2 API.
 *
 * JSON:
 * {
 *   "code": 200,
 *   "result": {
 *     "overview": { ... },
 *     "subjects": [ ... ]
 *   }
 * }
 */
@Getter
@Builder
public class AttendanceScreenResponse {
    private AttendanceOverviewResponse overview;
    private List<AttendanceSubjectResponse> subjects;
}