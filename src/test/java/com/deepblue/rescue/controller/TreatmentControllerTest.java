package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.TreatmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
@Import(GlobalExceptionHandler.class)
class TreatmentControllerTest {

    private static final String VALID_BODY = """
            {
                "animalCode": "AN-001",
                "specialistCode": "CR-01",
                "performedAt": "2026-08-21T09:00:00",
                "type": "HYDRATION",
                "description": "Hydration session with electrolyte support"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService treatmentService;

    @Test
    void shouldCreateTreatment() throws Exception {
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "CR-01",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.HYDRATION,
                "Hydration session with electrolyte support"
        );
        TreatmentResponse response = new TreatmentResponse(
                1L,
                "AN-001",
                "CR-01",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.HYDRATION,
                "Hydration session with electrolyte support"
        );
        when(treatmentService.register(request)).thenReturn(response);

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.specialistCode").value("CR-01"))
                .andExpect(jsonPath("$.type").value("HYDRATION"));

        verify(treatmentService).register(any(CreateTreatmentRequest.class));
    }

    @Test
    void shouldReturnBadRequestForMissingFields() throws Exception {
        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.details.animalCode")
                        .value("Animal code is required"))
                .andExpect(jsonPath("$.details.description")
                        .value("Description is required"));

        verify(treatmentService, never())
                .register(any(CreateTreatmentRequest.class));
    }

    @Test
    void shouldReturnNotFoundForUnknownAnimal() throws Exception {
        when(treatmentService.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "Animal AN-999 does not exist."
                ));

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY.replace("AN-001", "AN-999"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Animal AN-999 does not exist."))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturnConflictForBusinessRule() throws Exception {
        when(treatmentService.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "Animal AN-001 is not eligible for treatment."
                ));

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Animal AN-001 is not eligible for treatment."))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
