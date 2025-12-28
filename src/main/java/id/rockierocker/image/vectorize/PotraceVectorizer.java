//package id.rockierocker.image.vectorize;
//
//import java.nio.file.Path;
//import java.util.concurrent.TimeUnit;
//
//public class PotraceVectorizer implements Vectorizer {
//
//    private final String potraceCmd;
//
//    public PotraceVectorizer(String potraceCmd) {
//        this.potraceCmd = potraceCmd;
//    }
//
//    @Override
//    public String getName() {
//        return "Potrace";
//    }
//
//    @Override
//    public void vectorize(Path input, Path output, VectorizeOption opt)
//            throws Exception {
//
//        ProcessBuilder pb = new ProcessBuilder(
//                potraceCmd,
//                input.toString(),
//                "-s",
//                "-o", output.toString(),
//                "--turdsize", String.valueOf(opt.getTurdSize()),
//                "--alphamax", String.valueOf(opt.getAlphaMax()),
//                "--opttolerance", String.valueOf(opt.getOptTolerance())
//        );
//
//        exec(pb, "Potrace", 30); // 30 seconds timeout
//    }
//
//    private void exec(ProcessBuilder pb, String name, int timeoutSeconds) throws Exception {
//        pb.redirectErrorStream(true);
//        Process p = pb.start();
//
//        // Read output in separate thread to prevent buffer overflow
//        StringBuilder output = new StringBuilder();
//        Thread outputReader = new Thread(() -> {
//            try {
//                java.io.BufferedReader reader = new java.io.BufferedReader(
//                        new java.io.InputStreamReader(p.getInputStream())
//                );
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    output.append(line).append("\n");
//                }
//            } catch (Exception e) {
//                // Ignore
//            }
//        });
//        outputReader.setDaemon(true);
//        outputReader.start();
//
//        // Wait with timeout
//        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
//
//        if (!finished) {
//            p.destroyForcibly();
//            throw new RuntimeException(name + " timed out after " + timeoutSeconds + " seconds");
//        }
//
//        int exitCode = p.exitValue();
//        if (exitCode != 0) {
//            throw new RuntimeException(name + " failed with exit code " + exitCode +
//                    ". Output: " + output.toString());
//        }
//    }
//}
//
