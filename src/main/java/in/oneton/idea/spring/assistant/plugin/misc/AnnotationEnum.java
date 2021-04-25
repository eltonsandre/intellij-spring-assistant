package in.oneton.idea.spring.assistant.plugin.misc;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum AnnotationEnum {

    VALUE("Value", "org.springframework.beans.factory.annotation.Value", true),
    CONFIGURATION_PROPERTIES("ConfigurationProperties", "org.springframework.boot.context.properties.ConfigurationProperties", false),

    FEIGN_CLIENT("FeignClient", "org.springframework.cloud.openfeign.FeignClient", true),
    KAFKA_LISTENER("KafkaListener", "org.springframework.kafka.annotation.KafkaListener", true),
    ROCKETMQ_MESSAGE_LISTENER("RocketMQMessageListener", "org.apache.rocketmq.spring.annotation.RocketMQMessageListener", true),
    UNKNOW("AnnotationUnknow", "annotation.unknow", false);

    private final String name;
    private final String qualifiedName;
    private final boolean hasPlaceholder;

    private static final Map<String, AnnotationEnum> VALUES = Arrays.stream(AnnotationEnum.values())
            .collect(Collectors.toMap(AnnotationEnum::getQualifiedName, e -> e));

    public static AnnotationEnum fromQualifiedName(@NonNull final String qualifiedName) {
        return VALUES.getOrDefault(qualifiedName, UNKNOW);
    }

    public static boolean contains(final String qualifiedName) {
        return VALUES.containsKey(qualifiedName);
    }

    public static boolean notContains(final String qualifiedName) {
        return !contains(qualifiedName);
    }

}