package kigali.clinic.rw.repository;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // ------------------------------------------------------------------
    // Existing methods (CRUD rules + Practical Exercise 4). Unchanged.
    // ------------------------------------------------------------------
    boolean existsByDoctorId(UUID doctorId);

    boolean existsByPatientId(UUID patientId);

    List<Appointment> findByDoctorIdAndStatusOrderByAppointmentDateAsc(UUID doctorId, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.appointmentDate < :today ORDER BY a.appointmentDate ASC")
    List<Appointment> findPastAppointmentsByStatus(@Param("status") AppointmentStatus status, @Param("today") Date today);

    @Query("SELECT a.doctor.id AS doctorId, a.doctor.firstName AS firstName, a.doctor.lastName AS lastName, COUNT(a) AS appointmentCount FROM Appointment a GROUP BY a.doctor.id, a.doctor.firstName, a.doctor.lastName ORDER BY COUNT(a) DESC")
    List<DoctorAppointmentCount> countAppointmentsPerDoctor();

    @Query("SELECT DISTINCT a FROM Appointment a JOIN a.doctor d JOIN d.specializations s WHERE LOWER(s.name)=LOWER(:specializationName) ORDER BY a.appointmentDate ASC")
    List<Appointment> findAppointmentsByDoctorSpecialization(@Param("specializationName") String specializationName);

    Page<Appointment> findByDoctorId(UUID doctorId, Pageable pageable);

    interface DoctorAppointmentCount {
        UUID getDoctorId();
        String getFirstName();
        String getLastName();
        Long getAppointmentCount();
    }

    // ------------------------------------------------------------------
    // Part A (DERIVED: the query is built from the method name only)
    // ------------------------------------------------------------------

    // A2: "ByStatus" -> WHERE status = ?, "OrderByAppointmentDateAsc" -> ORDER BY appointment_date ASC.
    // Spring converts ?status=SCHEDULED into the enum before it reaches this method.
    List<Appointment> findByStatusOrderByAppointmentDateAsc(AppointmentStatus status);

    // A3: "Between" is inclusive on both ends (SQL BETWEEN), so start and end dates are both returned.
    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAsc(Date start, Date end);

    // A4: existsBy + And + Not.
    // "DoctorId" navigates doctor.id; "StatusNot" becomes status <> ?.
    // Passing CANCELLED as the status means: is there a NON-cancelled appointment for this doctor on this date?
    boolean existsByDoctorIdAndAppointmentDateAndStatusNot(UUID doctorId, Date appointmentDate, AppointmentStatus status);

    // ------------------------------------------------------------------
    // Part C (JPQL aggregates and bulk updates)
    // ------------------------------------------------------------------

    // C1: one row per status that actually has appointments (GROUP BY only emits existing groups,
    // so a status with zero appointments never appears). Each row is [status, count].
    @Query("SELECT a.status, COUNT(a) FROM Appointment a GROUP BY a.status")
    List<Object[]> countAppointmentsByStatus();

    // C4: one UPDATE statement, no Java loop. It returns the number of rows changed.
    // - @Modifying tells Spring Data this query changes data (otherwise it expects a SELECT).
    // - clearAutomatically drops stale entities from the persistence context after the bulk update,
    //   because bulk JPQL bypasses the entity manager and would otherwise leave old statuses in memory.
    // - a.doctor.id compares the doctor_id foreign key directly, so no join is needed in an UPDATE.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Appointment a SET a.status = :cancelled "
         + "WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status <> :completed")
    int cancelDay(@Param("doctorId") UUID doctorId,
                  @Param("date") Date date,
                  @Param("cancelled") AppointmentStatus cancelled,
                  @Param("completed") AppointmentStatus completed);

    // Bonus: single JPQL DELETE for every CANCELLED appointment dated strictly before the given date.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Appointment a WHERE a.status = :status AND a.appointmentDate < :date")
    int deleteByStatusBefore(@Param("status") AppointmentStatus status, @Param("date") Date date);
}
