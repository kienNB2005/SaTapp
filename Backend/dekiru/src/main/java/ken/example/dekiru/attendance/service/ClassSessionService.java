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
    ken.example.dekiru.academic.repository.RoomRepository roomRepository;
    ken.example.dekiru.academic.repository.SemesterRepository semesterRepository;
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
        PeriodTime periodTime = periodTimeRepository.findById(session.getActualPeriodStart())
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

        // Nếu session chưa mở, kiểm tra thời gian có hợp lệ không
        if (session.getStatus() != ClassSession.Status.open) {
            validateSessionOpenTime(session);
        }

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
            PeriodTime periodTime = periodTimeRepository.findById(session.getActualPeriodStart())
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
        PeriodTime startPeriod = periodTimeRepository.findById(session.getActualPeriodStart())
                .orElse(null);
        PeriodTime endPeriod = periodTimeRepository.findById(session.getActualPeriodEnd())
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
                .periodStart(session.getActualPeriodStart())
                .periodEnd(session.getActualPeriodEnd())
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

    public List<WeeklySessionDto> getLecturerSessionsByWeek(Long semesterId, int week) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        ken.example.dekiru.academic.entity.Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        int relativeWeek = week - (semester.getStartWeek() != null ? semester.getStartWeek() : 1);
        java.time.LocalDate startDate = semester.getStartDate().plusWeeks(relativeWeek);
        java.time.LocalDate endDate = startDate.plusDays(6);

        List<ClassSession> sessions = classSessionRepository
                .findByActualLecturer_IdAndSessionDateBetween(lecturerId, startDate, endDate);

        return sessions.stream().map(s -> WeeklySessionDto.builder()
                .id(s.getId())
                .classSessionId(s.getId())
                .subjectName(s.getSchedule().getSubject().getName())
                .className(s.getSchedule().getAdminClass().getCode())
                .roomCode(s.getActualRoom().getCode())
                .sessionDate(s.getSessionDate())
                .status(s.getStatus().name())
                .sessionNumber(s.getSessionNumber() != null ? s.getSessionNumber().byteValue() : null)
                .makeupForId(s.getMakeupFor() != null ? s.getMakeupFor().getId() : null)
                .originalSessionDate(s.getMakeupFor() != null ? s.getMakeupFor().getSessionDate() : null)
                .periodStart(s.getActualPeriodStart())
                .periodEnd(s.getActualPeriodEnd())
                .dayOfWeek(s.getSessionDate().getDayOfWeek().getValue() == 7 ? 8 : s.getSessionDate().getDayOfWeek().getValue() + 1)
                .build()).collect(Collectors.toList());
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

    @Transactional
    public void cancelClassSession(Long sessionId, String reason) {
        ClassSession session = getAndValidateLecturerSession(sessionId, true);
        if (session.getStatus() != ClassSession.Status.scheduled) {
            throw new AppException(ErrorCode.INVALID_SESSION_STATUS);
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new AppException(ErrorCode.CANCEL_REASON_REQUIRED);
        }
        
        if (session.getMakeupFor() != null) {
            classSessionRepository.delete(session);
        } else {
            session.setStatus(ClassSession.Status.cancelled);
            session.setCancelReason(reason);
            session.setCancelledBy(session.getActualLecturer().getUser());
            session.setCancelledAt(LocalDateTime.now());
        }
    }

    @Transactional
    public void adminCancelClassSession(Long sessionId, String reason, ken.example.dekiru.security.entity.User adminUser) {
        ClassSession session = classSessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));
        if (session.getStatus() != ClassSession.Status.scheduled) {
            throw new AppException(ErrorCode.INVALID_SESSION_STATUS);
        }
        
        if (session.getMakeupFor() != null) {
            classSessionRepository.delete(session);
        } else {
            session.setStatus(ClassSession.Status.cancelled);
            session.setCancelReason(reason);
            session.setCancelledBy(adminUser);
            session.setCancelledAt(LocalDateTime.now());
        }
    }

    @Transactional
    public ClassSession createMakeupSession(Long originalSessionId, MakeupSessionRequest request) {
        ClassSession originalSession = getAndValidateLecturerSession(originalSessionId, true);
        
        if (originalSession.getStatus() != ClassSession.Status.cancelled) {
            throw new AppException(ErrorCode.SESSION_NOT_CANCELLED);
        }
        
        if (classSessionRepository.existsByMakeupFor_IdAndStatusNot(originalSessionId, ClassSession.Status.cancelled)) {
            throw new AppException(ErrorCode.MAKEUP_ALREADY_EXISTS);
        }

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

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        ClassSession makeupSession = ClassSession.builder()
                .schedule(originalSession.getSchedule())
                .actualRoom(room)
                .actualLecturer(originalSession.getActualLecturer())
                .actualPeriodStart(request.getPeriodStart())
                .actualPeriodEnd(request.getPeriodEnd())
                .sessionDate(request.getSessionDate())
                .sessionNumber(originalSession.getSessionNumber()) // Vẫn giữ số thứ tự của buổi gốc
                .status(ClassSession.Status.scheduled)
                .gpsEnabled(originalSession.getGpsEnabled())
                .makeupFor(originalSession)
                .build();

        return classSessionRepository.save(makeupSession);
    }

    @Transactional
    public ClassSession adminCreateMakeupSession(Long originalSessionId, MakeupSessionRequest request) {
        ClassSession originalSession = classSessionRepository.findByIdWithLock(originalSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));
        
        if (originalSession.getStatus() != ClassSession.Status.cancelled) {
            throw new AppException(ErrorCode.SESSION_NOT_CANCELLED);
        }
        
        if (classSessionRepository.existsByMakeupFor_IdAndStatusNot(originalSessionId, ClassSession.Status.cancelled)) {
            throw new AppException(ErrorCode.MAKEUP_ALREADY_EXISTS);
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        ClassSession makeupSession = ClassSession.builder()
                .schedule(originalSession.getSchedule())
                .actualRoom(room)
                .actualLecturer(originalSession.getActualLecturer())
                .actualPeriodStart(request.getPeriodStart())
                .actualPeriodEnd(request.getPeriodEnd())
                .sessionDate(request.getSessionDate())
                .sessionNumber(originalSession.getSessionNumber()) // Vẫn giữ số thứ tự của buổi gốc
                .status(ClassSession.Status.scheduled)
                .gpsEnabled(originalSession.getGpsEnabled())
                .makeupFor(originalSession)
                .build();

        return classSessionRepository.save(makeupSession);
    }

    public List<Room> findAvailableRooms(java.time.LocalDate sessionDate, Byte periodStart, Byte periodEnd) {
        if (periodStart > periodEnd || periodStart < 1 || periodEnd > 15) {
            throw new AppException(ErrorCode.INVALID_PERIOD);
        }
        return roomRepository.findAvailableRooms(sessionDate, periodStart, periodEnd);
    }

    /*
    public List<SuggestedSlotDto> getSuggestedSlots(Long sessionId, Integer weeks) {
        ClassSession originalSession = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        if (originalSession.getStatus() != ClassSession.Status.cancelled) {
            throw new AppException(ErrorCode.SESSION_NOT_CANCELLED);
        }

        int searchWeeks = (weeks == null || weeks <= 0) ? 2 : weeks;
        int daysToSearch = searchWeeks * 7;

        java.time.LocalDate startDate = java.time.LocalDate.now();
        java.time.LocalDate semesterEndDate = originalSession.getSchedule().getSemester().getEndDate();
        java.time.LocalDate endDate = startDate.plusDays(daysToSearch);
        if (endDate.isAfter(semesterEndDate)) {
            endDate = semesterEndDate;
        }

        if (startDate.isAfter(endDate)) {
            return java.util.Collections.emptyList();
        }

        Long adminClassId = originalSession.getSchedule().getAdminClass().getId();
        Long lecturerId = originalSession.getActualLecturer().getId();
        Long scheduleId = originalSession.getSchedule().getId();
        int duration = originalSession.getActualPeriodEnd() - originalSession.getActualPeriodStart() + 1;

        // Load active sessions for class and lecturer in memory
        List<ClassSession> classSessions = classSessionRepository.findBySchedule_AdminClass_IdAndSessionDateBetweenAndStatusNot(
                adminClassId, startDate, endDate, ClassSession.Status.cancelled);
        List<ClassSession> lecturerSessions = classSessionRepository.findByActualLecturer_IdAndSessionDateBetweenAndStatusNot(
                lecturerId, startDate, endDate, ClassSession.Status.cancelled);
        List<ClassSession> scheduleSessions = classSessionRepository.findBySchedule_IdAndSessionDateBetween(
                scheduleId, startDate, endDate);

        List<SuggestedSlotDto> suggestions = new java.util.ArrayList<>();

        // Loop through each day from startDate to endDate (inclusive)
        for (java.time.LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // Loop through potential starting periods
            for (int s = 1; s <= 16 - duration; s++) {
                int e = s + duration - 1;

                // 1. Check unique constraint on schedule, sessionDate, actualPeriodStart (including cancelled ones)
                boolean uniqueViolation = false;
                for (ClassSession cs : scheduleSessions) {
                    if (cs.getSessionDate().equals(date) && cs.getActualPeriodStart() == s) {
                        uniqueViolation = true;
                        break;
                    }
                }
                if (uniqueViolation) {
                    continue;
                }

                // 2. Check class conflict
                boolean classConflicted = false;
                for (ClassSession cs : classSessions) {
                    if (cs.getSessionDate().equals(date) && cs.getActualPeriodStart() <= e && cs.getActualPeriodEnd() >= s) {
                        classConflicted = true;
                        break;
                    }
                }
                if (classConflicted) {
                    continue;
                }

                // 3. Check lecturer conflict
                boolean lecturerConflicted = false;
                for (ClassSession cs : lecturerSessions) {
                    if (cs.getSessionDate().equals(date) && cs.getActualPeriodStart() <= e && cs.getActualPeriodEnd() >= s) {
                        lecturerConflicted = true;
                        break;
                    }
                }
                if (lecturerConflicted) {
                    continue;
                }

                // 4. Query available rooms
                List<ken.example.dekiru.academic.entity.Room> rooms = roomRepository.findAvailableRooms(date, (byte) s, (byte) e);
                if (!rooms.isEmpty()) {
                    List<DropdownOption> roomOptions = rooms.stream()
                            .map(r -> new DropdownOption(r.getId(), r.getCode(), r.getCode() + (r.getBuilding() != null ? " - " + r.getBuilding() : "")))
                            .toList();

                    String dayOfWeekStr = getDayOfWeekVietnamese(date);
                    suggestions.add(new SuggestedSlotDto(date, (byte) s, (byte) e, dayOfWeekStr, roomOptions));

                    if (suggestions.size() >= 15) {
                        return suggestions;
                    }
                }
            }
        }

        return suggestions;
    }

    private String getDayOfWeekVietnamese(java.time.LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY: return "Thứ Hai";
            case TUESDAY: return "Thứ Ba";
            case WEDNESDAY: return "Thứ Tư";
            case THURSDAY: return "Thứ Năm";
            case FRIDAY: return "Thứ Sáu";
            case SATURDAY: return "Thứ Bảy";
            case SUNDAY: return "Chủ Nhật";
            default: return "";
        }
    }
    */
}
