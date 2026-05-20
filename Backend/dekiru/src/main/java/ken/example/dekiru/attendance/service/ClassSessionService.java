package ken.example.dekiru.attendance.service;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.attendance.entity.CheckoutEvent;
import ken.example.dekiru.schedule.entity.Schedule;
import ken.example.dekiru.academic.entity.Room;
import ken.example.dekiru.schedule.entity.PeriodTime;
import ken.example.dekiru.student.entity.Student;
import ken.example.dekiru.attendance.entity.Attendance;


import ken.example.dekiru.academic.mapper.AdministrativeClassMapper;
import ken.example.dekiru.academic.mapper.SubjectMapper;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import ken.example.dekiru.schedule.repository.PeriodTimeRepository;
import ken.example.dekiru.student.repository.StudentRepository;
import ken.example.dekiru.schedule.repository.ScheduleRepository;
import ken.example.dekiru.security.repository.UserRepository;
import ken.example.dekiru.attendance.dto.*;
import ken.example.dekiru.attendance.repository.AttendanceRepository;
import ken.example.dekiru.attendance.repository.CheckoutEventRepository;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.security.service.JWTService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassSessionService {

    ClassSessionRepository classSessionRepository;
    AttendanceRepository attendanceRepository;
    StudentRepository studentRepository;
    PeriodTimeRepository periodTimeRepository;
    CheckoutEventRepository checkoutEventRepository;
    UserRepository userRepository;
    SecurityUtils securityUtils;
    JWTService jwtService;
    AttendanceSseService attendanceSseService;
    ScheduleRepository scheduleRepository;
    AdministrativeClassMapper administrativeClassMapper;
    SubjectMapper subjectMapper;
    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private ClassSession getAndValidateLecturerSession(Long sessionId, boolean useLock) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        ClassSession session = useLock ? 
                classSessionRepository.findByIdWithLock(sessionId).orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND)) : 
                classSessionRepository.findById(sessionId).orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        if (!session.getActualLecturer().getId().equals(lecturerId)) {
            throw new AppException(ErrorCode.NO_PERMISSION_ON_SESSION);
        }
        return session;
    }

    //KIỂM TRA BUỔI HỌC PHẢI Ở TRẠNG THÁI OPEN MỚI ĐƯỢC PHÉP CHECK OUT
    private void ensureSessionIsOpen(ClassSession session) {
        if (session.getStatus() != ClassSession.Status.open) {
            throw new AppException(ErrorCode.INVALID_SESSION_STATUS);
        }
    }

    private String determineQrType(Long sessionId) {
        boolean isCheckoutActive = checkoutEventRepository.existsByClassSession_IdAndClosedAtIsNull(sessionId);
        return isCheckoutActive ? "CHECK_OUT" : "CHECK_IN";
    }

    private QrTokenResponse generateQrToken(ClassSession session, String qrType) {
        String rawToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(60);
        
        session.setQrToken(rawToken);
        session.setQrExpiresAt(expiresAt);
        // Hibernate Dirty Checking will handle the DB update on commit
        
        String qrCodeData = jwtService.generateQrCodeData(session.getId(), rawToken, qrType);
        return new QrTokenResponse(qrCodeData, expiresAt, session.getId(), qrType);
    }

    private Double calculateGpsDistance(ClassSession session, double lat, double lng) {
        Room room = session.getActualRoom();
        if (room.getLatitude() == null || room.getLongitude() == null) return null;
        return calculateHaversineDistance(lat, lng, room.getLatitude().doubleValue(), room.getLongitude().doubleValue());
    }

    private boolean isWithinGpsRadius(ClassSession session, double distance) {
        return distance <= session.getActualRoom().getGpsRadiusM();
    }

    private void validateSessionOpenTime(ClassSession session) {
        // Lấy thời gian bắt đầu buổi học
        PeriodTime periodTime = periodTimeRepository.findById(session.getSchedule().getPeriodStart())
                .orElseThrow(() -> new AppException(ErrorCode.PERIOD_TIME_NOT_FOUND));
        
        LocalDateTime sessionStartTime = LocalDateTime.of(session.getSessionDate(), periodTime.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        
        // Thời gian sớm nhất để mở: 15 phút trước thời gian bắt đầu
        LocalDateTime earliestOpenTime = sessionStartTime.minusMinutes(15);
        
        // Kiểm tra: Nếu bây giờ sớm hơn 15 phút so với giờ bắt đầu
        if (now.isBefore(earliestOpenTime)) {
            throw new AppException(ErrorCode.SESSION_NOT_YET_STARTED);
        }
        
        // Kiểm tra: Nếu buổi học đã kết thúc (quá muộn)
        if (now.isAfter(sessionStartTime.plusHours(4))) { // Giả định 1 buổi học tối đa 4h
            throw new AppException(ErrorCode.SESSION_ALREADY_ENDED);
        }
    }

    @Transactional
    public QrTokenResponse openClassSession(Long sessionId) {
        ClassSession session = getAndValidateLecturerSession(sessionId, true);
        


        // SỬA LỖI TRƯỜNG HỢP DATABASE BỊ NULL QR TOKEN
        if (session.getStatus() == ClassSession.Status.open) {
            if (session.getQrToken() == null) {
                session.setQrToken(UUID.randomUUID().toString());
                session.setQrExpiresAt(LocalDateTime.now().plusSeconds(60));
            }
            String currentType = determineQrType(sessionId);
            String qrCodeData = jwtService.generateQrCodeData(sessionId, session.getQrToken(), currentType);

            return new QrTokenResponse(qrCodeData, session.getQrExpiresAt(), sessionId, currentType);
        }

        if (classSessionRepository.existsByActualLecturer_IdAndStatus(session.getActualLecturer().getId(), ClassSession.Status.open)) {
            throw new AppException(ErrorCode.CLASS_SESSION_ALREADY_OPEN);
        }

//        // Nếu session chưa mở, kiểm tra thời gian có hợp lệ không
//        if (session.getStatus() != ClassSession.Status.open) {
//            validateSessionOpenTime(session);
//        }

        if (session.getStatus() != ClassSession.Status.scheduled) {
            throw new AppException(ErrorCode.INVALID_SESSION_STATUS);
        }

        // 2. Batch Insert điểm danh ban đầu
        if (!attendanceRepository.existsByClassSession_Id(sessionId)) {
            String adminClassCode = session.getSchedule().getAdminClass().getCode();
            List<Student> students = studentRepository.findAllByAdminClass_Code(adminClassCode);

            List<Attendance> attendances = students.stream().map(student -> Attendance.builder()
                    .classSession(session)
                    .student(student)
                    .status(Attendance.Status.absent)
                    .isLate(false)
                    .leftEarly(false)
                    .build()).collect(Collectors.toList());

            attendanceRepository.saveAll(attendances);
        }

        session.setStatus(ClassSession.Status.open);
        session.setOpenedAt(LocalDateTime.now());

        return generateQrToken(session, "CHECK_IN");
    }

    @Transactional
    public QrTokenResponse startCheckOutQr(Long sessionId, int checkoutMins) {
        ClassSession session = getAndValidateLecturerSession(sessionId, true);
        ensureSessionIsOpen(session);

        if (checkoutEventRepository.existsByClassSession_IdAndClosedAtIsNull(sessionId)) {
            throw new AppException(ErrorCode.CHECKOUT_ALREADY_ACTIVE);
        }

        CheckoutEvent checkoutEvent = CheckoutEvent.builder()
                .classSession(session).triggeredBy(session.getActualLecturer().getUser())
                .triggeredAt(LocalDateTime.now()).deadlineAt(LocalDateTime.now().plusMinutes(checkoutMins))
                .build();
        checkoutEventRepository.save(checkoutEvent);

        return generateQrToken(session, "CHECK_OUT");
    }

    @Transactional
    public QrTokenResponse refreshQrToken(Long sessionId) {
        ClassSession session = getAndValidateLecturerSession(sessionId, true);
        ensureSessionIsOpen(session);
        return generateQrToken(session, determineQrType(sessionId));
    }

    @Transactional
    public void closeClassSession(Long sessionId) {
        ClassSession session = getAndValidateLecturerSession(sessionId, true);
        ensureSessionIsOpen(session);

        // 1. Tìm sự kiện Check-out đang mở
        checkoutEventRepository.findFirstByClassSession_IdAndClosedAtIsNullOrderByTriggeredAtDesc(sessionId)
                .ifPresent(event -> {

                    // 🚀 BƯỚC QUAN TRỌNG: Đánh dấu những SV có mặt nhưng chưa Check-out là "Về sớm"
                    attendanceRepository.markLeftEarlyForSession(sessionId, event);

                    // Sau đó mới đóng sự kiện lại
                    event.setClosedAt(LocalDateTime.now());
                    checkoutEventRepository.save(event); // Lưu ý: Cần save lại event nếu object đang bị detach, hoặc JPA sẽ tự flush nếu đang trong context. Tốt nhất là cứ gọi save cho chắc chắn.
                });

        // 2. Cập nhật trạng thái buổi học
        session.setStatus(ClassSession.Status.closed);
        session.setQrToken(null);
        session.setQrExpiresAt(null);
        session.setClosedAt(LocalDateTime.now());

        // classSessionRepository.save(session); // Đừng quên lưu lại session nhé (nếu method này chưa có)

        // 3. Gửi sự kiện SSE đóng buổi học cho Frontend
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                attendanceSseService.pushSessionClosed(sessionId);
            }
        });
    }

    @Transactional
    public void studentAttend(StudentAttendRequest request) {
        Long studentId = securityUtils.getCurrentStudentId();

        // 1. Giải mã Payload
        QrDataPayload payload = jwtService.extractQrCodeData(request.getToken());

        // 2. Kiểm tra Session có tồn tại không
        ClassSession session = classSessionRepository.findById(payload.sessionId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        // Kiểm tra trạng thái buổi học
        if (!session.getStatus().equals(ClassSession.Status.open)) {
            throw new AppException(ErrorCode.INVALID_SESSION_STATUS);
        }

        // 3. So khớp Token gốc trong Database
        if (session.getQrToken() == null || !session.getQrToken().equals(payload.qrToken())) {
            throw new AppException(ErrorCode.QR_INVALID);
        }

        // 4. Kiểm tra thời hạn Token
        if (LocalDateTime.now().isAfter(session.getQrExpiresAt())) {
            throw new AppException(ErrorCode.QR_EXPIRED);
        }

        // 5. Kiểm tra type hợp lệ dựa vào sự kiện
        boolean isCheckoutActive = checkoutEventRepository.existsByClassSession_IdAndClosedAtIsNull(session.getId());
        if ("CHECK_OUT".equalsIgnoreCase(payload.type()) && !isCheckoutActive) {
            throw new AppException(ErrorCode.CHECKOUT_NOT_ALLOWED);
        }
        if ("CHECK_IN".equalsIgnoreCase(payload.type()) && isCheckoutActive) {
            throw new AppException(ErrorCode.CHECKIN_NOT_ALLOWED);
        }

        String deviceId = request.getDeviceId();
        double lat = request.getLat();
        double lng = request.getLng();

        // 6. Kiểm tra gian lận qua Device ID
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            boolean isDeviceUsed = attendanceRepository.existsByClassSession_IdAndDeviceIdAndStatusAndStudent_IdNot(
                    session.getId(), deviceId, Attendance.Status.present, studentId);
            if (isDeviceUsed) {
                throw new AppException(ErrorCode.DEVICE_ALREADY_USED);
            }
        }

        // 7. Cập nhật record điểm danh cho sinh viên (Sử dụng Lock chống Spam Click)
        Attendance attendance = attendanceRepository.findByClassSessionIdAndStudentIdWithLock(session.getId(), studentId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_IN_CLASS));

        if (attendance.getStatus() == Attendance.Status.excused) {
            throw new AppException(ErrorCode.ATTENDANCE_EXCUSED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            attendance.setDeviceId(deviceId);
        }

        if ("CHECK_IN".equalsIgnoreCase(payload.type())) {
            if (attendance.getScannedAt() != null) {
                throw new AppException(ErrorCode.ALREADY_CHECKED_IN);
            }
            attendance.setStatus(Attendance.Status.present);
            attendance.setScannedAt(now);
            attendance.setScanLat(java.math.BigDecimal.valueOf(lat).setScale(7, java.math.RoundingMode.HALF_UP));
            attendance.setScanLng(java.math.BigDecimal.valueOf(lng).setScale(7, java.math.RoundingMode.HALF_UP));

            // Xử lý GPS
            if (session.getGpsEnabled()) {
                Double distance = calculateGpsDistance(session, lat, lng);
                if (distance != null) {
                    attendance.setDistanceM(distance.shortValue());
                    boolean isVerified = isWithinGpsRadius(session, distance);
                    attendance.setGpsVerified(isVerified);
                    if (!isVerified) {
                        throw new AppException(ErrorCode.OUT_OF_LOCATION);
                    }
                } else {
                    attendance.setGpsVerified(null);
                }
            } else {
                attendance.setGpsVerified(null);
            }

            // Xử lý đi muộn
            PeriodTime periodTime = periodTimeRepository.findById(session.getSchedule().getPeriodStart())
                    .orElseThrow(() -> new AppException(ErrorCode.PERIOD_TIME_NOT_FOUND));

            LocalDateTime scheduledStartTime = LocalDateTime.of(session.getSessionDate(), periodTime.getStartTime());
            long diffMinutes = java.time.Duration.between(scheduledStartTime, now).toMinutes();

//            if (diffMinutes > session.getSchedule().getMaxLateMin()) {
//                throw new AppException(ErrorCode.ATTENDANCE_LIMIT_EXCEEDED);
//            }

            if (diffMinutes > session.getSchedule().getLateThresholdMin()) {
                attendance.setIsLate(true);
                attendance.setLateMinutes((short) Math.max(0, diffMinutes));
            } else {
                attendance.setIsLate(false);
                attendance.setLateMinutes((short) 0);
            }
        } else if ("CHECK_OUT".equalsIgnoreCase(payload.type())) {
            if (attendance.getScannedAt() == null) {
                throw new AppException(ErrorCode.NOT_CHECKED_IN);
            }
            if (attendance.getCheckedOutAt() != null) {
                throw new AppException(ErrorCode.ALREADY_CHECKED_OUT);
            }

            CheckoutEvent event = checkoutEventRepository.findFirstByClassSession_IdAndClosedAtIsNullOrderByTriggeredAtDesc(session.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_EVENT_NOT_FOUND));

            attendance.setCheckoutEvent(event);
            attendance.setCheckedOutAt(now);
            attendance.setCheckoutLat(java.math.BigDecimal.valueOf(lat).setScale(7, java.math.RoundingMode.HALF_UP));
            attendance.setCheckoutLng(java.math.BigDecimal.valueOf(lng).setScale(7, java.math.RoundingMode.HALF_UP));
            attendance.setLeftEarly(false);

            // Xử lý GPS checkout
            if (session.getGpsEnabled()) {
                Double distance = calculateGpsDistance(session, lat, lng);
                if (distance != null && !isWithinGpsRadius(session, distance)) {
                    throw new AppException(ErrorCode.OUT_OF_LOCATION);
                }
            }
        } else {
            throw new AppException(ErrorCode.INVALID_ATTENDANCE_TYPE);
        }
        Attendance saved = attendanceRepository.save(attendance);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        attendanceSseService.pushAttendanceUpdate(session.getId(), saved);
                    }
                }
        );
    }

    private double calculateHaversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000; // Bán kính trái đất (mét)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }


    public ClassSessionDetailDto getSessionDetail(Long sessionId) {
        ClassSession session = getAndValidateLecturerSession(sessionId, false);
        Schedule schedule = session.getSchedule();
        Room room = session.getActualRoom();
        long total = classSessionRepository.countByScheduleId(schedule.getId());

        // Lấy giờ bắt đầu/kết thúc từ PeriodTime
        PeriodTime startPeriod = periodTimeRepository.findById(schedule.getPeriodStart())
                .orElse(null);
        PeriodTime endPeriod = periodTimeRepository.findById(schedule.getPeriodEnd())
                .orElse(null);

        // Tái sử dụng logic lấy trạng thái form helper đã viết
        String qrCodeData = null;
        String qrType = null;
        if (session.getStatus() == ClassSession.Status.open && session.getQrToken() != null) {
            qrType = determineQrType(sessionId);
            qrCodeData = jwtService.generateQrCodeData(sessionId, session.getQrToken(), qrType);
        }

        return ClassSessionDetailDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .subjectName(schedule.getSubject().getName())
                .subjectCode(schedule.getSubject().getCode())
                .className(schedule.getAdminClass().getCode())
                .roomCode(room.getCode())
                .building(room.getBuilding())
                .periodStart(schedule.getPeriodStart())
                .periodEnd(schedule.getPeriodEnd())
                .periodStartTime(startPeriod != null ? startPeriod.getStartTime() : null)
                .periodEndTime(endPeriod != null ? endPeriod.getEndTime() : null)
                .sessionNumber(session.getSessionNumber() != null ? session.getSessionNumber().intValue() : null)
                .totalSessions(total)
                .sessionDate(session.getSessionDate())
                .openedAt(session.getOpenedAt())
                .gpsEnabled(session.getGpsEnabled())
                .qrCodeData(qrCodeData)
                .qrExpiresAt(session.getQrExpiresAt())
                .qrType(qrType)
                .build();
    }

    public List<ClassSessionListDto> getSessionListForClassAndSubject(Long adminClassId, Long subjectId) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        return classSessionRepository.findClassSessionsListForAdminClassAndSubject(adminClassId, subjectId, lecturerId);
    }

    public List<DropdownOption> getAdminClassesForLecturer() {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        return administrativeClassMapper.toDropdownOptionList(scheduleRepository.findDistinctAdminClassesByLecturer(lecturerId));
    }

    public List<DropdownOption> getSubjectsForLecturerAndClass(Long adminClassId) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        return subjectMapper.toDropdownOptionList(scheduleRepository.findDistinctSubjectsByLecturerAndAdminClass(lecturerId, adminClassId));
    }

    public List<DropdownOption> getSubjectsForLecturer() {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        return subjectMapper.toDropdownOptionList(scheduleRepository.findDistinctSubjectsByLecturer(lecturerId));
    }
}
