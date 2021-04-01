package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import lombok.Data;

import javax.annotation.Nullable;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-property-attributes
 */
@Data
public class SpringConfigurationMetadataDeprecation {
    // In some manifests, this value is null for some strange reason
    @Nullable
    private SpringConfigurationMetadataDeprecationLevel level;
    @Nullable
    private String reason;
    @Nullable
    private String replacement;
}
