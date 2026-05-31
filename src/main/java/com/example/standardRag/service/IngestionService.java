package com.example.standardRag.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.example.standardRag.dto.UploadResponseDto;
import com.example.standardRag.entity.DocumentEntity;
import com.example.standardRag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class IngestionService {
    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public UploadResponseDto ingest(MultipartFile file) throws IOException {
        String source = file.getOriginalFilename();

        String normalizedName = normalizeDocumentName(source);

        String documentId = "DOC-" + NanoIdUtils.randomNanoId(
                NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
                NanoIdUtils.DEFAULT_ALPHABET,
                8
        );

        DocumentEntity documentEntity = DocumentEntity.builder()
                .documentId(documentId)
                .fileName(source)
                .normalizedName(normalizedName)
                .uploadedAt(LocalDateTime.now())
                .build();

        documentRepository.save(documentEntity);

        var resource = new InputStreamResource(file.getInputStream());
        var pdfReader = new PagePdfDocumentReader(resource);
        List<Document> rawDocs = pdfReader.read();
        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.split(rawDocs);
        chunks.forEach(chunk -> {
            chunk.getMetadata().put("documentId", documentId);
            chunk.getMetadata().put("source", source);
            chunk.getMetadata().put("normalizedName", normalizedName);
        });
        vectorStore.add(chunks);

        return UploadResponseDto.builder()
                .documentId(documentId)
                .filename(source)
                .message("Upload Successful")
                .build();
    }

    private String normalizeDocumentName(String documentName) {
        String normalizedName = documentName.trim().toLowerCase();
        if(normalizedName.endsWith(".pdf")) {
            normalizedName = normalizedName.substring(0, normalizedName.length() - 4);
        }

        return normalizedName;
    }
}
