package com.banking.customer_service.dto;

import com.banking.customer_service.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycDocumentUploadRequest {
    @NotNull(message = "customer id is required")
    private Long customerId;

    @NotNull(message = "document type is required")
    private DocumentType documentType;

    @NotBlank(message = "file url is required")
    private String fileUrl;
    @NotBlank(message = "document number is required")
    private String documentNumber;

    @NotBlank(message = "document holder name is required")
    private String documentHolderName;
}
