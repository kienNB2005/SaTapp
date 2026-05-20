package ken.example.dekiru.security.repository;

import ken.example.dekiru.security.entity.RedisToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisTokenRepository extends CrudRepository<RedisToken,String> {
}
