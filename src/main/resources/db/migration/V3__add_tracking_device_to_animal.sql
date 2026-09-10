-- V3__add_tracking_device_to_animal.sql
-- Agregar soporte para dispositivos GPS de seguimiento

-- La columna debe ser NULL por defecto pero cuando exista deberá ser única.
ALTER TABLE animals ADD COLUMN tracking_device_code VARCHAR(50) UNIQUE;