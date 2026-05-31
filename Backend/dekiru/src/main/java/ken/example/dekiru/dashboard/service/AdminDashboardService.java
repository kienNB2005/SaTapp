package ken.example.dekiru.dashboard.service;

import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.academic.repository.AdministrativeClassRepository;
import ken.example.dekiru.academic.repository.LecturerRepository;
import ken.example.dekiru.academic.repository.SemesterRepository;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.attendance.entity.ClassSessionRequest;
import ken.example.dekiru.attendance.entity.RequestStatus;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import ken.example.dekiru.attendance.repository.ClassSessionRequestRepository;
import ken.example.dekiru.dashboard.dto.AdminDashboardResponse;
import ken.example.dekiru.report.dto.AdminSchoolReportStatsProjection;
import ken.example.dekiru.report.repository.AdminReportRepository;
import ken.example.dekiru.student.repository.StudentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardService {

    SemesterRepository semesterRepository;
    StudentRepository studentRepository;
    LecturerRepository lecturerRepository;
    AdministrativeClassRepository administrativeClassRepository;
    ClassSessionRepository classSessionRepository;
    ClassSessionRequestRepository classSessionRequestRepository;
    AdminReportRepository adminReportRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        // 1. Lấy học kỳ active hiện tại
        Optional<Semester> activeSemesterOpt = semesterRepository.findByIsActiveTrue();
        String activeSemesterName = activeSemesterOpt.map(Semester::getName).orElse("Không có học kỳ hoạt động");
        Long semesterId = activeSemesterOpt.map(Semester::getId).orElse(null);

        // 2. Thống kê tổng số lượng danh mục
        long totalStudents = studentRepository.count();
        long totalLecturers = lecturerRepository.count();
        long totalClasses = administrativeClassRepository.count();

        // 3. Tạo Stats tổng quan
        AdminDashboardResponse.Stats stats = new AdminDashboardResponse.Stats(
                activeSemesterName,
                totalStudents,
                totalLecturers,
                totalClasses
        );

        // 4. Lấy danh sách các ca học hôm nay và tổng hợp số lượng
        LocalDate today = LocalDate.now();
        List<ClassSession> todaySessions = classSessionRepository.findBySessionDate(today);

        long totalToday = todaySessions.stream()
                .filter(cs -> cs.getStatus() != ClassSession.Status.cancelled)
                .count();
        long activeToday = todaySessions.stream()
                .filter(cs -> cs.getStatus() == ClassSession.Status.open)
                .count();
        long completedToday = todaySessions.stream()
                .filter(cs -> cs.getStatus() == ClassSession.Status.closed)
                .count();

        AdminDashboardResponse.TodayOverview todayOverview = new AdminDashboardResponse.TodayOverview(
                totalToday,
                activeToday,
                completedToday
        );

        // 5. Lọc danh sách các ca học đang mở (active)
        List<AdminDashboardResponse.ActiveSessionDto> activeSessionDtos = todaySessions.stream()
                .filter(cs -> cs.getStatus() == ClassSession.Status.open)
                .map(cs -> {
                    String lecturerName = cs.getActualLecturer().getUser().getFullName();
                    String initial = getInitials(lecturerName);
                    String periodText = "Tiết " + cs.getActualPeriodStart() + " – " + cs.getActualPeriodEnd();
                    String openedAtStr = cs.getOpenedAt() != null ? cs.getOpenedAt().format(TIME_FORMATTER) : "--:--";

                    return new AdminDashboardResponse.ActiveSessionDto(
                            cs.getId(),
                            lecturerName,
                            initial,
                            cs.getSchedule().getSubject().getName(),
                            cs.getSchedule().getAdminClass().getCode(),
                            cs.getActualRoom().getCode(),
                            periodText,
                            openedAtStr
                    );
                })
                .collect(Collectors.toList());

        // 6. Lấy danh sách yêu cầu xin nghỉ/bù chờ duyệt
        List<ClassSessionRequest> pendingCancels = classSessionRequestRepository.findByCancelStatus(RequestStatus.pending);
        List<ClassSessionRequest> pendingMakeups = classSessionRequestRepository.findByMakeupStatus(RequestStatus.pending);

        List<AdminDashboardResponse.PendingRequestDto> pendingRequestDtos = new ArrayList<>();

        // Map yêu cầu xin nghỉ (CANCEL)
        for (ClassSessionRequest req : pendingCancels) {
            String lecturerName = req.getLecturer().getUser().getFullName();
            String details = "Xin nghỉ ngày " + req.getClassSession().getSessionDate() + 
                    " (Lý do: " + (req.getCancelReason() != null ? req.getCancelReason() : "Không có lý do") + ")";
            String timeStr = req.getCreatedAt() != null ? req.getCreatedAt().format(DATE_TIME_FORMATTER) : "";

            pendingRequestDtos.add(new AdminDashboardResponse.PendingRequestDto(
                    req.getId(),
                    lecturerName,
                    "CANCEL",
                    "Yêu cầu nghỉ dạy",
                    req.getClassSession().getSchedule().getSubject().getName(),
                    req.getClassSession().getSchedule().getAdminClass().getCode(),
                    details,
                    timeStr,
                    "pending"
            ));
        }

        // Map yêu cầu dạy bù (MAKEUP)
        for (ClassSessionRequest req : pendingMakeups) {
            String lecturerName = req.getLecturer().getUser().getFullName();
            String details = "Dạy bù ngày " + req.getMakeupDate() + 
                    " · Tiết " + req.getMakeupPeriodStart() + "–" + req.getMakeupPeriodEnd() + 
                    " · Phòng " + (req.getMakeupRoom() != null ? req.getMakeupRoom().getCode() : "Chưa chọn");
            String timeStr = req.getCreatedAt() != null ? req.getCreatedAt().format(DATE_TIME_FORMATTER) : "";

            pendingRequestDtos.add(new AdminDashboardResponse.PendingRequestDto(
                    req.getId(),
                    lecturerName,
                    "MAKEUP",
                    "Đề xuất dạy bù",
                    req.getClassSession().getSchedule().getSubject().getName(),
                    req.getClassSession().getSchedule().getAdminClass().getCode(),
                    details,
                    timeStr,
                    "pending"
            ));
        }

        // Sắp xếp các yêu cầu chờ duyệt mới nhất lên trên đầu
        pendingRequestDtos.sort(Comparator.comparing(AdminDashboardResponse.PendingRequestDto::time).reversed());

        return new AdminDashboardResponse(stats, todayOverview, activeSessionDtos, pendingRequestDtos);
    }

    // Hàm lấy chữ cái viết tắt của giảng viên (VD: Nguyễn Văn Nam -> NVN hoặc NM)
    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "GV";
        }
        String[] words = fullName.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(words[0].length(), 2)).toUpperCase();
        }
        // Lấy chữ cái đầu của họ và tên chính
        String first = words[0].substring(0, 1);
        String last = words[words.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }
}
