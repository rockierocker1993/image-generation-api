package id.rockierocker.image.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sticker_character")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StickerCharacter extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", columnDefinition = "text")
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category", columnDefinition = "jsonb")
    private List<String> category; // animal, anime, lion, etc

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sticker_character_expression",
            joinColumns = @JoinColumn(name = "sticker_character_id"),
            inverseJoinColumns = @JoinColumn(name = "character_expression_id")
    )
    private List<CharacterExpression> expressions = new ArrayList<>();


}
