package za.vodacom.repoprofile.config;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import za.vodacom.repoprofile.domain.model.ProviderType;
import za.vodacom.repoprofile.ports.out.ClientResolver;
import za.vodacom.repoprofile.ports.out.SourceCodeClient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the correct {@link SourceCodeClient} adapter for a given provider.
 * <p>
 * Each adapter registers itself via Spring's {@code @Component("github")} naming convention.
 * This resolver maps {@link ProviderType} \u2192 adapter at startup and provides O(1) lookups.
 */
@Component
public class SourceCodeClientResolver implements ClientResolver {

    private final Map<ProviderType, SourceCodeClient> clients;

    public SourceCodeClientResolver(List<SourceCodeClient> adapters) {
        this.clients = new EnumMap<>(ProviderType.class);

        for (SourceCodeClient adapter : adapters) {
            // Use AnnotationUtils.findAnnotation to see through Spring AOP proxies
            Component annotation = AnnotationUtils.findAnnotation(adapter.getClass(), Component.class);
            if (annotation != null && !annotation.value().isEmpty()) {
                ProviderType type = ProviderType.from(annotation.value());
                clients.put(type, adapter);
            }
        }
    }

    /**
     * Returns the adapter for the given provider.
     *
     * @throws IllegalArgumentException if the provider has no registered adapter
     */
    public SourceCodeClient resolve(ProviderType provider) {
        SourceCodeClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("No adapter registered for provider: " + provider.getKey());
        }
        return client;
    }

    @Override
    public SourceCodeClient resolve(String providerKey) {
        return resolve(ProviderType.from(providerKey));
    }
}
