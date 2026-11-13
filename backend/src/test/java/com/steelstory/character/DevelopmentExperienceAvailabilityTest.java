package com.steelstory.character;

import com.steelstory.character.internal.application.CharacterService;
import com.steelstory.character.internal.web.DevelopmentExperienceController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DevelopmentExperienceAvailabilityTest {
    @ParameterizedTest
    @CsvSource({"production,true,false", "production,false,false", "local,false,false", "local,true,true",
            "local,missing,false", "production,missing,false"})
    void requiresBothLocalProfileAndExplicitOptIn(String profile, String property, boolean available) {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(DevelopmentExperienceController.class)
                .withBean(CharacterService.class, () -> mock(CharacterService.class))
                .withPropertyValues("spring.profiles.active=" + profile);
        if (!property.equals("missing")) {
            runner = runner.withPropertyValues("steelstory.dev.xp.enabled=" + property);
        }

        runner.run(context -> assertThat(context.getBeansOfType(DevelopmentExperienceController.class))
                .hasSize(available ? 1 : 0));
    }
}
