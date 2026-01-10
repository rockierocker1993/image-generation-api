package id.rockierocker.image.dto.promptai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptAIRequest {

    private String prompt;
    private List<String> category;
    private String type;
}

