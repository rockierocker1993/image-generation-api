package id.rockierocker.image.controller;

import id.rockierocker.image.dto.svgconversion.VtraceConversionDto;
import id.rockierocker.image.service.SvgConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RestController
@RequestMapping("/svg-conversion")
public class SvgConversionController {

    private final SvgConversionService svgConversionService;

    @PostMapping(path = "/vtrace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> vtrace(@RequestParam("file") MultipartFile file, @ModelAttribute VtraceConversionDto vtraceConversionDto)  {
        return svgConversionService.convertToSvgVTrace(file, vtraceConversionDto);
    }

    @PostMapping(path = "/vtrace-crop-mode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> vtraceCropMode(@RequestParam("file") MultipartFile file, @ModelAttribute VtraceConversionDto vtraceConversionDto)  {
        return svgConversionService.convertToSvgVTraceCropMode(file, vtraceConversionDto);
    }

}
