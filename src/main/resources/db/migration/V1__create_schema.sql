-- V1__create_schema.sql
-- Crear todas las tablas del sistema

-- Tabla rescue_centers
CREATE TABLE rescue_centers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL
);

-- Tabla rescue_cases
CREATE TABLE rescue_cases (
    id BIGSERIAL PRIMARY KEY,
    case_code VARCHAR(50) NOT NULL UNIQUE,
    rescue_date DATE NOT NULL,
    rescue_location VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    rescue_center_id BIGINT NOT NULL REFERENCES rescue_centers(id),
    CONSTRAINT chk_status CHECK (status IN ('ADMITTED', 'UNDER_EVALUATION', 'IN_REHABILITATION', 'READY_FOR_RELEASE', 'RELEASED', 'CLOSED'))
);

-- Índice para rescue_center_id en rescue_cases
CREATE INDEX idx_rescue_cases_rescue_center_id ON rescue_cases(rescue_center_id);

-- Índice para status en rescue_cases
CREATE INDEX idx_rescue_cases_status ON rescue_cases(status);

-- Índice para rescue_date en rescue_cases
CREATE INDEX idx_rescue_cases_rescue_date ON rescue_cases(rescue_date);

-- Tabla animals
CREATE TABLE animals (
    id BIGSERIAL PRIMARY KEY,
    animal_code VARCHAR(50) NOT NULL UNIQUE,
    common_name VARCHAR(255) NOT NULL,
    scientific_name VARCHAR(255) NOT NULL,
    sex VARCHAR(50) NOT NULL,
    rescue_case_id BIGINT NOT NULL UNIQUE REFERENCES rescue_cases(id)
);

-- Tabla medical_records
CREATE TABLE medical_records (
    id BIGSERIAL PRIMARY KEY,
    animal_id BIGINT NOT NULL UNIQUE REFERENCES animals(id),
    initial_weight NUMERIC(10, 2),
    initial_condition VARCHAR(255),
    injuries TEXT,
    observations TEXT
);

-- Tabla specialists
CREATE TABLE specialists (
    id BIGSERIAL PRIMARY KEY,
    professional_code VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true
);

-- Tabla expertise
CREATE TABLE expertise (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Tabla specialist_expertise (tabla asociativa para N:M)
CREATE TABLE specialist_expertise (
    specialist_id BIGINT NOT NULL REFERENCES specialists(id),
    expertise_id BIGINT NOT NULL REFERENCES expertise(id),
    PRIMARY KEY (specialist_id, expertise_id)
);

-- Tabla treatments
CREATE TABLE treatments (
    id BIGSERIAL PRIMARY KEY,
    animal_id BIGINT NOT NULL REFERENCES animals(id),
    specialist_id BIGINT NOT NULL REFERENCES specialists(id),
    performed_at TIMESTAMP NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT
);

-- Índice para animal_id en treatments
CREATE INDEX idx_treatments_animal_id ON treatments(animal_id);

-- Índice para specialist_id en treatments
CREATE INDEX idx_treatments_specialist_id ON treatments(specialist_id);

-- Índice para performed_at en treatments
CREATE INDEX idx_treatments_performed_at ON treatments(performed_at);
