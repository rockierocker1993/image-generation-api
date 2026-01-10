package id.rockierocker.image.service;

import id.rockierocker.image.constant.ResponseCode;
import id.rockierocker.image.dto.characterexpression.CharacterExpressionDto;
import id.rockierocker.image.dto.characterexpression.CharacterExpressionRequest;
import id.rockierocker.image.exception.BadRequestException;
import id.rockierocker.image.exception.InternalServerErrorException;
import id.rockierocker.image.model.CharacterExpression;
import id.rockierocker.image.model.StickerCharacter;
import id.rockierocker.image.repository.CharacterExpressionRepository;
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
public class CharacterExpressionService {

    private final CharacterExpressionRepository characterExpressionRepository;
    private final StickerCharacterRepository stickerCharacterRepository;

    @Transactional(readOnly = true)
    public List<CharacterExpressionDto> getAllCharacterExpressions() {
        log.info("Fetching all character expressions");
        return characterExpressionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CharacterExpressionDto> getAllCharacterExpressions(Pageable pageable) {
        log.info("Fetching all character expressions with pagination");
        return characterExpressionRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public CharacterExpressionDto getCharacterExpressionById(Long id) {
        log.info("Fetching character expression by id: {}", id);
        CharacterExpression expression = characterExpressionRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));
        return toDto(expression);
    }

//    @Transactional(readOnly = true)
//    public List<CharacterExpressionDto> getCharacterExpressionsByCharacterId(Long characterId) {
//        log.info("Fetching character expressions by character id: {}", characterId);
//        StickerCharacter character = stickerCharacterRepository.findById(characterId)
//                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));
//
//        return characterExpressionRepository.findByStickerCharacter(character).stream()
//                .map(this::toDto)
//                .collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public List<CharacterExpressionDto> getCharacterExpressionsByCategory(String category) {
        log.info("Fetching character expressions by category: {}", category);
        return characterExpressionRepository.findByCategory(category).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CharacterExpressionDto createCharacterExpression(CharacterExpressionRequest request) {
        log.info("Creating new character expression");

        StickerCharacter character = stickerCharacterRepository.findById(request.getStickerCharacterId())
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        CharacterExpression expression = CharacterExpression.builder()
                .expression(request.getExpression())
                .category(request.getCategory())
                //.stickerCharacter(character)
                .build();

        try {
            CharacterExpression saved = characterExpressionRepository.save(expression);
            log.info("Character expression created successfully with id: {}", saved.getId());
            return toDto(saved);
        } catch (Exception e) {
            log.error("Error creating character expression: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_SAVE_DATA);
        }
    }

    @Transactional
    public CharacterExpressionDto updateCharacterExpression(Long id, CharacterExpressionRequest request) {
        log.info("Updating character expression with id: {}", id);

        CharacterExpression expression = characterExpressionRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        StickerCharacter character = stickerCharacterRepository.findById(request.getStickerCharacterId())
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        expression.setExpression(request.getExpression());
        expression.setCategory(request.getCategory());
        //expression.setStickerCharacter(character);

        try {
            CharacterExpression updated = characterExpressionRepository.save(expression);
            log.info("Character expression updated successfully with id: {}", updated.getId());
            return toDto(updated);
        } catch (Exception e) {
            log.error("Error updating character expression: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_UPDATE_DATA);
        }
    }

    @Transactional
    public void deleteCharacterExpression(Long id) {
        log.info("Deleting character expression with id: {}", id);

        CharacterExpression expression = characterExpressionRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        try {
            characterExpressionRepository.delete(expression);
            log.info("Character expression deleted successfully with id: {}", id);
        } catch (Exception e) {
            log.error("Error deleting character expression: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_DELETE_DATA);
        }
    }

    private CharacterExpressionDto toDto(CharacterExpression expression) {
        return CharacterExpressionDto.builder()
                .id(expression.getId())
                .expression(expression.getExpression())
                .category(expression.getCategory())
                //.stickerCharacterId(expression.getStickerCharacter() != null ? expression.getStickerCharacter().getId() : null)
                //.stickerCharacterName(expression.getStickerCharacter() != null ? expression.getStickerCharacter().getName() : null)
                .build();
    }
}

