package id.rockierocker.image.vectorize;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * VTracer vectorizer implementation.
 */
@Slf4j
public class VTracerVectorizer extends AbstractVectorizer implements Vectorizer {

    private final String vtracerCmd;

    public VTracerVectorizer(String vtracerCmd) {
        this.vtracerCmd = vtracerCmd;
    }

    @Override
    public String getName() {
        return "VTracer";
    }


    @Override
    public void vectorize(Path input, Path output, List<String> additionalCommand)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                vtracerCmd,
                "--input", input.toAbsolutePath().toString(),
                "--output", output.toAbsolutePath().toString()
        );
        if(additionalCommand!=null && !additionalCommand.isEmpty()){
            pb.command().addAll(additionalCommand);
        }
        exec(pb, "vtracer");
    }
}

