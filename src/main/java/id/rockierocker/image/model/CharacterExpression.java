package id.rockierocker.image.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "character_expression")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterExpression extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expression", columnDefinition = "text")
    private String expression;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category", columnDefinition = "jsonb")
    private List<String> category; // animal, anime, lion, etc
}
