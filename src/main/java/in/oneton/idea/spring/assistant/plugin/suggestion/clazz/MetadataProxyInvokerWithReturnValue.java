package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

public interface MetadataProxyInvokerWithReturnValue<T> {
    T invoke(MetadataProxy delegate);
}
