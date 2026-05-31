package ken.example.dekiru.attendance.service;

import ken.example.dekiru.academic.entity.Room;
import ken.example.dekiru.academic.repository.RoomRepository;
import ken.example.dekiru.attendance.dto.ApproveSessionRequest;
import ken.example.dekiru.attendance.dto.CancelSessionRequest;
import ken.example.dekiru.attendance.dto.MakeupSessionRequest;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.attendance.entity.ClassSessionRequest;
import ken.example.dekiru.attendance.entity.RequestStatus;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import ken.example.dekiru.attendance.repository.ClassSessionRequestRepository;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.schedule.entity.PeriodTime;
import ken.example.dekiru.schedule.repository.PeriodTimeRepository;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.security.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassSessionRequestService {

    ClassSessionRequestRepository requestRepository;
    ClassSessionRepository classSessionRepository;
    PeriodTimeRepository periodTimeRepository;
    RoomRepository roomRepository;
    UserRepository userRepository;
    SecurityUtils securityUtils;
    ClassSessionService classSessionService;

    // Giảng viên gọi: Gửi yêu cầu hủy buổi
    @Transactional
    public ClassSessionRequest createCancelRequest(Long classSessionId, CancelSessionRequest request) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        
        ClassSession session = classSessionRepository.findById(classSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        if (!session.getActualLecturer().getId().equals(lecturerId)) {
            throw new AppException(ErrorCode.NO_PERMISSION_ON_SESSION);
        }

        if (session.getStatus() != ClassSession.Status.scheduled) {
            throw new AppException(ErrorCode.INVALID_SESSION_STATUS);
        }

        // Kiểm tra thời gian: phải gửi trước 15 phút
        PeriodTime periodTime = periodTimeRepository.findById(session.getActualPeriodStart())
                .orElseThrow(() -> new AppException(ErrorCode.PERIOD_TIME_NOT_FOUND));
        LocalDateTime sessionStartTime = LocalDateTime.of(session.getSessionDate(), periodTime.getStartTime());
        
        if (LocalDateTime.now().isAfter(sessionStartTime.minusMinutes(15))) {
            throw new AppException(ErrorCode.CANCEL_TOO_LATE);
        }

        ClassSessionRequest newRequest = ClassSessionRequest.builder()
                .lecturer(session.getActualLecturer())
                .classSession(session)
                .cancelReason(request.getCancelReason())
                .cancelStatus(RequestStatus.pending)
                .build();

        return requestRepository.save(newRequest);
    }

    // Giảng viên gọi: Gửi yêu cầu dạy bù
    @Transactional
    public ClassSessionRequest createMakeupRequest(Long originalSessionId, MakeupSessionRequest request) {
        Long lecturerId = securityUtils.getCurrentLecturerId();

        ClassSession originalSession = classSessionRepository.findById(originalSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        if (!originalSession.getActualLecturer().getId().equals(lecturerId)) {
            throw new AppException(ErrorCode.NO_PERMISSION_ON_SESSION);
        }

        if (originalSession.getStatus() != ClassSession.Status.cancelled) {
            throw new AppException(ErrorCode.SESSION_NOT_CANCELLED);
        }

        // Kiểm tra logic xung đột giống hệt ClassSessionService
        java.time.LocalDate makeupDate = request.getSessionDate();
        java.time.LocalDate originalDate = originalSession.getSessionDate();
        java.time.LocalDate semesterEndDate = originalSession.getSchedule().getSemester().getEndDate();

        if (makeupDate.isBefore(originalDate)) {
            throw new AppException(ErrorCode.MAKEUP_DATE_BEFORE_ORIGINAL);
        }
        if (!makeupDate.isAfter(java.time.LocalDate.now())) {
            throw new AppException(ErrorCode.MAKEUP_DATE_MUST_BE_FUTURE);
        }
        if (!makeupDate.isBefore(semesterEndDate)) {
            throw new AppException(ErrorCode.MAKEUP_DATE_AFTER_SEMESTER);
        }
        if (request.getPeriodStart() > request.getPeriodEnd() || request.getPeriodStart() < 1 || request.getPeriodEnd() > 15) {
            throw new AppException(ErrorCode.INVALID_PERIOD);
        }
        if (classSessionRepository.existsByScheduleIdAndSessionDateAndActualPeriodStart(originalSession.getSchedule().getId(), request.getSessionDate(), request.getPeriodStart())) {
            throw new AppException(ErrorCode.DUPLICATE_SESSION_DATE);
        }
        if (classSessionRepository.countConflictForClass(request.getSessionDate(), request.getPeriodStart(), request.getPeriodEnd(), originalSession.getSchedule().getAdminClass().getId()) > 0) {
            throw new AppException(ErrorCode.CLASS_CONFLICT);
        }
        if (classSessionRepository.countConflictForRoom(request.getSessionDate(), request.getPeriodStart(), request.getPeriodEnd(), request.getRoomId()) > 0) {
            throw new AppException(ErrorCode.ROOM_CONFLICT);
        }
        if (classSessionRepository.countConflictForLecturer(request.getSessionDate(), request.getPeriodStart(), request.getPeriodEnd(), originalSession.getActualLecturer().getId()) > 0) {
            throw new AppException(ErrorCode.LECTURER_CONFLICT);
        }

        Room makeupRoom = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        // Tìm bản ghi request trước đó đã duyệt hủy
        ClassSessionRequest sessionRequest = requestRepository.findByClassSession_Id(originalSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_REQUEST_NOT_FOUND));

        // Update thông tin bù
        sessionRequest.setMakeupStatus(RequestStatus.pending);
        sessionRequest.setMakeupDate(request.getSessionDate());
        sessionRequest.setMakeupPeriodStart(request.getPeriodStart().intValue());
        sessionRequest.setMakeupPeriodEnd(request.getPeriodEnd().intValue());
        sessionRequest.setMakeupRoom(makeupRoom);

        return requestRepository.save(sessionRequest);
    }

    // Admin lấy danh sách nghỉ chờ duyệt
    public List<ClassSessionRequest> getPendingCancelRequests() {
        return requestRepository.findByCancelStatus(RequestStatus.pending);
    }

    // Admin lấy danh sách bù chờ duyệt
    public List<ClassSessionRequest> getPendingMakeupRequests() {
        return requestRepository.findByMakeupStatus(RequestStatus.pending);
    }

    // Giảng viên lấy lịch sử yêu cầu của mình
    public List<ClassSessionRequest> getMyRequests() {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        return requestRepository.findByLecturer_Id(lecturerId);
    }

    // Admin duyệt yêu cầu
    @Transactional
    public ClassSessionRequest approveRequest(Long requestId, String type) {
        Long adminId = securityUtils.getCurrentUserId();
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ClassSessionRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_REQUEST_NOT_FOUND));

        if ("cancel".equalsIgnoreCase(type)) {
            if (request.getCancelStatus() != RequestStatus.pending) {
                throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
            }
            request.setCancelStatus(RequestStatus.approved);
            request.setApprovedBy(admin);
            request.setApprovedAt(LocalDateTime.now());
            // Hủy buổi gốc
            classSessionService.adminCancelClassSession(request.getClassSession().getId(), request.getCancelReason(), admin);

        } else if ("makeup".equalsIgnoreCase(type)) {
            if (request.getMakeupStatus() != RequestStatus.pending) {
                throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
            }
            request.setMakeupStatus(RequestStatus.approved);
            request.setApprovedBy(admin);
            request.setApprovedAt(LocalDateTime.now());
            
            MakeupSessionRequest makeupDTO = MakeupSessionRequest.builder()
                    .sessionDate(request.getMakeupDate())
                    .periodStart(request.getMakeupPeriodStart().byteValue())
                    .periodEnd(request.getMakeupPeriodEnd().byteValue())
                    .roomId(request.getMakeupRoom().getId())
                    .build();
            // Tạo buổi bù
            classSessionService.adminCreateMakeupSession(request.getClassSession().getId(), makeupDTO);
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
        }

        return requestRepository.save(request);
    }

    // Admin từ chối yêu cầu
    @Transactional
    public ClassSessionRequest rejectRequest(Long requestId, String type, ApproveSessionRequest dto) {
        Long adminId = securityUtils.getCurrentUserId();
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ClassSessionRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_REQUEST_NOT_FOUND));

        if ("cancel".equalsIgnoreCase(type)) {
            if (request.getCancelStatus() != RequestStatus.pending) {
                throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
            }
            request.setCancelStatus(RequestStatus.rejected);
            request.setRejectReason(dto.getRejectReason());
            request.setApprovedBy(admin);
            request.setApprovedAt(LocalDateTime.now());
        } else if ("makeup".equalsIgnoreCase(type)) {
            if (request.getMakeupStatus() != RequestStatus.pending) {
                throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
            }
            request.setMakeupStatus(RequestStatus.rejected);
            request.setRejectReason(dto.getRejectReason());
            request.setApprovedBy(admin);
            request.setApprovedAt(LocalDateTime.now());
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
        }

        return requestRepository.save(request);
    }
}
