package id.rockierocker.image.service;

import id.rockierocker.image.constant.ResponseCode;
import id.rockierocker.image.dto.promptai.PromptAIDto;
import id.rockierocker.image.dto.promptai.PromptAIRequest;
import id.rockierocker.image.exception.BadRequestException;
import id.rockierocker.image.exception.InternalServerErrorException;
import id.rockierocker.image.model.PromptAI;
import id.rockierocker.image.repository.PromptAIRepository;
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
public class PromptAIService {

    private final PromptAIRepository promptAIRepository;

    @Transactional(readOnly = true)
    public List<PromptAIDto> getAllPrompts() {
        log.info("Fetching all prompts");
        return promptAIRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PromptAIDto> getAllPrompts(Pageable pageable) {
        log.info("Fetching all prompts with pagination");
        return promptAIRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public PromptAIDto getPromptById(Long id) {
        log.info("Fetching prompt by id: {}", id);
        PromptAI prompt = promptAIRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));
        return toDto(prompt);
    }

    @Transactional(readOnly = true)
    public List<PromptAIDto> getPromptsByType(String type) {
        log.info("Fetching prompts by type: {}", type);
        return promptAIRepository.findByType(type).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromptAIDto> getPromptsByCategory(String category) {
        log.info("Fetching prompts by category: {}", category);
        return promptAIRepository.findByCategory(category).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromptAIDto> getPromptsByTypeAndCategory(String type, String category) {
        log.info("Fetching prompts by type: {} and category: {}", type, category);
        return promptAIRepository.findByTypeAndCategory(type, category).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PromptAIDto createPrompt(PromptAIRequest request) {
        log.info("Creating new prompt");

        PromptAI prompt = PromptAI.builder()
                .prompt(request.getPrompt())
                .category(request.getCategory())
                .type(request.getType())
                .build();

        try {
            PromptAI saved = promptAIRepository.save(prompt);
            log.info("Prompt created successfully with id: {}", saved.getId());
            return toDto(saved);
        } catch (Exception e) {
            log.error("Error creating prompt: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_SAVE_DATA);
        }
    }

    @Transactional
    public PromptAIDto updatePrompt(Long id, PromptAIRequest request) {
        log.info("Updating prompt with id: {}", id);

        PromptAI prompt = promptAIRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        prompt.setPrompt(request.getPrompt());
        prompt.setCategory(request.getCategory());
        prompt.setType(request.getType());

        try {
            PromptAI updated = promptAIRepository.save(prompt);
            log.info("Prompt updated successfully with id: {}", updated.getId());
            return toDto(updated);
        } catch (Exception e) {
            log.error("Error updating prompt: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_UPDATE_DATA);
        }
    }

    @Transactional
    public void deletePrompt(Long id) {
        log.info("Deleting prompt with id: {}", id);

        PromptAI prompt = promptAIRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ResponseCode.DATA_NOT_FOUND));

        try {
            promptAIRepository.delete(prompt);
            log.info("Prompt deleted successfully with id: {}", id);
        } catch (Exception e) {
            log.error("Error deleting prompt: {}", e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.FAILED_DELETE_DATA);
        }
    }

    private PromptAIDto toDto(PromptAI prompt) {
        return PromptAIDto.builder()
                .id(prompt.getId())
                .prompt(prompt.getPrompt())
                .category(prompt.getCategory())
                .type(prompt.getType())
                .build();
    }
}

