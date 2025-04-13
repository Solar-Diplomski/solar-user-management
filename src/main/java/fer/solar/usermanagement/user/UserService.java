package fer.solar.usermanagement.user;

import fer.solar.usermanagement.user.dto.CreateUserRequest;
import fer.solar.usermanagement.user.dto.CreateUserResponse;
import fer.solar.usermanagement.user.dto.PaginatedUserResponse;
import fer.solar.usermanagement.user.dto.UpdateUserRequest;
import fer.solar.usermanagement.user.dto.UserResponse;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<CreateUserResponse> createUser(CreateUserRequest request);

    Mono<PaginatedUserResponse> listUsers(int page, int size);

    Mono<UserResponse> getUserById(String userId);

    /**
     * Updates a user's roles in Auth0.
     *
     * @param userId The ID of the user to update.
     * @param request The request containing the list of role IDs to assign.
     * @return A Mono that completes when the roles are updated.
     */
    Mono<Void> updateUser(String userId, UpdateUserRequest request);

    Mono<Void> deleteUser(String userId);
} 