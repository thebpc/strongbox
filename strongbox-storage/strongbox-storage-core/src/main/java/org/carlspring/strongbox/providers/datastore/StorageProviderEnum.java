package org.carlspring.strongbox.providers.datastore;

import org.carlspring.strongbox.api.Describable;

/**
 * @author Przemyslaw Fusik
 */
public enum StorageProviderEnum
        implements Describable
{

    FILESYSTEM("file-system");

    private String value;

    StorageProviderEnum(String value)
    {
        this.value = value;
    }

    @Override
    public String describe()
    {
        return value;
    }

}
