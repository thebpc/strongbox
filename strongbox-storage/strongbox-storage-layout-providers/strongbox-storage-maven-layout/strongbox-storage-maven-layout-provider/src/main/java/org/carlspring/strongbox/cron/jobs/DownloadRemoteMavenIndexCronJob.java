package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.jobs.fields.*;
import org.carlspring.strongbox.storage.indexing.IndexTypeEnum;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexFounder;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.springframework.core.env.Environment;
import static org.carlspring.strongbox.storage.indexing.RepositoryIndexFounder.RepositoryIndexFounderQualifier;

/**
 * @author Kate Novik
 * @author carlspring
 */
public class DownloadRemoteMavenIndexCronJob
        extends JavaCronJob
{

    public static final String STRONGBOX_DOWNLOAD_INDEXES = "strongbox.download.indexes";

    private static final String PROPERTY_STORAGE_ID = "storageId";

    private static final String PROPERTY_REPOSITORY_ID = "repositoryId";

    private static final Set<CronJobField> FIELDS = ImmutableSet.of(
            new CronJobStorageIdAutocompleteField(new CronJobStringTypeField(
                    new CronJobOptionalField(new CronJobNamedField(PROPERTY_STORAGE_ID)))),
            new CronJobRepositoryIdAutocompleteField(new CronJobStringTypeField(
                    new CronJobOptionalField(new CronJobNamedField(PROPERTY_REPOSITORY_ID)))));

    @Inject
    @RepositoryIndexFounderQualifier(IndexTypeEnum.REMOTE)
    private RepositoryIndexFounder repositoryIndexFounder;

    @Inject
    private ConfigurationManager configurationManager;

    @Override
    public void executeTask(CronTaskConfigurationDto config)
            throws IOException
    {
        logger.debug("Executing DownloadRemoteIndexCronJob.");

        String storageId = config.getProperty(PROPERTY_STORAGE_ID);
        String repositoryId = config.getProperty(PROPERTY_REPOSITORY_ID);

        Repository repository = configurationManager.getRepository(storageId, repositoryId);

        repositoryIndexFounder.apply(repository);
    }

    @Override
    public boolean enabled(CronTaskConfigurationDto configuration,
                           Environment env)
    {
        if (!super.enabled(configuration, env))
        {
            return false;
        }

        String storageId = configuration.getProperty(PROPERTY_STORAGE_ID);
        String repositoryId = configuration.getProperty(PROPERTY_REPOSITORY_ID);

        boolean shouldDownloadIndexes = shouldDownloadAllRemoteRepositoryIndexes();
        boolean shouldDownloadRepositoryIndex = shouldDownloadRepositoryIndex(storageId, repositoryId);

        return shouldDownloadIndexes || shouldDownloadRepositoryIndex;
    }

    @Override
    public CronJobDefinition getCronJobDefinition()
    {
        return CronJobDefinition.newBuilder()
                                .jobClass(DownloadRemoteMavenIndexCronJob.class.getName())
                                .name("Download Remote Maven Index Cron Job")
                                .description("Download Remote Maven Index Cron Job")
                                .fields(FIELDS)
                                .build();
    }

    public static boolean shouldDownloadAllRemoteRepositoryIndexes()
    {
        return System.getProperty(STRONGBOX_DOWNLOAD_INDEXES) == null ||
               Boolean.parseBoolean(System.getProperty(STRONGBOX_DOWNLOAD_INDEXES));
    }

    public static boolean shouldDownloadRepositoryIndex(String storageId,
                                                        String repositoryId)
    {
        return (System.getProperty(STRONGBOX_DOWNLOAD_INDEXES + "." + storageId + "." + repositoryId) == null ||
                Boolean.parseBoolean(System.getProperty(STRONGBOX_DOWNLOAD_INDEXES + "." + storageId + "."
                                                        + repositoryId)))
               &&
               isIncludedDespiteWildcard(storageId, repositoryId);
    }

    public static boolean isIncludedDespiteWildcard(String storageId,
                                                    String repositoryId)
    {
        return // is excluded by wildcard
                !Boolean.parseBoolean(System.getProperty(STRONGBOX_DOWNLOAD_INDEXES + "." + storageId + ".*")) &&
                // and is explicitly included
                Boolean.parseBoolean(System.getProperty(STRONGBOX_DOWNLOAD_INDEXES + "." + storageId + "."
                                                        + repositoryId));
    }

}
