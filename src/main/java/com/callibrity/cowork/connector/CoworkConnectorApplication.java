package com.callibrity.cowork.connector;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(CoworkConnectorApplication.AppHints.class)
public class CoworkConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoworkConnectorApplication.class, args);
    }

    static class AppHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // json-sKema (used by mocapi for tool-input JSON Schema validation) loads its
            // draft meta-schemas from the classpath but does not ship native-image metadata.
            hints.resources().registerPattern("json-meta-schemas/*");
            hints.resources().registerPattern("json-meta-schemas/draft2020-12/*");
        }
    }
}
