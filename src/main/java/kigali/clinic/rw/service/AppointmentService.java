package kigali.clinic.rw.service;

import java.util.List;
import java.util.UUID;

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
        if (input.getStatus() == null) {
            input.setStatus(AppointmentStatus.SCHEDULED);
        }
        return appointmentRepository.save(input);
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
