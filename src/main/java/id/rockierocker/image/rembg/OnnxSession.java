package id.rockierocker.image.rembg;

import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OnnxSession {
    private static Map<String, Object> sessionLoaded = new HashMap<>();
    public static OrtSession getSession(String modelPath) throws Exception {
        log.info("Load ONNX model from path: {}", modelPath);
        OrtSession ortSession = (OrtSession) sessionLoaded.get(modelPath);
        if(ortSession == null){
            log.info("ONNX model not loaded yet. Loading...");
            ortSession = ai.onnxruntime.OrtEnvironment.getEnvironment().createSession(modelPath);
            sessionLoaded.put(modelPath, ortSession);
        } else {
            log.info("ONNX model already loaded. Reusing existing session.");
        }
        return ortSession;
    }
}
