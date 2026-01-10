package id.rockierocker.image.dto.characterexpression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterExpressionDto {
    private Long id;
    private String expression;
    private List<String> category;
    private Long stickerCharacterId;
    private String stickerCharacterName;
}

