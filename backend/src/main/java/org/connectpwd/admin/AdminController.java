package org.connectpwd.admin;

import lombok.RequiredArgsConstructor;
import org.connectpwd.admin.dto.*;
import org.connectpwd.common.ApiResponse;
import org.connectpwd.scoring.IsaaScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /** GET /api/v1/admin/stats */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDTO>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.stats()));
    }

    /** GET /api/v1/admin/users?page=0&size=20&sort=createdAt,desc */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserDTO>>> listUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String dir) {

        Sort sort = dir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(pageable)));
    }

    /** GET /api/v1/admin/sessions?page=0&size=20 */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<AdminSessionDTO>>> listSessions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String dir) {

        Sort sort = dir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(adminService.listSessions(pageable)));
    }

    /** GET /api/v1/admin/sessions/{sessionId}/responses */
    @GetMapping("/sessions/{sessionId}/responses")
    public ResponseEntity<ApiResponse<List<AdminResponseDTO>>> listResponses(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listResponses(sessionId)));
    }

    /** GET /api/v1/admin/sessions/{sessionId}/score */
    @GetMapping("/sessions/{sessionId}/score")
    public ResponseEntity<ApiResponse<IsaaScore>> getScore(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getScore(sessionId)));
    }
}
