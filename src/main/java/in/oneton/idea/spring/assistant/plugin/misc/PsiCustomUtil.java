package in.oneton.idea.spring.assistant.plugin.misc;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.daemon.impl.analysis.JavaGenericsUtil;
import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiCapturedWildcardType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.TimeoutUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.clazz.GenericClassMemberWrapper;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForFile;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static com.intellij.openapi.roots.ModuleRootManager.getInstance;
import static com.intellij.openapi.util.Key.create;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_ITERABLE;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING;
import static com.intellij.psi.CommonClassNames.JAVA_UTIL_MAP;
import static com.intellij.psi.JavaPsiFacade.getElementFactory;
import static com.intellij.psi.PsiModifier.PUBLIC;
import static com.intellij.psi.PsiModifier.STATIC;
import static com.intellij.psi.PsiType.NULL;
import static com.intellij.psi.util.CachedValueProvider.Result.create;
import static com.intellij.psi.util.CachedValuesManager.getCachedValue;
import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.PropertyUtil.getPropertyName;
import static com.intellij.psi.util.PropertyUtil.isSimplePropertyGetter;
import static com.intellij.psi.util.PropertyUtil.isSimplePropertySetter;
import static com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT;
import static com.intellij.psi.util.PsiTypesUtil.getClassType;
import static com.intellij.psi.util.PsiTypesUtil.hasUnresolvedComponents;
import static com.intellij.psi.util.PsiUtil.extractIterableTypeParameter;
import static com.intellij.psi.util.PsiUtil.resolveGenericsClassInType;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.BOOLEAN;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.BYTE;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.CHAR;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.DOUBLE;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.ENUM;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.FLOAT;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.INT;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.ITERABLE;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.KNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.LONG;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.MAP;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.SHORT;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.STRING;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNDEFINED;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static java.util.Objects.requireNonNull;

@UtilityClass
public class PsiCustomUtil {
    private static final Logger log = Logger.getInstance(PsiCustomUtil.class);

    private static final Key<CachedValue<Map<String, GenericClassMemberWrapper>>>
            SPRING_ASSISTANT_PLUGIN_PROPERTY_TO_CLASS_MEMBER_WRAPPER_KEY =
            create("spring_assistant_plugin_property_to_class_member_wrapper");
    private static final Key<CachedValue<PsiType>>
            SPRING_ASSISTANT_PLUGIN_ERASE_FREE_TYPE_PARAMETER_TYPE_KEY =
            create("spring_assistant_plugin_eraseFreeTypeParameterType");
    private static final Key<CachedValue<PsiType>> SPRING_ASSISTANT_PLUGIN_FIRST_PARAMETER_TYPE_KEY =
            create("spring_assistant_plugin_firstParameterType");
    private static final Key<CachedValue<PsiType>> SPRING_ASSISTANT_PLUGIN_RETURN_TYPE_KEY =
            create("spring_assistant_plugin_returnType");

    @Nullable
    public static PsiType safeGetValidType(@NotNull Module module, @NotNull String fqn) {
        try {
            // Intellij expects inner classes to be referred via `.` instead of `$`
            PsiType type = JavaPsiFacade.getInstance(module.getProject()).getParserFacade()
                    .createTypeFromText(fqn.replaceAll("\\$", "."), null);
            boolean typeValid = isValidType(type);
            if (typeValid) {
                if (type instanceof PsiClassType) {
                    return (PsiClassType) type;
                } else if (type instanceof PsiArrayType) {
                    return (PsiArrayType) type;
                }
            }
            return null;
        } catch (IncorrectOperationException e) {
            debug(() -> log.debug("Unable to find class fqn " + fqn));
            return null;
        }
    }

    @NotNull
    public static PsiType getReferredPsiType(PsiElement psiElement) {
        if (psiElement instanceof PsiField) {
            return ((PsiField) psiElement).getType();
        } else if (psiElement instanceof PsiMethod) {
            return requireNonNull(((PsiMethod) psiElement).getReturnType());
        } else if (psiElement instanceof PsiClass) {
            return getClassType((PsiClass) psiElement);
        }
        throw new RuntimeException(
                "Method supports psiElement of type PsiField, PsiMethod & PsiClass only");
    }

    @Nullable
    public static Map<PsiTypeParameter, PsiType> getTypeParameters(@NotNull PsiElement psiElement) {
        PsiType psiType = getReferredPsiType(psiElement);
        return getTypeParameters(psiType);
    }

    @Nullable
    public static Map<PsiTypeParameter, PsiType> getTypeParameters(PsiType type) {
        if (type instanceof PsiArrayType) {
            return getTypeParameters(((PsiArrayType) type).getComponentType());
        } else if (type instanceof PsiPrimitiveType) {
            return null;
        } else if (type instanceof PsiClassType) {
            PsiClassType.ClassResolveResult resolveResult =
                    ((PsiClassType) type).resolveGenerics();
            if (resolveResult.isValidResult()) {
                return resolveResult.getSubstitutor().getSubstitutionMap();
            }
        }
        return null;
    }

    @NotNull
    public static SuggestionNodeType getSuggestionNodeType(PsiType type) {
        if (type == null) {
            return UNDEFINED;
        } else if (type instanceof PsiArrayType) {
            return ARRAY;
        } else if (type instanceof PsiPrimitiveType) {
            SuggestionNodeType nodeType = getSuggestionNodeTypeForPrimitive(type);
            return nodeType != null ? nodeType : UNKNOWN_CLASS;
        } else if (type instanceof PsiClassType) {
            SuggestionNodeType nodeType = getSuggestionNodeTypeForPrimitive(type);
            if (nodeType != null) {
                return nodeType;
            } else if (type.getCanonicalText().equals(JAVA_LANG_STRING)) {
                return STRING;
            }

            // TODO: Need to check if this is required or not?
            PsiClassType psiClassType = (PsiClassType) type;
            PsiClassType.ClassResolveResult classResolveResult = psiClassType.resolveGenerics();
            if (classResolveResult.isValidResult()) {
                PsiClass psiClass = requireNonNull(classResolveResult.getElement());
                if (psiClass.isEnum()) {
                    return ENUM;
                } else if ("java.math.BigDecimal".equals(psiClass.getQualifiedName())) {
                    return DOUBLE;
                } else if ("java.nio.charset.Charset".equals(psiClass.getQualifiedName())
                        // charset is a string
                        || "org.springframework.http.MediaType".equals(psiClass.getQualifiedName())
                        || "java.net.InetAddress".equals(psiClass.getQualifiedName()) // ip address or hostname
                        || "java.net.URI".equals(psiClass.getQualifiedName()) // url
                        || requireNonNull(psiClass.getQualifiedName()).startsWith("java.lang.Class")
                        // expecting class name
                        || "org.springframework.core.io.Resource".equals(psiClass.getQualifiedName())
                        // spring resource such as classpath://myfile.json, file:///opt/myfile.json
                        || "java.util.Date".equals(psiClass.getQualifiedName()) || "java.sql.Date"
                        .equals(psiClass.getQualifiedName()) || "java.time.LocalDate"
                        .equals(psiClass.getQualifiedName()) || "java.time.LocalTime"
                        .equals(psiClass.getQualifiedName()) || "java.time.LocalDateTime"
                        .equals(psiClass.getQualifiedName()) || "java.time.ZoneId"
                        .equals(psiClass.getQualifiedName()) || "java.time.ZonedDateTime"
                        .equals(psiClass.getQualifiedName()) || "java.time.Instant"
                        .equals(psiClass.getQualifiedName()) || "java.time.Duration"
                        .equals(psiClass.getQualifiedName())// date & duation
                        || "java.time.Period".equals(psiClass.getQualifiedName()) || "java.util.Locale"
                        .equals(psiClass.getQualifiedName())) {
                    return STRING;
                } else if (isMap(psiClass)) {
                    return MAP;
                } else if (isIterable(psiClass)) {
                    return ITERABLE;
                } else {
                    return KNOWN_CLASS;
                }
            }
        }
        return UNKNOWN_CLASS;
    }

    @Nullable
    private static SuggestionNodeType getSuggestionNodeTypeForPrimitive(PsiType type) {
        if (PsiType.BOOLEAN.equals(type) || PsiType.BOOLEAN
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return BOOLEAN;
        } else if (PsiType.BYTE.equals(type) || PsiType.BYTE
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return BYTE;
        } else if (PsiType.SHORT.equals(type) || PsiType.SHORT
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return SHORT;
        } else if (PsiType.INT.equals(type) || PsiType.INT
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return INT;
        } else if (PsiType.LONG.equals(type) || PsiType.LONG
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return LONG;
        } else if (PsiType.FLOAT.equals(type) || PsiType.FLOAT
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return FLOAT;
        } else if (PsiType.DOUBLE.equals(type) || PsiType.DOUBLE
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return DOUBLE;
        } else if (PsiType.CHAR.equals(type) || PsiType.CHAR
                .equals(PsiPrimitiveType.getUnboxedType(type))) {
            return CHAR;
        }
        return null;
    }

    @Nullable
    public static String toClassFqn(@NotNull PsiType type) {
        if (type instanceof PsiArrayType) {
            String componentLongName = toClassFqn(((PsiArrayType) type).getComponentType());
            if (componentLongName != null) {
                return componentLongName + "[]";
            }
        } else if (type instanceof PsiPrimitiveType) {
            return type.getPresentableText();
        } else if (type instanceof PsiClassType) {
            return type.getCanonicalText();
        }
        return null;
    }

    @Nullable
    public static String toClassNonQualifiedName(@NotNull PsiType type) {
        if (type instanceof PsiArrayType) {
            String componentLongName =
                    toClassNonQualifiedName(((PsiArrayType) type).getComponentType());
            if (componentLongName != null) {
                return componentLongName + "[]";
            }
        } else if (type instanceof PsiPrimitiveType) {
            return type.getPresentableText();
        } else if (type instanceof PsiClassType) {
            return ((PsiClassType) type).getClassName();
        }
        return null;
    }

    private static boolean isMap(@NotNull PsiClass psiClass) {
        return isClassSameOrDescendantOf(psiClass, JAVA_UTIL_MAP) || isClassSameOrDescendantOf(psiClass,
                "java.util.Hashtable");
    }

    private static boolean isIterable(@NotNull PsiClass psiClass) {
        return isClassSameOrDescendantOf(psiClass, JAVA_LANG_ITERABLE);
    }

    private static boolean isClassSameOrDescendantOf(@NotNull PsiClass psiClass,
                                                     String expectedClassFqn) {
        return psiClass.getQualifiedName() != null && isInheritor(psiClass, expectedClassFqn);
    }

    @NotNull
    public static PsiType getBoxedTypeFromPrimitiveType(Module module,
                                                        PsiPrimitiveType primitiveType) {
        PsiType boxedPrimitiveType = safeGetValidType(module, requireNonNull(primitiveType.getBoxedTypeName()));
        assert boxedPrimitiveType instanceof PsiClassType;
        return boxedPrimitiveType;
    }

    @Nullable
    public static String typeToFqn(Module module, @NotNull PsiType type) {
        if (isValidType(type)) {
            if (type instanceof PsiArrayType) {
                type = ((PsiArrayType) type).getComponentType();
                return type.getCanonicalText();
            } else if (type instanceof PsiPrimitiveType) {
                return getBoxedTypeFromPrimitiveType(module, (PsiPrimitiveType) type).getCanonicalText();
            } else if (type instanceof PsiClassType) {
                return type.getCanonicalText();
            }
        }
        return null;
    }

    @Nullable
    public static Set<PsiClass> computeDependencies(Module module, @NotNull PsiType type) {
        PsiType originalType = type;
        if (isValidType(type)) {
            if (type instanceof PsiArrayType) {
                return computeDependencies(module, ((PsiArrayType) type).getComponentType());
            } else if (type instanceof PsiPrimitiveType) {
                type = getBoxedTypeFromPrimitiveType(module, (PsiPrimitiveType) type);
            } else if (type instanceof PsiWildcardType) {
                type = ((PsiWildcardType) type).getBound();
            } else if (type instanceof PsiCapturedWildcardType) {
                PsiType lowerBound = ((PsiCapturedWildcardType) type).getLowerBound();
                type = (lowerBound != NULL ? lowerBound : ((PsiCapturedWildcardType) type).getUpperBound());
            }

            if (type instanceof PsiClassType) {
                PsiClassType classType = (PsiClassType) type;
                Collection<PsiType> typeParams =
                        classType.resolveGenerics().getSubstitutor().getSubstitutionMap().values();
                TObjectHashingStrategy<PsiClass> nameComparingHashingStrategy =
                        new TObjectHashingStrategy<>() {
                            @Override
                            public int computeHashCode(PsiClass psiClass) {
                                return requireNonNull(psiClass.getQualifiedName()).hashCode();
                            }

                            @Override
                            public boolean equals(PsiClass psiClass, PsiClass other) {
                                return psiClass.hashCode() == other.hashCode();
                            }
                        };
                Set<PsiClass> dependencies = new THashSet<>(nameComparingHashingStrategy);
                dependencies.add(toValidPsiClass(classType));
                for (PsiType typeParam : typeParams) {
                    if (typeParam
                            != null) { // if the user specified raw class such as Map instead of Map<String, String>
                        Set<PsiClass> childDependencies = computeDependencies(module, typeParam);
                        if (childDependencies == null) {
                            return null;
                        }
                        dependencies.addAll(childDependencies);
                    }
                }
                return dependencies;
            }

            throw new IllegalAccessError(
                    "Only supports PsiArrayType, PsiPrimitiveType, PsiWildcardType, PsiCapturedWildcardType & PsiClassType. Does not support type: "
                            + originalType.getCanonicalText());
        }
        return null;
    }

    @Nullable
    public static PsiClass toValidPsiClass(@NotNull PsiClassType type) {
        if (isValidType(type)) {
            return type.resolve();
        }
        return null;
    }

    // Copied & modified from PsiUtil.ensureValidType
    public static boolean isValidType(@NotNull PsiType type) {
        if (!type.isValid()) {
            TimeoutUtil.sleep(1); // to see if processing in another thread suddenly makes the type valid again (which is a bug)
            if (!type.isValid()) {
                return false;
            }
        }

        if (type instanceof PsiArrayType) {
            return isValidType(((PsiArrayType) type).getComponentType());
        } else if (type instanceof PsiWildcardType) {
            PsiType bound = ((PsiWildcardType) type).getBound();

            return bound != null && isValidType(bound);
        } else if (type instanceof PsiCapturedWildcardType) {
            PsiType lowerBound = ((PsiCapturedWildcardType) type).getLowerBound();
            type = (lowerBound != NULL ? lowerBound : ((PsiCapturedWildcardType) type).getUpperBound());

            return type != NULL && isValidType(type);
        } else if (type instanceof PsiClassType) {
            final var psiClassType = (PsiClassType) type;
            try {
                var classResolveResult = psiClassType.resolveGenerics();
                return classResolveResult.isValidResult() && isValidElement(requireNonNull(classResolveResult.getElement())) && !hasUnresolvedComponents(type);
            } catch (final com.intellij.openapi.project.IndexNotReadyException exception) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the element is valid. If not, throws {@link com.intellij.psi.PsiInvalidElementAccessException} with
     * a meaningful message that points to the reasons why the element is not valid and may contain the stack trace
     * when it was invalidated.
     */
    // Copied & modified from PsiUtilCore.ensureValid
    private static boolean isValidElement(@NotNull PsiElement element) {
        if (!element.isValid()) {
            TimeoutUtil.sleep(
                    1); // to see if processing in another thread suddenly makes the element valid again (which is a bug)
            return element.isValid();
        }
        return true;
    }

    @Nullable
    public static PsiType getComponentType(@NotNull PsiType type) {
        if (type instanceof PsiArrayType) {
            return ((PsiArrayType) type).getComponentType();
        }
        return extractIterableTypeParameter(type, true);
    }

    @Contract("_, null->false")
    private static boolean representsCollection(@NotNull PsiClass psiClass, @Nullable PsiType type) {
        return type != null && getCollectionItemType(psiClass, type) != null;
    }

    @Nullable
    private static PsiType getCollectionItemType(@NotNull PsiClass psiClass, @NotNull PsiType type) {
        return JavaGenericsUtil.getCollectionItemType(type, psiClass.getResolveScope());
    }

    @Nullable
    public static Map<String, GenericClassMemberWrapper> getSanitisedPropertyToPsiMemberWrapper(
            @Nullable PsiClass psiClass) {
        if (psiClass != null) {
            return getCachedValue(psiClass, SPRING_ASSISTANT_PLUGIN_PROPERTY_TO_CLASS_MEMBER_WRAPPER_KEY,
                    () -> create(prepareWritableProperties(psiClass), MODIFICATION_COUNT));
        }
        return null;
    }

    @NotNull
    private static Map<String, GenericClassMemberWrapper> prepareWritableProperties(
            @NotNull PsiClass psiClass) {
        final Map<String, GenericClassMemberWrapper> memberNameToMemberWrapper = new THashMap<>();
        for (PsiMethod method : psiClass.getAllMethods()) {
            if (method.hasModifierProperty(STATIC) || !method.hasModifierProperty(PUBLIC)) {
                continue;
            }
            if (isSimplePropertyGetter(method)) {
                PsiMember acceptableMember = method;
                final String propertyName = getPropertyName(method);
                assert propertyName != null;

                PsiMethod setter = findInstancePropertySetter(psiClass, propertyName);
                if (setter != null) {
                    final PsiType setterArgType = setter.getParameterList().getParameters()[0].getType();
                    final PsiField field = psiClass.findFieldByName(propertyName, true);
                    if (field != null && !field.hasModifierProperty(STATIC)) {
                        final PsiType fieldType = getWritablePropertyType(psiClass, field);
                        if (fieldType == null || setterArgType.isConvertibleFrom(fieldType)) {
                            acceptableMember = field;
                        }
                    }
                } else {
                    final PsiType returnType = method.getReturnType();
                    if (representsCollection(psiClass, returnType)) {
                        final PsiField field = psiClass.findFieldByName(propertyName, true);
                        if (field != null && !field.hasModifierProperty(STATIC)) {
                            final PsiType fieldType = getWritablePropertyType(psiClass, field);
                            if (fieldType == null || returnType.isAssignableFrom(fieldType)) {
                                acceptableMember = field;
                            }
                        }
                    } else {
                        acceptableMember = null;
                    }
                }
                if (acceptableMember != null)
                    memberNameToMemberWrapper
                            .put(sanitise(propertyName), new GenericClassMemberWrapper(acceptableMember));
            }
        }
        return memberNameToMemberWrapper;
    }

    @Nullable
    private static PsiType getWritablePropertyType(@Nullable PsiClass containingClass,
                                                   @Nullable PsiElement declaration) {
        if (declaration instanceof PsiField) {
            return getFieldType((PsiField) declaration);
        }
        if (declaration instanceof PsiMethod) {
            final PsiMethod method = (PsiMethod) declaration;
            if (method.getParameterList().getParametersCount() != 0) {
                return getSetterArgumentType(method);
            }
            final String propertyName = getPropertyName(method);
            final PsiClass psiClass =
                    containingClass != null ? containingClass : method.getContainingClass();
            if (propertyName != null && containingClass != null) {
                final PsiMethod setter = findInstancePropertySetter(psiClass, propertyName);
                if (setter != null) {
                    final PsiType setterArgumentType = getSetterArgumentType(setter);
                    if (setterArgumentType != null)
                        return setterArgumentType;
                }
            }
            return getGetterReturnType(method);
        }
        return null;
    }

    @Nullable
    private static PsiType getFieldType(final PsiField field) {
        return getCachedValue(field, SPRING_ASSISTANT_PLUGIN_ERASE_FREE_TYPE_PARAMETER_TYPE_KEY, () -> {
            final PsiType fieldType = field.getType();
            final PsiClassType.ClassResolveResult resolveResult = resolveGenericsClassInType(fieldType);
            final PsiClass fieldClass = resolveResult.getElement();
            if (fieldClass == null) {
                final PsiType propertyType = eraseFreeTypeParameters(fieldType, field);
                return create(propertyType, MODIFICATION_COUNT);
            }
            return null;
        });
    }

    @Nullable
    private static PsiType getSetterArgumentType(@NotNull PsiMethod method) {
        return getCachedValue(method, SPRING_ASSISTANT_PLUGIN_FIRST_PARAMETER_TYPE_KEY, () -> {
            final PsiParameter[] parameters = method.getParameterList().getParameters();
            if (!method.hasModifierProperty(STATIC) && parameters.length == 1) {
                final PsiType argumentType = eraseFreeTypeParameters(parameters[0].getType(), method);
                return create(argumentType, MODIFICATION_COUNT);
            }
            return create(null, MODIFICATION_COUNT);
        });
    }

    @Nullable
    private static PsiType eraseFreeTypeParameters(@Nullable PsiType psiType,
                                                   @NotNull PsiMember member) {
        final PsiClass containingClass = member.getContainingClass();
        return eraseFreeTypeParameters(psiType, containingClass);
    }

    @Nullable
    private static PsiType eraseFreeTypeParameters(@Nullable PsiType psiType,
                                                   @Nullable PsiClass containingClass) {
        if (containingClass == null) {
            return null;
        }
        return getElementFactory(containingClass.getProject()).createRawSubstitutor(containingClass)
                .substitute(psiType);
    }

    private static PsiType getGetterReturnType(@NotNull PsiMethod method) {
        return getCachedValue(method, SPRING_ASSISTANT_PLUGIN_RETURN_TYPE_KEY, () -> {
            final PsiType returnType = eraseFreeTypeParameters(method.getReturnType(), method);
            return create(returnType, MODIFICATION_COUNT);
        });
    }

    @Nullable
    private static PsiMethod findInstancePropertySetter(@NotNull PsiClass psiClass,
                                                        @Nullable String propertyName) {
        if (StringUtil.isEmpty(propertyName))
            return null;
        final String suggestedSetterName = PropertyUtil.suggestSetterName(propertyName);
        final PsiMethod[] setters = psiClass.findMethodsByName(suggestedSetterName, true);
        for (PsiMethod setter : setters) {
            if (setter.hasModifierProperty(PUBLIC) && !setter.hasModifierProperty(STATIC)
                    && isSimplePropertySetter(setter)) {
                return setter;
            }
        }
        return null;
    }

    @Nullable
    public static Module findModule(@NotNull PsiElement element) {
        return findModuleForPsiElement(element);
    }

    @Nullable
    public static Module findModule(@NotNull InsertionContext context) {
        return findModuleForFile(context.getFile().getVirtualFile(), context.getProject());
    }

    @Nullable
    public static String computeDocumentation(PsiMember member) {
        PsiDocComment docComment;
        if (member instanceof PsiField) {
            docComment = ((PsiField) member).getDocComment();
        } else if (member instanceof PsiMethod) {
            docComment = ((PsiMethod) member).getDocComment();
        } else {
            throw new RuntimeException("Method supports targets of type PsiField & PsiMethod only");
        }
        if (docComment != null) {
            StringBuilder builder = new StringBuilder();
            new JavaDocInfoGenerator(member.getProject(), member)
                    .generateCommonSection(builder, docComment);
            return builder.toString().trim();
        }
        return null;
    }

    @Nullable
    public static VirtualFile findFileUnderRootInModule(Module module, String targetFileName) {
        VirtualFile[] contentRoots = getInstance(module).getContentRoots();
        for (VirtualFile contentRoot : contentRoots) {
            VirtualFile childFile = findFileUnderRootInModule(contentRoot, targetFileName);
            if (childFile != null) {
                return childFile;
            }
        }
        return null;
    }

    @Nullable
    public static VirtualFile findFileUnderRootInModule(@NotNull VirtualFile contentRoot,
                                                        String targetFileName) {
        return contentRoot.findChild(targetFileName);
    }

    /**
     * Debug logging can be enabled by adding fully classified class name/package name with # prefix
     * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
     *
     * @param doWhenDebug code to execute when debug is enabled
     */
    private static void debug(Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }

}
