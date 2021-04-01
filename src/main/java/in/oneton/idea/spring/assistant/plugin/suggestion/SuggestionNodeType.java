package in.oneton.idea.spring.assistant.plugin.suggestion;

import com.intellij.icons.AllIcons;

import javax.swing.*;

public enum SuggestionNodeType {
    BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR, STRING,
    /**
     * Known set of values. Similar to enum, but does not correspond to a class
     */
    VALUES, ENUM, ARRAY, ITERABLE, MAP, KNOWN_CLASS, UNKNOWN_CLASS, UNDEFINED;

    public static final String CARET = "<caret>";

    public boolean isWholeNumber() {
        return this == BYTE || this == SHORT || this == INT || this == LONG;
    }

    public boolean isDecimal() {
        return this == FLOAT || this == DOUBLE;
    }

    public boolean representsLeaf() {
        return representsPrimitiveOrString() || representsEnumOrValues() || this == UNKNOWN_CLASS
                || this == UNDEFINED;
    }

    private boolean representsEnumOrValues() {
        return this == ENUM || this == VALUES;
    }

    private boolean representsPrimitiveOrString() {
        return this == BOOLEAN || isWholeNumber() || isDecimal() || this == CHAR || this == STRING;
    }

    public boolean representsArrayOrCollection() {
        return this == ARRAY || this == ITERABLE;
    }

    public Icon getIcon() {
        if (representsPrimitiveOrString()) {
            return AllIcons.Nodes.Property;
        } else if (representsEnumOrValues()) {
            return AllIcons.Nodes.Enum;
        } else if (representsArrayOrCollection()) {
            return AllIcons.Json.Array;
        } else {
            return AllIcons.Json.Object;
        }
    }

}
