package id.rockierocker.image.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "prompt_ai")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptAI extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt", columnDefinition = "text")
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category", columnDefinition = "jsonb")
    private List<String> category; // sticker, anime, lion, etc

    @Column(name = "type" ,length = 15)
    private String type;// sticker prompt, and etc

    @Column(name = "image_template_prompt")
    private String imageTemplatePrompt;

}
