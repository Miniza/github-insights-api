package za.vodacom.repoprofile.ports.out;

/**
 * Driven port – resolves the correct {@link SourceCodeClient} adapter for a given provider key.
 */
public interface ClientResolver {

    SourceCodeClient resolve(String providerKey);
}
