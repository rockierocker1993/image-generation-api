package id.rockierocker.image.repository;

import id.rockierocker.image.model.RembgConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RembgConfigRepository extends JpaRepository<RembgConfig, Long> {

    Optional<RembgConfig> findFirstByConfigCode(String configCode);

}

