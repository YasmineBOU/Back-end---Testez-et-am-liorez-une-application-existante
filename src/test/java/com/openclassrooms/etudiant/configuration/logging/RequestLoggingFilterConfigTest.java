package com.openclassrooms.etudiant.configuration.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("RequestLoggingFilterConfigTest")
@DisplayName("Tests for RequestLoggingFilterConfig")
class RequestLoggingFilterConfigTest {

    @Test
    @DisplayName("Given RequestLoggingFilterConfig, when commonsRequestLoggingFilter is called, then filter is configured as expected.")
    void test_commonsRequestLoggingFilter_returnsConfiguredFilter() {
        // GIVEN
        RequestLoggingFilterConfig config = new RequestLoggingFilterConfig();

        // WHEN
        CommonsRequestLoggingFilter filter = config.commonsRequestLoggingFilter();

        // THEN
        assertTrue((Boolean) ReflectionTestUtils.getField(filter, "includeQueryString"));
        assertTrue((Boolean) ReflectionTestUtils.getField(filter, "includePayload"));
        assertEquals(10000, ReflectionTestUtils.getField(filter, "maxPayloadLength"));
        assertEquals("REQUEST DATA: ", ReflectionTestUtils.getField(filter, "afterMessagePrefix"));
    }
}
