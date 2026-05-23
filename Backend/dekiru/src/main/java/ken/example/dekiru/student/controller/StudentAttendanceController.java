package ken.example.dekiru.student.controller;

import ken.example.dekiru.academic.dto.SemesterResponse;
import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.academic.mapper.SemesterMapper;
import ken.example.dekiru.academic.service.SemesterService;
import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.student.dto.AttendanceScreenResponse;
import ken.example.dekiru.student.dto.AttendanceSubjectResponse;
import ken.example.dekiru.student.service.StudentAttendanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import ken.example.dekiru.attendance.service.ClassSessionService;
import ken.example.dekiru.attendance.dto.StudentAttendRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me/attendances")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentAttendanceController {

      StudentAttendanceService studentAttendanceService;
      SemesterMapper semesterMapper;
      SemesterService semesterService;
      ClassSessionService classSessionService;

    @GetMapping("/semesters")
    public ApiResponse<List<SemesterResponse>> getStudentSemesters() {
        return ApiResponse.success(
                studentAttendanceService.getSemestersForFilter(),
                "Lấy danh sách bộ lọc học kỳ thành công"
        );
    }
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
    @GetMapping("/overview")
    public ApiResponse<AttendanceScreenResponse> getAttendanceScreen(
            @RequestParam(required = false) Long semesterId) {
        return ApiResponse.success(
                studentAttendanceService.getAttendanceScreen(semesterId)
        );
    }

    /**
     * GET /student/attendance/danger
     *
     * Lấy riêng danh sách môn đang vượt ngưỡng cấm thi.
     * Dùng cho push notification hoặc widget cảnh báo nhanh ở màn hình chính.
     */
    @GetMapping("/danger")
    public ApiResponse<List<AttendanceSubjectResponse>> getDangerSubjects() {
        return ApiResponse.success(
                studentAttendanceService.getDangerSubjects()
        );
    }

    @PostMapping("/submit-qr")
    public ApiResponse<Void> submitQr(@RequestBody StudentAttendRequest request) {
        classSessionService.studentAttend(request);
        return ApiResponse.success(null, "Điểm danh thành công");
    }
}