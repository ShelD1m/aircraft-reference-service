package ru.demo.nsi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import ru.demo.nsi.config.ActorContext;
import ru.demo.nsi.integration.auth.AuthClient;
import ru.demo.nsi.api.ApiException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActorContextTest {
    @Test @SuppressWarnings("unchecked") void authPermissionsAreAuthoritativeAndDemoHeadersAreIgnored() {
        var request=new MockHttpServletRequest();
        request.addHeader("Authorization","Bearer user"); request.addHeader("X-Demo-Role","SUPERVISOR");
        AuthClient auth=mock(AuthClient.class); ObjectProvider<AuthClient> provider=mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(auth);
        when(auth.verifyBearer("Bearer user")).thenReturn(new AuthClient.Identity("7ca85f64-5717-4562-b3fc-2c963f66afa7","ADMIN"));
        when(auth.hasPermission(anyString(),eq("reference.record.write"))).thenReturn(true);
        var actor=new ActorContext(request,provider,true);
        actor.editor(); actor.editor();
        verify(auth,times(1)).verifyBearer("Bearer user");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,actor::approveVersion).status);
        assertEquals("7ca85f64-5717-4562-b3fc-2c963f66afa7",actor.actorId());
    }
    @Test @SuppressWarnings("unchecked") void unavailableAuthNeverFallsBackToDemoSupervisor() {
        var request=new MockHttpServletRequest(); request.addHeader("X-Demo-Role","SUPERVISOR");
        AuthClient auth=mock(AuthClient.class); ObjectProvider<AuthClient> provider=mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(auth);
        when(auth.verifyBearer(null)).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED,"Missing Bearer"));
        var actor=new ActorContext(request,provider,true);
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ApiException.class,actor::editor).status);
        verify(auth,never()).hasPermission(anyString(),anyString());
    }
}
