package com.example.standardRag.controller;

import com.example.standardRag.dto.UploadResponseDto;
import com.example.standardRag.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
}
