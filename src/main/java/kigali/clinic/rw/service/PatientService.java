package kigali.clinic.rw.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kigali.clinic.rw.domain.Patient;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.PatientRepository;

@Service
@Transactional
public class PatientService {
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Patient create(Patient patient) {
        return patientRepository.save(patient);
    }

    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Patient findById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }

    public Patient update(UUID id, Patient input) {
        Patient existing = findById(id);
        existing.setFirstName(input.getFirstName());
        existing.setLastName(input.getLastName());
        existing.setDateOfBirth(input.getDateOfBirth());
        return patientRepository.save(existing);
    }

    public void delete(UUID id) {
        Patient patient = findById(id);
        if (appointmentRepository.existsByPatientId(id)) {
            throw new RelationshipConflictException(
                    "Patient cannot be deleted while appointments reference the patient");
        }
        patientRepository.delete(patient);
    }
}
