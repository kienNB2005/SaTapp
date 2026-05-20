package ken.example.dekiru.dashboard.service;

import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.academic.repository.SemesterRepository;
import ken.example.dekiru.dashboard.dto.DashboardResponse;
import ken.example.dekiru.dashboard.dto.SemesterSummary;
import ken.example.dekiru.dashboard.entity.VLecturerSemesterSummary;
import ken.example.dekiru.dashboard.entity.VLecturerToday;
import ken.example.dekiru.dashboard.entity.VLecturerWeek;
import ken.example.dekiru.dashboard.entity.VScheduleProgress;
import ken.example.dekiru.dashboard.repository.LecturerSemesterSummaryRepository;
import ken.example.dekiru.dashboard.repository.LecturerTodayRepository;
import ken.example.dekiru.dashboard.repository.LecturerWeekRepository;
import ken.example.dekiru.dashboard.repository.ScheduleProgressRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DashboardService v3 — đã sửa các lỗi:
 *
 *  1. Dùng v_lecturer_semester_summary để lấy tất cả số liệu 4 thẻ
 *     trong 1 query thay vì nhiều query riêng lẻ sai logic.
 *
 *  2. Dùng v_lecturer_week cho panel "Lịch tuần này" và
 *     thẻ "Tuần này" (đếm buổi + môn đúng theo tuần, không phải HK).
 *
 *  3. DashboardResponse chuyển sang package dto (tránh Service → Controller).
 *
 *  4. semesterRepo chỉ gọi 1 lần, cache vào biến local.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class DashboardService {

      LecturerTodayRepository todayRepo;
      LecturerWeekRepository weekRepo;
      ScheduleProgressRepository progressRepo;
      LecturerSemesterSummaryRepository summaryRepo;
      SemesterRepository sesmesterRepo;


    // =========================================================
    //  PUBLIC API
    // =========================================================

    public DashboardResponse getDashboard(Long lecturerId, Long semesterId) {

        // 1. Xác định học kỳ — chỉ query 1 lần, cache vào biến
        Long sid = (semesterId != null)
                ? semesterId
                : resolveActiveSemesterId();

        // 2. Query song song (Spring lazy — thực thi khi dùng)
        List<VLecturerToday>    todaySessions = todayRepo.findByLecturerId(lecturerId);

        // Panel "Lịch tuần này" — view đã lọc T2..CN tuần hiện tại
        List<VLecturerWeek>     weekSessions  =
                weekRepo.findByLecturerIdOrderBySessionDateAscPeriodStartAsc(lecturerId);

        // Tiến độ từng môn
        List<VScheduleProgress> progress      =
                progressRepo.findByLecturerIdAndSemesterId(lecturerId, sid);

        // Tổng hợp 4 thẻ — 1 query duy nhất từ v_lecturer_semester_summary
        VLecturerSemesterSummary rawSummary =
                summaryRepo.findByLecturerIdAndSemesterId(lecturerId, sid)
                        .orElseThrow(() -> new IllegalStateException(
                                "Không tìm thấy dữ liệu tổng hợp cho lecturerId="
                                + lecturerId + ", semesterId=" + sid));

        // 3. Build DTO
        SemesterSummary summary = buildSemesterSummary(todaySessions, weekSessions, rawSummary);

        return new DashboardResponse(summary, todaySessions, weekSessions, progress);
    }

    // =========================================================
    //  PRIVATE
    // =========================================================

    private Long resolveActiveSemesterId() {
        return sesmesterRepo.findByIsActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa có học kỳ nào đang active. Admin cần set is_active = 1."))
                .getId();
    }

    /**
     * Tổng hợp 4 thẻ — toàn bộ tính toán trên memory, không query thêm DB.
     *
     *  Thẻ 1 "Buổi hôm nay"  → đếm từ todaySessions
     *  Thẻ 2 "Tuần này"      → đếm từ weekSessions  (✅ đã sửa — dùng v_lecturer_week)
     *  Thẻ 3 "Học kỳ này"    → lấy từ rawSummary    (✅ đã sửa — dùng v_lecturer_semester_summary)
     *  Thẻ 4 "Chuyên cần TB" → lấy từ rawSummary    (✅ đã sửa — cùng nguồn)
     */
    private SemesterSummary buildSemesterSummary(
            List<VLecturerToday>     todaySessions,
            List<VLecturerWeek>      weekSessions,
            VLecturerSemesterSummary raw) {

        SemesterSummary dto = new SemesterSummary();

        dto.setSemesterId(raw.getSemesterId());
        dto.setSemesterName(raw.getSemesterName());

        // ── Thẻ 1: Buổi hôm nay ──────────────────────────────
        dto.setSessionsToday(todaySessions.size());
        dto.setOpenCount((int) todaySessions.stream()
                .filter(s -> ClassSession.Status.open.equals(s.getStatus())).count());
        dto.setUpcomingCount((int) todaySessions.stream()
                .filter(s -> ClassSession.Status.scheduled.equals(s.getStatus())).count());

        // ── Thẻ 2: Tuần này ──────────────────────────────────
        // Đếm trực tiếp trên weekSessions (đã lọc T2..CN tuần này)
        dto.setSessionsThisWeek(weekSessions.size());
        long subjectsThisWeek = weekSessions.stream()
                .map(VLecturerWeek::getSubjectName)
                .distinct()
                .count();
        dto.setSubjectsThisWeek((int) subjectsThisWeek);

        // ── Thẻ 3: Học kỳ này ────────────────────────────────
        dto.setTotalSessions(nvl(raw.getTotalSessionsSemester()));
        dto.setClosedSessions(nvl(raw.getClosedSessions()));
        dto.setRemainingSessions(nvl(raw.getRemainingSessions()));

        // ── Thẻ 4: Chuyên cần TB ─────────────────────────────
        dto.setAvgAttendanceRate(raw.getAvgAttendanceRate());

        return dto;
    }

    private static int nvl(Integer v) { return v != null ? v : 0; }
}
