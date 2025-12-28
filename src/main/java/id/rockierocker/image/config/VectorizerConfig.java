package id.rockierocker.image.config;

import id.rockierocker.image.vectorize.InkscapeVectorizer;
import id.rockierocker.image.vectorize.VTracerVectorizer;
import id.rockierocker.image.vectorize.Vectorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorizerConfig {


    @Bean(name = "vectorizerVtrace")
    public Vectorizer vectorizerVtrace() {
        return new VTracerVectorizer("vtracer");
    }

    @Bean(name = "vectorizerInkscape")
    public Vectorizer vectorizerInkscape() {
        return new InkscapeVectorizer("inkscape");
    }

}
