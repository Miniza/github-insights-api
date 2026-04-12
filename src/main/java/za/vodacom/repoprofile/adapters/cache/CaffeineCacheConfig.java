package za.vodacom.repoprofile.adapters.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import za.vodacom.repoprofile.util.Constants;

import java.time.Duration;

@Configuration
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${cache.max-size:500}") int maxSize,
            @Value("${cache.ttl:300s}") Duration ttl) {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                Constants.CACHE_PROFILES,
                Constants.CACHE_REPOS
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl));
        return manager;
    }
}
