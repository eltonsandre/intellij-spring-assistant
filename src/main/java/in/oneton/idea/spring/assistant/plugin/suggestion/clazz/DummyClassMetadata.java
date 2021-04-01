package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.SuggestionDocumentationHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.safeGetValidType;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.toClassFqn;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.toClassNonQualifiedName;

/**
 * Is a placeholder for classes that are known leafs with no additional navigation support
 * For e.g, all numbers, character & string classes from jdk
 */
public class DummyClassMetadata extends ClassMetadata {

    private final SuggestionNodeType nodeType;

    DummyClassMetadata(@NotNull SuggestionNodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    protected void init(Module module) {
    }

    @Nullable
    @Override
    protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
        if (!isEmpty(pathSegment)) {
            switch (nodeType) {
                case BYTE:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Byte.parseByte(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case SHORT:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Short.parseShort(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case INT:
                    //noinspection EmptyCatchBlock
                    try {
                        Integer.parseInt(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case LONG:
                    //noinspection EmptyCatchBlock
                    try {
                        Long.parseLong(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case FLOAT:
                    //noinspection EmptyCatchBlock
                    try {
                        Float.parseFloat(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case DOUBLE:
                    //noinspection EmptyCatchBlock
                    try {
                        Double.parseDouble(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case CHAR:
                    if (pathSegment.length() == 1) {
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    }
                    break;
                case STRING:
                    return new DummyKeySuggestionDocumentationHelper(pathSegment);
            }
        }
        return null;
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix) {
        return null;
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix, @Nullable Set<String> siblingsToExclude) {
        return null;
    }

    @Nullable
    @Override
    protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
                                                               List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                               int pathSegmentStartIndex) {
        return null;
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
        return null;
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                       @Nullable Set<String> siblingsToExclude) {
        return null;
    }

    @Override
    protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
                                                                    List<SuggestionNode> matchesRootTillMe, String prefix,
                                                                    @Nullable Set<String> siblingsToExclude) {
        return null;
    }

    @Nullable
    @Override
    protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                                String originalValue) {
        return null;
    }

    @Override
    public boolean doCheckIsLeaf(Module module) {
        return true;
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType() {
        return nodeType;
    }

    @Nullable
    @Override
    public PsiType getPsiType(Module module) {
        switch (nodeType) {
            case BYTE:
                return PsiType.BYTE;
            case SHORT:
                return PsiType.SHORT;
            case INT:
                return PsiType.INT;
            case LONG:
                return PsiType.LONG;
            case FLOAT:
                return PsiType.FLOAT;
            case DOUBLE:
                return PsiType.DOUBLE;
            case CHAR:
                return PsiType.CHAR;
            case STRING:
                return safeGetValidType(module, JAVA_LANG_STRING);
            case UNKNOWN_CLASS:
            default:
                return null;
        }
    }


    private class DummyKeySuggestionDocumentationHelper implements SuggestionDocumentationHelper {

        private final String value;

        DummyKeySuggestionDocumentationHelper(String value) {
            this.value = value;
        }

        @Nullable
        @Override
        public String getOriginalName() {
            return value;
        }

        @NotNull
        @Override
        public Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                                List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
            Suggestion.SuggestionBuilder builder = Suggestion.builder()
                    .suggestionToDisplay(dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors))
                    .numOfAncestors(numOfAncestors).matchesTopFirst(matchesRootTillMe)
                    .icon(nodeType.getIcon()).fileType(fileType);

            PsiType psiType = getPsiType(module);
            if (psiType != null) {
                builder.shortType(toClassNonQualifiedName(psiType));
            }
            return builder.build();
        }

        @Override
        public boolean supportsDocumentation() {
            return true;
        }

        @NotNull
        @Override
        public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
            /*
             * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
             * <p>Long description</p>
             */
            StringBuilder builder =
                    new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

            PsiType psiType = getPsiType(module);
            if (psiType != null) {
                String classFqn = toClassFqn(psiType);
                StringBuilder linkBuilder = new StringBuilder();
                createHyperlink(linkBuilder, classFqn, classFqn, false);
                builder.append(" (").append(linkBuilder.toString()).append(")");
            }

            return builder.toString();
        }

        @NotNull
        @Override
        public SuggestionNodeType getSuggestionNodeType(Module module) {
            return nodeType;
        }
    }

}
