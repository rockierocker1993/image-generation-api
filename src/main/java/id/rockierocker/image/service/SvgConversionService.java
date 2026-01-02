package id.rockierocker.image.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.rockierocker.image.constant.*;
import id.rockierocker.image.dto.svgconversion.VtraceConversionDto;
import id.rockierocker.image.exception.BadRequestException;
import id.rockierocker.image.exception.InternalServerErrorException;
import id.rockierocker.image.model.Icon;
import id.rockierocker.image.model.VtraceConfig;
import id.rockierocker.image.preprocess.Preprocess;
import id.rockierocker.image.preprocess.PreprocessEnum;
import id.rockierocker.image.preprocess.model.PreprocessConfig;
import id.rockierocker.image.rembg.Rembg;
import id.rockierocker.image.repository.IconRepository;
import id.rockierocker.image.repository.PreprocessConfigRepository;
import id.rockierocker.image.repository.VtraceConfigRepository;
import id.rockierocker.image.util.CommonUtil;
import id.rockierocker.image.util.ImageUtil;
import id.rockierocker.image.vectorize.Vectorizer;
import id.rockierocker.image.vectorize.constant.VTracerColorMode;
import id.rockierocker.image.vectorize.constant.VTracerCurveFittingMode;
import id.rockierocker.image.vectorize.constant.VTracerHierarchical;
import id.rockierocker.image.vectorize.constant.VectorizeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class SvgConversionService {

    private final OutputDirectoryManagerService outputDirectoryManagerService;
    @Value("${image.allowed.extensions:png,jpg,jpeg}")
    private List<String> allowedExtensions = List.of("png", "jpg", "jpeg");

    private final Vectorizer vectorizerVtrace;
    private final Vectorizer vectorizerInkscape;

    private final IconRepository iconRepository;
    private final PreprocessConfigRepository preprocessConfigRepository;
    private final VtraceConfigRepository vtraceConfigRepository;
    private final ObjectMapper objectMapper;
    private final Rembg rembg;

    // Small immutable holder for input initialization results (replaces passing loose Map)
    private static record InputInfo(String originalFilename, String ext, byte[] inputBytes, File inputFile) {}

    /* VTRACE SVG CONVERSION
     *  see the doc for more info: https://github.com/visioncortex/vtracer?tab=readme-ov-file
     * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<byte[]> convertToSvgVTrace(MultipartFile file, VtraceConversionDto vtraceConversionDto) {
        log.info("Starting SVG conversion using {}", vectorizerVtrace.getName());
        List<File> processedImages = new ArrayList<>();

        // Build command arguments in a clear, single step
        List<String> additionalCommand = buildAdditionalCommandList(vtraceConversionDto.getVtraceConfigCode());

        InputInfo init = createInputInfo(file);
        String originalFilename = init.originalFilename();
        String ext = init.ext();
        byte[] inputBytes = init.inputBytes();
        File inputFile = init.inputFile();
        // Save a copy of the original upload to processedImages (do it once)
        try {
            Path originalPath = writeTempFile("original-" + originalFilename + "-", "." + ext, inputBytes);
            addProcessedFile(processedImages, originalPath);
        } catch (Exception e) {
            log.warn("Failed to copy original input file to processed images", e);
        }

        if(!"png".equalsIgnoreCase(ext)) {
            // do convert to png first
            log.info("Converting input image to PNG format before VTrace vectorization.");
            try {
                BufferedImage bufferedImage = ImageUtil.toBufferedImage(inputFile, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
                byte[] pngBytes = ImageUtil.toBytesPng(bufferedImage);
                ext = "png";
                Path pngPath = writeTempFile("converted-png-" + originalFilename + "-", "."+ext, pngBytes);
                addProcessedFile(processedImages, pngPath);
                // Update inputFile and ext for further processing
                inputFile = pngPath.toFile();

                log.info("Input image converted to PNG successfully.");
            } catch (Exception e) {
                log.warn("Failed to convert input image to PNG", e);
            }

        }

        // Remove background only when image has no transparency
        removeBackgroundIfNeeded(inputFile, originalFilename, ext, processedImages);

        // Preprocess image if requested
        inputFile = preprocess(vtraceConversionDto.getPreprocessStepCode(), inputFile, originalFilename, ext, processedImages);

        // Vectorize
        byte[] svgBytes = doVectorization(vectorizerVtrace, inputFile, originalFilename, additionalCommand);

        // Persist original image record
        Icon originalImage = iconRepository.save(
                Icon.builder()
                        .name(originalFilename)
                        .size((long) inputBytes.length)
                        .description("Original Image before vectorization")
                        .format(ext)
                        .filePath(inputFile.getPath())
                        .vectorizeType(VectorizeType.VTRACE.name())
                        .build()
        );

        // Persist generated SVG record
        Icon icon = iconRepository.save(
                Icon.builder()
                        .originalImage(originalImage)
                        .name(originalFilename)
                        .size((long) svgBytes.length)
                        .description("Converted SVG icon")
                        .format("svg")
                        .data(svgBytes)
                        .vectorizeType(VectorizeType.VTRACE.name())
                        .build()
        );

        // Save processed images to output directory asynchronously
        saveImageFile(processedImages, icon.getId());

        return buildResponseEntity(svgBytes);
    }

    /* INKSCAPE SVG CONVERSION
     *  see the doc for more info: https://wiki.inkscape.org/wiki/Using_the_Command_Line
     * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<byte[]> convertToSvgInkscape(MultipartFile file) {
        log.info("Starting SVG conversion using {}", vectorizerInkscape.getName());

        InputInfo init = createInputInfo(file);
        String originalFilename = init.originalFilename();
        String ext = init.ext();
        byte[] inputBytes = init.inputBytes();
        File inputFile = init.inputFile();

        byte[] svgBytes = doVectorization(vectorizerInkscape, inputFile, originalFilename, new ArrayList<>());

        Icon originalImage = iconRepository.save(
                Icon.builder()
                        .name(originalFilename)
                        .size((long) inputBytes.length)
                        .description("Original Image before vectorization")
                        .format(ext)
                        .filePath(inputFile.getPath())
                        .vectorizeType(VectorizeType.INKSCAPE.name())
                        .build()
        );

        iconRepository.save(
                Icon.builder()
                        .originalImage(originalImage)
                        .name(originalFilename)
                        .size((long) svgBytes.length)
                        .description("Converted SVG icon")
                        .format("svg")
                        .data(svgBytes)
                        .vectorizeType(VectorizeType.INKSCAPE.name())
                        .build()
        );
        return buildResponseEntity(svgBytes);
    }

    // Create InputInfo from uploaded MultipartFile (was mapInitializeInput)
    private InputInfo createInputInfo(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = CommonUtil.getExtensionLower(originalFilename);
        if (!ext.isEmpty() && !allowedExtensions.contains(ext)) {
            log.info("Unsupported file extension for SVG conversion: {}", ext);
            throw new BadRequestException(ResponseCode.EXTENSION_NOT_SUPPORTED);
        }

        InputStream inputStream = CommonUtil.getInputStream(file, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
        log.info("Creating temporary input file for SVG conversion.");
        byte[] inputBytes = CommonUtil.getBytes(inputStream, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
        File inputFile = outputDirectoryManagerService.createTempFile("upload-" + originalFilename + "-", "." + ext,
                inputBytes, new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE));
        log.info("Temporary input file created: {}", inputFile.getAbsolutePath());
        return new InputInfo(originalFilename, ext, inputBytes, inputFile);
    }

    // Extracted helper for background removal to keep main flow linear and readable
    private void removeBackgroundIfNeeded(File inputFile, String originalFilename, String ext, List<File> processedImages) {
        try {
            BufferedImage buffered = ImageUtil.toBufferedImage(inputFile, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
            if (ImageUtil.hasTransparency(buffered)) {
                log.info("image has transparency; skipping background removal.");
                return;
            }

            log.info("image has no transparency. Removing background from image before VTrace vectorization.");
            try {
                byte[] inputByteBgRemoved = rembg.removeBackground(CommonUtil.toInputStream(inputFile, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE)));
                Path rembgPath = writeTempFile("rembg-" + originalFilename + "-", "." + ext, inputByteBgRemoved);
                addProcessedFile(processedImages, rembgPath);
                Files.write(inputFile.toPath(), inputByteBgRemoved);
                log.info("Background removed successfully.");
            } catch (Exception e) {
                log.warn("Failed to remove background", e);
            }
        } catch (Exception e) {
            log.warn("Failed to read input file while checking transparency", e);
        }
    }

    // Helper: convert additional command map to flat list of args
    private List<String> buildAdditionalCommandList(String vtraceConfigCode) {
        Map<String, Object> additionalCommandMap = buildAdditionalCommandMap(vtraceConfigCode);
        List<String> additionalCommand = new ArrayList<>();
        additionalCommandMap.forEach((k, v) -> {
            if (Objects.nonNull(v)) {
                additionalCommand.add(k);
                additionalCommand.add(String.valueOf(v).trim());
            }
        });
        return additionalCommand;
    }

    private Map<String, Object> buildAdditionalCommandMap(String vtraceConfigCode) {
        log.info("Building additional command map for VTrace vectorization. by config code: {}", vtraceConfigCode);
        VtraceConfig vtraceConfig = vtraceConfigRepository.findFirstByConfigCode(vtraceConfigCode)
                .orElseThrow(() -> new BadRequestException(ResponseCode.VTRACE_CONFIG_NOT_FOUND));

        VTracerColorMode colorMode = VTracerColorMode.fromString(vtraceConfig.getColorMode());
        if (colorMode == null)
            colorMode = VTracerColorMode.COLOR;

        VTracerHierarchical hierarchical = VTracerHierarchical.fromString(vtraceConfig.getHierarchical());
        if (hierarchical == null)
            hierarchical = VTracerHierarchical.STACKED;

        VTracerCurveFittingMode curveFittingMode = VTracerCurveFittingMode.fromString(vtraceConfig.getCurveFittingMode());
        if (curveFittingMode == null)
            curveFittingMode = VTracerCurveFittingMode.SPLINE;

        Map<String, Object> additionalCommandMap = new HashMap<>();
        additionalCommandMap.put("--colormode", colorMode.getCommand());
        if (colorMode == VTracerColorMode.COLOR)
            additionalCommandMap.put("--hierarchical", hierarchical.getCommand());

        additionalCommandMap.put("--filter_speckle", vtraceConfig.getFilterSpeckle());
        additionalCommandMap.put("--color_precision", vtraceConfig.getColorPrecision());
        additionalCommandMap.put("--gradient_step", vtraceConfig.getGradientStep());
        additionalCommandMap.put("--mode", curveFittingMode.getCommand());
        if (curveFittingMode == VTracerCurveFittingMode.SPLINE) {
            additionalCommandMap.put("--segment_length", vtraceConfig.getSegmentLength());
            additionalCommandMap.put("--splice_threshold", vtraceConfig.getSpliceThreshold());
            additionalCommandMap.put("--corner_threshold", vtraceConfig.getCornerThreshold());
        }
        return additionalCommandMap;
    }

    // Helper: create temp file and write bytes, returning the Path
    private Path writeTempFile(String prefix, String suffix, byte[] data) throws IOException {
        Path path = outputDirectoryManagerService.createTempFile(prefix, suffix,
                new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE)).toPath();
        Files.write(path, data);
        return path;
    }

    private File preprocess(String preprocessConfigCode, File inputFile, String originalFilename, String ext, List<File> processedImages) {
        var preprocessConfig = preprocessConfigRepository.findFirstByConfigCode(preprocessConfigCode).orElse(null);
        if (Objects.nonNull(preprocessConfig)) {
            log.info("Preprocessing input file before VTrace vectorization.");
            try {
                List<String> preprocessSteps = preprocessConfig.getSteps();
                PreprocessConfig config = objectMapper.convertValue(preprocessConfig, PreprocessConfig.class);
                BufferedImage bufferedImage = ImageUtil.toBufferedImage(inputFile, new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD_TO_BUFFERED_IMAGE));
                for (String step : preprocessSteps) {
                    PreprocessEnum preprocessEnum = PreprocessEnum.fromString(step);
                    BufferedImage newBufferedImage = Preprocess.process(preprocessEnum, bufferedImage, config, new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD));
                    if (newBufferedImage == null) {
                        log.warn("Preprocess step '{}' returned null, stopping further preprocessing.", step);
                        break;
                    }
                    byte[] imageBytes = ImageUtil.toBytes(newBufferedImage);
                    Files.write(inputFile.toPath(), imageBytes);
                     Path preprocessPath = outputDirectoryManagerService.createTempFile("preprocess-" + step + "-" + originalFilename + "-", "." + ext,
                             new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE)).toPath();
                     Files.write(preprocessPath, imageBytes);
                     processedImages.add(preprocessPath.toFile());
                     bufferedImage = newBufferedImage;
                 }
             } catch (Exception e) {
                 log.error("Error during preprocessing: " + e.getMessage(), e);
             }
        }
        return inputFile;
    }

    private void addProcessedFile(List<File> processedImages, Path path) {
        if (path != null) processedImages.add(path.toFile());
    }

    private ResponseEntity<byte[]> buildResponseEntity(byte[] svgBytes) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"result.svg\""
                )
                .contentLength(svgBytes.length)
                .body(svgBytes);
    }

    private byte[] doVectorization(Vectorizer vectorizer, File inputFile, String originalFilename, List<String> additionalCommand) {
        log.info("Performing vectorization using " + vectorizer.getName());
        try {
            File outputFile = outputDirectoryManagerService.createOutputFile("vectorized-" + originalFilename + ".svg");
            vectorizer.vectorize(inputFile.toPath(), outputFile.toPath(), additionalCommand);
            byte[] outputBytes = Files.readAllBytes(outputFile.toPath());
            log.info("Vectorization completed: " + outputFile.getAbsolutePath());
            return outputBytes;
        } catch (Exception e) {
            log.error("Error during vectorization: " + e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.VECTORIZE_FAILED);
        }
    }

    @Async
    private void saveImageFile(List<File> files, Long id) {
        File dir = outputDirectoryManagerService.createOutputFile(String.valueOf(id));
        if (!dir.exists()) {
            boolean ok = dir.mkdir();
            if (!ok) log.warn("Could not create output directory: {}", dir.getAbsolutePath());
        }

        for (File file : files) {
            try {
                Files.copy(file.toPath(), Path.of(dir.getAbsolutePath(), file.getName()));
            } catch (Exception e) {
                log.warn("Failed to copy temporary file: {}", file.getAbsolutePath(), e);
            }
        }
    }

}
