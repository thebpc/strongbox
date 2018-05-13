package org.carlspring.strongbox.storage.indexing;

import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RepositoryPathLock;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Przemyslaw Fusik
 */
public abstract class AbstractRepositoryIndexFounder
        implements RepositoryIndexFounder
{

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private RepositoryPathLock repositoryPathLock;

    @Override
    public RepositoryPath apply(Repository repository)
            throws IOException
    {
        final RepositoryPath repositoryIndexDirectoryPath = getRepositoryIndexDirectoryPathResolver().resolve(
                repository);

        final Lock lock = repositoryPathLock.lock(repositoryIndexDirectoryPath).writeLock();
        lock.lock();

        try (final RepositoryCloseableIndexingContext indexingContext = getRepositoryIndexingContextFactory().create(
                repository))
        {
            onIndexingContextCreated(repositoryIndexDirectoryPath, indexingContext);
        }
        finally
        {
            lock.unlock();
        }

        return repositoryIndexDirectoryPath;
    }

    protected abstract void onIndexingContextCreated(RepositoryPath repositoryIndexDirectoryPath,
                                                     RepositoryCloseableIndexingContext indexingContext)
            throws IOException;

    protected abstract RepositoryIndexingContextFactory getRepositoryIndexingContextFactory();

    protected abstract RepositoryIndexDirectoryPathResolver getRepositoryIndexDirectoryPathResolver();
}
