package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.Trie;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.any;
import static java.util.Arrays.stream;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataHint {
    private static final Pattern KEY_REGEX_PATTERN_FOR_MAP = Pattern.compile("\\.keys$");
    private static final Pattern VALUE_REGEX_PATTERN_FOR_MAP = Pattern.compile("\\.values$");

    @Setter
    @Getter
    private String name;
    @Setter
    @Nullable
    private SpringConfigurationMetadataHintValue[] values;
    @Setter
    @Nullable
    private SpringConfigurationMetadataValueProvider[] providers;

    @Nullable
    private Map<String, SpringConfigurationMetadataHintValue> valueLookup;
    @Nullable
    private Trie<String, SpringConfigurationMetadataHintValue> valueTrie;

    /**
     * If the property that corresponds with this hint represents a map, Hint's key would be end with `.keys`/`.values`
     *
     * @return property name that corresponds to this hint
     */
    public String getExpectedPropertyName() {
        return VALUE_REGEX_PATTERN_FOR_MAP
                .matcher(KEY_REGEX_PATTERN_FOR_MAP.matcher(name).replaceAll("")).replaceAll("");
    }

    public boolean representsKeyOfMap() {
        return KEY_REGEX_PATTERN_FOR_MAP.matcher(name).find();
    }

    public boolean representsValueOfMap() {
        return VALUE_REGEX_PATTERN_FOR_MAP.matcher(name).find();
    }

    public boolean hasPredefinedValues() {
        return values != null && values.length != 0;
    }

    @Nullable
    public SpringConfigurationMetadataHintValue findHintValueWithName(String pathSegment) {
        SpringConfigurationMetadataHintValue value = null;
        if (valueLookup != null) {
            value = valueLookup.get(sanitise(pathSegment));
        }

        if (value == null) {
            if (providers != null && stream(providers).anyMatch(provider -> provider.getType() == any)) {
                value =
                        SpringConfigurationMetadataHintValue.builder().nameAsObjOrArray(pathSegment).build();
            }
        }
        return value;
    }

    public Collection<SpringConfigurationMetadataHintValue> findHintValuesWithPrefix(
            String querySegmentPrefix) {
        if (valueTrie != null) {
            return valueTrie.prefixMap(sanitise(querySegmentPrefix)).values();
        }
        return null;
    }

}
