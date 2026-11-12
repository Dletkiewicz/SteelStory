package com.steelstory;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {
    @Test
    void verifiesBoundariesWithoutScaffoldingGameplayModules() {
        System.setProperty("spring.modulith.detection-strategy", "explicitly-annotated");
        try {
            var modules = ApplicationModules.of(SteelstoryApplication.class);
            modules.verify();
            assertThat(modules.stream()).isEmpty();
        } finally {
            System.clearProperty("spring.modulith.detection-strategy");
        }
    }
}
