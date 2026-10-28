package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    @Test
    void shouldRegisterTreatmentWhenAllRulesPass() {
        RescueCase rescueCase = new RescueCase(
                "RES-100", LocalDate.of(2026, 8, 20), "Bahia Concha", RescueStatus.IN_REHABILITATION);

        Animal animal = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        specialist.setActive(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");

        Treatment treatment = new Treatment(
                animal, specialist, request.performedAt(), request.type(), request.description());

        TreatmentResponse response = new TreatmentResponse(
                1L, "AN-001", "SPEC-001", request.performedAt(),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");

        when(treatmentRepository.save(any(Treatment.class))).thenReturn(treatment);
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(response);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldRejectInactiveSpecialist() {
        RescueCase rescueCase = new RescueCase(
                "RES-100", LocalDate.of(2026, 8, 20), "Bahia Concha", RescueStatus.IN_REHABILITATION);

        Animal animal = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        specialist.setActive(false);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"ADMITTED", "READY_FOR_RELEASE", "RELEASED", "CLOSED"})
    void shouldRejectTreatmentWhenRescueCaseCannotReceiveTreatment(RescueStatus status) {
        RescueCase rescueCase = new RescueCase(
                "RES-100", LocalDate.of(2026, 8, 20), "Bahia Concha", status);

        Animal animal = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        specialist.setActive(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.OBSERVATION, "Post-release observation.");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("is not eligible for treatment");

        verify(treatmentRepository, never()).save(any());
    }
}