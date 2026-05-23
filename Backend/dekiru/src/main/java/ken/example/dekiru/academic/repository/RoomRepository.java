package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByCode(String code);
    Optional<Room> findByCode(String code);
    List<Room> findAllByCodeIn(List<String> codes);

    @org.springframework.data.jpa.repository.Query("""
        SELECT r FROM Room r 
        WHERE r.id NOT IN (
            SELECT cs.actualRoom.id FROM ken.example.dekiru.attendance.entity.ClassSession cs
            WHERE cs.status <> ken.example.dekiru.attendance.entity.ClassSession.Status.cancelled
              AND cs.sessionDate = :sessionDate
              AND cs.actualPeriodStart <= :periodEnd
              AND cs.actualPeriodEnd >= :periodStart
        )
    """)
    List<Room> findAvailableRooms(
        @org.springframework.data.repository.query.Param("sessionDate") java.time.LocalDate sessionDate,
        @org.springframework.data.repository.query.Param("periodStart") Byte periodStart,
        @org.springframework.data.repository.query.Param("periodEnd") Byte periodEnd
    );
}

