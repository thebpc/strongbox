package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.jobs.fields.*;
import org.carlspring.strongbox.providers.layout.LayoutProviderRegistry;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.indexing.IndexTypeEnum;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexFounder;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * @author Kate Novik.
 */
public class RebuildMavenIndexesCronJob
        extends JavaCronJob
{

    private static final String PROPERTY_STORAGE_ID = "storageId";

    private static final String PROPERTY_REPOSITORY_ID = "repositoryId";

    private static final Set<CronJobField> FIELDS = ImmutableSet.of(
            new CronJobStorageIdAutocompleteField(new CronJobStringTypeField(
                    new CronJobRequiredField(new CronJobNamedField(PROPERTY_STORAGE_ID)))),
            new CronJobRepositoryIdAutocompleteField(new CronJobStringTypeField(
                    new CronJobRequiredField(new CronJobNamedField(PROPERTY_REPOSITORY_ID)))));

    @Inject
    private LayoutProviderRegistry layoutProviderRegistry;

    @Inject
    @RepositoryIndexFounder.RepositoryIndexFounderQualifier(IndexTypeEnum.LOCAL)
    private RepositoryIndexFounder repositoryIndexFounder;

    @Override
    public void executeTask(CronTaskConfigurationDto config)
            throws Throwable
    {
        logger.debug("Executing RebuildMavenIndexesCronJob ...");

        String storageId = config.getRequiredProperty(PROPERTY_STORAGE_ID);
        String repositoryId = config.getRequiredProperty(PROPERTY_REPOSITORY_ID);

        Storage storage = layoutProviderRegistry.getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);

        repositoryIndexFounder.apply(repository);
    }

    @Override
    public CronJobDefinition getCronJobDefinition()
    {
        return CronJobDefinition.newBuilder()
                                .jobClass(RebuildMavenIndexesCronJob.class.getName())
                                .name("Rebuild Maven Indexes Cron Job")
                                .description("Rebuild Maven Indexes Cron Job")
                                .fields(FIELDS)
                                .build();
    }

}
