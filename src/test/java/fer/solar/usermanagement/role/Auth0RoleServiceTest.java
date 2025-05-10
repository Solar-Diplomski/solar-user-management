package fer.solar.usermanagement.role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Arrays;

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
import com.auth0.client.mgmt.filter.RolesFilter;
import com.auth0.json.mgmt.permissions.Permission;
import com.auth0.json.mgmt.permissions.PermissionsPage;
import com.auth0.json.mgmt.roles.Role;
import com.auth0.json.mgmt.roles.RolesPage;

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
    void createRole_should_throwRuntimeException_when_apiCallFails() throws Exception {
        String roleName = "Failing Test Role";
        String roleDescription = "A role that will fail to be created";

        CreateRoleRequest createRequest = new CreateRoleRequest();
        createRequest.setName(roleName);
        createRequest.setDescription(roleDescription);

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);
        RolesEntity rolesEntityMockInstance = managementAPI.roles();

        when(rolesEntityMockInstance.create(any(Role.class)).execute())
            .thenThrow(new com.auth0.exception.Auth0Exception("Failed to create role"));

        StepVerifier.create(auth0RoleService.createRole(createRequest))
                .expectErrorMatches(throwable -> {
                    assertThat(throwable)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Failed to create role in Auth0");
                    assertThat(throwable.getCause())
                        .isInstanceOf(com.auth0.exception.Auth0Exception.class)
                        .hasMessage("Failed to create role");
                    return true;
                })
                .verify();

        verify(rolesEntityMockInstance).create(any(Role.class));
        verify(auth0Config).getRefreshedManagementAPI();
    }

    @Test
    void listRoles_should_returnSortedPaginatedRoleResponse_when_apiCallSucceeds() throws Exception {
        int page = 0;
        int size = 2;
        int totalElements = 3;
        int totalPages = 2;

        Role role1 = mock(Role.class);
        when(role1.getId()).thenReturn("role_1");
        when(role1.getName()).thenReturn("Zebra Role");
        when(role1.getDescription()).thenReturn("Description 1");

        Role role2 = mock(Role.class);
        when(role2.getId()).thenReturn("role_2");
        when(role2.getName()).thenReturn("Alpha Role");
        when(role2.getDescription()).thenReturn("Description 2");

        Permission perm1 = new Permission();
        perm1.setName("read:data");
        Permission perm2 = new Permission();
        perm2.setName("write:data");

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);
        RolesEntity rolesEntityMockInstance = managementAPI.roles();

        RolesPage rolesPage = mock(RolesPage.class);
        when(rolesPage.getItems()).thenReturn(Arrays.asList(role1, role2));
        when(rolesPage.getTotal()).thenReturn(totalElements);
        when(rolesPage.getLimit()).thenReturn(size);
        when(rolesPage.getStart()).thenReturn(page * size);

        PermissionsPage permissionsPage1 = mock(PermissionsPage.class);
        when(permissionsPage1.getItems()).thenReturn(Collections.singletonList(perm1));
        PermissionsPage permissionsPage2 = mock(PermissionsPage.class);
        when(permissionsPage2.getItems()).thenReturn(Arrays.asList(perm1, perm2));

        when(rolesEntityMockInstance.list(any(RolesFilter.class)).execute().getBody())
            .thenReturn(rolesPage);
        when(rolesEntityMockInstance.listPermissions(eq("role_1"), any(PageFilter.class)).execute().getBody())
            .thenReturn(permissionsPage1);
        when(rolesEntityMockInstance.listPermissions(eq("role_2"), any(PageFilter.class)).execute().getBody())
            .thenReturn(permissionsPage2);

        StepVerifier.create(auth0RoleService.listRoles(page, size))
                .expectNextMatches(response -> {
                    assertThat(response.getCurrentPage()).isEqualTo(page);
                    assertThat(response.getPageSize()).isEqualTo(size);
                    assertThat(response.getTotalElements()).isEqualTo(totalElements);
                    assertThat(response.getTotalPages()).isEqualTo(totalPages);

                    assertThat(response.getContent()).hasSize(2);
                    
                    assertThat(response.getContent().get(0).getName()).isEqualTo("Alpha Role");
                    assertThat(response.getContent().get(1).getName()).isEqualTo("Zebra Role");

                    RoleResponse firstRole = response.getContent().get(0);
                    assertThat(firstRole.getId()).isEqualTo("role_2");
                    assertThat(firstRole.getName()).isEqualTo("Alpha Role");
                    assertThat(firstRole.getDescription()).isEqualTo("Description 2");
                    assertThat(firstRole.getPermissions()).containsExactlyInAnyOrder("read:data", "write:data");

                    RoleResponse secondRole = response.getContent().get(1);
                    assertThat(secondRole.getId()).isEqualTo("role_1");
                    assertThat(secondRole.getName()).isEqualTo("Zebra Role");
                    assertThat(secondRole.getDescription()).isEqualTo("Description 1");
                    assertThat(secondRole.getPermissions()).containsExactly("read:data");

                    return true;
                })
                .verifyComplete();

        verify(rolesEntityMockInstance).list(any(RolesFilter.class));
        verify(rolesEntityMockInstance).listPermissions(eq("role_1"), any(PageFilter.class));
        verify(rolesEntityMockInstance).listPermissions(eq("role_2"), any(PageFilter.class));
        verify(auth0Config, times(3)).getRefreshedManagementAPI();
    }

    @Test
    void listRoles_should_throwRuntimeException_when_apiCallFails() throws Exception {
        int page = 0;
        int size = 10;

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);
        RolesEntity rolesEntityMockInstance = managementAPI.roles();

        when(rolesEntityMockInstance.list(any(RolesFilter.class)).execute())
            .thenThrow(new com.auth0.exception.Auth0Exception("Failed to list roles"));

        StepVerifier.create(auth0RoleService.listRoles(page, size))
                .expectErrorMatches(throwable -> {
                    assertThat(throwable)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Failed to list roles from Auth0");
                    assertThat(throwable.getCause())
                        .isInstanceOf(com.auth0.exception.Auth0Exception.class)
                        .hasMessage("Failed to list roles");
                    return true;
                })
                .verify();

        verify(rolesEntityMockInstance).list(any(RolesFilter.class));
        verify(auth0Config).getRefreshedManagementAPI();
    }

    @Test
    void getRoleById_should_returnRoleResponse_when_apiCallSucceeds() throws Exception {
        String roleId = "test_role_123";
        String roleName = "Test Role";
        String roleDescription = "A test role";

        Permission perm1 = new Permission();
        perm1.setName("read:data");
        Permission perm2 = new Permission();
        perm2.setName("write:data");

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);
        RolesEntity rolesEntityMockInstance = managementAPI.roles();

        Role mockRole = mock(Role.class);
        when(mockRole.getId()).thenReturn(roleId);
        when(mockRole.getName()).thenReturn(roleName);
        when(mockRole.getDescription()).thenReturn(roleDescription);

        PermissionsPage permissionsPage = mock(PermissionsPage.class);
        when(permissionsPage.getItems()).thenReturn(Arrays.asList(perm1, perm2));

        when(rolesEntityMockInstance.get(roleId).execute().getBody())
            .thenReturn(mockRole);
        when(rolesEntityMockInstance.listPermissions(eq(roleId), any(PageFilter.class)).execute().getBody())
            .thenReturn(permissionsPage);

        StepVerifier.create(auth0RoleService.getRoleById(roleId))
                .expectNextMatches(response -> {
                    assertThat(response.getId()).isEqualTo(roleId);
                    assertThat(response.getName()).isEqualTo(roleName);
                    assertThat(response.getDescription()).isEqualTo(roleDescription);
                    assertThat(response.getPermissions())
                        .containsExactlyInAnyOrder("read:data", "write:data");
                    return true;
                })
                .verifyComplete();

        verify(rolesEntityMockInstance).listPermissions(eq(roleId), any(PageFilter.class));
        verify(auth0Config, times(2)).getRefreshedManagementAPI();
    }

    @Test
    void getRoleById_should_throwRuntimeException_when_apiCallFails() throws Exception {
        String roleId = "test_role_123";

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);
        RolesEntity rolesEntityMockInstance = managementAPI.roles();

        when(rolesEntityMockInstance.get(roleId).execute())
            .thenThrow(new com.auth0.exception.Auth0Exception("Failed to get role"));

        StepVerifier.create(auth0RoleService.getRoleById(roleId))
                .expectErrorMatches(throwable -> {
                    assertThat(throwable)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Failed to get role " + roleId);
                    assertThat(throwable.getCause())
                        .isInstanceOf(com.auth0.exception.Auth0Exception.class)
                        .hasMessage("Failed to get role");
                    return true;
                })
                .verify();

        verify(auth0Config).getRefreshedManagementAPI();
    }

    @Test
    void deleteRole_should_throwRuntimeException_when_apiCallFails() throws Exception {
        String roleId = "test_role_123";

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);
        when(managementAPI.roles().delete(roleId).execute())
            .thenThrow(new com.auth0.exception.Auth0Exception("Failed to delete role"));

        StepVerifier.create(auth0RoleService.deleteRole(roleId))
                .expectErrorMatches(throwable -> {
                    assertThat(throwable)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Failed to delete role in Auth0");
                    assertThat(throwable.getCause())
                        .isInstanceOf(com.auth0.exception.Auth0Exception.class)
                        .hasMessage("Failed to delete role");
                    return true;
                })
                .verify();

        verify(auth0Config).getRefreshedManagementAPI();
    }

    @Test
    void deleteRole_should_returnVoid_when_apiCallSucceeds() throws Exception {
        String roleId = "test_role_123";

        when(auth0Config.getRefreshedManagementAPI()).thenReturn(managementAPI);
        when(managementAPI.roles().delete(roleId).execute()).thenReturn(null);

        StepVerifier.create(auth0RoleService.deleteRole(roleId))
                .verifyComplete();

        verify(auth0Config).getRefreshedManagementAPI();
    }

}
