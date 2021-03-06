package org.apereo.cas.metadata;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.Arrays;

/**
 * This is {@link CasConfigurationMetadataRepository}.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 5.2.0
 */
public class CasConfigurationMetadataRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasConfigurationMetadataRepository.class);
    
    private final ConfigurationMetadataRepository configMetadataRepo;

    public CasConfigurationMetadataRepository() {
        this("classpath*:META-INF/spring-configuration-metadata.json");
    }

    /**
     * Instantiates a new Cas configuration metadata repository.
     * Scans the context looking for spring configuration metadata
     * resources and then loads them all into a repository instance.
     *
     * @param resource the resource
     */
    public CasConfigurationMetadataRepository(final String resource) {
        try {
            final Resource[] resources = new PathMatchingResourcePatternResolver().getResources(resource);
            final ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
            Arrays.stream(resources).forEach(Unchecked.consumer(r -> {
                try (InputStream in = r.getInputStream()) {
                    builder.withJsonResource(in);
                }
            }));
            configMetadataRepo = builder.build();
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public ConfigurationMetadataRepository getRepository() {
        return configMetadataRepo;
    }
}
