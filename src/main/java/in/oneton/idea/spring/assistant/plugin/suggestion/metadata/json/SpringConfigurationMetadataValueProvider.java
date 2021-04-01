package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#_value_providers
 */
@Data
public class SpringConfigurationMetadataValueProvider {
    /**
     * The name of the provider to use to offer additional content assistance for the element to which the hint refers.
     */
    @JsonProperty("name")
    private SpringConfigurationMetadataValueProviderType type;
    /**
     * Any additional parameter that the provider supports (check the documentation of the provider for more details).
     */
    private SpringConfigurationMetadataValueProviderParams parameters;
}
