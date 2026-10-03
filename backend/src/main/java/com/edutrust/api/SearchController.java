package com.edutrust.api;

import com.edutrust.retrieval.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public SearchResponse search(@RequestBody SearchRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must not be empty");
        }
        return new SearchResponse(request.question(), searchService.search(request.question()));
    }

    public record SearchRequest(String question) {
    }

    public record SearchResponse(String question, List<SearchService.SearchHit> hits) {
    }
}
