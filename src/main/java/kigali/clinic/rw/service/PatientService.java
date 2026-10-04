package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kigali.clinic.rw.domain.Patient;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.PatientRepository;

@Service
@Transactional
public class PatientService {
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          DoctorRepository doctorRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
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

    // ------------------------------------------------------------------
    // Quiz features
    // ------------------------------------------------------------------

    // A1
    @Transactional(readOnly = true)
    public List<Patient> findByLastName(String lastName) {
        return patientRepository.findByLastNameIgnoreCaseOrderByFirstNameAsc(lastName);
    }

    /**
     * B4: an EMPTY Optional means "this doctor id does not exist" (the controller answers 404).
     * An Optional holding an empty list means "doctor exists but has no patients yet" (200 with []).
     * The two cases must stay separate, which is why a plain List is not enough here.
     */
    @Transactional(readOnly = true)
    public Optional<List<Patient>> findByDoctor(UUID doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            return Optional.empty();
        }
        return Optional.of(patientRepository.findPatientsOfDoctor(doctorId));
    }

    // C2
    @Transactional(readOnly = true)
    public List<Patient> findFrequent(long min) {
        return patientRepository.findFrequentPatients(min);
    }
}
