package id.rockierocker.image.repository;

import id.rockierocker.image.model.CharacterExpression;
import id.rockierocker.image.model.StickerCharacter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterExpressionRepository extends JpaRepository<CharacterExpression, Long> {

    //List<CharacterExpression> findByStickerCharacter(StickerCharacter stickerCharacter);

    //Page<CharacterExpression> findByStickerCharacter(StickerCharacter stickerCharacter, Pageable pageable);

    @Query(value = "SELECT * FROM character_expression WHERE category::jsonb @> to_jsonb(:category::text)", nativeQuery = true)
    List<CharacterExpression> findByCategory(@Param("category") String category);

    @Query("SELECT ce FROM CharacterExpression ce WHERE LOWER(ce.expression) LIKE LOWER(CONCAT('%', :expression, '%'))")
    List<CharacterExpression> findByExpressionContaining(@Param("expression") String expression);
}

