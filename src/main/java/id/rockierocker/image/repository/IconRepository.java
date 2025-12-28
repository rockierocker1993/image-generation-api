package id.rockierocker.image.repository;

import id.rockierocker.image.model.Icon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface IconRepository extends JpaRepository<Icon, Long> {

}

