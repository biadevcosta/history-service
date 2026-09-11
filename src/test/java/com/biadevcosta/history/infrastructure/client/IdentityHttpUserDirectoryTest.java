package com.biadevcosta.history.infrastructure.client;

import com.biadevcosta.history.infrastructure.config.IdentityClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IdentityHttpUserDirectoryTest {

    private MockRestServiceServer server;
    private IdentityHttpUserDirectory directory;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        directory = new IdentityHttpUserDirectory(builder, new IdentityClientProperties("http://identity:8080"));
    }

    @Test
    void findUserName_mapsIdentityResponseToName() {
        server.expect(requestTo("http://identity:8080/users/pat-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        { "id": "pat-1", "name": "John Doe", "email": "john@example.com", "role": "PATIENT" }
                        """, MediaType.APPLICATION_JSON));

        Optional<String> name = directory.findUserName("pat-1");

        assertThat(name).contains("John Doe");
        server.verify();
    }

    @Test
    void findUserName_whenIdentityReturns404_returnsEmpty() {
        server.expect(requestTo("http://identity:8080/users/ghost"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<String> name = directory.findUserName("ghost");

        assertThat(name).isEmpty();
        server.verify();
    }
}
