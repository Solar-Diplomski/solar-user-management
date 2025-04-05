package fer.solar.usermanagement.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {
    private String email;
    private String connection; // e.g., "Username-Password-Authentication"
    private List<String> roleIds; // List of Auth0 Role IDs to assign
    private String resultUrl; // Optional: URL to redirect user after password change
    // Add other relevant fields if needed, like name, etc.
    // Auth0 invitation will handle the password setup
} 