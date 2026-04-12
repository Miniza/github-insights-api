package za.vodacom.repoprofile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.vodacom.repoprofile.application.dto.ErrorResponse;
import za.vodacom.repoprofile.application.dto.ProfileResponse;
import za.vodacom.repoprofile.application.dto.PagedResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.ports.in.ProfileUseCase;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Profile Insights", description = "Endpoints for querying user profiles, repositories and search history across providers (GitHub, GitLab, Bitbucket)")
public class ProfileController {

    private final ProfileUseCase profileUseCase;

    public ProfileController(ProfileUseCase profileUseCase) {
        this.profileUseCase = profileUseCase;
    }

    @GetMapping("/profiles/{username}")
    @Operation(
            summary = "Get user profile",
            description = "Fetches profile details including top language and repositories sorted by stars. Supports multiple providers via the 'provider' query parameter.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Unsupported provider",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "502", description = "Provider API error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<ProfileResponse> getProfile(
            @Parameter(description = "Username on the source-code platform", example = "octocat")
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?$", message = "Invalid username format") String username,
            @Parameter(description = "Source-code provider", example = "github",
                    schema = @Schema(allowableValues = {"github", "gitlab", "bitbucket"}))
            @RequestParam(defaultValue = "github") String provider) {
        return ResponseEntity.ok(profileUseCase.getProfile(username, provider));
    }

    @GetMapping("/profiles/{username}/repos")
    @Operation(
            summary = "Get user repositories",
            description = "Returns public repositories sorted by stargazers count (descending) with pagination support.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Repositories retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Unsupported provider or invalid pagination",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<PagedResponse<RepoResponse>> getRepositories(
            @Parameter(description = "Username on the source-code platform", example = "octocat")
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?$", message = "Invalid username format") String username,
            @Parameter(description = "Source-code provider", example = "github",
                    schema = @Schema(allowableValues = {"github", "gitlab", "bitbucket"}))
            @RequestParam(defaultValue = "github") String provider,
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") int page,
            @Parameter(description = "Results per page (1–100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "perPage must be at least 1") @Max(value = 100, message = "perPage must not exceed 100") int perPage) {
        return ResponseEntity.ok(profileUseCase.getRepositories(username, provider, page, perPage));
    }

    @GetMapping("/searches")
    @Operation(
            summary = "Get search history",
            description = "Returns the last 50 profile searches with timestamps and summaries"
    )
    public ResponseEntity<List<SearchSummary>> getSearchHistory() {
        return ResponseEntity.ok(profileUseCase.getSearchHistory());
    }
}
