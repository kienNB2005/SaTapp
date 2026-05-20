package ken.example.dekiru.attendance.repository;

import ken.example.dekiru.attendance.entity.CheckoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckoutEventRepository extends JpaRepository<CheckoutEvent, Long> {
    boolean existsByClassSession_IdAndClosedAtIsNull(Long sessionId);
    Optional<CheckoutEvent> findFirstByClassSession_IdAndClosedAtIsNullOrderByTriggeredAtDesc(Long sessionId);
    List<CheckoutEvent> findByClosedAtIsNullAndDeadlineAtBefore(LocalDateTime now);
}