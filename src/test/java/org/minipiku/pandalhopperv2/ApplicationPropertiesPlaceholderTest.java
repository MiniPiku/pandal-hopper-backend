package org.minipiku.pandalhopperv2;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards against self-referential placeholders in application.properties.
 *
 * <p>Writing {@code app.foo=${app.foo:default}} looks like "use the override if
 * present, else this default", but the placeholder names the key it is defining.
 * Spring detects the cycle and throws, which surfaces far from the cause — as
 * {@code BeanCreationException: Error creating bean with name
 * 'OAuth2SuccessHandler'}, whichever bean happens to inject it first.
 *
 * <p>Resolves the real file, so a regression here fails the build rather than
 * the next application start.
 */
class ApplicationPropertiesPlaceholderTest {

    private StandardEnvironment environmentWithRealProperties() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
                new ResourcePropertySource("classpath:application.properties"));
        return env;
    }

    @Test
    void appPropertiesResolveWithoutCircularReference() throws Exception {
        StandardEnvironment env = environmentWithRealProperties();

        assertThat(env.resolveRequiredPlaceholders("${app.frontend.callback-url}"))
                .isNotBlank()
                .doesNotContain("${");

        assertThat(env.resolveRequiredPlaceholders("${app.cors.allowed-origins}"))
                .isNotBlank()
                .doesNotContain("${");
    }

    /** Pins the failure mode, so the guard above cannot silently stop guarding. */
    @Test
    void selfReferentialPlaceholderIsDetectedAsCircular() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "broken", java.util.Map.of("app.some-key", "${app.some-key:fallback}")));

        assertThatThrownBy(() -> env.resolveRequiredPlaceholders("${app.some-key}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Circular placeholder reference");
    }
}
