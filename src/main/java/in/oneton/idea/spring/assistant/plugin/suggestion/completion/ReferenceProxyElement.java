package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.lang.Language;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author zhoumengjie02, darren
 */
@ToString(of = "nodeNavigationPathDotDelimited")
public class ReferenceProxyElement extends LightElement {

    @Getter
    private final DocumentationProvider target;

    @Getter
    private final boolean requestedForTargetValue;

    @Nullable
    @Getter
    private final String value;

    @Getter
    private final String nodeNavigationPathDotDelimited;

    ReferenceProxyElement(@NotNull final PsiManager manager, @NotNull final Language language,
                          String nodeNavigationPathDotDelimited, @NotNull final DocumentationProvider target,
                          @Nullable String value) {
        super(manager, language);
        this.nodeNavigationPathDotDelimited = nodeNavigationPathDotDelimited;
        this.target = target;
        this.requestedForTargetValue = false;
        this.value = value;
    }

    @Override
    public String getText() {
        return nodeNavigationPathDotDelimited;
    }
}