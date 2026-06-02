package ken.example.dekiru.attendance.service;

import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDate;
import ken.example.dekiru.attendance.dto.MakeupSessionRequest;
import ken.example.dekiru.attendance.dto.SuggestedSlotDto;
import ken.example.dekiru.schedule.entity.Schedule;
import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.academic.repository.RoomRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClassSessionServiceTest {

    @Mock
    ClassSessionRepository classSessionRepository;

    @Mock
    RoomRepository roomRepository;

    @Mock
    SecurityUtils securityUtils;

    @InjectMocks
    ClassSessionService classSessionService;

    @Test
    void cancelClassSession_regularSession_shouldSetStatusToCancelled() {
        // Arrange
        Long sessionId = 1L;
        Long lecturerId = 2L;
        String reason = "Lecturer is busy with other urgent school duties";

        User user = new User();
        Lecturer lecturer = new Lecturer();
        lecturer.setId(lecturerId);
        lecturer.setUser(user);

        ClassSession session = new ClassSession();
        session.setId(sessionId);
        session.setStatus(ClassSession.Status.scheduled);
        session.setActualLecturer(lecturer);
        session.setMakeupFor(null); // Regular session, not a makeup session

        when(securityUtils.getCurrentLecturerId()).thenReturn(lecturerId);
        when(classSessionRepository.findByIdWithLock(sessionId)).thenReturn(Optional.of(session));

        // Act
        classSessionService.cancelClassSession(sessionId, reason);

        // Assert
        assertEquals(ClassSession.Status.cancelled, session.getStatus());
        assertEquals(reason, session.getCancelReason());
        assertEquals(user, session.getCancelledBy());
        assertNotNull(session.getCancelledAt());
        verify(classSessionRepository, never()).delete(any());
    }

    @Test
    void cancelClassSession_makeupSession_shouldDeleteFromRepository() {
        // Arrange
        Long sessionId = 1L;
        Long lecturerId = 2L;
        String reason = "Lecturer wants to cancel this makeup session";

        User user = new User();
        Lecturer lecturer = new Lecturer();
        lecturer.setId(lecturerId);
        lecturer.setUser(user);

        ClassSession originalSession = new ClassSession();
        originalSession.setId(99L);

        ClassSession session = new ClassSession();
        session.setId(sessionId);
        session.setStatus(ClassSession.Status.scheduled);
        session.setActualLecturer(lecturer);
        session.setMakeupFor(originalSession); // This is a makeup session

        when(securityUtils.getCurrentLecturerId()).thenReturn(lecturerId);
        when(classSessionRepository.findByIdWithLock(sessionId)).thenReturn(Optional.of(session));

        // Act
        classSessionService.cancelClassSession(sessionId, reason);

        // Assert
        verify(classSessionRepository, times(1)).delete(session);
        // Ensure no other status changes or cancellations are recorded on it
        assertNotEquals(ClassSession.Status.cancelled, session.getStatus());
    }

    @Test
    void cancelClassSession_invalidStatus_shouldThrowException() {
        // Arrange
        Long sessionId = 1L;
        Long lecturerId = 2L;
        String reason = "Reason";

        Lecturer lecturer = new Lecturer();
        lecturer.setId(lecturerId);

        ClassSession session = new ClassSession();
        session.setId(sessionId);
        session.setStatus(ClassSession.Status.open); // Already open, cannot cancel
        session.setActualLecturer(lecturer);

        when(securityUtils.getCurrentLecturerId()).thenReturn(lecturerId);
        when(classSessionRepository.findByIdWithLock(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            classSessionService.cancelClassSession(sessionId, reason);
        });
        assertEquals(ErrorCode.INVALID_SESSION_STATUS, exception.getErrorCode());
        verify(classSessionRepository, never()).delete(any());
    }

    @Test
    void cancelClassSession_reasonTooShort_shouldThrowException() {
        // Arrange
        Long sessionId = 1L;
        Long lecturerId = 2L;
        String reason = "abc"; // Too short (less than 5 chars)

        Lecturer lecturer = new Lecturer();
        lecturer.setId(lecturerId);

        ClassSession session = new ClassSession();
        session.setId(sessionId);
        session.setStatus(ClassSession.Status.scheduled);
        session.setActualLecturer(lecturer);

        when(securityUtils.getCurrentLecturerId()).thenReturn(lecturerId);
        when(classSessionRepository.findByIdWithLock(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            classSessionService.cancelClassSession(sessionId, reason);
        });
        assertEquals(ErrorCode.CANCEL_REASON_REQUIRED, exception.getErrorCode());
        verify(classSessionRepository, never()).delete(any());
    }

    @Test
    void createMakeupSession_duplicateSessionDateAndPeriod_shouldThrowException() {
        // Arrange
        Long originalSessionId = 10L;
        Long lecturerId = 2L;
        LocalDate makeupDate = LocalDate.of(2026, 6, 1);

        Lecturer lecturer = new Lecturer();
        lecturer.setId(lecturerId);

        Semester semester = new Semester();
        semester.setEndDate(LocalDate.of(2026, 7, 1));

        Schedule schedule = new Schedule();
        schedule.setId(100L);
        schedule.setSemester(semester);

        ClassSession originalSession = new ClassSession();
        originalSession.setId(originalSessionId);
        originalSession.setStatus(ClassSession.Status.cancelled);
        originalSession.setActualLecturer(lecturer);
        originalSession.setSchedule(schedule);
        originalSession.setSessionDate(LocalDate.of(2026, 5, 25));

        MakeupSessionRequest request = new MakeupSessionRequest();
        request.setSessionDate(makeupDate);
        request.setPeriodStart((byte) 1);
        request.setPeriodEnd((byte) 3);
        request.setRoomId(50L);

        when(securityUtils.getCurrentLecturerId()).thenReturn(lecturerId);
        when(classSessionRepository.findByIdWithLock(originalSessionId)).thenReturn(Optional.of(originalSession));
        when(classSessionRepository.existsByMakeupFor_IdAndStatusNot(originalSessionId, ClassSession.Status.cancelled)).thenReturn(false);
        when(classSessionRepository.existsByScheduleIdAndSessionDateAndActualPeriodStart(schedule.getId(), makeupDate, request.getPeriodStart())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            classSessionService.createMakeupSession(originalSessionId, request);
        });
        assertEquals(ErrorCode.DUPLICATE_SESSION_DATE, exception.getErrorCode());
    }

//    @Test
//    void getSuggestedSlots_shouldReturnValidSuggestions() {
//        // Arrange
//        Long originalSessionId = 10L;
//        LocalDate today = LocalDate.now();
//
//        Semester semester = new Semester();
//        semester.setEndDate(today.plusDays(30));
//
//        Schedule schedule = new Schedule();
//        schedule.setId(100L);
//        schedule.setSemester(semester);
//        
//        ken.example.dekiru.academic.entity.AdministrativeClass adminClass = new ken.example.dekiru.academic.entity.AdministrativeClass();
//        adminClass.setId(200L);
//        schedule.setAdminClass(adminClass);
//
//        Lecturer lecturer = new Lecturer();
//        lecturer.setId(300L);
//
//        ClassSession originalSession = new ClassSession();
//        originalSession.setId(originalSessionId);
//        originalSession.setStatus(ClassSession.Status.cancelled);
//        originalSession.setActualLecturer(lecturer);
//        originalSession.setSchedule(schedule);
//        originalSession.setActualPeriodStart((byte) 1);
//        originalSession.setActualPeriodEnd((byte) 3);
//        originalSession.setSessionDate(today.minusDays(2));
//
//        when(classSessionRepository.findById(originalSessionId)).thenReturn(Optional.of(originalSession));
//
//        LocalDate startDate = today;
//        LocalDate endDate = today.plusDays(14);
//        
//        when(classSessionRepository.findBySchedule_AdminClass_IdAndSessionDateBetweenAndStatusNot(
//                eq(200L), eq(startDate), eq(endDate), eq(ClassSession.Status.cancelled)))
//                .thenReturn(java.util.Collections.emptyList());
//
//        when(classSessionRepository.findByActualLecturer_IdAndSessionDateBetweenAndStatusNot(
//                eq(300L), eq(startDate), eq(endDate), eq(ClassSession.Status.cancelled)))
//                .thenReturn(java.util.Collections.emptyList());
//
//        when(classSessionRepository.findBySchedule_IdAndSessionDateBetween(
//                eq(100L), eq(startDate), eq(endDate)))
//                .thenReturn(java.util.Collections.emptyList());
//
//        ken.example.dekiru.academic.entity.Room room = new ken.example.dekiru.academic.entity.Room();
//        room.setId(400L);
//        room.setCode("A101");
//        room.setBuilding("Building A");
//
//        when(roomRepository.findAvailableRooms(any(), any(), any()))
//                .thenReturn(java.util.List.of(room));
//
//        // Act
//        java.util.List<SuggestedSlotDto> result = classSessionService.getSuggestedSlots(originalSessionId, 2);
//
//        // Assert
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//        // Verify we got the first slot for today
//        SuggestedSlotDto firstSlot = result.get(0);
//        assertEquals(today, firstSlot.sessionDate());
//        assertEquals((byte) 1, firstSlot.periodStart());
//        assertEquals((byte) 3, firstSlot.periodEnd());
//        assertEquals("A101", firstSlot.availableRooms().get(0).code());
//    }
}
