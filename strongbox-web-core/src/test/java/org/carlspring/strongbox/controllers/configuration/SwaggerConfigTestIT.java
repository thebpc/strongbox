package org.carlspring.strongbox.controllers.configuration;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

@IntegrationTest
@Execution(CONCURRENT)
public class SwaggerConfigTestIT extends RestAssuredBaseTest
{

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
    }

    @Test
    public void testSwaggerUIStaticResources()
    {
        String url = getContextBaseUrl() + "/docs/rest/api.html";

        given().accept(MediaType.TEXT_HTML_VALUE)
               .when()
               .get(url)
               .peek() // Use peek() to print the output
               .then()
               .statusCode(HttpStatus.OK.value()) // check http status code
               .body(containsString("Swagger"));
    }

    @Test
    public void testSwaggerOpenAPIEndpoint()
    {
        String url = getContextBaseUrl() + "/v2/api-docs";

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek() // Use peek() to print the output
               .then()
               .statusCode(HttpStatus.OK.value()) // check http status code
               .body(containsString("carlspring"));
    }

}
