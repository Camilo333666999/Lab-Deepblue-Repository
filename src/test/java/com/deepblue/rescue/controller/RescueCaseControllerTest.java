package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService rescueCaseService;

    private RescueCaseResponse response() {
        return new RescueCaseResponse(
                1L,
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                RescueStatus.ADMITTED,
                "DB-CAR",
                "AN-001"
        );
    }

    @Test
    void shouldReturnRescueCaseByCode() throws Exception {
        when(rescueCaseService.findByCode("RES-001"))
                .thenReturn(response());

        mockMvc.perform(get("/api/rescue-cases/RES-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-001"))
                .andExpect(jsonPath("$.status").value("ADMITTED"))
                .andExpect(jsonPath("$.animalCode").value("AN-001"));

        verify(rescueCaseService).findByCode("RES-001");
    }

    @Test
    void shouldReturnNotFoundForUnknownRescueCase() throws Exception {
        when(rescueCaseService.findByCode("RES-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Rescue case RES-999 does not exist."
                ));

        mockMvc.perform(get("/api/rescue-cases/RES-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Rescue case RES-999 does not exist."))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturnCasesByStatus() throws Exception {
        RescueCaseResponse inRehabilitation = new RescueCaseResponse(
                2L,
                "RES-002",
                LocalDate.of(2026, 8, 21),
                "Taganga",
                RescueStatus.IN_REHABILITATION,
                "DB-CAR",
                "AN-002"
        );
        when(rescueCaseService.findByStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(inRehabilitation, inRehabilitation));

        mockMvc.perform(
                        get("/api/rescue-cases")
                                .param("status", "IN_REHABILITATION")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status")
                        .value("IN_REHABILITATION"));

        verify(rescueCaseService)
                .findByStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldReturnBadRequestForInvalidStatus() throws Exception {
        mockMvc.perform(
                        get("/api/rescue-cases")
                                .param("status", "FLYING")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status")
                        .value("Invalid value: FLYING"));

        verify(rescueCaseService, never()).findByStatus(any());
    }

    @Test
    void shouldChangeStatus() throws Exception {
        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(
                        RescueStatus.IN_REHABILITATION
                );
        RescueCaseResponse changed = new RescueCaseResponse(
                1L,
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION,
                "DB-CAR",
                "AN-001"
        );
        when(rescueCaseService.changeStatus(
                "RES-001",
                request
        )).thenReturn(changed);

        mockMvc.perform(
                        patch("/api/rescue-cases/RES-001/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"IN_REHABILITATION\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-001"))
                .andExpect(jsonPath("$.status").value("IN_REHABILITATION"));
    }

    @Test
    void shouldReturnBadRequestForMissingStatus() throws Exception {
        mockMvc.perform(
                        patch("/api/rescue-cases/RES-001/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.details.status")
                        .value("Status is required"));

        verify(rescueCaseService, never())
                .changeStatus(anyString(), any());
    }

    @Test
    void shouldReturnConflictForInvalidTransition() throws Exception {
        when(rescueCaseService.changeStatus(
                anyString(),
                any(ChangeRescueStatusRequest.class)
        )).thenThrow(new BusinessRuleException(
                "Invalid transition from ADMITTED to RELEASED."
        ));

        mockMvc.perform(
                        patch("/api/rescue-cases/RES-001/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"RELEASED\"}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Invalid transition from ADMITTED to RELEASED."));
    }

    @Test
    void shouldReturnBadRequestForMalformedEnumJson() throws Exception {
        mockMvc.perform(
                        patch("/api/rescue-cases/RES-001/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"FLYING\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body")
                        .value("Check JSON syntax and enum values"));

        verify(rescueCaseService, never())
                .changeStatus(anyString(), any());
    }

    @Test
    void shouldReturnInternalServerErrorOnUnexpectedError() throws Exception {
        when(rescueCaseService.findByCode("RES-001"))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/rescue-cases/RES-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isEmpty());
    }
}
