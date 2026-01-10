package id.rockierocker.image.controller;

import id.rockierocker.image.dto.stickercharacter.StickerCharacterDto;
import id.rockierocker.image.dto.stickercharacter.StickerCharacterRequest;
import id.rockierocker.image.service.StickerCharacterService;
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
@RequestMapping("/api/sticker-characters")
@RequiredArgsConstructor
@Slf4j
public class StickerCharacterController {

    private final StickerCharacterService stickerCharacterService;

    @GetMapping
    public ResponseEntity<List<StickerCharacterDto>> getAllCharacters() {
        log.info("GET /api/sticker-characters - Get all sticker characters");
        List<StickerCharacterDto> characters = stickerCharacterService.getAllCharacters();
        return ResponseEntity.ok(characters);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<StickerCharacterDto>> getAllCharactersPageable(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("GET /api/sticker-characters/page - Get all sticker characters with pagination");
        Page<StickerCharacterDto> characters = stickerCharacterService.getAllCharacters(pageable);
        return ResponseEntity.ok(characters);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StickerCharacterDto> getCharacterById(@PathVariable Long id) {
        log.info("GET /api/sticker-characters/{} - Get sticker character by id", id);
        StickerCharacterDto character = stickerCharacterService.getCharacterById(id);
        return ResponseEntity.ok(character);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<StickerCharacterDto> getCharacterByName(@PathVariable String name) {
        log.info("GET /api/sticker-characters/name/{} - Get sticker character by name", name);
        StickerCharacterDto character = stickerCharacterService.getCharacterByName(name);
        return ResponseEntity.ok(character);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<StickerCharacterDto>> getCharactersByCategory(@PathVariable String category) {
        log.info("GET /api/sticker-characters/category/{} - Get sticker characters by category", category);
        List<StickerCharacterDto> characters = stickerCharacterService.getCharactersByCategory(category);
        return ResponseEntity.ok(characters);
    }

    @PostMapping
    public ResponseEntity<StickerCharacterDto> createCharacter(@RequestBody StickerCharacterRequest request) {
        log.info("POST /api/sticker-characters - Create new sticker character");
        StickerCharacterDto created = stickerCharacterService.createCharacter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StickerCharacterDto> updateCharacter(
            @PathVariable Long id,
            @RequestBody StickerCharacterRequest request) {
        log.info("PUT /api/sticker-characters/{} - Update sticker character", id);
        StickerCharacterDto updated = stickerCharacterService.updateCharacter(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharacter(@PathVariable Long id) {
        log.info("DELETE /api/sticker-characters/{} - Delete sticker character", id);
        stickerCharacterService.deleteCharacter(id);
        return ResponseEntity.noContent().build();
    }
}

