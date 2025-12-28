package id.rockierocker.image.conversion;

import id.rockierocker.image.service.OutputDirectoryManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Component
public class SvgConversion<T> {

    private final OutputDirectoryManagerService outputDirectoryManager;

    @Value("${potrace.location:}")
    String potracePath;
    /**
     * Process PBM file to convert to SVG using potrace
     */
    public File process(File inputFile) throws Exception {
        if (inputFile == null || !inputFile.exists()) {
            log.info("PBM file does not exist: " + inputFile);
            throw new IllegalArgumentException("PBM file does not exist: " + inputFile);
        }
        if (!isPbmFile(inputFile)) {
            log.info("Input file is not a valid PBM file: " + inputFile.getName());
            throw new IllegalArgumentException("Input file is not a valid PBM file: " + inputFile.getName());
        }

        if(potracePath != null) {
            boolean validPotrace = testPotraceCommand(potracePath);
            if (!validPotrace) {
                potracePath = null; // Reset to trigger detection
                log.warn("potrace location is invalid: " + potracePath);
                log.warn("Attempting to detect potrace executable");
            }
        }
        if(Objects.isNull(potracePath))
            potracePath = detectPotracePath();

        if (potracePath == null)
            throw new IllegalStateException("Potrace executable not found. Please install potrace or set path manually.");

        log.info("Converting PBM to SVG: " + inputFile.getName());
        // Create output file in configured/temp directory
        String outputFileName = getOutputFileName(inputFile.getName());
        File outputFile = outputDirectoryManager.createOutputFile(outputFileName);

        // Build potrace command
        List<String> command = new ArrayList<>();
        command.add(potracePath);
        command.add("-s"); // SVG output
        command.add("-o");
        command.add(outputFile.getAbsolutePath());
        command.add(inputFile.getAbsolutePath());

        // Execute potrace
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Wait for completion
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Potrace failed with exit code " + exitCode +
                    "\nOutput: " + output.toString());
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("Potrace did not create output file");
        }

        log.info("SVG file created: " + outputFile.getAbsolutePath());

        if (outputDirectoryManager.isTemporary()) {
            log.info("File will be automatically cleaned up after configured time period");
        }

        return outputFile;
    }

    private boolean isPbmFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String firstLine = br.readLine();
            return "P1".equals(firstLine) || "P4".equals(firstLine);
        } catch (IOException e) {
            log.warn("Error reading file to check PBM format: " + e.getMessage());
            return false;
        }
    }

    /**
     * Process PBM file and return SVG as ByteArrayOutputStream
     */
    public ByteArrayOutputStream processToByteArrayOutputStream(File inputFile) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(Files.readAllBytes(process(inputFile).toPath()));
        return baos;
    }

    /**
     * Process PBM file and return SVG as ByteArrayOutputStream
     */
    public ByteArrayOutputStream processToByteArrayOutputStream(File inputFile, RuntimeException e) {
        try {
            return processToByteArrayOutputStream(inputFile);
        } catch (Exception ex) {
            log.error("Error during SVG conversion: " + ex.getMessage(), ex);
            throw e;
        }
    }

    /**
     * Detect potrace executable path based on OS
     */
    private String detectPotracePath() {
        String os = System.getProperty("os.name").toLowerCase();

        // Try common locations
        String[] possiblePaths;

        if (os.contains("win")) {
            // Windows paths
            possiblePaths = new String[] {
                    "./tools/potrace.exe",
                    "data\\potrace\\src\\potrace.exe",
                    "..\\..\\data\\potrace\\src\\potrace.exe",
                    "C:\\Program Files\\potrace\\potrace.exe",
                    System.getenv("POTRACE_HOME") != null ?
                            System.getenv("POTRACE_HOME") + "\\potrace.exe" : null
            };
        } else {
            // Linux/Mac paths
            possiblePaths = new String[] {
                    "potrace",
                    "/usr/bin/potrace",
                    "/usr/local/bin/potrace",
                    "data/potrace/src/potrace",
                    "../../data/potrace/src/potrace",
                    System.getenv("POTRACE_HOME") != null ?
                            System.getenv("POTRACE_HOME") + "/potrace" : null
            };
        }

        // Test each path
        for (String path : possiblePaths) {
            if (path == null) continue;

            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                log.info("Found potrace at: " + path);
                return path;
            }

            // Try executing it (might be in PATH)
            if (testPotraceCommand(path)) {
                log.info("Found potrace in PATH: " + path);
                return path;
            }
        }

        log.warn("Potrace executable not found. Install potrace or set POTRACE_HOME environment variable.");
        return null;
    }

    private boolean testPotraceCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0 || exitCode == 1; // Some versions return 1 for --version
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate output file name from input file name
     */
    private String getOutputFileName(String inputFileName) {
        // Remove extension and add .svg
        int lastDot = inputFileName.lastIndexOf('.');
        String baseName = (lastDot > 0) ? inputFileName.substring(0, lastDot) : inputFileName;
        return baseName + ".svg";
    }
}
