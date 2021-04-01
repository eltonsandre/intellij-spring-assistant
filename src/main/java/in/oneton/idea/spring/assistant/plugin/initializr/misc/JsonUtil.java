package in.oneton.idea.spring.assistant.plugin.initializr.misc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ydq
 */
@UtilityClass
public class JsonUtil {
    @Getter
    private static final ObjectMapper OBJECT_MAPPER = commonConfig(new ObjectMapper());

    private static ObjectMapper commonConfig(ObjectMapper objectMapper) {
        return objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(), true);
    }


    public static <T> T fromJson(String json, Class<T> classOfT) throws IOException {
        return OBJECT_MAPPER.readValue(json, classOfT);
    }

    public static <T> T fromJson(BufferedReader reader, Class<T> classOfT) throws IOException {
        return OBJECT_MAPPER.readValue(reader, classOfT);
    }

    public static <T> T fromJson(InputStream inputStream, Class<T> classOfT) throws IOException {
        return OBJECT_MAPPER.readValue(inputStream, classOfT);
    }
}
