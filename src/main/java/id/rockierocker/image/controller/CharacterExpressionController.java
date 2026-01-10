package id.rockierocker.image.controller;

import id.rockierocker.image.dto.characterexpression.CharacterExpressionDto;
import id.rockierocker.image.dto.characterexpression.CharacterExpressionRequest;
import id.rockierocker.image.service.CharacterExpressionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/character-expressions")
@RequiredArgsConstructor
@Slf4j
public class CharacterExpressionController {

    private final CharacterExpressionService characterExpressionService;

    @GetMapping
    public ResponseEntity<List<CharacterExpressionDto>> getAllCharacterExpressions() {
        log.info("GET /api/character-expressions - Get all character expressions");
        List<CharacterExpressionDto> expressions = characterExpressionService.getAllCharacterExpressions();
        return ResponseEntity.ok(expressions);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<CharacterExpressionDto>> getAllCharacterExpressionsPageable(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("GET /api/character-expressions/page - Get all character expressions with pagination");
        Page<CharacterExpressionDto> expressions = characterExpressionService.getAllCharacterExpressions(pageable);
        return ResponseEntity.ok(expressions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterExpressionDto> getCharacterExpressionById(@PathVariable Long id) {
        log.info("GET /api/character-expressions/{} - Get character expression by id", id);
        CharacterExpressionDto expression = characterExpressionService.getCharacterExpressionById(id);
        return ResponseEntity.ok(expression);
    }

//    @GetMapping("/character/{characterId}")
//    public ResponseEntity<List<CharacterExpressionDto>> getCharacterExpressionsByCharacterId(
//            @PathVariable Long characterId) {
//        log.info("GET /api/character-expressions/character/{} - Get expressions by character id", characterId);
//        List<CharacterExpressionDto> expressions = characterExpressionService.getCharacterExpressionsByCharacterId(characterId);
//        return ResponseEntity.ok(expressions);
//    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<CharacterExpressionDto>> getCharacterExpressionsByCategory(
            @PathVariable String category) {
        log.info("GET /api/character-expressions/category/{} - Get expressions by category", category);
        List<CharacterExpressionDto> expressions = characterExpressionService.getCharacterExpressionsByCategory(category);
        return ResponseEntity.ok(expressions);
    }

    @PostMapping
    public ResponseEntity<CharacterExpressionDto> createCharacterExpression(
            @RequestBody CharacterExpressionRequest request) {
        log.info("POST /api/character-expressions - Create new character expression");
        CharacterExpressionDto created = characterExpressionService.createCharacterExpression(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CharacterExpressionDto> updateCharacterExpression(
            @PathVariable Long id,
            @RequestBody CharacterExpressionRequest request) {
        log.info("PUT /api/character-expressions/{} - Update character expression", id);
        CharacterExpressionDto updated = characterExpressionService.updateCharacterExpression(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharacterExpression(@PathVariable Long id) {
        log.info("DELETE /api/character-expressions/{} - Delete character expression", id);
        characterExpressionService.deleteCharacterExpression(id);
        return ResponseEntity.noContent().build();
    }
}

