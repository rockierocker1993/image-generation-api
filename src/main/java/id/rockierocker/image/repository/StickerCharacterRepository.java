package id.rockierocker.image.repository;

import id.rockierocker.image.model.StickerCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StickerCharacterRepository extends JpaRepository<StickerCharacter, Long> {

    Optional<StickerCharacter> findByName(String name);

    @Query(value = "SELECT * FROM sticker_character WHERE category::jsonb @> to_jsonb(:category::text)", nativeQuery = true)
    List<StickerCharacter> findByCategory(@Param("category") String category);

    @Query("SELECT sc FROM StickerCharacter sc WHERE LOWER(sc.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<StickerCharacter> findByNameContaining(@Param("name") String name);
}

