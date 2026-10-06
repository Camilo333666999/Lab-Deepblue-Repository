package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@Import(GlobalExceptionHandler.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    private AnimalResponse animalResponse() {
        return new AnimalResponse(
                1L,
                "AN-001",
                "Hawksbill turtle",
                "Eretmochelys imbricata",
                AnimalSex.FEMALE,
                "RES-001",
                RescueStatus.IN_REHABILITATION
        );
    }

    private TreatmentResponse treatmentResponse() {
        return new TreatmentResponse(
                1L,
                "AN-001",
                "CR-01",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Wound cleaning and antibiotic application"
        );
    }

    @Test
    void shouldReturnAnimalByCode() throws Exception {
        when(animalService.findByCode("AN-001"))
                .thenReturn(animalResponse());

        mockMvc.perform(get("/api/animals/AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.commonName")
                        .value("Hawksbill turtle"))
                .andExpect(jsonPath("$.rescueStatus")
                        .value("IN_REHABILITATION"));

        verify(animalService).findByCode("AN-001");
    }

    @Test
    void shouldReturnAnimalsInRehabilitation() throws Exception {
        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of(
                        animalResponse(),
                        animalResponse()
                ));

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"));

        verify(animalService).findAnimalsInRehabilitation();
    }

    @Test
    void shouldReturnAnimalTreatments() throws Exception {
        when(treatmentService.findByAnimalCode("AN-001"))
                .thenReturn(List.of(treatmentResponse()));

        mockMvc.perform(get("/api/animals/AN-001/treatments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("WOUND_CARE"))
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"));

        verify(treatmentService).findByAnimalCode("AN-001");
    }

    @Test
    void shouldReturnTreatmentEligibility() throws Exception {
        when(animalService.canReceiveTreatment("AN-001"))
                .thenReturn(true);

        mockMvc.perform(
                        get("/api/animals/AN-001/treatment-eligibility")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment("AN-001");
    }

    @Test
    void shouldReturnNotFoundForUnknownAnimal() throws Exception {
        when(animalService.findByCode("AN-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Animal AN-999 does not exist."
                ));

        mockMvc.perform(get("/api/animals/AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Animal AN-999 does not exist."))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturnNotFoundForUnknownAnimalEligibility() throws Exception {
        when(animalService.canReceiveTreatment("AN-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Animal AN-999 does not exist."
                ));

        mockMvc.perform(
                        get("/api/animals/AN-999/treatment-eligibility")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Animal AN-999 does not exist."));
    }
}