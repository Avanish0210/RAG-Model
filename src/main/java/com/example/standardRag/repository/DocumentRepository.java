package com.example.standardRag.repository;


import com.example.standardRag.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByDocumentId(String documentId);

    Optional<DocumentEntity> findByDocumentIdAndActiveTrue(String documentId);

    Optional<DocumentEntity> findTopByDocumentIdOrderByVersionDesc(String documentId);
}
