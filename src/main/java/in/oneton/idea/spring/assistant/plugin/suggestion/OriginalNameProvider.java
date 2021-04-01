package in.oneton.idea.spring.assistant.plugin.suggestion;

import javax.annotation.Nullable;

public interface OriginalNameProvider {
    /**
     * @return original name without any sanitising, null if the suggestion node does not have any corresponding name
     */
    @Nullable
    String getOriginalName();
}
