package com.edutrust.api;

import com.edutrust.retrieval.SearchService;
import com.edutrust.generation.ConflictResolutionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final ConflictResolutionService conflictResolutionService;

    public SearchController(SearchService searchService, ConflictResolutionService conflictResolutionService) {
        this.searchService = searchService;
        this.conflictResolutionService = conflictResolutionService;
    }

    @PostMapping
    public SearchResponse search(
            @RequestBody SearchRequest request,
            @RequestParam(value = "version", defaultValue = "v1") String version) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must not be empty");
        }
        if (version.equalsIgnoreCase("v3")) {
            var resolution = conflictResolutionService.resolve(request.question(), searchService.searchHybrid(request.question()));
            return new SearchResponse(request.question(), resolution.selectedEvidence(), resolution.conflictDetected(),
                    resolution.resolution(), resolution.selectedVersion(), resolution.selectedDocument());
        }
        List<SearchService.SearchHit> hits = switch (version.toLowerCase()) {
            case "v1" -> searchService.search(request.question());
            case "v2" -> searchService.searchHybrid(request.question());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported retrieval version: " + version);
        };
        return new SearchResponse(request.question(), hits, false, "no_conflict", null, null);
    }

    public record SearchRequest(String question) {
    }

    public record SearchResponse(String question, List<SearchService.SearchHit> hits,
                                 boolean conflictDetected, String conflictResolution,
                                 String selectedVersion, String selectedDocument) {
    }
}
