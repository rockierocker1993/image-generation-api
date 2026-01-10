package id.rockierocker.image.dto.stickercharacter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StickerCharacterRequest {

    private String name;

    private List<String> category;
}

