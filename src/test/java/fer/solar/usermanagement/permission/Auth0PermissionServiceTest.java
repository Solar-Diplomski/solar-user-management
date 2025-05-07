package fer.solar.usermanagement.permission;

import fer.solar.usermanagement.config.Auth0Config;
import fer.solar.usermanagement.permission.dto.PaginatedPermissionResponse;
import fer.solar.usermanagement.permission.dto.PermissionResponse;
import fer.solar.usermanagement.permission.dto.UpdatePermissionsRequest;
import fer.solar.usermanagement.permission.dto.UpdatePermissionsRequest.PermissionUpdate;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.ResourceServerEntity;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.resourceserver.ResourceServer;
import com.auth0.json.mgmt.resourceserver.Scope;
import com.auth0.net.Request;
import com.auth0.net.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


@ExtendWith(MockitoExtension.class)
public class Auth0PermissionServiceTest {

    private static final String API_IDENTIFIER = "test-api-identifier";

    @Mock
    private Auth0Config auth0Config;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ManagementAPI mockManagementAPI;

    @Mock
    private ResourceServerEntity mockResourceServerEntity;

    @Mock
    private Request<ResourceServer> mockResourceServerRequest;

    @Mock
    private Response<ResourceServer> mockResourceServerResponse;

    @InjectMocks
    private Auth0PermissionService auth0PermissionService;

    @Test
    void listPermissions_should_returnSortedPaginatedPermissionResponse_when_allApiCallsSucceed() throws Exception {
        when(auth0Config.getRefreshedManagementAPI()).thenReturn(mockManagementAPI);
        when(auth0Config.getApiGatewayIdentifier()).thenReturn(API_IDENTIFIER);

        Scope scopeWrite = new Scope("write:data");
        scopeWrite.setDescription("Write data permission");
        Scope scopeRead = new Scope("read:data");
        scopeRead.setDescription("Read data permission");
        
        List<Scope> unsortedScopes = Arrays.asList(scopeWrite, scopeRead); 

        ResourceServer resourceServer = new ResourceServer();
        resourceServer.setScopes(unsortedScopes);

        when(mockManagementAPI.resourceServers().get(API_IDENTIFIER).execute().getBody()).thenReturn(resourceServer);

        Mono<PaginatedPermissionResponse> result = auth0PermissionService.listPermissions();

        StepVerifier.create(result)
                .expectNextMatches(paginatedResponse -> {
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getCurrentPage()).isEqualTo(0);
                    assertThat(paginatedResponse.getTotalElements()).isEqualTo(2);
                    assertThat(paginatedResponse.getPageSize()).isEqualTo(2);
                    assertThat(paginatedResponse.getTotalPages()).isEqualTo(1);
                    assertThat(paginatedResponse.getContent()).hasSize(2);

                    PermissionResponse firstPermission = paginatedResponse.getContent().get(0);
                    assertThat(firstPermission.getPermissionName()).isEqualTo("read:data");
                    assertThat(firstPermission.getDescription()).isEqualTo("Read data permission");

                    PermissionResponse secondPermission = paginatedResponse.getContent().get(1);
                    assertThat(secondPermission.getPermissionName()).isEqualTo("write:data");
                    assertThat(secondPermission.getDescription()).isEqualTo("Write data permission");

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void listPermissions_should_returnEmptyPaginatedPermissionResponse_when_apiThrowsException() throws Exception {
        when(auth0Config.getRefreshedManagementAPI()).thenReturn(mockManagementAPI);
        when(auth0Config.getApiGatewayIdentifier()).thenReturn(API_IDENTIFIER);

        com.auth0.exception.Auth0Exception auth0Exception = new com.auth0.exception.Auth0Exception("API call failed");
        when(mockManagementAPI.resourceServers().get(API_IDENTIFIER).execute()).thenThrow(auth0Exception);

        Mono<PaginatedPermissionResponse> result = auth0PermissionService.listPermissions();

        StepVerifier.create(result)
            .expectNextMatches(paginatedResponse -> {
                assertThat(paginatedResponse).isNotNull();
                assertThat(paginatedResponse.getContent()).isEmpty();
                assertThat(paginatedResponse.getCurrentPage()).isEqualTo(0);
                assertThat(paginatedResponse.getPageSize()).isEqualTo(0);
                assertThat(paginatedResponse.getTotalElements()).isEqualTo(0);
                assertThat(paginatedResponse.getTotalPages()).isEqualTo(0);
                return true;
            })
            .verifyComplete();
    }

    @Test
    void listPermissions_should_returnEmptyPaginatedPermissionResponse_when_noPermissionsFound() throws Exception {
        when(auth0Config.getRefreshedManagementAPI()).thenReturn(mockManagementAPI);
        when(auth0Config.getApiGatewayIdentifier()).thenReturn(API_IDENTIFIER);

        ResourceServer resourceServerWithNoScopes = new ResourceServer();
        resourceServerWithNoScopes.setScopes(java.util.Collections.emptyList());

        when(mockManagementAPI.resourceServers().get(API_IDENTIFIER).execute().getBody()).thenReturn(resourceServerWithNoScopes);

        Mono<PaginatedPermissionResponse> result = auth0PermissionService.listPermissions();

        StepVerifier.create(result)
            .expectNextMatches(paginatedResponse -> {
                assertThat(paginatedResponse).isNotNull();
                assertThat(paginatedResponse.getContent()).isEmpty();
                assertThat(paginatedResponse.getCurrentPage()).isEqualTo(0);
                assertThat(paginatedResponse.getPageSize()).isEqualTo(0);
                assertThat(paginatedResponse.getTotalElements()).isEqualTo(0);
                assertThat(paginatedResponse.getTotalPages()).isEqualTo(0);
                return true;
            })
            .verifyComplete();
    }

    @Test
    void updatePermissions_should_updateResourceServer_when_requestIsValid() throws Exception {
        when(auth0Config.getRefreshedManagementAPI()).thenReturn(mockManagementAPI);
        when(auth0Config.getApiGatewayIdentifier()).thenReturn(API_IDENTIFIER);
        when(mockManagementAPI.resourceServers()).thenReturn(mockResourceServerEntity);
        when(mockResourceServerEntity.update(eq(API_IDENTIFIER), any(ResourceServer.class))).thenReturn(mockResourceServerRequest);
        when(mockResourceServerRequest.execute()).thenReturn(mockResourceServerResponse);

        PermissionUpdate permDto1 = new PermissionUpdate();
        permDto1.setPermissionName("edit:config");
        permDto1.setDescription("Edit configuration");

        PermissionUpdate permDto2 = new PermissionUpdate();
        permDto2.setPermissionName("view:logs");
        permDto2.setDescription("View system logs");

        List<PermissionUpdate> requestedPermissionUpdates = Arrays.asList(permDto1, permDto2);

        UpdatePermissionsRequest updateRequest = new UpdatePermissionsRequest();
        updateRequest.setPermissions(requestedPermissionUpdates);

        ArgumentCaptor<ResourceServer> resourceServerCaptor = ArgumentCaptor.forClass(ResourceServer.class);

        Mono<Void> result = auth0PermissionService.updatePermissions(updateRequest);

        StepVerifier.create(result)
            .verifyComplete();

        verify(mockResourceServerEntity).update(eq(API_IDENTIFIER), resourceServerCaptor.capture());

        ResourceServer capturedResourceServer = resourceServerCaptor.getValue();
        assertThat(capturedResourceServer).isNotNull();
        assertThat(capturedResourceServer.getScopes()).hasSize(2);

        assertThat(capturedResourceServer.getScopes())
            .extracting(Scope::getValue, Scope::getDescription)
            .containsExactlyInAnyOrder(
                tuple(permDto1.getPermissionName(), permDto1.getDescription()),
                tuple(permDto2.getPermissionName(), permDto2.getDescription())
            );
    }

    @Test
    void updatePermissions_should_throwRuntimeException_when_apiThrowsException() throws Exception {
        when(auth0Config.getRefreshedManagementAPI()).thenReturn(mockManagementAPI);
        when(auth0Config.getApiGatewayIdentifier()).thenReturn(API_IDENTIFIER);
        when(mockManagementAPI.resourceServers()).thenReturn(mockResourceServerEntity);
        when(mockResourceServerEntity.update(eq(API_IDENTIFIER), any(ResourceServer.class))).thenReturn(mockResourceServerRequest);

        Auth0Exception auth0Exception = new Auth0Exception("API call failed during update");
        when(mockResourceServerRequest.execute()).thenThrow(auth0Exception);

        UpdatePermissionsRequest updateRequest = new UpdatePermissionsRequest();
        updateRequest.setPermissions(java.util.Collections.emptyList()); 

        Mono<Void> result = auth0PermissionService.updatePermissions(updateRequest);

        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }
    
}
