package ken.example.dekiru.attendance.scheduler;

import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassSessionScheduler {

    private final ClassSessionRepository classSessionRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Runs at midnight every day
    @Transactional
    public void closeOpenSessions() {
        log.info("Running scheduled task to close open class sessions...");
        List<ClassSession> openSessions = classSessionRepository.findByStatusAndSessionDateBefore(ClassSession.Status.open, LocalDate.now());
        
        if (!openSessions.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (ClassSession session : openSessions) {
                session.setStatus(ClassSession.Status.closed);
                session.setClosedAt(now);
                session.setQrToken(null);
                session.setQrExpiresAt(null);
            }
            classSessionRepository.saveAll(openSessions);
            log.info("Successfully closed {} open class sessions.", openSessions.size());
        } else {
            log.info("No open class sessions found to close.");
        }
    }
}

