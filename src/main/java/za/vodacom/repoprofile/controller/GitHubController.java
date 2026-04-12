package za.vodacom.repoprofile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.vodacom.repoprofile.application.dto.ErrorResponse;
import za.vodacom.repoprofile.application.dto.GitHubProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.ports.in.GitHubUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "GitHub Profile Insights", description = "Endpoints for querying GitHub user profiles, repositories and search history")
public class GitHubController {

    private final GitHubUseCase gitHubUseCase;

    public GitHubController(GitHubUseCase gitHubUseCase) {
        this.gitHubUseCase = gitHubUseCase;
    }

    @GetMapping("/profiles/{username}")
    @Operation(
            summary = "Get GitHub user profile",
            description = "Fetches profile details including top language and repositories sorted by stars",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "GitHub user not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "502", description = "GitHub API error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<GitHubProfileResponse> getProfile(
            @Parameter(description = "GitHub username", example = "octocat") @PathVariable String username) {
        return ResponseEntity.ok(gitHubUseCase.getProfile(username));
    }

    @GetMapping("/profiles/{username}/repos")
    @Operation(
            summary = "Get user repositories",
            description = "Returns public repositories sorted by stargazers count (descending)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Repositories retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "GitHub user not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<List<RepoResponse>> getRepositories(
            @Parameter(description = "GitHub username", example = "octocat") @PathVariable String username) {
        return ResponseEntity.ok(gitHubUseCase.getRepositories(username));
    }

    @GetMapping("/searches")
    @Operation(
            summary = "Get search history",
            description = "Returns the last 50 profile searches with timestamps and summaries"
    )
    public ResponseEntity<List<SearchSummary>> getSearchHistory() {
        return ResponseEntity.ok(gitHubUseCase.getSearchHistory());
    }
}
