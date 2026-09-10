package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Test 1: Métodos heredados
    @Test
    void testInheritedMethods() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        
        // save
        RescueCenter saved = rescueCenterRepository.save(center);
        assertThat(saved.getId()).isNotNull();

        // findById
        var retrieved = rescueCenterRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCode()).isEqualTo("DB-CAR");

        // existsById
        boolean exists = rescueCenterRepository.existsById(saved.getId());
        assertThat(exists).isTrue();

        // count
        long count = rescueCenterRepository.count();
        assertThat(count).isGreaterThan(0);
    }

    // Test 2: Relación 1:N
    @Test
    void testOneToManyRelationship() {
        RescueCenter center = new RescueCenter("DB-TEST", "Test Center", "Test City");
        
        RescueCase case1 = new RescueCase("RES-001", LocalDate.now(), "Location 1", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-002", LocalDate.now(), "Location 2", RescueStatus.ADMITTED);
        
        center.addCase(case1);
        center.addCase(case2);
        
        rescueCenterRepository.save(center);
        
        RescueCenter retrieved = rescueCenterRepository.findById(center.getId()).get();
        assertThat(retrieved.getCases()).hasSize(2);
    }

    // Test 3: Relación 1:1 RescueCase-Animal
    @Test
    void testOneToOneRescueCaseAnimal() {
        RescueCenter center = new RescueCenter("DB-ANIM", "Animal Center", "Test");
        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.of(2026, 8, 18), 
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        
        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        
        rescueCenterRepository.save(center);
        
        RescueCase retrieved = rescueCaseRepository.findById(rescueCase.getId()).get();
        assertThat(retrieved.getAnimal()).isNotNull();
        assertThat(retrieved.getAnimal().getCommonName()).isEqualTo("Green Sea Turtle");
        
        Animal retrievedAnimal = animalRepository.findById(animal.getId()).get();
        assertThat(retrievedAnimal.getRescueCase()).isNotNull();
        assertThat(retrievedAnimal.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    // Test 4: Relación 1:1 Animal-MedicalRecord
    @Test
    void testOneToOneAnimalMedicalRecord() {
        RescueCenter center = new RescueCenter("DB-MED", "Medical Center", "Test");
        RescueCase rescueCase = new RescueCase("RES-MED-001", LocalDate.now(), "Location", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        
        Animal animal = new Animal("AN-MED-001", "Dolphin", "Tursiops", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);
        
        MedicalRecord record = new MedicalRecord(new BigDecimal("28.40"), "STABLE");
        record.setInjuries("Left front flipper injury");
        animal.assignMedicalRecord(record);
        
        rescueCenterRepository.save(center);
        
        Animal retrieved = animalRepository.findById(animal.getId()).get();
        assertThat(retrieved.getMedicalRecord()).isNotNull();
        assertThat(retrieved.getMedicalRecord().getInitialWeight()).isEqualByComparingTo(new BigDecimal("28.40"));
    }

    // Test 5: Relación N:M
    @Test
    void testManyToManyRelationship() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").get();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").get();
        
        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);
        
        specialistRepository.save(elena);
        
        Specialist retrieved = specialistRepository.findById(elena.getId()).get();
        assertThat(retrieved.getExpertiseAreas()).hasSize(2);
    }

    // Test 6: Query Method simple
    @Test
    void testQueryMethodFindByCode() {
        RescueCenter center = new RescueCenter("DB-QM", "Query Method Test", "Test");
        rescueCenterRepository.save(center);
        
        var result = rescueCenterRepository.findByCode("DB-QM");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Query Method Test");
    }

    // Test 7: Query Method navegando relaciones
    @Test
    void testQueryMethodNavigatingRelations() {
        RescueCenter center = new RescueCenter("DB-NAV", "Navigation Test", "Santa Marta");
        RescueCase rescueCase = new RescueCase("RES-NAV-001", LocalDate.now(), "Location", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        
        Animal animal = new Animal("AN-NAV-001", "Turtle", "Testudines", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        
        rescueCenterRepository.save(center);
        
        List<Animal> animals = animalRepository.findByRescueCaseRescueCenterCode("DB-NAV");
        assertThat(animals).hasSize(1);
        assertThat(animals.get(0).getCommonName()).isEqualTo("Turtle");
    }

    // Test 8: @Query JPQL especialistas
    @Test
    void testQueryJPQLSpecialists() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").get();
        
        Specialist elena = new Specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");
        elena.setActive(true);
        elena.addExpertise(trauma);
        
        Specialist sofia = new Specialist("SPEC-S", "Sofia", "López", "sofia@deepblue.org");
        sofia.setActive(true);
        sofia.addExpertise(trauma);
        
        specialistRepository.saveAll(List.of(elena, sofia));
        
        List<Specialist> result = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(result).hasSize(2);
    }

    // Test 9: @Query JPQL tratamientos entre fechas
    @Test
    void testQueryJPQLTreatmentsBetweenDates() {
        RescueCenter center = new RescueCenter("DB-TREAT", "Treatment Center", "Test");
        RescueCase rescueCase = new RescueCase("RES-TREAT", LocalDate.now(), "Location", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        
        Animal animal = new Animal("AN-TREAT", "Fish", "Piscis", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);
        
        Specialist specialist = new Specialist("SPEC-T", "Tom", "Smith", "tom@deepblue.org");
        
        Treatment t1 = new Treatment(animal, specialist, 
                LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE);
        Treatment t2 = new Treatment(animal, specialist, 
                LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION);
        Treatment t3 = new Treatment(animal, specialist, 
                LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION);
        
        rescueCenterRepository.save(center);
        specialistRepository.save(specialist);
        treatmentRepository.saveAll(List.of(t1, t2, t3));
        
        List<Treatment> results = treatmentRepository.findTreatmentsBetweenDates(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59)
        );
        
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(TreatmentType.HYDRATION);
    }

    // Test 10: Test UNIQUE constraint
    @Test
    void testUniqueConstraint() {
        RescueCenter center = new RescueCenter("DB-UNIQ", "Unique Test", "Test");
        rescueCenterRepository.saveAndFlush(center);
        
        RescueCenter duplicate = new RescueCenter("DB-UNIQ", "Different Name", "Different City");
        
        assertThatThrownBy(() -> rescueCenterRepository.saveAndFlush(duplicate))
                .isNotNull();
    }

    // Test 11: Escenario integrador completo
    @Test
    void testCompletePersistenceScenario() {
        // Crear centro
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        
        // Crear caso
        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 18),
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        
        // Crear animal
        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        
        // Crear expediente
        MedicalRecord record = new MedicalRecord(new BigDecimal("27.80"), "STABLE");
        record.setInjuries("Injury caused by fishing net");
        record.setObservations("Possible plastic ingestion");
        animal.assignMedicalRecord(record);
        
        // Crear especialista
        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        
        // Agregar experiencias
        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").get();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").get();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").get();
        
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);
        
        // Crear tratamientos
        Treatment treatment1 = new Treatment(animal, elena,
                LocalDateTime.now(), TreatmentType.WOUND_CARE);
        treatment1.setDescription("Cleaning of left front flipper");
        
        Treatment treatment2 = new Treatment(animal, elena,
                LocalDateTime.now(), TreatmentType.HYDRATION);
        treatment2.setDescription("Subcutaneous fluid therapy");
        
        // Persistir todo
        rescueCenterRepository.save(center);
        specialistRepository.save(elena);
        treatmentRepository.saveAll(List.of(treatment1, treatment2));
        
        // Verificaciones
        assertThat(rescueCaseRepository.findByCaseCode("RES-2026-100")).isPresent();
        
        List<RescueCase> inRehab = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(inRehab).hasSizeGreaterThan(0);
        
        List<Animal> animalsByCenterCode = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(animalsByCenterCode).hasSize(1);
        
        List<Specialist> specialistsWithTrauma = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(specialistsWithTrauma).hasSizeGreaterThan(0);
        
        List<Treatment> animalTreatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(animalTreatments).hasSize(2);
    }

    // Test 12: Consulta 4 del reto integrador — nombre común contiene texto ignorando mayúsculas
    @Test
    void testFindByCommonNameContainingIgnoreCase() {
        RescueCenter center = new RescueCenter("DB-CONTAIN", "Contain Test", "Test");
        RescueCase rescueCase = new RescueCase("RES-CONTAIN", LocalDate.now(), "Location", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal turtle = new Animal("AN-CONTAIN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(turtle);

        rescueCenterRepository.save(center);

        List<Animal> results = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCommonName()).isEqualTo("Green Sea Turtle");
    }

    // Test 13: Reto sin guía — animales en rehabilitación tratados por especialistas con expertise Trauma
    @Test
    void testFindAnimalsInRehabilitationWithSpecialistExpertise() {
        RescueCenter center = new RescueCenter("DB-RETO", "Reto Test", "Test");
        RescueCase rescueCase = new RescueCase("RES-RETO", LocalDate.now(), "Location", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        Animal turtle = new Animal("AN-RETO", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(turtle);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").get();
        Specialist elena = new Specialist("SPEC-RETO", "Elena", "Vargas", "elena-reto@deepblue.org");
        elena.setActive(true);
        elena.addExpertise(trauma);

        Treatment treatment = new Treatment(turtle, elena, LocalDateTime.now(), TreatmentType.WOUND_CARE);

        rescueCenterRepository.save(center);
        specialistRepository.save(elena);
        treatmentRepository.save(treatment);

        List<Animal> results = animalRepository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION);
        assertThat(results).hasSize(1);

        List<Animal> rehabWithTrauma = treatmentRepository.findAnimalsInRehabilitationWithSpecialistExpertise(
                RescueStatus.IN_REHABILITATION, "trauma");
        assertThat(rehabWithTrauma).hasSize(1);
        assertThat(rehabWithTrauma.get(0).getAnimalCode()).isEqualTo("AN-RETO");
    }

    // Test 14: V3 — tracking_device_code agregado por migración Flyway
    @Test
    void testTrackingDeviceCodeColumn() {
        RescueCenter center = new RescueCenter("DB-GPS", "GPS Test", "Test");
        RescueCase rescueCase = new RescueCase("RES-GPS", LocalDate.now(), "Location", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-GPS", "Dolphin", "Tursiops", AnimalSex.UNKNOWN);
        animal.setTrackingDeviceCode("GPS-DEVICE-001");
        rescueCase.assignAnimal(animal);

        rescueCenterRepository.save(center);

        Animal retrieved = animalRepository.findById(animal.getId()).get();
        assertThat(retrieved.getTrackingDeviceCode()).isEqualTo("GPS-DEVICE-001");
    }

    // Test 15: Paso 53 — Query Method por status (3 casos, 2 IN_REHABILITATION)
    @Test
    void testQueryMethodFilterByStatus() {
        RescueCenter center = new RescueCenter("DB-STATUS", "Status Test", "Test");

        RescueCase c1 = new RescueCase("RES-053-001", LocalDate.now().minusDays(2), "Loc 1", RescueStatus.IN_REHABILITATION);
        RescueCase c2 = new RescueCase("RES-053-002", LocalDate.now().minusDays(1), "Loc 2", RescueStatus.READY_FOR_RELEASE);
        RescueCase c3 = new RescueCase("RES-053-003", LocalDate.now(), "Loc 3", RescueStatus.IN_REHABILITATION);

        center.addCase(c1);
        center.addCase(c2);
        center.addCase(c3);

        rescueCenterRepository.save(center);

        List<RescueCase> inRehab = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(inRehab).hasSize(2);
    }

    // Test 16: Paso 54 — Query Method navegando relaciones con dos centros (exclusión)
    @Test
    void testQueryMethodAnimalsByCenterExclusion() {
        RescueCenter car = new RescueCenter("DB-CAR", "Caribbean", "Santa Marta");
        RescueCase carCase = new RescueCase("RES-CAR-054", LocalDate.now(), "Bahía Concha", RescueStatus.ADMITTED);
        car.addCase(carCase);
        Animal carTurtle = new Animal("AN-CAR-054", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        carCase.assignAnimal(carTurtle);

        RescueCenter pac = new RescueCenter("DB-PAC", "Pacific", "Buenaventura");
        RescueCase pacCase = new RescueCase("RES-PAC-054", LocalDate.now(), "Playa", RescueStatus.ADMITTED);
        pac.addCase(pacCase);
        Animal pacSeal = new Animal("AN-PAC-054", "Sea Lion", "Zalophus", AnimalSex.MALE);
        pacCase.assignAnimal(pacSeal);

        rescueCenterRepository.save(car);
        rescueCenterRepository.save(pac);

        List<Animal> carAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(carAnimals).hasSize(1);
        assertThat(carAnimals).extracting(Animal::getAnimalCode).containsExactly("AN-CAR-054");
    }

    // Test 17: Paso 60 — Constraint FK violada en PostgreSQL
    @Test
    void testForeignKeyConstraint() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO treatments (animal_id, specialist_id, performed_at, type) VALUES (999999, 999999, NOW(), 'WOUND_CARE')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Test 18: Paso 61 — Constraint CHECK en rescue_cases.status
    @Test
    void testCheckConstraint() {
        jdbcTemplate.update("INSERT INTO rescue_centers (code, name, city) VALUES ('DB-CHK', 'Check Test', 'Test')");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id) " +
                        "VALUES ('RES-CHK', '2026-01-01', NULL, 'INVALID_STATUS', " +
                        "(SELECT id FROM rescue_centers WHERE code = 'DB-CHK'))"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}