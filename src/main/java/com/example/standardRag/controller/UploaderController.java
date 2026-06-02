package com.example.standardRag.controller;

import com.example.standardRag.dto.UploadResponseDto;
import com.example.standardRag.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class UploaderController {
    private final IngestionService ingestionService;

    @PostMapping("/upload")
    public UploadResponseDto upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ingestionService.ingest(file);
    }

    @PutMapping("/upload/{documentId}")
    public UploadResponseDto update(@PathVariable("documentId") String documentId, @RequestParam("file") MultipartFile file) throws IOException {
        return ingestionService.update(documentId , file);
    }
}
