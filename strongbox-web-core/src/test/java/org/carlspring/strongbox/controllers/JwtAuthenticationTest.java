package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;
import org.carlspring.strongbox.users.security.SecurityTokenProvider;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.jose4j.jwt.NumericDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author adavid9
 * @author Pablo Tirado
 */
@IntegrationTest
public class JwtAuthenticationTest
        extends RestAssuredBaseTest
{

    private static final String UNAUTHORIZED_MESSAGE_CODE = "ExceptionTranslationFilter.insufficientAuthentication";

    private final MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Inject
    private SecurityTokenProvider securityTokenProvider;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();

        setContextBaseUrl(getContextBaseUrl() + "/api/users");
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testJWTAuthShouldPassWithToken()
            throws Exception
    {
        String url = getContextBaseUrl();

        String basicAuth = "Basic YWRtaW46cGFzc3dvcmQ=";

        String body = given().header(HttpHeaders.AUTHORIZATION, basicAuth)
                             .accept(MediaType.APPLICATION_JSON_VALUE)
                             .when()
                             .get(getContextBaseUrl() + "/login")
                             .then()
                             .statusCode(HttpStatus.OK.value())
                             .extract()
                             .asString();
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body(notNullValue());
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();

        // this token will expire after 1 hour
        String tokenValue = getTokenValue(body);
        given().header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(tokenValue))
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(notNullValue());
    }

    @Test
    public void testJWTAuthShouldFailWithoutToken()
    {
        String defaultErrorMessage = messages.getMessage(UNAUTHORIZED_MESSAGE_CODE,
                                                         Locale.ENGLISH);

        String errorMessage = messages.getMessage(UNAUTHORIZED_MESSAGE_CODE,
                                                  defaultErrorMessage);

        String decodedErrorMessage = new String(errorMessage.getBytes(ISO_8859_1),
                                                Charset.defaultCharset());

        String url = getContextBaseUrl();

        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", equalTo(decodedErrorMessage));
    }

    @Test
    public void testJWTInvalidToken()
    {
        String url = getContextBaseUrl();

        String invalidToken = "ABCD";

        given().header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(invalidToken))
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", equalTo("invalid.token"));
    }

    @Test
    public void testJWTExpirationToken()
            throws Exception
    {
        String url = getContextBaseUrl();

        // create token that will expire after 1 second
        String expiredToken = securityTokenProvider.getToken("admin", Collections.emptyMap(), 3, null);

        given().header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(expiredToken))
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(notNullValue());

        TimeUnit.SECONDS.sleep(3);

        given().header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(expiredToken))
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", equalTo("expired"));
    }

    @Test
    public void testJWTIssuedAtFuture()
            throws Exception
    {
        String url = getContextBaseUrl();

        NumericDate futureNumericDate = NumericDate.now();
        // add five minutes to the current time to create a JWT issued in the future
        futureNumericDate.addSeconds(300);

        String token = securityTokenProvider.getToken("admin", Collections.emptyMap(), 10, futureNumericDate);

        given().header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(token))
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", equalTo("invalid.token"));
    }

    private String getTokenValue(String body)
            throws JSONException
    {
        JSONObject extractToken = new JSONObject(body);
        return extractToken.getString("token");
    }

    private String getAuthorizationHeader(String tokenValue)
    {
        return String.format("Bearer %s", tokenValue);
    }

}
