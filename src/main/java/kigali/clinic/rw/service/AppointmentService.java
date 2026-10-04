package kigali.clinic.rw.service;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.domain.Patient;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.PatientRepository;

@Service
@Transactional
public class AppointmentService {

    // Shared by create() and the /save controller so the message is defined once.
    public static final String DOUBLE_BOOKED_MESSAGE = "Doctor is already booked on that date";

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    public Appointment create(Appointment input) {
        input.setDoctor(resolveDoctor(input.getDoctor()));
        input.setPatient(resolvePatient(input.getPatient()));
        // A4: re-checked here so EVERY way of creating an appointment is protected,
        // not only the /save endpoint.
        if (isDoctorBooked(input)) {
            throw new RelationshipConflictException(DOUBLE_BOOKED_MESSAGE);
        }
        if (input.getStatus() == null) {
            input.setStatus(AppointmentStatus.SCHEDULED);
        }
        return appointmentRepository.save(input);
    }

    /**
     * A4: true when the doctor already has a NON-cancelled appointment on the same date.
     * Returns false when the doctor or date is missing, so create() can report the
     * proper "doctor not found" error instead of failing on a null here.
     */
    @Transactional(readOnly = true)
    public boolean isDoctorBooked(Appointment input) {
        if (input.getDoctor() == null || input.getDoctor().getId() == null
                || input.getAppointmentDate() == null) {
            return false;
        }
        return appointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusNot(
                input.getDoctor().getId(),
                input.getAppointmentDate(),
                AppointmentStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found: " + id));
    }

    public Appointment update(UUID id, Appointment input) {
        Appointment existing = findById(id);
        ensureMutable(existing);

        existing.setAppointmentDate(input.getAppointmentDate());
        existing.setReason(input.getReason());
        existing.setStatus(input.getStatus() == null
                ? existing.getStatus() : input.getStatus());
        existing.setDoctor(resolveDoctor(input.getDoctor()));
        existing.setPatient(resolvePatient(input.getPatient()));
        return appointmentRepository.save(existing);
    }

    public void delete(UUID id) {
        Appointment existing = findById(id);
        ensureMutable(existing);
        appointmentRepository.delete(existing);
    }

    // ------------------------------------------------------------------
    // Quiz features
    // ------------------------------------------------------------------

    // A2
    @Transactional(readOnly = true)
    public List<Appointment> findByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatusOrderByAppointmentDateAsc(status);
    }

    // A3
    @Transactional(readOnly = true)
    public List<Appointment> findBetween(Date start, Date end) {
        return appointmentRepository.findByAppointmentDateBetweenOrderByAppointmentDateAsc(start, end);
    }

    // C1: rows of [status, count]
    @Transactional(readOnly = true)
    public List<Object[]> countByStatus() {
        return appointmentRepository.countAppointmentsByStatus();
    }

    // C4: bulk UPDATE needs a transaction. The class is already @Transactional,
    // but it is repeated on the method because this is the one that writes in bulk.
    @Transactional
    public String cancelDay(UUID doctorId, Date date) {
        int changed = appointmentRepository.cancelDay(
                doctorId, date, AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED);
        return changed + " appointments cancelled";
    }

    // Bonus: paging is delegated to Spring Data; the Pageable comes from the controller.
    @Transactional(readOnly = true)
    public Page<Appointment> findPage(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    // Bonus: bulk DELETE of old cancellations.
    @Transactional
    public String deleteCancelledBefore(Date date) {
        int deleted = appointmentRepository.deleteByStatusBefore(AppointmentStatus.CANCELLED, date);
        return deleted + " appointments deleted";
    }

    private void ensureMutable(Appointment appointment) {
        AppointmentStatus status = appointment.getStatus();
        if (status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.CANCELLED) {
            throw new RelationshipConflictException(
                    "Completed or cancelled appointments are read-only");
        }
    }

    private Doctor resolveDoctor(Doctor requestedDoctor) {
        if (requestedDoctor == null || requestedDoctor.getId() == null) {
            throw new ResourceNotFoundException(
                    "A valid doctor ID is required for an appointment");
        }
        return doctorRepository.findById(requestedDoctor.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + requestedDoctor.getId()));
    }

    private Patient resolvePatient(Patient requestedPatient) {
        if (requestedPatient == null || requestedPatient.getId() == null) {
            throw new ResourceNotFoundException(
                    "A valid patient ID is required for an appointment");
        }
        return patientRepository.findById(requestedPatient.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found: " + requestedPatient.getId()));
    }
}
