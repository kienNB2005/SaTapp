package ken.example.dekiru.student.controller;

import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.student.dto.AttendanceScreenResponse;
import ken.example.dekiru.student.dto.AttendanceSubjectResponse;
import ken.example.dekiru.student.service.StudentAttendanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentAttendanceController {

      StudentAttendanceService studentAttendanceService;

    /**
     * GET /student/attendance
     *
     * Trả toàn bộ dữ liệu màn hình chuyên cần trong 1 request:
     * - overview : tổng quan (4 badge + tỉ lệ %)
     * - subjects : danh sách môn kèm progress bar, đã sort danger lên đầu
     *
     * Response mẫu:
     * {
     *   "code": 200,
     *   "message": "success",
     *   "result": {
     *     "overview": {
     *       "studentCode": "22IT001",
     *       "fullName": "Nguyễn Văn An",
     *       "adminClassCode": "CNTT-K22A",
     *       "semesterName": "HK1-2024-2025",
     *       "totalPassed": 22,
     *       "totalPresent": 16,
     *       "totalAbsent": 5,
     *       "totalExcused": 1,
     *       "totalLate": 1,
     *       "totalLeaveEarly": 0,
     *       "attendanceRatePct": 77.3
     *     },
     *     "subjects": [
     *       {
     *         "subjectId": 3,
     *         "subjectCode": "MTH201",
     *         "subjectName": "Toán rời rạc",
     *         "credits": 3,
     *         "lecturerName": "TS. Trần Minh Đức",
     *         "totalSessions": 15,
     *         "passedSessions": 9,
     *         "remainingSessions": 6,
     *         "presentCount": 5,
     *         "absentCount": 4,
     *         "excusedCount": 0,
     *         "lateCount": 0,
     *         "leaveEarlyCount": 0,
     *         "attendanceRatePct": 55.6,
     *         "maxAbsentAllowed": 3,
     *         "isDanger": true,
     *         "attendanceStatus": "danger"
     *       }
     *     ]
     *   }
     * }
     */
    @GetMapping("/attendance")
    public ApiResponse<AttendanceScreenResponse> getAttendanceScreen() {
        return ApiResponse.success(
                studentAttendanceService.getAttendanceScreen()
        );
    }

    /**
     * GET /student/attendance/danger
     *
     * Lấy riêng danh sách môn đang vượt ngưỡng cấm thi.
     * Dùng cho push notification hoặc widget cảnh báo nhanh ở màn hình chính.
     */
    @GetMapping("/attendance/danger")
    public ApiResponse<List<AttendanceSubjectResponse>> getDangerSubjects() {
        return ApiResponse.success(
                studentAttendanceService.getDangerSubjects()
        );
    }
}