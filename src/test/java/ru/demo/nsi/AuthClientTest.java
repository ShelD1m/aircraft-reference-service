package ru.demo.nsi;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import ru.demo.nsi.integration.auth.AuthClient;
import ru.demo.nsi.api.ApiException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.http.client.ClientHttpRequestFactory;

class AuthClientTest {
    @Test void matchesAuthMainCamelCaseAndServiceCredential() {
        var builder=spy(RestClient.builder());
        var server=MockRestServiceServer.bindTo(builder).build();
        doReturn(builder).when(builder).requestFactory(any(ClientHttpRequestFactory.class));
        var client=new AuthClient(builder,"http://auth","service-secret");
        String id="7ca85f64-5717-4562-b3fc-2c963f66afa7";
        server.expect(requestTo("http://auth/api/auth/verify"))
            .andExpect(header("Authorization","Bearer service-secret"))
            .andExpect(content().json("{\"token\":\"user-token\"}"))
            .andRespond(withSuccess("{\"userId\":\""+id+"\",\"role\":\"ADMIN\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://auth/api/auth/permissions/check"))
            .andExpect(content().json("{\"userId\":\""+id+"\",\"permission\":\"reference.read\"}"))
            .andRespond(withSuccess("{\"allowed\":true}",MediaType.APPLICATION_JSON));
        assertEquals(id,client.verifyBearer("Bearer user-token").userId());
        assertTrue(client.hasPermission(id,"reference.read")); server.verify();
    }
    @Test void mapsInvalidTokenAndDoesNotFallbackOnAuthFailure() {
        var builder=spy(RestClient.builder());
        var server=MockRestServiceServer.bindTo(builder).build();
        doReturn(builder).when(builder).requestFactory(any(ClientHttpRequestFactory.class));
        var client=new AuthClient(builder,"http://auth","");
        server.expect(requestTo("http://auth/api/auth/verify")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ApiException.class,()->client.verifyBearer("Bearer bad")).status);
        server.verify();
    }
}
