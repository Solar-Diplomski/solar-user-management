package fer.solar.usermanagement.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PaginatedUserResponse {
    private List<UserResponse> content; // List of users for the current page
    private int currentPage;            // Current page number (0-indexed)
    private int pageSize;               // Number of items per page
    private int totalPages;             // Total number of pages
    private long totalElements;         // Total number of users across all pages
} 