package com.banking.customer_service.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRequest {
    @NotBlank(message = "first name is must required")
    private String firstName;
    @NotBlank(message = "last  name is must required")
    private String lastName;
    @NotBlank(message = "phone number is must required")
    private String phone;
    @NotBlank(message = "address is must required")
    private String address;

    @NotNull(message = "date of birth is required")
    @Past(message = "date of birth must be in the past")
    private LocalDate dateOfBirth;
}
