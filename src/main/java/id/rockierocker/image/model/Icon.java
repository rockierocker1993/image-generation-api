package id.rockierocker.image.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "icons")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Icon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_image_id")
    private Icon originalImage;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "format", length = 50)
    private String format;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data")
    private byte[] data;

    @Column(name = "size")
    private Long size;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "vectorize_type", length = 10)
    private String vectorizeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;
}

