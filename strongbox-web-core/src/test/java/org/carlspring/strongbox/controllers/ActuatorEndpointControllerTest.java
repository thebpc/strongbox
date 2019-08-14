package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author: adavid9
 */
@IntegrationTest
public class ActuatorEndpointControllerTest
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
        setContextBaseUrl("/api/monitoring");
    }

    @Test
    @WithUserDetails("admin")
    public void testStrongboxInfo()
    {

        String url = getContextBaseUrl() + "/info";

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("strongbox", notNullValue());

        String version = propertiesBooter.getStrongboxVersion();

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("strongbox.version", equalTo(version));

        String revision = propertiesBooter.getStrongboxRevision();

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("strongbox.revision", equalTo(revision));
    }
}
