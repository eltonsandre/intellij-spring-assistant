package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.getBoxedTypeFromPrimitiveType;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.getSuggestionNodeType;

@UtilityClass
public final class ClassSuggestionNodeFactory {

    static ClassMetadata newClassMetadata(@NotNull PsiType type) {
        SuggestionNodeType nodeType = getSuggestionNodeType(type);
        switch (nodeType) {
            case BOOLEAN:
                return new BooleanClassMetadata();
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case CHAR:
            case STRING:
                return new DummyClassMetadata(nodeType);
            case ENUM:
                return new EnumClassMetadata((PsiClassType) type);
            case ITERABLE:
                return new IterableClassMetadata((PsiClassType) type);
            case MAP:
                return new MapClassMetadata((PsiClassType) type);
            case KNOWN_CLASS:
                return new GenericClassMetadata((PsiClassType) type);
            case UNKNOWN_CLASS:
                return new DummyClassMetadata(nodeType);
            default:
                throw new IllegalStateException(
                        "Class suggestion node for the specified class " + type + " is undefined");
        }
    }

    @NotNull
    public static MetadataProxy newMetadataProxy(Module module, @NotNull PsiType type) {
        if (type instanceof PsiArrayType) {
            return new ArrayMetadataProxy(module, (PsiArrayType) type);
        } else if (type instanceof PsiPrimitiveType) {
            PsiPrimitiveType primitiveType = (PsiPrimitiveType) type;
            type = getBoxedTypeFromPrimitiveType(module, primitiveType);
        }

        if (type instanceof PsiClassType) {
            SuggestionNodeType suggestionNodeType = getSuggestionNodeType(type);
            if (suggestionNodeType == SuggestionNodeType.MAP) {
                return new MapClassMetadataProxy((PsiClassType) type);
            } else {
                return new ClassMetadataProxy((PsiClassType) type);
            }
        }

        throw new IllegalAccessError(
                "Supports only PsiArrayType, PsiPrimitiveType & PsiClassType types");
    }

}
