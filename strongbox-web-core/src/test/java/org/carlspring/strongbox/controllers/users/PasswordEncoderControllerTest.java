package org.carlspring.strongbox.controllers.users;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Przemyslaw Fusik
 */
@IntegrationTest
@Execution(CONCURRENT)
public class PasswordEncoderControllerTest
        extends RestAssuredBaseTest
{

    @Inject
    PasswordEncoder passwordEncoder;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();

        setContextBaseUrl("/api/users/password-encoder");
    }

    @Test
    void shouldEncodeProperly()
    {
        String url = getContextBaseUrl() + "/password";
        final String encodedPassword = given().when()
                                              .get(url)
                                              .peek()
                                              .then()
                                              .statusCode(HttpStatus.OK.value())
                                              .extract()
                                              .asString();

        assertTrue(passwordEncoder.matches("password", encodedPassword));
    }

}
