package za.vodacom.repoprofile.domain.strategy;

import za.vodacom.repoprofile.domain.model.Repo;

import java.util.List;

/**
 * Strategy interface for determining a user's top programming language.
 */
public interface LanguageStrategy {

    String determineTopLanguage(List<Repo> repos);
}
