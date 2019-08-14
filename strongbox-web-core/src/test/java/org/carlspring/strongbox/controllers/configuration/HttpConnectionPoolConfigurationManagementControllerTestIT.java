package org.carlspring.strongbox.controllers.configuration;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryData;

import javax.inject.Inject;
import java.util.Optional;

import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.pool.PoolStats;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Pablo Tirado
 */
@IntegrationTest
@Execution(CONCURRENT)
public class HttpConnectionPoolConfigurationManagementControllerTestIT
        extends RestAssuredBaseTest
{

    @Inject
    private ConfigurationManager configurationManager;


    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();

        setContextBaseUrl("/api/configuration/proxy/connection-pool");
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE })
    void testSetAndGetMaxNumberOfConnectionsForProxyRepository(String acceptHeader)
    {

        int newMaxNumberOfConnections = 200;

        String url = getContextBaseUrl() + "/max/{numberOfConnections}";

        ValidatableMockMvcResponse response = given().accept(acceptHeader)
                                                     .when()
                                                     .put(url, newMaxNumberOfConnections)
                                                     .peek()
                                                     .then()
                                                     .statusCode(HttpStatus.OK.value());

        String message = "Max number of connections for proxy repository was updated successfully.";
        validateResponseBody(response, acceptHeader, message);

        url = getContextBaseUrl();

        response = given().accept(acceptHeader)
                          .when()
                          .get(url)
                          .peek()
                          .then()
                          .statusCode(HttpStatus.OK.value());

        validateResponseBodyConnections(response, acceptHeader, newMaxNumberOfConnections);
    }

    @Test
    public void testSetAndGetMaxNumberOfConnectionsForProxyRepositoryWithJsonAcceptHeader()
    {
        int newMaxNumberOfConnections = 200;

        String url = getContextBaseUrl() + "/max/{numberOfConnections}";

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .put(url, newMaxNumberOfConnections)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("message", equalTo("Max number of connections for proxy repository was updated successfully."));

        url = getContextBaseUrl();

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("numberOfConnections", equalTo(newMaxNumberOfConnections));
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE })
    void testSetAndGetDefaultNumberOfConnectionsForProxyRepository(String acceptHeader)
    {
        int newDefaultNumberOfConnections = 5;

        String url = getContextBaseUrl() + "/default/{numberOfConnections}";

        ValidatableMockMvcResponse response = given().accept(acceptHeader)
                                                     .when()
                                                     .put(url, newDefaultNumberOfConnections)
                                                     .peek()
                                                     .then()
                                                     .statusCode(HttpStatus.OK.value());

        String message = "Default number of connections for proxy repository was updated successfully.";
        validateResponseBody(response, acceptHeader, message);

        url = getContextBaseUrl() + "/default-number";

        response = given().accept(acceptHeader)
                          .when()
                          .get(url)
                          .peek()
                          .then()
                          .statusCode(HttpStatus.OK.value());

        validateResponseBodyConnections(response, acceptHeader, newDefaultNumberOfConnections);
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE })
    void testSetAndGetNumberOfConnectionsForProxyRepositoryWithTextAcceptHeader(String acceptHeader)
    {
        Configuration configuration = configurationManager.getConfiguration();
        Optional<Repository> repositoryOpt = configuration.getStorages()
                                                          .values()
                                                          .stream()
                                                          .filter(stg -> MapUtils.isNotEmpty(stg.getRepositories()))
                                                          .flatMap(stg -> stg.getRepositories().values().stream())
                                                          .map(r -> (RepositoryData)r)
                                                          .filter(repository ->
                                                                          repository.getRemoteRepository() != null &&
                                                                          repository.getRemoteRepository().getUrl() !=
                                                                          null)
                                                          .map(r -> (Repository) r)
                                                          .findAny();

        Assumptions.assumeTrue(repositoryOpt.isPresent());
        Repository repository = repositoryOpt.get();
        String storageId = repository.getStorage().getId();
        String repositoryId = repository.getId();
        int numberOfConnections = 5;

        String url = getContextBaseUrl() + "/{storageId}/{respositoryId}/{numberOfConnections}";

        given().accept(MediaType.TEXT_PLAIN_VALUE)
               .when()
               .put(url, storageId, repositoryId, numberOfConnections)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(equalTo("Number of pool connections for repository was updated successfully."));

        ValidatableMockMvcResponse response = given().accept(acceptHeader)
                                                     .when()
                                                     .put(url, storageId, repositoryId, numberOfConnections)
                                                     .peek()
                                                     .then()
                                                     .statusCode(HttpStatus.OK.value());

        String message = "Number of pool connections for repository was updated successfully.";
        validateResponseBody(response, acceptHeader, message);

        url = getContextBaseUrl() + "/{storageId}/{respositoryId}";

        PoolStats expectedPoolStats = new PoolStats(0, 0, 0, numberOfConnections);
        given().accept(MediaType.TEXT_PLAIN_VALUE)
               .when()
               .get(url, storageId, repositoryId)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(containsString("max: " + expectedPoolStats.getMax()));

        response = given().accept(acceptHeader)
                          .when()
                          .get(url, storageId, repositoryId)
                          .peek()
                          .then()
                          .statusCode(HttpStatus.OK.value());

        validateResponseBodyPoolStats(acceptHeader, response, expectedPoolStats);
    }

    private void validateResponseBodyConnections(ValidatableMockMvcResponse response,
                                                 String acceptHeader,
                                                 int newMaxNumberOfConnections)
    {
        if (acceptHeader.equals(MediaType.APPLICATION_JSON_VALUE))
        {
            response.body("numberOfConnections", equalTo(newMaxNumberOfConnections));
        }
        else if (acceptHeader.equals(MediaType.TEXT_PLAIN_VALUE))
        {
            response.body(equalTo(String.valueOf(newMaxNumberOfConnections)));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported content type: " + acceptHeader);
        }
    }

    private void validateResponseBody(ValidatableMockMvcResponse response,
                                      String acceptHeader,
                                      String message)
    {
        if (acceptHeader.equals(MediaType.APPLICATION_JSON_VALUE))
        {
            response.body("message", equalTo(message));
        }
        else if (acceptHeader.equals(MediaType.TEXT_PLAIN_VALUE))
        {
            response.body(equalTo(message));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported content type: " + acceptHeader);
        }
    }

    private void validateResponseBodyPoolStats(String acceptHeader,
                                               ValidatableMockMvcResponse response,
                                               PoolStats expectedPoolStats)
    {
        if (acceptHeader.equals(MediaType.APPLICATION_JSON_VALUE))
        {
            response.body("max", equalTo(expectedPoolStats.getMax()));
        }
        else if (acceptHeader.equals(MediaType.TEXT_PLAIN_VALUE))
        {
            response.body(containsString("max: " + expectedPoolStats.getMax()));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported content type: " + acceptHeader);
        }
    }
}
