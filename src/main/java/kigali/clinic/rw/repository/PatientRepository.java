package kigali.clinic.rw.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kigali.clinic.rw.domain.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // A1 (DERIVED): "LastNameIgnoreCase" -> UPPER(last_name) = UPPER(?),
    // "OrderByFirstNameAsc" -> sorted A to Z. No match gives an empty list.
    List<Patient> findByLastNameIgnoreCaseOrderByFirstNameAsc(String lastName);

    // B4 (JPQL): join the patient's appointments collection, then navigate a.doctor.id.
    // A patient with several appointments with the same doctor produces several joined rows,
    // so DISTINCT collapses them to one Patient.
    @Query("SELECT DISTINCT p FROM Patient p JOIN p.appointments a WHERE a.doctor.id = :doctorId")
    List<Patient> findPatientsOfDoctor(@Param("doctorId") UUID doctorId);

    // C2 (JPQL): group the joined rows per patient, keep groups with at least :min rows (HAVING),
    // and put the busiest patient first. SELECT p returns the Patient entity itself.
    // :min is a long because COUNT(...) is a Long in JPQL.
    @Query("SELECT p FROM Patient p JOIN p.appointments a GROUP BY p HAVING COUNT(a) >= :min ORDER BY COUNT(a) DESC")
    List<Patient> findFrequentPatients(@Param("min") long min);
}
