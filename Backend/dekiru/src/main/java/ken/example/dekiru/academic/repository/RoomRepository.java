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
}

