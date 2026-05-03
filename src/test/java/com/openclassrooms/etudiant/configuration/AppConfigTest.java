package com.openclassrooms.etudiant.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("AppConfigTest")
@DisplayName("Tests for AppConfig")
class AppConfigTest {

    @Test
    @DisplayName("Given AppConfig, when propertySourcesPlaceholderConfigurer is called, then a non-null configurer is returned.")
    void test_propertySourcesPlaceholderConfigurer_returnsConfiguredBean() {
        // WHEN
        PropertySourcesPlaceholderConfigurer configurer = AppConfig.propertySourcesPlaceholderConfigurer();

        // THEN
        assertNotNull(configurer);
    }
}
