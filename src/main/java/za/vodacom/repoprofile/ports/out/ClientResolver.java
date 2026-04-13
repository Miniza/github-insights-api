package za.vodacom.repoprofile.ports.out;

/**
 * Resolves the correct {@link SourceCodeClient} for a given provider key.
 */
public interface ClientResolver {

    SourceCodeClient resolve(String providerKey);
}
