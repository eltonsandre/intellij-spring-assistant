package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.computeDocumentation;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.getReferredPsiType;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.toClassNonQualifiedName;
import static in.oneton.idea.spring.assistant.plugin.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static in.oneton.idea.spring.assistant.plugin.suggestion.handler.YamlValueInsertHandler.unescapeValue;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

/**
 * Documentation is never requested for a class, instead its requested for properties within the class.
 * This acts as a proxy for documentation & basic details about the property/getter method that contains actual documentation
 */
public class GenericClassMemberWrapper implements SuggestionNode, SuggestionDocumentationHelper {

    private final PsiMember member;

    @NotNull
    private final String originalName;
    @Nullable
    private final String documentation;
    @Nullable
    private final String shortType;
    private final boolean deprecated;

    private MetadataProxy proxy;

    public GenericClassMemberWrapper(@NotNull PsiMember member) {
        this.member = member;
        this.originalName = requireNonNull(member.getName());
        this.documentation = computeDocumentation(member);
        this.shortType = toClassNonQualifiedName(getReferredPsiType(member));
        this.deprecated = this.computeDeprecationStatus();
    }

    public MetadataProxy getMemberReferredClassMetadataProxy(Module module) {
        if (this.proxy == null) {
            this.proxy = newMetadataProxy(module, getReferredPsiType(this.member));
        }
        return this.proxy;
    }

    private <T> T doWithMemberReferredClassProxy(Module module, ProxyInvoker<T> invoker,
                                                 T defaultValue) {
        MetadataProxy delegate = this.getMemberReferredClassMetadataProxy(module);
        if (delegate != null) {
            return invoker.invoke(delegate);
        }
        return defaultValue;
    }

    @Nullable
    @Override
    public List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                          List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                          int pathSegmentStartIndex) {
        return this.doWithMemberReferredClassProxy(module, proxy -> proxy
                .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                        pathSegmentStartIndex), null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex) {
        return this.doWithMemberReferredClassProxy(module, proxy -> proxy
                .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                        querySegmentPrefixes, querySegmentPrefixStartIndex), null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex, @Nullable Set<String> siblingsToExclude) {
        return this.doWithMemberReferredClassProxy(module, proxy -> proxy
                .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                        querySegmentPrefixes, querySegmentPrefixStartIndex, siblingsToExclude), null);
    }

    @NotNull
    public String getOriginalName() {
        return this.originalName;
    }

    @Nullable
    @Override
    public String getNameForDocumentation(Module module) {
        return this.doWithMemberReferredClassProxy(module, proxy -> {
            if (proxy.targetRepresentsArray() || proxy.targetClassRepresentsIterable(module)) {
                return this.originalName + "[]";
            } else {
                return this.originalName;
            }
        }, this.originalName);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix) {
        return this.findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix, null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix,
                                                               @Nullable Set<String> siblingsToExclude) {
        return this.doWithMemberReferredClassProxy(module, proxy -> proxy
                .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix,
                        siblingsToExclude), null);
    }

    @Override
    public boolean isLeaf(Module module) {
        return this.doWithMemberReferredClassProxy(module, proxy -> proxy.isLeaf(module), true);
    }

    @Override
    public boolean isMetadataNonProperty() {
        return false;
    }

    private boolean computeDeprecationStatus() {
        if (this.member instanceof PsiField) {
            return stream(((PsiField) this.member).getType().getAnnotations()).anyMatch(annotation -> {
                String fqn = annotation.getQualifiedName();
                return fqn != null && fqn.equals("java.lang.Deprecated");
            });
        } else if (this.member instanceof PsiMethod) {
            return stream(requireNonNull(((PsiMethod) this.member).getReturnType()).getAnnotations())
                    .anyMatch(annotation -> {
                        String fqn = annotation.getQualifiedName();
                        return fqn != null && fqn.equals("java.lang.Deprecated");
                    });
        }
        throw new RuntimeException("Method supports targets of type PsiField & PsiMethod only");
    }

    @NotNull
    @Override
    public Suggestion buildSuggestionForKey(Module module, FileType fileType, List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
        final Icon icon = this.doWithMemberReferredClassProxy(module,
                proxy -> proxy.getSuggestionNodeType(module), SuggestionNodeType.UNKNOWN_CLASS).getIcon();

        Suggestion.SuggestionBuilder builder = Suggestion.builder()
                .suggestionToDisplay(this.originalName)
                .description(this.documentation)
                .shortType(this.shortType)
                .numOfAncestors(numOfAncestors)
                .matchesTopFirst(matchesRootTillMe)
                .icon(icon);

        if (this.deprecated) {
            builder.deprecationLevel(SpringConfigurationMetadataDeprecationLevel.warning);
        }

        return builder.fileType(fileType).build();
    }

    @Override
    public boolean supportsDocumentation() {
        return true;
    }

    @NotNull
    @Override
    public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
        return "<b>" + nodeNavigationPathDotDelimited + "</b>" + new JavaDocumentationProvider()
                .generateDoc(this.member, this.member);
    }

    @Override
    public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                           String originalValue) {
        return "<b>" + nodeNavigationPathDotDelimited + "</b> =  <b>" + unescapeValue(originalValue) + "</b>"
                + new JavaDocumentationProvider().generateDoc(this.member, this.member);
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
        return this.doWithMemberReferredClassProxy(module, proxy -> proxy.getSuggestionNodeType(module),
                SuggestionNodeType.UNKNOWN_CLASS);
    }

    private interface ProxyInvoker<T> {
        T invoke(MetadataProxy proxy);
    }

}
