package com.cloudclean.file;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.FileItemResponse;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/files", "/api/v1/files"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "File Management", description = "Endpoints for file search, filtering, metadata retrieval, and deletion")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    @Operation(summary = "List and filter files", description = "Retrieves paginated catalog of files with category filtering and sorting")
    public ResponseEntity<ApiResponse<PageResponse<FileItemResponse>>> getFiles(
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "size") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        String userId = SecurityUtils.getCurrentUserId();
        PageResponse<FileItemResponse> files = fileService.getFiles(userId, category, page, limit, sortBy, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @GetMapping("/search")
    @Operation(summary = "Search files", description = "Searches indexed files across names, locations, and detection reasons")
    public ResponseEntity<ApiResponse<PageResponse<FileItemResponse>>> searchFiles(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        String userId = SecurityUtils.getCurrentUserId();
        PageResponse<FileItemResponse> files = fileService.searchFiles(userId, q, page, limit);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file details", description = "Returns full metadata, hashes, blur scores, and diagnostics for a file")
    public ResponseEntity<ApiResponse<FileItemResponse>> getFileById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        FileItemResponse file = fileService.getFileById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(file));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete file", description = "Soft deletes a file and moves it to the safe recovery trash pool")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        fileService.deleteFile(userId, id);
        return ResponseEntity.ok(ApiResponse.success("File moved to trash successfully", null));
    }
}
