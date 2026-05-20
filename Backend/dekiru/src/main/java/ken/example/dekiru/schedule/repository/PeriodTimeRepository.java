package ken.example.dekiru.schedule.repository;

import ken.example.dekiru.schedule.entity.PeriodTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodTimeRepository extends JpaRepository<PeriodTime, Byte> {
}

