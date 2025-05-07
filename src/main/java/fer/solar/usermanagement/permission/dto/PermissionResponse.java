package fer.solar.usermanagement.permission.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionResponse {
    private String permissionName;
    private String description;
} 