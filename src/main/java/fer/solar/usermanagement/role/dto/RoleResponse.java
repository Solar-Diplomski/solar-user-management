package fer.solar.usermanagement.role.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class RoleResponse {
    private String id;
    private String name;
    private String description;
    private List<String> permissions;
} 