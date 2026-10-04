package kigali.clinic.rw.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.domain.Office;

@Repository
public interface OfficeRepository  extends JpaRepository<Office,UUID>{

    List<Office> findByName(String name);

    Optional<Office> findByNameAndOfficeNumber(String name, int officeNumber);

    Optional<Office> findByDoctor(Doctor doctor);

    Optional<Office>  findByOfficeNumber(int officeNumber);

    // C3 (JPQL): Appointment -> Doctor -> Office, one group per office.
    // Each row is [officeName, officeNumber, appointmentCount], busiest first.
    // Doctors without an office drop out because the join to office is an inner join.
    // The second sort key makes ties deterministic (lowest office number wins).
    // The caller passes PageRequest.of(0, 1) so the database returns only the top row.
    @Query("SELECT o.name, o.officeNumber, COUNT(a) FROM Appointment a "
         + "JOIN a.doctor d JOIN d.office o "
         + "GROUP BY o.id, o.name, o.officeNumber "
         + "ORDER BY COUNT(a) DESC, o.officeNumber ASC")
    List<Object[]> findOfficesByAppointmentCount(Pageable pageable);

}
