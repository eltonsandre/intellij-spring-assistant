package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import in.oneton.idea.spring.assistant.plugin.suggestion.handler.AnnotationsKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.suggestion.handler.PropertiesKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.suggestion.handler.PropertiesValueInsertHandler;
import in.oneton.idea.spring.assistant.plugin.suggestion.handler.YamlKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.suggestion.handler.YamlValueInsertHandler;

public enum FileType {

    YAML {
        @Override
        public InsertHandler<LookupElement> newKeyInsertHandler() {
            return new YamlKeyInsertHandler();
        }

        @Override
        public InsertHandler<LookupElement> newValueInsertHandler() {
            return new YamlValueInsertHandler();
        }

    }, PROPERTIES {
        @Override
        public InsertHandler<LookupElement> newKeyInsertHandler() {
            return new PropertiesKeyInsertHandler();
        }

        @Override
        public InsertHandler<LookupElement> newValueInsertHandler() {
            return new PropertiesValueInsertHandler();
        }
    },

    JAVA {
        @Override
        public InsertHandler<LookupElement> newKeyInsertHandler() {
            return new AnnotationsKeyInsertHandler();
        }

        @Override
        public InsertHandler<LookupElement> newValueInsertHandler() {
            return null;
        }
    };

    public abstract InsertHandler<LookupElement> newKeyInsertHandler();

    public abstract InsertHandler<LookupElement> newValueInsertHandler();

}
