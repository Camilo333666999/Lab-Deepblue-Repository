package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Long> {
    
    @Query("""
            select distinct s
            from Specialist s
            join s.expertiseAreas e
            where s.active = true
            and lower(e.name) = lower(:expertiseName)
            order by s.lastName, s.firstName
            """)
    List<Specialist> findActiveByExpertise(@Param("expertiseName") String expertiseName);
    
    @Query("""
            select distinct s
            from Specialist s
            join s.expertiseAreas e
            where lower(e.name) = lower(:expertiseName)
            order by s.lastName, s.firstName
            """)
    List<Specialist> findByExpertise(@Param("expertiseName") String expertiseName);
}