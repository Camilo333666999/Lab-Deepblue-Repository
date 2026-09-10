package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);
    
    @Query("""
            select t
            from Treatment t
            where t.performedAt >= :start
            and t.performedAt <= :end
            order by t.performedAt asc
            """)
    List<Treatment> findTreatmentsBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    @Query("""
            select t
            from Treatment t
            where t.animal.rescueCase.rescueCenter.code = :centerCode
            order by t.performedAt asc
            """)
    List<Treatment> findTreatmentsByRescueCenter(@Param("centerCode") String centerCode);
    
    @Query("""
            select distinct t
            from Treatment t
            join t.specialist s
            join s.expertiseAreas e
            where lower(e.name) = lower(:expertiseName)
            order by t.performedAt asc
            """)
    List<Treatment> findTreatmentsBySpecialistExpertise(@Param("expertiseName") String expertiseName);
    
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
    List<Animal> findAnimalsInRehabilitationWithSpecialistExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName);
}