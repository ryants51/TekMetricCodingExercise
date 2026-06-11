package com.interview.repository;

import com.interview.entity.Part;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, UUID> {
}
