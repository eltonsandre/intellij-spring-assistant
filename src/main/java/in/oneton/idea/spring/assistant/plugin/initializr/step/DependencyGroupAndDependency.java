package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.miguelfonseca.completely.IndexAdapter;
import com.miguelfonseca.completely.data.Indexable;
import com.miguelfonseca.completely.data.ScoredObject;
import com.miguelfonseca.completely.text.index.FuzzyIndex;
import com.miguelfonseca.completely.text.index.PatriciaTrie;
import com.miguelfonseca.completely.text.match.EditDistanceAutomaton;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

@Getter
@Builder
public class DependencyGroupAndDependency implements Indexable {
    private final InitializerMetadata.DependencyComposite.DependencyGroup group;
    private final InitializerMetadata.DependencyComposite.DependencyGroup.Dependency dependency;

    @Override
    public List<String> getFields() {
        return asList(group.getName(), dependency.getName(), dependency.getDescription());
    }

    static class DependencyGroupAndDependencyAdapter
            implements IndexAdapter<DependencyGroupAndDependency> {
        private final FuzzyIndex<DependencyGroupAndDependency> index = new PatriciaTrie<>();

        @Override
        public Collection<ScoredObject<DependencyGroupAndDependency>> get(String token) {
            // TODO: Not sure the value of threshold here :). Following sample from https://github.com/fmmfonseca/completely/blob/master/sample/src/main/java/com/miguelfonseca/completely/SampleAdapter.java
            // Set threshold according to the token length
            double threshold = Math.log(Math.max(token.length() - 1, 1));
            return index.getAny(new EditDistanceAutomaton(token, threshold));
        }

        @Override
        public boolean put(String token, @Nullable DependencyGroupAndDependency value) {
            return index.put(token, value);
        }

        @Override
        public boolean remove(DependencyGroupAndDependency value) {
            return index.remove(value);
        }
    }
}
