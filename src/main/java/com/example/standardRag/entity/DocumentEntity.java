package com.example.standardRag.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String documentId;
    @Column(nullable = false)
    private String userId;
    private String fileName;
    private String normalizedName;
    private Integer version;
    private Boolean active;
    private LocalDateTime uploadedAt;

}
