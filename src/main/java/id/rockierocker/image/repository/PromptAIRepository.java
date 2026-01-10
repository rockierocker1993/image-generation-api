package id.rockierocker.image.repository;

import id.rockierocker.image.model.PromptAI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptAIRepository extends JpaRepository<PromptAI, Long> {

    List<PromptAI> findByType(String type);

    @Query(value = "SELECT * FROM prompt_ai WHERE category::jsonb @> to_jsonb(:category::text)", nativeQuery = true)
    List<PromptAI> findByCategory(@Param("category") String category);

    @Query("SELECT p FROM PromptAI p WHERE LOWER(p.prompt) LIKE LOWER(CONCAT('%', :prompt, '%'))")
    List<PromptAI> findByPromptContaining(@Param("prompt") String prompt);

    @Query(value = "SELECT * FROM prompt_ai WHERE type = :type AND category::jsonb @> to_jsonb(:category::text)", nativeQuery = true)
    List<PromptAI> findByTypeAndCategory(@Param("type") String type, @Param("category") String category);
}

