package fer.solar.usermanagement.permission;

import fer.solar.usermanagement.permission.dto.PaginatedPermissionResponse;
import fer.solar.usermanagement.permission.dto.UpdatePermissionsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public Mono<PaginatedPermissionResponse> listPermissions() {
        return permissionService.listPermissions();
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updatePermissions(@RequestBody UpdatePermissionsRequest request) {
        return permissionService.updatePermissions(request);
    }
    
} 