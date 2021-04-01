package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.openapi.module.Module;

public interface DocumentationProvider {
    /**
     * @return false if an intermediate node (neither group, nor property, nor class). true otherwise
     */
    boolean supportsDocumentation();

    /**
     * @param module                         module
     * @param nodeNavigationPathDotDelimited node path
     * @return Documentation for key under cursor. Includes available choices for values (if applicable). Null if documentation is not available
     */
    String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited);

    /**
     * @param module                         module
     * @param nodeNavigationPathDotDelimited node path
     * @param originalValue                  value selected/typed by user for the current node used as key
     * @return Documentation for selected value with current node as key
     */
    String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                    String originalValue);
}
