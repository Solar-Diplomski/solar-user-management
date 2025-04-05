package fer.solar.usermanagement.user;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.roles.Role;
import com.auth0.json.mgmt.tickets.PasswordChangeTicket;
import com.auth0.json.mgmt.users.User;
import com.auth0.json.mgmt.users.UsersPage;
import com.auth0.net.Request;
import com.auth0.net.Response;
import fer.solar.usermanagement.config.Auth0Config;
import fer.solar.usermanagement.user.dto.CreateUserRequest;
import fer.solar.usermanagement.user.dto.PaginatedUserResponse;
import fer.solar.usermanagement.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Auth0UserService implements UserService {

    private final Auth0Config auth0Config;

    // Helper to generate a random password
    private String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length * 3 / 4 + 1]; // Ensure enough bytes for encoding
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    @Override
    public Mono<String> createUser(CreateUserRequest request) {
        return Mono.fromCallable(() -> {
            ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();

            User newUser = new User(request.getConnection());
            newUser.setEmail(request.getEmail());
            newUser.setEmailVerified(false);
            // Set a temporary random password to satisfy API requirement
            newUser.setPassword(generateRandomPassword(16));

            try {
                User createdUser = mgmt.users().create(newUser).execute().getBody();
                log.info("Auth0 user created with ID: {}", createdUser.getId());

                if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
                    mgmt.users().addRoles(createdUser.getId(), request.getRoleIds()).execute();
                    log.info("Assigned roles {} to user {}", request.getRoleIds(), createdUser.getId());
                }

                PasswordChangeTicket ticketRequest = new PasswordChangeTicket(createdUser.getId());
                ticketRequest.setResultUrl(request.getResultUrl());
                ticketRequest.setMarkEmailAsVerified(false);
                ticketRequest.setIncludeEmailInRedirect(false);

                String ticketUrl = mgmt.tickets()
                        .requestPasswordChange(ticketRequest)
                        .addParameter("ttl_sec", 86400)
                        .execute()
                        .getBody()
                        .getTicket();
                log.info("Generated password change ticket URL for user {}", createdUser.getId());

                return ticketUrl;
            } catch (Auth0Exception e) {
                log.error("Error creating Auth0 user, assigning roles, or generating password ticket for email {}: {}", request.getEmail(), e.getMessage(), e);
                throw new RuntimeException("Failed to create user or generate password ticket in Auth0: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PaginatedUserResponse> listUsers(int page, int size) {
        return Mono.fromCallable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    UserFilter filter = new UserFilter()
                            .withPage(page, size)
                            .withTotals(true);

                    try {
                        return mgmt.users().list(filter).execute().getBody();
                    } catch (Auth0Exception e) {
                        log.error("Error listing Auth0 users (page={}, size={}): {}", page, size, e.getMessage(), e);
                        throw new RuntimeException("Failed to list users from Auth0", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(usersPage -> {
                    Flux<UserResponse> userResponseFlux = Flux.fromIterable(usersPage.getItems())
                            .flatMap(this::mapUserToResponse);

                    return userResponseFlux.collectList().map(userResponses -> {
                        long totalElements = usersPage.getTotal();
                        int totalPages = (int) Math.ceil((double) totalElements / size);
                         if (size <= 0) totalPages = 0;

                        return PaginatedUserResponse.builder()
                                .content(userResponses)
                                .currentPage(page)
                                .pageSize(size)
                                .totalElements(totalElements)
                                .totalPages(totalPages)
                                .build();
                    });
                });
    }

    @Override
    public Mono<UserResponse> getUserById(String userId) {
        return Mono.fromCallable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    return mgmt.users().get(userId, null).execute().getBody();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::mapUserToResponse);
    }

    @Override
    public Mono<Void> deleteUser(String userId) {
        return Mono.fromRunnable(() -> {
            try {
                ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                mgmt.users().delete(userId).execute();
                log.info("Deleted Auth0 user with ID: {}", userId);
            } catch (Auth0Exception e) {
                log.error("Error deleting Auth0 user with ID {}: {}", userId, e.getMessage(), e);
                throw new RuntimeException("Failed to delete user in Auth0", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<UserResponse> mapUserToResponse(User user) {
        return Mono.fromCallable(() -> {
                    ManagementAPI mgmt = auth0Config.getRefreshedManagementAPI();
                    List<Role> roles = mgmt.users().listRoles(user.getId(), null).execute().getBody().getItems();
                    List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());

                    return UserResponse.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .name(user.getName())
                            .picture(user.getPicture())
                            .lastLogin(user.getLastLogin() != null ? user.getLastLogin().toString() : null)
                            .roles(roleNames)
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(Auth0Exception.class, e -> {
                    log.error("Error fetching roles for user {}: {}", user.getId(), e.getMessage());
                    return Mono.just(UserResponse.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .name(user.getName())
                            .picture(user.getPicture())
                            .lastLogin(user.getLastLogin() != null ? user.getLastLogin().toString() : null)
                            .roles(Collections.emptyList())
                            .build());
                });
    }

    // ... (Update user stub needs updated UpdateUserRequest import if uncommented) ...
} 