package id.rockierocker.image.repository;

import id.rockierocker.image.model.VtraceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VtraceConfigRepository extends JpaRepository<VtraceConfig, Long> {
    Optional<VtraceConfig> findFirstByConfigCode(String configCode);
}

