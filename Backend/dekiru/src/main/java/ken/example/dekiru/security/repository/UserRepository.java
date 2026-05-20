package ken.example.dekiru.security.repository;
import ken.example.dekiru.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByRole(User.Role role);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByRole(User.Role role);
    List<User> findAllByEmailIn(List<String> emails);
}
