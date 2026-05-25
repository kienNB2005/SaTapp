package ken.example.dekiru.attendance.repository;

import ken.example.dekiru.attendance.entity.ClassSessionRequest;
import ken.example.dekiru.attendance.entity.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRequestRepository extends JpaRepository<ClassSessionRequest, Long> {
    @EntityGraph(attributePaths = {"classSession", "classSession.schedule", "classSession.schedule.subject", "classSession.schedule.adminClass", "classSession.actualRoom", "lecturer", "makeupRoom"})
    List<ClassSessionRequest> findByCancelStatus(RequestStatus status);
    
    @EntityGraph(attributePaths = {"classSession", "classSession.schedule", "classSession.schedule.subject", "classSession.schedule.adminClass", "classSession.actualRoom", "lecturer", "makeupRoom"})
    List<ClassSessionRequest> findByMakeupStatus(RequestStatus status);
    
    @EntityGraph(attributePaths = {"classSession", "classSession.schedule", "classSession.schedule.subject", "classSession.schedule.adminClass", "classSession.actualRoom", "lecturer", "makeupRoom"})
    List<ClassSessionRequest> findByLecturer_Id(Long lecturerId);
    
    Optional<ClassSessionRequest> findByClassSession_Id(Long classSessionId);
}
