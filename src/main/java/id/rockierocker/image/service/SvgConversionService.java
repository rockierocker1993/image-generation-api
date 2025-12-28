package id.rockierocker.image.service;

import id.rockierocker.image.constant.*;
import id.rockierocker.image.conversion.PbmConversion;
import id.rockierocker.image.dto.svgconversion.VtraceConversionDto;
import id.rockierocker.image.exception.BadRequestException;
import id.rockierocker.image.exception.InternalServerErrorException;
import id.rockierocker.image.model.Icon;
import id.rockierocker.image.repository.IconRepository;
import id.rockierocker.image.util.CommonUtil;
import id.rockierocker.image.util.TransparencyDetectorUtil;
import id.rockierocker.image.vectorize.Vectorizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
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
    private final RemoveBackgroundService removeBackgroundService;


    /* VTRACE SVG CONVERSION
    *  see the doc for more info: https://github.com/visioncortex/vtracer?tab=readme-ov-file
    * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<byte[]> convertToSvgVTrace(MultipartFile file, VtraceConversionDto vtraceConversionDto) {
        log.info("Starting SVG conversion using {}", vectorizerVtrace.getName());

        Map<String, Object> additionalCommandMap = buildAdditionalCommandMap(vtraceConversionDto);

        List<String> additionalCommand = new ArrayList<>();
        additionalCommandMap.forEach((k,v) -> {
            if(Objects.nonNull(v)){
                additionalCommand.add(k);
                additionalCommand.add(String.valueOf(v).trim());
            }
        });
        Map<String,Object> mapInitializeInput = mapInitializeInput(file);
        String originalFilename = (String) mapInitializeInput.get("originalFilename");
        String ext = (String) mapInitializeInput.get("ext");
        byte[] inputBytes = (byte[]) mapInitializeInput.get("inputBytes");
        File inputFile = (File) mapInitializeInput.get("inputFile");

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

        iconRepository.save(
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
        return buildResponseEntity(svgBytes);
    }


    /* INKSCAPE SVG CONVERSION
     *  see the doc for more info: https://wiki.inkscape.org/wiki/Using_the_Command_Line
     * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<byte[]> convertToSvgInkscape(MultipartFile file) {
        log.info("Starting SVG conversion using {}", vectorizerInkscape.getName());

        Map<String,Object> mapInitializeInput = mapInitializeInput(file);
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

    private Map<String,Object> mapInitializeInput(MultipartFile file) {
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
        File inputFile = outputDirectoryManagerService.createTempFile("upload-"+originalFilename+"-", "."+ext,
                inputBytes, new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE));
        log.info("Temporary input file created: {}", inputFile.getAbsolutePath());

        double transparencyRatio = TransparencyDetectorUtil.transparencyRatio(inputStream, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
        if(transparencyRatio < 0.4){
            InputStream inputStreamBgRemoved = removeBackgroundService.removeBackground(inputFile);
            if(Objects.nonNull(inputStreamBgRemoved)){
                inputBytes = CommonUtil.getBytes(inputStream, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
                inputFile = outputDirectoryManagerService.createTempFile("upload-removed-bg-"+originalFilename+"-", "."+ext,
                        inputBytes, new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE));
            }
        }

        mapInitializeInput.put("originalFilename", originalFilename);
        mapInitializeInput.put("ext", ext);
        mapInitializeInput.put("inputBytes", inputBytes);
        mapInitializeInput.put("inputFile", inputFile);
        return mapInitializeInput;
    }

    private Map<String,Object> buildAdditionalCommandMap(VtraceConversionDto vtraceCurveFittingConversionDto) {
        ColorMode colorMode = vtraceCurveFittingConversionDto.getColorMode();
        if (colorMode == null)
            colorMode = ColorMode.COLOR;

        Hierarchical hierarchical = vtraceCurveFittingConversionDto.getHierarchical();
        if (hierarchical == null)
            hierarchical = Hierarchical.STACKED;

        CurveFittingMode curveFittingMode = vtraceCurveFittingConversionDto.getCurveFittingMode();
        if (curveFittingMode == null)
            curveFittingMode = CurveFittingMode.SPLINE;

        Map<String, Object> additionalCommandMap = new HashMap<>();
        additionalCommandMap.put("--colormode", colorMode.getCommand());
        if(colorMode == ColorMode.COLOR)
            additionalCommandMap.put("--hierarchical", hierarchical.getCommand());

        additionalCommandMap.put("--filter_speckle", vtraceCurveFittingConversionDto.getFilterSpeckle());
        additionalCommandMap.put("--color_precision", vtraceCurveFittingConversionDto.getColorPrecision());
        additionalCommandMap.put("--gradient_step", vtraceCurveFittingConversionDto.getGradientStep());
        additionalCommandMap.put("--mode",curveFittingMode.getCommand());
        if(curveFittingMode == CurveFittingMode.SPLINE) {
            additionalCommandMap.put("--segment_length", vtraceCurveFittingConversionDto.getSegmentLength());
            additionalCommandMap.put("--splice_threshold", vtraceCurveFittingConversionDto.getSpliceThreshold());
            additionalCommandMap.put("--corner_threshold", vtraceCurveFittingConversionDto.getCornerThreshold());
        }
        return additionalCommandMap;
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
            File outputFile = outputDirectoryManagerService.createOutputFile("vectorized-"+originalFilename+".svg");
            vectorizer.vectorize(inputFile.toPath(), outputFile.toPath(), additionalCommand);
            byte[] outputBytes = Files.readAllBytes(outputFile.toPath());
            log.info("Vectorization completed: " + outputFile.getAbsolutePath());
            return outputBytes;
        } catch (Exception e){
            log.error("Error during vectorization: " + e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.VECTORIZE_FAILED);
        }

    }

}
