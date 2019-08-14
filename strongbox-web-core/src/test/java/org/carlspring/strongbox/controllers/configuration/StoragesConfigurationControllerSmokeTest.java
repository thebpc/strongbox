package org.carlspring.strongbox.controllers.configuration;

import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.forms.configuration.RepositoryForm;
import org.carlspring.strongbox.forms.configuration.StorageForm;
import org.carlspring.strongbox.providers.datastore.StorageProviderEnum;
import org.carlspring.strongbox.providers.layout.RawLayoutProvider;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;
import org.carlspring.strongbox.storage.repository.RepositoryPolicyEnum;
import org.carlspring.strongbox.storage.repository.RepositoryStatusEnum;
import org.carlspring.strongbox.storage.repository.RepositoryTypeEnum;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.carlspring.strongbox.controllers.configuration.StoragesConfigurationController.*;
import static org.carlspring.strongbox.rest.client.RestAssuredArtifactClient.OK;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

@IntegrationTest
@Execution(CONCURRENT)
public class StoragesConfigurationControllerSmokeTest
        extends RestAssuredBaseTest
{

    @Inject
    private PropertiesBooter propertiesBooter;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();

        setContextBaseUrl("/api/configuration/strongbox/storages");
    }

    @Test
    public void artifactShouldBeDeployedIntoNewRepository()
            throws IOException
    {
        String storageId = "storage-sccst";
        String repositoryId = "sccst-releases";
        String fileName = "dummy-file.zip";
        Path artifactPath = Paths.get(propertiesBooter.getVaultDirectory())
                                 .resolve("storages")
                                 .resolve(storageId)
                                 .resolve(repositoryId)
                                 .resolve(fileName);
        assertFalse(Files.exists(artifactPath));

        StorageForm storageForm = new StorageForm();
        storageForm.setId(storageId);
        storageForm.setBasedir("");

        // 1. Create storage
        String url = getContextBaseUrl();
        givenCustom().contentType(MediaType.APPLICATION_JSON_VALUE)
                     .accept(MediaType.APPLICATION_JSON_VALUE)
                     .body(storageForm)
                     .when()
                     .put(url)
                     .prettyPeek()
                     .then()
                     .statusCode(OK)
                     .body(containsString(SUCCESSFUL_SAVE_STORAGE));

        // 2. Create repository
        RepositoryForm repositoryForm = new RepositoryForm();
        repositoryForm.setId(repositoryId);
        repositoryForm.setLayout(RawLayoutProvider.ALIAS);
        repositoryForm.setImplementation(StorageProviderEnum.FILESYSTEM.describe());
        repositoryForm.setStatus(RepositoryStatusEnum.IN_SERVICE.describe());
        repositoryForm.setType(RepositoryTypeEnum.HOSTED.describe());
        repositoryForm.setPolicy(RepositoryPolicyEnum.RELEASE.describe());
        repositoryForm.setBasedir("");

        url = getContextBaseUrl() + "/{storageId}/{repositoryId}";
        givenCustom().contentType(MediaType.APPLICATION_JSON_VALUE)
                     .accept(MediaType.APPLICATION_JSON_VALUE)
                     .body(repositoryForm)
                     .when()
                     .put(url, storageForm.getId(), repositoryForm.getId())
                     .prettyPeek()
                     .then()
                     .statusCode(OK)
                     .body(containsString(SUCCESSFUL_REPOSITORY_SAVE));

        // 3. Deploy artifact
        url = "/storages/{storageId}/{repositoryId}/{artifactPath}";
        given().header(HttpHeaders.USER_AGENT, "Raw/*")
               .body(createZipFile())
               .when()
               .put(url, storageForm.getId(), repositoryForm.getId(), fileName)
               .then()
               .statusCode(HttpStatus.OK.value());

        assertTrue(Files.exists(artifactPath));

        // 4. Delete storage.
        url = getContextBaseUrl() + "/{storageId}";
        givenCustom().contentType(MediaType.TEXT_PLAIN_VALUE)
                     .accept(MediaType.TEXT_PLAIN_VALUE)
                     .param("force", true)
                     .when()
                     .delete(url, storageForm.getId())
                     .prettyPeek()
                     .then()
                     .statusCode(OK)
                     .body(containsString(SUCCESSFUL_STORAGE_REMOVAL));

        assertFalse(Files.exists(artifactPath));
    }

    private byte[] createZipFile()
            throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(os))
        {
            ZipEntry entry = new ZipEntry("dummy-file.txt");

            zos.putNextEntry(entry);
            zos.write("this is a test file".getBytes());
            zos.closeEntry();
        }

        return os.toByteArray();
    }

}
