package id.rockierocker.image.service;

import id.rockierocker.image.constant.ResponseCode;
import id.rockierocker.image.dto.stickercharacter.StickerCharacterDto;
import id.rockierocker.image.dto.stickercharacter.StickerCharacterRequest;
import id.rockierocker.image.exception.BadRequestException;
import id.rockierocker.image.exception.InternalServerErrorException;
import id.rockierocker.image.model.StickerCharacter;
import id.rockierocker.image.repository.StickerCharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StickerCharacterService {

    private final StickerCharacterRepository stickerCharacterRepository;

    @Transactional(readOnly = true)
    public List<StickerCharacterDto> getAllCharacters() {
        log.info("Fetching all sticker characters");
        return stickerCharacterRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StickerCharacterDto> getAllCharacters(Pageable pageable) {
        log.info("Fetching all sticker characters with pagination");
        return stickerCharacterRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public StickerCharacterDto getCharacterById(Long id) {
        log.info("Fetching sticker character by id: {}", id);
        StickerCharacter character = stickerCharacterRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));
        return toDto(character);
    }

    @Transactional(readOnly = true)
    public StickerCharacterDto getCharacterByName(String name) {
        log.info("Fetching sticker character by name: {}", name);
        StickerCharacter character = stickerCharacterRepository.findByName(name)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));
        return toDto(character);
    }

    @Transactional(readOnly = true)
    public List<StickerCharacterDto> getCharactersByCategory(String category) {
        log.info("Fetching sticker characters by category: {}", category);
        return stickerCharacterRepository.findByCategory(category).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public StickerCharacterDto createCharacter(StickerCharacterRequest request) {
        log.info("Creating new sticker character");

        // Check if character with same name already exists
        stickerCharacterRepository.findByName(request.getName())
                .ifPresent(c -> {
                    throw new BadRequestException(ResponseCode.DATA_ALREADY_EXISTS);
                });

        StickerCharacter character = StickerCharacter.builder()
                .name(request.getName())
                .category(request.getCategory())
                .build();

        try {
            StickerCharacter saved = stickerCharacterRepository.save(character);
            log.info("Sticker character created successfully with id: {}", saved.getId());
            return toDto(saved);
        } catch (Exception e) {
            log.error("Error creating sticker character: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_SAVE_DATA);
        }
    }

    @Transactional
    public StickerCharacterDto updateCharacter(Long id, StickerCharacterRequest request) {
        log.info("Updating sticker character with id: {}", id);

        StickerCharacter character = stickerCharacterRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        // Check if another character with same name already exists
        stickerCharacterRepository.findByName(request.getName())
                .ifPresent(c -> {
                    if (!c.getId().equals(id)) {
                        throw new BadRequestException(ResponseCode.DATA_ALREADY_EXISTS);
                    }
                });

        character.setName(request.getName());
        character.setCategory(request.getCategory());

        try {
            StickerCharacter updated = stickerCharacterRepository.save(character);
            log.info("Sticker character updated successfully with id: {}", updated.getId());
            return toDto(updated);
        } catch (Exception e) {
            log.error("Error updating sticker character: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_UPDATE_DATA);
        }
    }

    @Transactional
    public void deleteCharacter(Long id) {
        log.info("Deleting sticker character with id: {}", id);

        StickerCharacter character = stickerCharacterRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        try {
            stickerCharacterRepository.delete(character);
            log.info("Sticker character deleted successfully with id: {}", id);
        } catch (Exception e) {
            log.error("Error deleting sticker character: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_DELETE_DATA);
        }
    }

    private StickerCharacterDto toDto(StickerCharacter character) {
        return StickerCharacterDto.builder()
                .id(character.getId())
                .name(character.getName())
                .category(character.getCategory())
                .build();
    }
}

