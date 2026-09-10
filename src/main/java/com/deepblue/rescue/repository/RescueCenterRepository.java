package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RescueCenterRepository extends JpaRepository<RescueCenter, Long> {
}