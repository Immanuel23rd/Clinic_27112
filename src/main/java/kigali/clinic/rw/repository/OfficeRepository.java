package kigali.clinic.rw.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.domain.Office;

@Repository 
public interface OfficeRepository  extends JpaRepository<Office,UUID>{
    
    List<Office> findByName(String name);

    Optional<Office> findByNameAndOfficeNumber(String name, int officeNumber);

    Optional<Office> findByDoctor(Doctor doctor);

    Optional<Office>  findByOfficeNumber(int officeNumber);

}
