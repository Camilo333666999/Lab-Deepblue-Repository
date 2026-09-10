# DeepBlue Rescue - Persistencia con Spring Boot

Plataforma para organizaciones dedicadas al rescate y rehabilitación de fauna marina.

## Descripción del Proyecto

**DeepBlue Rescue** es un sistema de persistencia desarrollado con **Java 21**, **Spring Boot 4.1.1**, **Spring Data JPA**, **Hibernate**, **Flyway** y **PostgreSQL**. 

El proyecto implementa la capa de persistencia para gestionar:

- Centros de rescate
- Casos de rescate
- Animales rescatados
- Expedientes médicos
- Especialistas y sus áreas de experiencia
- Tratamientos realizados

## Modelo de Datos

### Entidades Principales

```
RescueCenter 1 ───────── N RescueCase
RescueCase 1 ───────── 1 Animal
Animal 1 ───────── 1 MedicalRecord
Animal 1 ───────── N Treatment
Specialist 1 ───────── N Treatment
Specialist N ───────── M Expertise
```

### Tablas

| Tabla | Propósito |
|-------|-----------|
| `rescue_centers` | Centros de rescate |
| `rescue_cases` | Casos de rescate |
| `animals` | Animales rescatados |
| `medical_records` | Expedientes médicos |
| `specialists` | Especialistas en rescate |
| `expertise` | Áreas de especialidad |
| `specialist_expertise` | Relación N:M especialistas-expertise |
| `treatments` | Tratamientos realizados |

### Relaciones

**1:N - RescueCenter → RescueCase**
- Un centro puede tener múltiples casos
- Foreign key: `rescue_center_id`

**1:1 - RescueCase → Animal**
- Un caso tiene exactamente un animal
- Foreign key: `rescue_case_id` (UNIQUE)

**1:1 - Animal → MedicalRecord**
- Un animal tiene exactamente un expediente
- Foreign key: `animal_id` (UNIQUE)

**1:N - Animal → Treatment**
- Un animal puede recibir múltiples tratamientos
- Foreign key: `animal_id`

**1:N - Specialist → Treatment**
- Un especialista puede realizar múltiples tratamientos
- Foreign key: `specialist_id`

**N:M - Specialist ↔ Expertise**
- Tabla asociativa: `specialist_expertise`
- Un especialista puede tener múltiples áreas de experiencia
- Un área puede tener múltiples especialistas

## Tecnologías

- **Java 21**
- **Spring Boot 4.1.x**
- **Spring Data JPA**
- **Hibernate 7.x**
- **PostgreSQL 18**
- **Flyway** (Migraciones)
- **Testcontainers** (Testing)
- **Maven**

## Estructura del Proyecto

```
deepblue-rescue/
├── pom.xml
├── README.md
├── src/main/java/com/deepblue/rescue/
│   ├── DeepBlueRescueApplication.java
│   ├── domain/
│   │   ├── Animal.java
│   │   ├── AnimalSex.java
│   │   ├── Expertise.java
│   │   ├── MedicalRecord.java
│   │   ├── RescueCase.java
│   │   ├── RescueCenter.java
│   │   ├── RescueStatus.java
│   │   ├── Specialist.java
│   │   ├── Treatment.java
│   │   └── TreatmentType.java
│   └── repository/
│       ├── AnimalRepository.java
│       ├── ExpertiseRepository.java
│       ├── MedicalRecordRepository.java
│       ├── RescueCaseRepository.java
│       ├── RescueCenterRepository.java
│       ├── SpecialistRepository.java
│       └── TreatmentRepository.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__create_schema.sql
│       ├── V2__insert_expertise_catalog.sql
│       └── V3__add_tracking_device_to_animal.sql
└── src/test/java/com/deepblue/rescue/
    └── PersistenceIntegrationTest.java
```

## Configuración

### Application.yml

```yaml
spring:
  application:
    name: deepblue-rescue

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/deepblue}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate  # Flyway crea el esquema
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
```

### ¿Por qué `ddl-auto: validate`?

- **Flyway es responsable** de crear y evolucionar el esquema
- Hibernate solo **valida** que las entidades JPA coincidan con la BD
- Evita inconsistencias entre migraciones SQL y código Java
- Garantiza control de versiones del esquema

## Migraciones Flyway

### V1__create_schema.sql

Crea todas las tablas con:
- Primary keys (BIGSERIAL)
- Foreign keys con referencias
- Unique constraints
- Check constraints (para estados válidos)
- Índices en columnas frecuentemente consultadas

### V2__insert_expertise_catalog.sql

Inserta datos de referencia:
- Marine Reptiles
- Marine Mammals
- Marine Birds
- Trauma
- Rehabilitation
- Toxicology

### V3__add_tracking_device_to_animal.sql

Evolución del esquema: agrega la columna `tracking_device_code` (nullable, `UNIQUE`) mediante `ALTER TABLE`. Demuestra que el esquema evoluciona con migraciones Flyway y no modificando `V1`.

## Query Methods Implementados

### RescueCenterRepository

```java
Optional<RescueCenter> findByCode(String code);
```

### RescueCaseRepository

```java
Optional<RescueCase> findByCaseCode(String caseCode);
List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);
List<RescueCase> findByRescueCenterCode(String centerCode);
List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate rescueDate);
```

### AnimalRepository

```java
Optional<Animal> findByAnimalCode(String animalCode);
List<Animal> findByCommonNameIgnoreCase(String commonName);
List<Animal> findByCommonNameContainingIgnoreCase(String text);
List<Animal> findByRescueCaseStatus(RescueStatus status);
List<Animal> findByRescueCaseRescueCenterCode(String centerCode);
```

### ExpertiseRepository

```java
Optional<Expertise> findByNameIgnoreCase(String name);
```

### SpecialistRepository

```java
@Query("""
    select distinct s
    from Specialist s
    join s.expertiseAreas e
    where s.active = true
    and lower(e.name) = lower(:expertiseName)
    order by s.lastName, s.firstName
    """)
List<Specialist> findActiveByExpertise(String expertiseName);

@Query("""
    select distinct s
    from Specialist s
    join s.expertiseAreas e
    where lower(e.name) = lower(:expertiseName)
    order by s.lastName, s.firstName
    """)
List<Specialist> findByExpertise(String expertiseName);
```

### TreatmentRepository

```java
List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

@Query("""
    select t
    from Treatment t
    where t.performedAt >= :start
    and t.performedAt <= :end
    order by t.performedAt asc
    """)
List<Treatment> findTreatmentsBetweenDates(LocalDateTime start, LocalDateTime end);

@Query("""
    select t
    from Treatment t
    where t.animal.rescueCase.rescueCenter.code = :centerCode
    order by t.performedAt asc
    """)
List<Treatment> findTreatmentsByRescueCenter(String centerCode);

@Query("""
    select distinct t
    from Treatment t
    join t.specialist s
    join s.expertiseAreas e
    where lower(e.name) = lower(:expertiseName)
    order by t.performedAt asc
    """)
List<Treatment> findTreatmentsBySpecialistExpertise(String expertiseName);

@Query("""
    select distinct a
    from Animal a
    where a.rescueCase.status = :status
    and exists (
        select 1 from Treatment t
        join t.specialist s
        join s.expertiseAreas e
        where t.animal = a
        and lower(e.name) = lower(:expertiseName)
    )
    """)
List<Animal> findAnimalsInRehabilitationWithSpecialistExpertise(RescueStatus status, String expertiseName);
```

## Consultas JPQL

### Especialistas activos por experiencia

```jpql
select distinct s
from Specialist s
join s.expertiseAreas e
where s.active = true
and lower(e.name) = lower(:expertiseName)
order by s.lastName, s.firstName
```

**Uso**: Encontrar especialistas activos con experiencia específica

### Tratamientos en intervalo de fechas

```jpql
select t
from Treatment t
where t.performedAt >= :start
and t.performedAt <= :end
order by t.performedAt asc
```

**Uso**: Auditoría de tratamientos en períodos específicos

### Tratamientos por especialista con experiencia

```jpql
select distinct t
from Treatment t
join t.specialist s
join s.expertiseAreas e
where lower(e.name) = lower(:expertiseName)
order by t.performedAt asc
```

**Uso**: Análisis de eficacia por área de especialidad

### Animales en rehabilitación tratados por especialistas con una expertise

```jpql
select distinct a
from Animal a
where a.rescueCase.status = :status
and exists (
    select 1 from Treatment t
    join t.specialist s
    join s.expertiseAreas e
    where t.animal = a
    and lower(e.name) = lower(:expertiseName)
)
```

**Uso**: Reto sin guía — recibe `RescueStatus` y nombre de expertise (ignorando mayúsculas), evita duplicados con `DISTINCT` y elimina animales sin tratamiento relevante usando `EXISTS`.

## Ejecución

### Requisitos Previos

- Java 21 JDK
- Maven 3.9+
- Docker (para Testcontainers)

### Construir el Proyecto

```bash
mvn clean install
```

### Ejecutar Tests

```bash
mvn clean test
```

Los tests utilizarán **Testcontainers** para crear un contenedor PostgreSQL 18 ephémero.

### Ejecutar con PostgreSQL local

```bash
# 1. Crear base de datos
createdb deepblue

# 2. Configurar variables de entorno (opcional)
export DB_URL=jdbc:postgresql://localhost:5432/deepblue
export DB_USER=postgres
export DB_PASSWORD=postgres

# 3. Ejecutar tests
mvn test
```

### Verificar Migraciones

Flyway mostrará en los logs:

```
Flyway Community Edition 12.x
2026-09-09 10:30:00,000 INFO : DB: PostgreSQL 18.0
2026-09-09 10:30:00,100 INFO : Successfully validated 3 migrations
2026-09-09 10:30:00,200 INFO : Executing migration: V1__create_schema
2026-09-09 10:30:00,300 INFO : Executing migration: V2__insert_expertise_catalog
2026-09-09 10:30:00,400 INFO : Executing migration: V3__add_tracking_device_to_animal
```

## Testing

### Tests de Integración

La clase `PersistenceIntegrationTest` incluye:

> **Nota sobre el Paso 47 (Test de Flyway):** este laboratorio no incluye  (por petición del profesor) el test explícito que consulta `flyway_schema_history` (Paso 52 / Paso 47). La creación del esquema por Flyway se verifica de forma indirecta: `ddl-auto: validate` exige que todas las tablas y columnas existan antes de arrancar el contexto, y los logs de Flyway muestran la ejecución de `V1`, `V2` y `V3`.

| Test | Propósito |
|------|-----------|
| `testInheritedMethods` | Prueba save, findById, existsById, count |
| `testOneToManyRelationship` | RescueCenter 1:N RescueCase |
| `testOneToOneRescueCaseAnimal` | RescueCase 1:1 Animal |
| `testOneToOneAnimalMedicalRecord` | Animal 1:1 MedicalRecord |
| `testManyToManyRelationship` | Specialist N:M Expertise |
| `testQueryMethodFindByCode` | Query Method simple |
| `testQueryMethodNavigatingRelations` | Navegación entre entidades |
| `testQueryJPQLSpecialists` | @Query con JPQL |
| `testQueryJPQLTreatmentsBetweenDates` | Consulta por rango de fechas |
| `testUniqueConstraint` | Validación de constraint UNIQUE |
| `testCompletePersistenceScenario` | Escenario integrador completo |
| `testFindByCommonNameContainingIgnoreCase` | Consulta 4: nombre común contenga texto |
| `testFindAnimalsInRehabilitationWithSpecialistExpertise` | Reto sin guía (RescueStatus + expertise) |
| `testTrackingDeviceCodeColumn` | V3: columna tracking_device_code |

### Verifying PostgreSQL Container

Mientras ejecutan los tests:

```bash
docker ps
# Verás un contenedor postgres:18-alpine temporal
```

## Características Destacadas

✅ **Validación en BD**: Constraints CHECK en status  
✅ **Relaciones complejas**: 1:1, 1:N, N:M correctamente mapeadas  
✅ **Cascade operations**: Eliminación automática de huérfanos  
✅ **Lazy loading**: Optimización de queries  
✅ **Query Methods**: Interfaz fluida de Spring Data JPA  
✅ **JPQL**: Consultas orientadas a objetos  
✅ **Flyway**: Versionado del esquema  
✅ **Testcontainers**: Tests contra BD real  
✅ **Índices estratégicos**: En FK, status, fechas  

## Explicaciones Conceptuales

### ¿Qué diferencia existe entre JPA, Hibernate, Spring Data JPA y PostgreSQL?

- **PostgreSQL**: Base de datos relacional
- **Hibernate**: ORM que traduce entidades Java a SQL
- **JPA**: Especificación que define cómo funcionan los ORMs
- **Spring Data JPA**: Abstracción de alto nivel que simplifica Hibernate

### ¿Qué significa `mappedBy`?

Indica que la relación es **bidireccional** pero el **otro lado** mantiene la FK. No genera nueva columna.

```java
@OneToMany(mappedBy = "rescueCenter")
private List<RescueCase> cases;  // No tiene @JoinColumn
```

### ¿Por qué Specialist N:M Expertise necesita tabla intermedia?

Porque un especialista puede tener múltiples áreas de expertise, y un área puede tener múltiples especialistas. No hay forma de almacenar esto en una sola tabla sin redundancia.

### ¿Por qué usar Testcontainers?

- Pruebas contra **BD real**, no H2
- **Aislamiento**: Cada test tiene su contenedor
- **Reproducibilidad**: Mismo entorno en dev y CI/CD
- **Validación**: Los constraints reales se ejecutan

## Diagrama de Navegación de Relaciones

```
Animal
  │
  ├─ rescueCase
  │   └─ rescueCenter
  │       └─ code
  │
  ├─ medicalRecord
  │
  └─ treatments
      ├─ specialist
      │   └─ expertiseAreas
      └─ performedAt
```

Esta es la estructura que se navega en Query Methods como:
```java
findByRescueCaseRescueCenterCode(String centerCode)
```

## Convenciones del Proyecto

- Sin `@Data` de Lombok (generador verbose manual)
- `BigDecimal` para pesos
- `LocalDate` para fechas
- `LocalDateTime` para timestamps
- `Enum` para estados y tipos
- Nombres en inglés (convención Spring)
- Métodos helper (`addCase`, `assignAnimal`, `addExpertise`) mantienen bidireccionalidad

## Referencias

- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [Hibernate ORM](https://hibernate.org/)
- [Flyway Migrations](https://flywaydb.org/)
- [Testcontainers](https://www.testcontainers.org/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Autor

Proyecto académico - Laboratorio de Persistencia con Spring Boot 4

## Licencia

MIT

---

**Resultado de aprendizaje esperado**: Comprender el recorrido completo desde modelo de negocio → modelo relacional → Flyway → PostgreSQL → SQL → Hibernate/JPA → Spring Data JPA → Repositories → Consultas → Pruebas de integración.
