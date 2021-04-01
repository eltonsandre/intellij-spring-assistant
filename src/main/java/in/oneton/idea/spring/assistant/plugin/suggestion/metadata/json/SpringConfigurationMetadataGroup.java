package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.codeInsight.documentation.DocumentationManager;
import in.oneton.idea.spring.assistant.plugin.misc.GenericUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.methodForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.removeGenerics;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.shortenedType;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.updateClassNameAsJavadocHtml;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0/reference/htmlsingle/#configuration-metadata-group-attributes
 */
@Data
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataGroup {

    private String name;
    @Nullable
    @JsonProperty("type")
    private String className;
    @Nullable
    private String description;
    @Nullable
    private String sourceType;
    @Nullable
    private String sourceMethod;
    @NotNull
    private SuggestionNodeType nodeType = SuggestionNodeType.UNDEFINED;

    public String getDocumentation(String nodeNavigationPathDotDelimited) {
        // Format for the documentation is as follows
        /*
         * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
         * <p>Long description</p>
         * or of this type
         * <p><b>Type</b> {@link com.acme.Array}[]</p>
         * <p><b>Declared at</b>{@link com.acme.GenericRemovedClass#method}></p> <-- only for groups with method info
         */
        StringBuilder builder =
                new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

        if (className != null) {
            builder.append(" (");
            updateClassNameAsJavadocHtml(builder, className);
            builder.append(")");
        }

        if (description != null) {
            builder.append("<p>").append(description).append("</p>");
        }

        if (sourceType != null) {
            String sourceTypeInJavadocFormat = removeGenerics(sourceType);

            if (sourceMethod != null) {
                sourceTypeInJavadocFormat += ("." + sourceMethod);
            }

            // lets show declaration point only if does not match the type
            if (!sourceTypeInJavadocFormat.equals(removeGenerics(className))) {
                StringBuilder buffer = new StringBuilder();
                DocumentationManager
                        .createHyperlink(buffer, methodForDocumentationNavigation(sourceTypeInJavadocFormat),
                                sourceTypeInJavadocFormat, false);
                sourceTypeInJavadocFormat = buffer.toString();
                builder.append("<p>Declared at ").append(sourceTypeInJavadocFormat).append("</p>");
            }
        }

        return builder.toString();
    }

    public Suggestion newSuggestion(FileType fileType, List<SuggestionNode> matchesRootTillMe,
                                    int numOfAncestors) {
        return Suggestion.builder().suggestionToDisplay(
                GenericUtil.dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors))
                .description(description).shortType(shortenedType(className)).numOfAncestors(numOfAncestors)
                .matchesTopFirst(matchesRootTillMe).icon(nodeType.getIcon()).fileType(fileType).build();
    }

}
