package kigali.clinic.rw.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import kigali.clinic.rw.domain.Specialization;

public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {

    Optional<Specialization> findByNameIgnoreCase(String name);

    // B3 (JPQL): IS EMPTY tests the doctors collection directly, so no join and no COUNT is needed.
    // Hibernate turns it into a NOT EXISTS subquery on the doctor_specialization join table.
    @Query("SELECT s FROM Specialization s WHERE s.doctors IS EMPTY")
    List<Specialization> findUnused();
}
