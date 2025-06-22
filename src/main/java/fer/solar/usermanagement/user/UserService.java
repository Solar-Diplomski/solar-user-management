package fer.solar.usermanagement.user;

import fer.solar.usermanagement.user.dto.CreateUserRequest;
import fer.solar.usermanagement.user.dto.CreateUserResponse;
import fer.solar.usermanagement.user.dto.PaginatedUserResponse;
import fer.solar.usermanagement.user.dto.UpdateUserRequest;
import fer.solar.usermanagement.user.dto.UserResponse;
import reactor.core.publisher.Mono;
//
public interface UserService {

    Mono<CreateUserResponse> createUser(CreateUserRequest request);

    Mono<PaginatedUserResponse> listUsers(int page, int size);

    Mono<UserResponse> getUserById(String userId);

    Mono<Void> updateUser(String userId, UpdateUserRequest request);

    Mono<Void> deleteUser(String userId);
} 
