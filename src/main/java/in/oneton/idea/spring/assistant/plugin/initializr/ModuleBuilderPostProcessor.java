package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;

import static com.intellij.openapi.extensions.ExtensionPointName.create;

public interface ModuleBuilderPostProcessor {
    ExtensionPointName<ModuleBuilderPostProcessor> EXTENSION_POINT_NAME =
            create("spring.assistant.initializr.moduleBuilderPostProcessor");

    /**
     * @param module module
     * @return true if project is imported, false otherwise
     */
    boolean postProcess(Module module);
}
