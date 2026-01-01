package id.rockierocker.image.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.rockierocker.image.constant.*;
import id.rockierocker.image.conversion.PbmConversion;
import id.rockierocker.image.dto.svgconversion.VtraceConversionDto;
import id.rockierocker.image.exception.BadRequestException;
import id.rockierocker.image.exception.InternalServerErrorException;
import id.rockierocker.image.model.Icon;
import id.rockierocker.image.model.VtraceConfig;
import id.rockierocker.image.preprocess.PreprocessStep;
import id.rockierocker.image.preprocess.model.PreprocessConfig;
import id.rockierocker.image.rembg.Rembg;
import id.rockierocker.image.rembg.constant.OnnxInputSize;
import id.rockierocker.image.repository.IconRepository;
import id.rockierocker.image.repository.PreprocessConfigRepository;
import id.rockierocker.image.repository.VtraceConfigRepository;
import id.rockierocker.image.util.CommonUtil;
import id.rockierocker.image.util.ImageUtil;
import id.rockierocker.image.util.TransparencyDetectorUtil;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class SvgConversionService {

    private final PbmConversion pbmConversion;
    private final OutputDirectoryManagerService outputDirectoryManagerService;
    @Value("${image.allowed.extensions:png,jpg,jpeg}")
    private List<String> allowedExtensions;

    private final Vectorizer vectorizerVtrace;
    private final Vectorizer vectorizerInkscape;

    private final IconRepository iconRepository;
    private final PreprocessConfigRepository preprocessConfigRepository;
    private final VtraceConfigRepository vtraceConfigRepository;
    private final ObjectMapper objectMapper;
    private final Rembg rembg;

    /* VTRACE SVG CONVERSION
     *  see the doc for more info: https://github.com/visioncortex/vtracer?tab=readme-ov-file
     * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<byte[]> convertToSvgVTrace(MultipartFile file, VtraceConversionDto vtraceConversionDto) {
        log.info("Starting SVG conversion using {}", vectorizerVtrace.getName());
        List<File> processedImages = new ArrayList<>();
        Map<String, Object> additionalCommandMap = buildAdditionalCommandMap(vtraceConversionDto.getVtraceConfigCode());

        List<String> additionalCommand = new ArrayList<>();
        additionalCommandMap.forEach((k, v) -> {
            if (Objects.nonNull(v)) {
                additionalCommand.add(k);
                additionalCommand.add(String.valueOf(v).trim());
            }
        });
        Map<String, Object> mapInitializeInput = mapInitializeInput(file);
        String originalFilename = (String) mapInitializeInput.get("originalFilename");
        String ext = (String) mapInitializeInput.get("ext");
        byte[] inputBytes = (byte[]) mapInitializeInput.get("inputBytes");
        File inputFile = (File) mapInitializeInput.get("inputFile");

        try {
            Path originalPath = outputDirectoryManagerService.createTempFile("original-" + originalFilename + "-", "." + ext,
                    new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE)).toPath();
            Files.write(originalPath, inputBytes);
            processedImages.add(Files.write(originalPath, inputBytes).toFile());
        } catch (Exception e) {
            log.warn("Failed to copy original input file to processed images", e);
        }

        if (!ImageUtil.hasTransparency(ImageUtil.toBufferedImage(inputFile, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE)))) {
            log.info("image has no transparency.");
            log.info("Removing background from image before VTrace vectorization.");
            try {
                byte[] inputByteBgRemoved = rembg.removeBackground(CommonUtil.toInputStream(inputFile, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE)));
                Path rembgPath = outputDirectoryManagerService.createTempFile("rembg-" + originalFilename + "-", "." + ext,
                        new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE)).toPath();
                Files.write(rembgPath, inputByteBgRemoved);
                processedImages.add(rembgPath.toFile());
                Files.write(inputFile.toPath(), inputByteBgRemoved);
                log.info("Background removed successfully.");
            } catch (Exception e) {
                log.warn("Failed to remove background", e);
            }
        }
        inputFile = preprocess(vtraceConversionDto.getPreprocessStepCode(), inputFile, originalFilename, ext, processedImages);

        byte[] svgBytes = doVectorization(vectorizerVtrace, inputFile, originalFilename, additionalCommand);

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

        saveImageFile(processedImages, icon.getId());

        return buildResponseEntity(svgBytes);
    }


    /* INKSCAPE SVG CONVERSION
     *  see the doc for more info: https://wiki.inkscape.org/wiki/Using_the_Command_Line
     * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<byte[]> convertToSvgInkscape(MultipartFile file) {
        log.info("Starting SVG conversion using {}", vectorizerInkscape.getName());

        Map<String, Object> mapInitializeInput = mapInitializeInput(file);
        String originalFilename = (String) mapInitializeInput.get("originalFilename");
        String ext = (String) mapInitializeInput.get("ext");
        byte[] inputBytes = (byte[]) mapInitializeInput.get("inputBytes");
        File inputFile = (File) mapInitializeInput.get("inputFile");

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

    private Map<String, Object> mapInitializeInput(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        Map<String, Object> mapInitializeInput = new HashMap<>();
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
        mapInitializeInput.put("originalFilename", originalFilename);
        mapInitializeInput.put("ext", ext);
        mapInitializeInput.put("inputBytes", inputBytes);
        mapInitializeInput.put("inputFile", inputFile);
        return mapInitializeInput;
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

    private File preprocess(String preprocessConfigCode, File inputFile, String originalFilename, String ext, List<File> processedImages) {
        var preprocessConfig = preprocessConfigRepository.findFirstByConfigCode(preprocessConfigCode).orElse(null);
        if (Objects.nonNull(preprocessConfig)) {
            log.info("Preprocessing input file before VTrace vectorization.");
            try {
                List<String> preprocessSteps = preprocessConfig.getSteps();
                PreprocessConfig config = objectMapper.convertValue(preprocessConfig, PreprocessConfig.class);
                BufferedImage bufferedImage = ImageUtil.toBufferedImage(inputFile, new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD_TO_BUFFERED_IMAGE));
                for (String step : preprocessSteps) {
                    BufferedImage newBufferedImage = PreprocessStep.process(step, bufferedImage, config, new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD));
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
        if (!dir.exists())
            dir.mkdir();

        for (File file : files) {
            try {
                Files.copy(file.toPath(), Path.of(dir.getAbsolutePath(), file.getName()));
            } catch (Exception e) {
                log.warn("Failed to delete temporary file: {}", file.getAbsolutePath(), e);
            }
        }
    }

}
