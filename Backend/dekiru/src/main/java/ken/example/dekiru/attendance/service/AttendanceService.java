package ken.example.dekiru.attendance.service;

import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import ken.example.dekiru.attendance.dto.AttendanceListDto;
import ken.example.dekiru.attendance.mapper.AttendanceMapper;
import ken.example.dekiru.attendance.repository.AttendanceRepository;
import ken.example.dekiru.attendance.repository.AttendanceSummaryProjection;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ken.example.dekiru.attendance.dto.AttendanceBatchUpdateRequest;
import ken.example.dekiru.attendance.entity.Attendance;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.security.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttendanceService {

    AttendanceRepository attendanceRepository;
    AttendanceMapper attendanceMapper;
    SecurityUtils securityUtils;
    ClassSessionRepository classSessionRepository;
    UserRepository userRepository;

    public Page<AttendanceListDto> getAttendanceListForEdit(Long sessionId, String search, String uiStatus, Pageable pageable) {
        Long currentLecturerId = securityUtils.getCurrentLecturerId();

        if (!classSessionRepository.existsByIdAndSchedule_Lecturer_Id(sessionId, currentLecturerId)) {
            throw new AppException(ErrorCode.NO_PERMISSION_ON_SESSION);
        }

        String dbStatus = null;
        Boolean isLate = null;

        // 1. Dịch uiStatus từ Frontend thành logic của Database
        if (uiStatus != null && !uiStatus.trim().isEmpty()) {
            switch (uiStatus.toUpperCase()) {
                case "PRESENT":
                    dbStatus = "present"; // Hoặc "present" tùy vào cách lưu trong DB của bác
                    isLate = false;
                    break;
                case "LATE":
                    dbStatus = "present";
                    isLate = true;
                    break;
                case "ABSENT":
                    dbStatus = "absent";
                    break;
                case "EXCUSED":
                    dbStatus = "excused";
                    break;
            }
        }

        // 2. Chuẩn hóa chuỗi search để tránh lỗi truyền rỗng
        String keyword = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // 3. Gọi hàm vào Database
        Page<AttendanceSummaryProjection> pageResult = attendanceRepository
                .findManualAttendancePaginated(sessionId, keyword, dbStatus, isLate, pageable);

        // 4. Map kết quả sang DTO và trả về
        return pageResult.map(attendanceMapper::toDto);
    }

    @Transactional
    public void updateAttendanceBatch(Long sessionId, AttendanceBatchUpdateRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }

        Long currentLecturerId = securityUtils.getCurrentLecturerId();
        if (!classSessionRepository.existsByIdAndSchedule_Lecturer_Id(sessionId, currentLecturerId)) {
            throw new AppException(ErrorCode.NO_PERMISSION_ON_SESSION);
        }

        Long currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<Long> attendanceIds = request.getItems().stream()
                .map(AttendanceBatchUpdateRequest.UpdateItem::getAttendanceId)
                .collect(Collectors.toList());

        List<Attendance> attendances = attendanceRepository.findAllById(attendanceIds);
        Map<Long, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getId, a -> a));

        List<Attendance> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AttendanceBatchUpdateRequest.UpdateItem item : request.getItems()) {
            Attendance attendance = attendanceMap.get(item.getAttendanceId());
            if (attendance == null || !attendance.getClassSession().getId().equals(sessionId)) {
                continue; // Bỏ qua nếu không tìm thấy hoặc sai buổi học
            }

            boolean isChanged = false;

            if (item.getUiStatus() != null) {
                switch (item.getUiStatus().toUpperCase()) {
                    case "PRESENT":
                        attendance.setStatus(Attendance.Status.present);
                        attendance.setIsLate(false);
                        break;
                    case "LATE":
                        attendance.setStatus(Attendance.Status.present);
                        attendance.setIsLate(true);
                        break;
                    case "ABSENT":
                        attendance.setStatus(Attendance.Status.absent);
                        attendance.setIsLate(false);
                        break;
                    case "EXCUSED":
                        attendance.setStatus(Attendance.Status.excused);
                        attendance.setIsLate(false);
                        break;
                }
                isChanged = true;
            }

            if (item.getLateMinutes() != null) {
                attendance.setLateMinutes(item.getLateMinutes());
                isChanged = true;
            } else if (item.getUiStatus() != null && !item.getUiStatus().equalsIgnoreCase("LATE")) {
                attendance.setLateMinutes(null);
                isChanged = true;
            }

            if (item.getLeftEarly() != null) {
                attendance.setLeftEarly(item.getLeftEarly());
                isChanged = true;
            }

            if (item.getNote() != null) {
                attendance.setNote(item.getNote());
                isChanged = true;
            }

            if (isChanged) {
                attendance.setEditedBy(currentUser);
                attendance.setEditedAt(now);
                toSave.add(attendance);
            }
        }

        if (!toSave.isEmpty()) {
            attendanceRepository.saveAll(toSave);
        }
    }
}