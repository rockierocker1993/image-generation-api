package id.rockierocker.image.vectorize;

import java.nio.file.Path;
import java.util.List;

public class InkscapeVectorizer extends AbstractVectorizer implements Vectorizer {

    private final String inkscapeCmd;

    public InkscapeVectorizer(String inkscapeCmd) {
        this.inkscapeCmd = inkscapeCmd;
    }

    @Override
    public String getName() {
        return "Inkscape";
    }

    @Override
    public void vectorize(Path input, Path output, List<String> additionalCommand)
            throws Exception {

        List<String> command = List.of(
                inkscapeCmd,
                input.toAbsolutePath().toString(),
                "--batch-process",
                //"--actions=TraceBitmap;ObjectToPath;ExportPlainSVG",
                "--actions=SelectAll;TraceBitmap;Delete;ExportPlainSVG",
                "--export-filename=" + output.toAbsolutePath()
        );

        ProcessBuilder pb = new ProcessBuilder(command);

        exec(pb, "Inkscape");
    }
}

