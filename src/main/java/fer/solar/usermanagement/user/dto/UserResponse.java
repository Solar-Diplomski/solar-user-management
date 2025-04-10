package fer.solar.usermanagement.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {
    private String userId;
    private String email;
    private String name;
    private String picture;
    private String lastLogin;
    private List<String> roles; // List of role names or IDs
} 