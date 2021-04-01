package in.oneton.idea.spring.assistant.plugin.initializr.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency.DependencyLinksContainer.DependencyLink;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DependencyOneOrMoreDeserializer implements JsonDeserializer<List<DependencyLink>> {

    @Override
    public List<DependencyLink> deserialize(JsonElement jsonElement, Type type,
                                            JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        List<DependencyLink> dependencyLinks = null;
        if (jsonElement instanceof JsonArray) {
            dependencyLinks = new ArrayList<>();
            JsonArray elements = (JsonArray) jsonElement;
            for (JsonElement element : elements) {
                dependencyLinks.add(newDependencyLink((JsonObject) element));
            }
        } else if (jsonElement instanceof JsonObject) {
            dependencyLinks = new ArrayList<>();
            DependencyLink dependencyLink = newDependencyLink((JsonObject) jsonElement);
            dependencyLinks.add(dependencyLink);
        }
        return dependencyLinks;
    }

    private DependencyLink newDependencyLink(JsonObject jsonObject) {
        DependencyLink.DependencyLinkBuilder builder = DependencyLink.builder();
        JsonElement href = jsonObject.get("href");
        if (href != null) {
            builder.href(href.getAsString());
        }
        JsonElement title = jsonObject.get("title");
        if (title != null) {
            builder.title(title.getAsString());
        }
        return builder.build();
    }

}
