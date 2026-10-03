package com.edutrust.api;

import com.edutrust.ingestion.IngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IngestionService.IngestionResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("department") String department,
            @RequestParam("docType") String docType,
            @RequestParam("academicYear") String academicYear,
            @RequestParam("version") String version,
            @RequestParam("docDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate docDate,
            @RequestParam(value = "authority", required = false) String authority) throws IOException {
        validatePdf(file);
        return ingestionService.ingest(file, new IngestionService.DocumentMetadata(
                title, department, docType, academicYear, version, docDate, authority));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<IngestionService.DocumentSummary> list() {
        return ingestionService.listDocuments();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingestionService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    private void validatePdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw badRequest("PDF file must not be empty");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw badRequest("Only PDF files are accepted");
        }
        byte[] prefix = file.getBytes();
        if (prefix.length < 5 || prefix[0] != '%' || prefix[1] != 'P'
                || prefix[2] != 'D' || prefix[3] != 'F' || prefix[4] != '-') {
            throw badRequest("The uploaded file is not a valid PDF");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
