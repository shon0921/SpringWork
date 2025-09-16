package kopo.poly.config;

import kopo.poly.dto.NoticeDTO;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public KeyGenerator noticeList() {
        return (target, method, param) -> {
            return "v1:notice_list";
        };
    }

    @Bean
    public org.springframework.cache.CacheManager CacheManager() {

        CachingProvider provider =
                Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");

        CacheManager jcacheManager = provider.getCacheManager();

        MutableConfiguration<String, List<NoticeDTO>> cfg =
                new MutableConfiguration<String, List<NoticeDTO>>()
                        .setTypes(String.class, (Class) List.class) // list is generic, so cast is needed
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(
                                new Duration(TimeUnit.MINUTES, 10)
                        ));


        if (jcacheManager.getCache("notice", String.class, List.class) == null) {
            jcacheManager.createCache("notice", cfg);
        }
        return new JCacheCacheManager(jcacheManager);
    }

}