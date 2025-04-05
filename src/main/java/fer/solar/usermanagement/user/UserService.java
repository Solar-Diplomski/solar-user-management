package fer.solar.usermanagement.user;

import fer.solar.usermanagement.user.dto.CreateUserRequest;
import fer.solar.usermanagement.user.dto.PaginatedUserResponse;
import fer.solar.usermanagement.user.dto.UserResponse;
import reactor.core.publisher.Mono;

public interface UserService {

    /**
     * Creates a new user with a temporary password and flags them for a mandatory reset on first login.
     *
     * @param request The request containing user details (email, connection) and role IDs.
     * @return A Mono containing the generated temporary password for the user.
     */
    Mono<String> createUser(CreateUserRequest request);

    /**
     * Retrieves a paginated list of users from Auth0.
     *
     * @param page The page number (0-indexed).
     * @param size The number of items per page.
     * @return A Mono containing the paginated user response.
     */
    Mono<PaginatedUserResponse> listUsers(int page, int size);

    /**
     * Retrieves a single user by their ID from Auth0.
     *
     * @param userId The ID of the user to retrieve.
     * @return A Mono containing the UserResponse or empty if not found.
     */
    Mono<UserResponse> getUserById(String userId);

    /**
     * Updates a user's details in Auth0 (implementation details TBD based on specific needs).
     *
     * @param userId The ID of the user to update.
     * @param request The request containing updated user details.
     * @return A Mono containing the updated UserResponse.
     */
    // Mono<UserResponse> updateUser(String userId, UpdateUserRequest request); // Assumes UpdateUserRequest in user.dto

    /**
     * Deletes a user from Auth0.
     *
     * @param userId The ID of the user to delete.
     * @return A Mono completing when the deletion is successful.
     */
    Mono<Void> deleteUser(String userId);
} 