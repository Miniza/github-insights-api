package za.vodacom.repoprofile.domain.strategy;

import org.springframework.stereotype.Component;
import za.vodacom.repoprofile.domain.model.Repo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Determines the top language by counting how many repositories use each language.
 */
@Component("byRepoCount")
public class ByRepoCountStrategy implements LanguageStrategy {

    @Override
    public String determineTopLanguage(List<Repo> repos) {
        return repos.stream()
                .filter(r -> r.language() != null && !r.language().isBlank())
                .collect(Collectors.groupingBy(Repo::language, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
