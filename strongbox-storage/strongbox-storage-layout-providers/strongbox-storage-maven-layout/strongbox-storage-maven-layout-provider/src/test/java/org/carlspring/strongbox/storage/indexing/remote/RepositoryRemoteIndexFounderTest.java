package org.carlspring.strongbox.storage.indexing.remote;

import org.carlspring.strongbox.artifact.generator.MavenArtifactGenerator;
import org.carlspring.strongbox.config.Maven2LayoutProviderTestConfig;
import org.carlspring.strongbox.providers.io.RepositoryPathResolver;
import org.carlspring.strongbox.storage.indexing.IndexTypeEnum;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexFounder;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexFounder.RepositoryIndexFounderQualifier;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.testing.MavenIndexedRepositorySetup;
import org.carlspring.strongbox.testing.TestCaseWithMavenArtifactGenerationAndIndexing;
import org.carlspring.strongbox.testing.artifact.ArtifactManagementTestExecutionListener;
import org.carlspring.strongbox.testing.artifact.TestArtifact;
import org.carlspring.strongbox.testing.storage.repository.RepositoryManagementTestExecutionListener;
import org.carlspring.strongbox.testing.storage.repository.TestRepository;
import org.carlspring.strongbox.testing.storage.repository.TestRepository.Remote;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.carlspring.strongbox.artifact.coordinates.MavenArtifactCoordinates.LAYOUT_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author carlspring
 */
@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration(classes = Maven2LayoutProviderTestConfig.class)
@Execution(CONCURRENT)
public class RepositoryRemoteIndexFounderTest
        extends TestCaseWithMavenArtifactGenerationAndIndexing
{

    private static final String REPOSITORY_RELEASES = "m2pr-releases";

    private static final String PROXY_REPOSITORY_URL =
            "http://localhost:48080/storages/" + STORAGE0 + "/" + REPOSITORY_RELEASES + "/";

    private static final String REPOSITORY_PROXY = "m2pr-proxied-releases";

    private static final String A1 = "org/carlspring/strongbox/strongbox-search-test/1.0/strongbox-search-test-1.0.jar";

    private static final String A2 = "org/carlspring/strongbox/strongbox-search-test/1.1/strongbox-search-test-1.1.jar";

    private static final String A3 = "org/carlspring/strongbox/strongbox-search-test/1.2/strongbox-search-test-1.2.jar";

    @Inject
    private RepositoryPathResolver repositoryPathResolver;

    @Inject
    @RepositoryIndexFounderQualifier(IndexTypeEnum.LOCAL)
    private RepositoryIndexFounder repositoryLocalIndexFounder;

    @Inject
    @RepositoryIndexFounderQualifier(IndexTypeEnum.REMOTE)
    private RepositoryIndexFounder repositoryRemoteIndexFounder;

    @Test
    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    public void testRepositoryIndexFetching(
            @TestRepository(layout = LAYOUT_NAME, repositoryId = REPOSITORY_RELEASES, setup = MavenIndexedRepositorySetup.class)
                    Repository repository,
            @TestRepository(layout = LAYOUT_NAME, repositoryId = REPOSITORY_PROXY, setup = MavenIndexedRepositorySetup.class)
            @Remote(url = PROXY_REPOSITORY_URL) Repository proxyRepository,
            @TestArtifact(repositoryId = REPOSITORY_RELEASES, resource = A1, generator = MavenArtifactGenerator.class)
                    Path a1,
            @TestArtifact(repositoryId = REPOSITORY_RELEASES, resource = A2, generator = MavenArtifactGenerator.class)
                    Path a2,
            @TestArtifact(repositoryId = REPOSITORY_RELEASES, resource = A3, generator = MavenArtifactGenerator.class)
                    Path a3)
            throws IOException
    {

        repositoryLocalIndexFounder.apply(repository);

        repositoryRemoteIndexFounder.apply(proxyRepository);

        Path indexPropertiesUpdaterFile = repositoryPathResolver.resolve(proxyRepository).resolve(
                ".index/remote/nexus-maven-repository-index-updater.properties");
        assertTrue(Files.exists(indexPropertiesUpdaterFile),
                   "Failed to retrieve nexus-maven-repository-index-updater.properties from the remote!");
    }

}
