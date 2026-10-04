package kigali.clinic.rw.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kigali.clinic.rw.domain.Doctor;


@Repository
public interface DoctorRepository extends JpaRepository<Doctor,UUID> {

    // B1 (JPQL): specializations is a collection, so it needs a JOIN (a dot would not work).
    // LOWER on both sides makes "cardiology" match "Cardiology".
    // DISTINCT protects against the same doctor appearing twice if several joined rows match.
    @Query("SELECT DISTINCT d FROM Doctor d JOIN d.specializations s WHERE LOWER(s.name) = LOWER(:name)")
    List<Doctor> findBySpecializationName(@Param("name") String name);

    // B2 (JPQL): office is a single-valued relation, so IS NULL tests the office_id foreign key.
    @Query("SELECT d FROM Doctor d WHERE d.office IS NULL ORDER BY d.lastName ASC")
    List<Doctor> findDoctorsWithoutOffice();
}
