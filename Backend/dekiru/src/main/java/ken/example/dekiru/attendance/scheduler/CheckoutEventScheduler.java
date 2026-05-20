package ken.example.dekiru.attendance.scheduler;

import ken.example.dekiru.attendance.entity.CheckoutEvent;
import ken.example.dekiru.attendance.repository.CheckoutEventRepository;
import ken.example.dekiru.attendance.repository.AttendanceRepository;
import ken.example.dekiru.attendance.service.AttendanceSseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Component // Khai báo đây là một Spring Bean để Spring quản lý
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckoutEventScheduler {

    // Gọi lại 2 repo cần thiết cho việc update
    CheckoutEventRepository checkoutEventRepository;
    AttendanceRepository attendanceRepository;
    AttendanceSseService attendanceSseService;

    @Scheduled(fixedDelay = 30_000) // chạy mỗi 30 giây
    @Transactional
    public void closeExpiredCheckoutEvents() {
        List<CheckoutEvent> expired = checkoutEventRepository
                .findByClosedAtIsNullAndDeadlineAtBefore(LocalDateTime.now());

        for (CheckoutEvent event : expired) {
            // Đánh left_early cho SV present nhưng chưa check-out
            attendanceRepository.markLeftEarlyForSession(
                    event.getClassSession().getId(), event);

            event.setClosedAt(LocalDateTime.now());
            checkoutEventRepository.save(event);

            // Push sau commit để tránh gửi data trước khi DB commit xong
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            attendanceSseService.pushLeftEarlyUpdate(event.getClassSession().getId());
                        }
                    }
            );
        }
    }
}