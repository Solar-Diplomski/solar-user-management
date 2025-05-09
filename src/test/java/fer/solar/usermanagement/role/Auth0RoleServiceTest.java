package fer.solar.usermanagement.role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.RolesEntity;
import com.auth0.client.mgmt.filter.PageFilter;
import com.auth0.json.mgmt.permissions.Permission;
import com.auth0.json.mgmt.permissions.PermissionsPage;
import com.auth0.json.mgmt.roles.Role;

import fer.solar.usermanagement.config.Auth0Config;
import fer.solar.usermanagement.role.dto.CreateRoleRequest;
import fer.solar.usermanagement.role.dto.RoleResponse;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class Auth0RoleServiceTest {

    @Mock
    private Auth0Config auth0Config;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ManagementAPI managementAPI;

    @InjectMocks
    private Auth0RoleService auth0RoleService;

    @Mock
    private Role mockResponseRole;

    @Captor
    private ArgumentCaptor<Role> roleArgumentCaptor;

    @Test
    void createRole_should_createRole_when_apiCallSucceeds() throws Exception {
        String roleId = "role_new_123";
        String roleName = "New Test Role";
        String roleDescription = "A new role for testing without initial permissions";

        CreateRoleRequest createRequest = new CreateRoleRequest();
        createRequest.setName(roleName);
        createRequest.setDescription(roleDescription);

        when(mockResponseRole.getId()).thenReturn(roleId);
        when(mockResponseRole.getName()).thenReturn(roleName);
        when(mockResponseRole.getDescription()).thenReturn(roleDescription);

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);

        RolesEntity rolesEntityMockInstance = managementAPI.roles();

        when(rolesEntityMockInstance.create(roleArgumentCaptor.capture()).execute().getBody()).thenReturn(mockResponseRole);

        when(rolesEntityMockInstance.get(roleId).execute().getBody()).thenReturn(mockResponseRole);

        PermissionsPage permissionsPageMock = mock(PermissionsPage.class);
        when(permissionsPageMock.getItems()).thenReturn(Collections.<Permission>emptyList());
        when(rolesEntityMockInstance.listPermissions(eq(roleId), any(PageFilter.class)).execute().getBody()).thenReturn(permissionsPageMock);

        StepVerifier.create(auth0RoleService.createRole(createRequest))
                .expectNextMatches(response -> {
                    assertThat(response.getId()).isEqualTo(roleId);
                    assertThat(response.getName()).isEqualTo(roleName);
                    assertThat(response.getDescription()).isEqualTo(roleDescription);
                    assertThat(response.getPermissions()).isNotNull();
                    assertThat(response.getPermissions()).isEmpty();
                    return true;
                })
                .verifyComplete();

        Role capturedRole = roleArgumentCaptor.getValue();
        assertThat(capturedRole.getName()).isEqualTo(roleName);
        assertThat(capturedRole.getDescription()).isEqualTo(roleDescription);

        verify(rolesEntityMockInstance).create(any(Role.class));
        verify(rolesEntityMockInstance).listPermissions(eq(roleId), any(PageFilter.class));
    }

    @Test
    void createRole_should_throwRuntimeException_when_apiCallFails() {

    }

    @Test
    void listRoles_should_returnSortedPaginatedRoleResponse_when_apiCallSucceeds() {

    }

    @Test
    void listRoles_should_throwRuntimeException_when_apiCallFails() {

    }

    @Test
    void getRoleById_should_returnRoleResponse_when_apiCallSucceeds() {

    }

    @Test
    void getRoleById_should_throwRuntimeException_when_apiCallFails() {

    }

    @Test
    void updateRole_should_throwRuntimeException_when_apiCallFailsOnAddingPermissions() {

    }

    @Test
    void updateRole_should_throwRuntimeException_when_apiCallFailsOnRemovingPermissions() {

    }

    @Test
    void updateRole_should_returnRoleResponse_when_apiCallsSucceed() {

    }

    @Test
    void deleteRole_should_throwRuntimeException_when_apiCallFails() {

    }

    @Test
    void deleteRole_should_returnVoid_when_apiCallSucceeds() {  

    }

}
