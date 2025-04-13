package fer.solar.usermanagement.role;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.PageFilter;
import com.auth0.client.mgmt.filter.RolesFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.permissions.PermissionsPage;
import com.auth0.json.mgmt.roles.RolesPage;
import com.auth0.json.mgmt.permissions.Permission;
import com.auth0.json.mgmt.roles.Role;
import com.auth0.json.mgmt.resourceserver.Scope;
import fer.solar.usermanagement.config.Auth0Config;
import fer.solar.usermanagement.role.dto.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import fer.solar.usermanagement.common.util.SortingUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Auth0RoleService implements RoleService {

    private final Auth0Config auth0Config;

    @Override
    public Mono<RoleResponse> createRole(CreateRoleRequest request) {
        return Mono.fromCallable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    Role newRole = new Role();
                    newRole.setName(request.getName());
                    newRole.setDescription(request.getDescription());
                    try {
                        Role createdRole = mgmt.roles().create(newRole).execute().getBody();
                        log.info("Created Auth0 role: {}", createdRole.getId());
                        // Permissions must be added via the update endpoint.
                        // Use getRoleById to ensure consistent response structure including permissions (empty initially)
                        return getRoleById(createdRole.getId()).block(); // Blocking acceptable in fromCallable on boundedElastic
                    } catch (Auth0Exception e) {
                        log.error("Error creating Auth0 role with name {}: {}", request.getName(), e.getMessage(), e);
                        throw new RuntimeException("Failed to create role in Auth0", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PaginatedRoleResponse> listRoles(int page, int size) {
        return fetchRolesPageFromAuth0(page, size)
                .flatMap(this::buildPaginatedRoleResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<RolesPage> fetchRolesPageFromAuth0(int page, int size) {
        return Mono.fromCallable(() -> {
            ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
            RolesFilter filter = new RolesFilter().withPage(page, size).withTotals(true);
            try {
                return mgmt.roles().list(filter).execute().getBody();
            } catch (Auth0Exception e) {
                log.error("Error fetching roles page from Auth0 (page={}, size={}): {}", page, size, e.getMessage(), e);
                throw new RuntimeException("Failed to list roles from Auth0", e);
            }
        });
    }

    private Mono<PaginatedRoleResponse> buildPaginatedRoleResponse(RolesPage rolesPage) {
        Flux<RoleResponse> roleResponseFlux = Flux.fromIterable(rolesPage.getItems())
                .flatMap(role -> fetchPermissionsForRole(role.getId())
                        .flatMap(permissions -> mapRoleToResponse(role, permissions)));

        return roleResponseFlux.collectList().map(roleResponses -> {
            roleResponses.sort(SortingUtils.createNullsFirstCaseInsensitiveComparator(RoleResponse::getName));

            long totalElements = rolesPage.getTotal();
            int pageSize = rolesPage.getLimit() != null ? rolesPage.getLimit() : (roleResponses.isEmpty() ? 0 : roleResponses.size());
            int currentPage = rolesPage.getStart() != null ? rolesPage.getStart() / Math.max(1, pageSize) : 0;
            int totalPages = (pageSize > 0) ? (int) Math.ceil((double) totalElements / pageSize) : (totalElements > 0 ? 1 : 0);

            return PaginatedRoleResponse.builder()
                    .content(roleResponses)
                    .currentPage(currentPage)
                    .pageSize(pageSize)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .build();
        });
    }

    private Mono<List<Permission>> fetchPermissionsForRole(String roleId) {
        return Mono.fromCallable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    PageFilter filter = new PageFilter();
                    try {
                        PermissionsPage page = mgmt.roles().listPermissions(roleId, filter).execute().getBody();
                        // Ensure items are not null before returning
                        return page.getItems() != null ? page.getItems() : Collections.<Permission>emptyList();
                    } catch (Auth0Exception e) {
                        log.error("Error fetching permissions for role {}: {}", roleId, e.getMessage());
                        throw new RuntimeException("Failed to fetch permissions for role " + roleId, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(Collections.emptyList());
    }

    private Mono<RoleResponse> mapRoleToResponse(Role role, List<Permission> permissions) {
        List<String> permissionNames = permissions != null ?
                permissions.stream().map(Permission::getName).collect(Collectors.toList()) :
                Collections.emptyList();

        return Mono.just(RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionNames)
                .build());
    }

    @Override
    public Mono<RoleResponse> getRoleById(String roleId) {
        return Mono.fromCallable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    return mgmt.roles().get(roleId).execute().getBody();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(role -> fetchPermissionsForRole(roleId)
                    .flatMap(permissions -> mapRoleToResponse(role, permissions))
                )
                .onErrorMap(Auth0Exception.class, e -> new RuntimeException("Failed to get role " + roleId, e));
    }

    @Override
    public Mono<RoleResponse> updateRole(String roleId, UpdateRoleRequest request) {
        Mono<Role> updateDetailsMono = Mono.fromCallable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    Role roleUpdates = new Role();
                    if (request.getName() != null) {
                        roleUpdates.setName(request.getName());
                    }
                    if (request.getDescription() != null) {
                        roleUpdates.setDescription(request.getDescription());
                    }
                    if (request.getName() == null && request.getDescription() == null) {
                        return mgmt.roles().get(roleId).execute().getBody();
                    }
                    Role updatedRole = mgmt.roles().update(roleId, roleUpdates).execute().getBody();
                    log.info("Updated Auth0 role base details: {}", roleId);
                    return updatedRole;
                })
                .subscribeOn(Schedulers.boundedElastic());

        Mono<Void> updatePermissionsMono = Mono.defer(() -> {
            if (request.getPermissions() != null) {
                return assignPermissionsToRole(roleId, request.getPermissions());
            } else {
                return Mono.empty();
            }
        });

        return updateDetailsMono
                .flatMap(updatedRole -> updatePermissionsMono.thenReturn(updatedRole.getId()))
                .flatMap(this::getRoleById)
                .doOnError(e -> log.error("Error updating Auth0 role {}: {}", roleId, e.getMessage(), e))
                .onErrorMap(Auth0Exception.class, e -> new RuntimeException("Failed to update role in Auth0", e));
    }

    private Mono<Void> assignPermissionsToRole(String roleId, @NonNull List<String> requestedPermissionNames) {
        return fetchPermissionsForRole(roleId)
                .flatMap(currentPermissions -> fetchAllScopesForApi()
                        .flatMap(apiScopes -> {
                            List<String> currentPermissionNames = currentPermissions.stream()
                                    .map(Permission::getName)
                                    .collect(Collectors.toList());

                            List<String> namesToRemove = currentPermissionNames.stream()
                                    .filter(name -> !requestedPermissionNames.contains(name))
                                    .collect(Collectors.toList());

                            List<String> namesToAdd = requestedPermissionNames.stream()
                                    .filter(name -> !currentPermissionNames.contains(name))
                                    .collect(Collectors.toList());

                            // Map names back to Permission objects using apiScopes
                            String apiIdentifier = auth0Config.getApiGatewayIdentifier();
                            List<Permission> permissionsToRemove = mapNamesToPermissions(namesToRemove, apiScopes, apiIdentifier);
                            List<Permission> permissionsToAdd = mapNamesToPermissions(namesToAdd, apiScopes, apiIdentifier);

                            log.debug("Role {}: Current permissions: {}, Requested permissions: {}", roleId, currentPermissionNames, requestedPermissionNames);
                            if (!permissionsToRemove.isEmpty()) {
                                log.info("Role {}: Attempting to remove permissions: {}", roleId, namesToRemove);
                            }
                            if (!permissionsToAdd.isEmpty()) {
                                log.info("Role {}: Attempting to add permissions: {}", roleId, namesToAdd);
                            }

                            Mono<Void> removeMono = permissionsToRemove.isEmpty() ? Mono.empty() : removePermissionsFromRoleApiCall(roleId, permissionsToRemove);
                            Mono<Void> addMono = permissionsToAdd.isEmpty() ? Mono.empty() : addPermissionsToRoleApiCall(roleId, permissionsToAdd);

                            return removeMono.then(addMono);
                        }))
                .doOnError(e -> log.error("Failed to update permissions for role {}: {}", roleId, e.getMessage(), e))
                .then(); // Ensure the final result is Mono<Void>
    }

    private List<Permission> mapNamesToPermissions(List<String> names, List<Scope> allApiScopes, String apiIdentifier) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        return allApiScopes.stream()
                .filter(scope -> names.contains(scope.getValue()))
                .map(scope -> {
                    Permission p = new Permission();
                    p.setName(scope.getValue());
                    p.setResourceServerId(apiIdentifier); // Use the correct API identifier
                    // p.setResourceServerName(...); // Generally not needed if identifier is set
                    // p.setDescription(...); // Description is not part of the identifier
                    return p;
                })
                .collect(Collectors.toList());
    }

    private Mono<Void> removePermissionsFromRoleApiCall(String roleId, List<Permission> permissionsToRemove) {
        return Mono.fromRunnable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    try {
                        mgmt.roles().removePermissions(roleId, permissionsToRemove).execute();
                        log.info("Role {}: Successfully removed permissions: {}", roleId,
                                permissionsToRemove.stream().map(Permission::getName).collect(Collectors.toList()));
                    } catch (Auth0Exception e) {
                        log.error("Role {}: Failed to remove permissions {}: {}", roleId,
                                permissionsToRemove.stream().map(Permission::getName).collect(Collectors.toList()), e.getMessage());
                        throw new RuntimeException("Failed to remove permissions from role " + roleId, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Mono<Void> addPermissionsToRoleApiCall(String roleId, List<Permission> permissionsToAdd) {
         // Check for permissions that were requested but not found in API scopes
        // This check should ideally happen earlier, maybe in mapNamesToPermissions or before calling it
        List<String> requestedNames = permissionsToAdd.stream().map(Permission::getName).collect(Collectors.toList());
        List<String> addedNames = permissionsToAdd.stream().map(Permission::getName).collect(Collectors.toList()); // Assuming mapNamesToPermissions filters correctly
        if (permissionsToAdd.size() != requestedNames.size()) { // Simplified check, refine if mapNamesToPermissions doesn't filter
             log.warn("Role {}: Some requested permission names for addition were not found or mapped in the API scopes: Requested={}, Mapped={}",
                     roleId, requestedNames, addedNames);
             // Decide if this is an error or just a warning. Throwing exception might be safer.
             // For now, we proceed with adding the ones we could map.
         }

        if (permissionsToAdd.isEmpty()) {
            return Mono.empty(); // Nothing to add
        }

        return Mono.fromRunnable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    try {
                        mgmt.roles().addPermissions(roleId, permissionsToAdd).execute();
                        log.info("Role {}: Successfully added permissions: {}", roleId,
                                permissionsToAdd.stream().map(Permission::getName).collect(Collectors.toList()));
                    } catch (Auth0Exception e) {
                        log.error("Role {}: Failed to add permissions {}: {}", roleId,
                                permissionsToAdd.stream().map(Permission::getName).collect(Collectors.toList()), e.getMessage());
                        throw new RuntimeException("Failed to add permissions to role " + roleId, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Mono<List<Scope>> fetchAllScopesForApi() {
        return Mono.fromCallable(() -> {
            ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
            String apiIdentifier = auth0Config.getApiGatewayIdentifier();
            try {
                return mgmt.resourceServers().get(apiIdentifier).execute().getBody().getScopes();
            } catch (Auth0Exception e) {
                 log.error("Failed to fetch scopes for resource server {}: {}", apiIdentifier, e.getMessage());
                 throw new RuntimeException("Failed to fetch API scopes", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteRole(String roleId) {
        return Mono.fromRunnable(() -> {
                    try {
                        ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                        mgmt.roles().delete(roleId).execute();
                        log.info("Deleted Auth0 role with ID: {}", roleId);
                    } catch (Auth0Exception e) {
                        log.error("Error deleting Auth0 role with ID {}: {}", roleId, e.getMessage(), e);
                        throw new RuntimeException("Failed to delete role in Auth0", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
} 