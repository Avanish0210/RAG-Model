package com.example.standardRag.controller;

import com.example.standardRag.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class UploaderController {
    private final VectorStore vectorStore;
    private final IngestionService ingestionService;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        ingestionService.ingest(file);
        return file.getOriginalFilename()+" has been uploaded";
    }
}
