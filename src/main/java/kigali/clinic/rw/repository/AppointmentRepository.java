package kigali.clinic.rw.repository;
import java.sql.Date; 
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
public interface AppointmentRepository extends JpaRepository<Appointment,UUID> {
 boolean existsByDoctorId(UUID doctorId); boolean existsByPatientId(UUID patientId);
 List<Appointment> findByDoctorIdAndStatusOrderByAppointmentDateAsc(UUID doctorId, AppointmentStatus status);
 @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.appointmentDate < :today ORDER BY a.appointmentDate ASC")
 List<Appointment> findPastAppointmentsByStatus(@Param("status") AppointmentStatus status,@Param("today") Date today);
 @Query("SELECT a.doctor.id AS doctorId, a.doctor.firstName AS firstName, a.doctor.lastName AS lastName, COUNT(a) AS appointmentCount FROM Appointment a GROUP BY a.doctor.id, a.doctor.firstName, a.doctor.lastName ORDER BY COUNT(a) DESC")
 List<DoctorAppointmentCount> countAppointmentsPerDoctor();
 @Query("SELECT DISTINCT a FROM Appointment a JOIN a.doctor d JOIN d.specializations s WHERE LOWER(s.name)=LOWER(:specializationName) ORDER BY a.appointmentDate ASC")
 List<Appointment> findAppointmentsByDoctorSpecialization(@Param("specializationName") String specializationName);
 Page<Appointment> findByDoctorId(UUID doctorId, Pageable pageable);
 interface DoctorAppointmentCount { UUID getDoctorId(); String getFirstName(); String getLastName(); Long getAppointmentCount(); }
}
