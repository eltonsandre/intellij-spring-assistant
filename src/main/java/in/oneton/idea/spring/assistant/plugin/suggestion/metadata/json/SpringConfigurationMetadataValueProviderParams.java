package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import lombok.Data;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#_value_providers
 */
@Data
public class SpringConfigurationMetadataValueProviderParams {
    private String target;
    private boolean concrete;
}
