package kigali.clinic.rw.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.domain.Office;
import kigali.clinic.rw.domain.Specialization;
import kigali.clinic.rw.repository.SpecializationRepository;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.OfficeRepository;

@Service
@Transactional
public class DoctorService {
    private final DoctorRepository doctorRepository;
    private final OfficeRepository officeRepository;
    private final AppointmentRepository appointmentRepository;
    private final SpecializationRepository specializationRepository;

    public DoctorService(DoctorRepository doctorRepository,
                         OfficeRepository officeRepository,
                         AppointmentRepository appointmentRepository,
                         SpecializationRepository specializationRepository) {
        this.doctorRepository = doctorRepository;
        this.officeRepository = officeRepository;
        this.appointmentRepository = appointmentRepository;
        this.specializationRepository = specializationRepository;
    }

    public Doctor create(Doctor input) {
        input.setOffice(resolveAvailableOffice(input.getOffice(), null));
        input.setSpecializations(resolveSpecializations(input.getSpecializations()));
        return doctorRepository.save(input);
    }

    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Doctor findById(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }

    public Doctor update(UUID id, Doctor input) {
        Doctor existing = findById(id);
        existing.setFirstName(input.getFirstName());
        existing.setLastName(input.getLastName());
        existing.setDateOfBirth(input.getDateOfBirth());
        existing.setSpecializations(resolveSpecializations(input.getSpecializations()));

        if (input.getOffice() != null) {
            Office requestedOffice = resolveAvailableOffice(input.getOffice(), existing.getId());
            existing.setOffice(requestedOffice);
        } else if (existing.getOffice() != null) {
            throw new RelationshipConflictException(
                    "A doctor cannot be unassigned from an office through this update");
        }

        return doctorRepository.save(existing);
    }

    public void delete(UUID id) {
        Doctor doctor = findById(id);
        if (appointmentRepository.existsByDoctorId(id)) {
            throw new RelationshipConflictException(
                    "Doctor cannot be deleted while appointments reference the doctor");
        }
        doctorRepository.delete(doctor);
    }

    private List<Specialization> resolveSpecializations(List<Specialization> requested) {
        if (requested == null || requested.isEmpty()) return new java.util.ArrayList<>();
        List<Specialization> resolved = new java.util.ArrayList<>();
        for (Specialization item : requested) {
            if (item == null || item.getId() == null) {
                throw new ResourceNotFoundException("A specialization ID is required");
            }
            Specialization specialization = specializationRepository.findById(item.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found: " + item.getId()));
            if (!resolved.contains(specialization)) resolved.add(specialization);
        }
        return resolved;
    }

    private Office resolveAvailableOffice(Office requestedOffice, UUID requestingDoctorId) {
        if (requestedOffice == null) {
            return null;
        }
        if (requestedOffice.getId() == null) {
            throw new ResourceNotFoundException("An office ID is required when assigning an office");
        }

        Office office = officeRepository.findById(requestedOffice.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Office not found: " + requestedOffice.getId()));

        if (office.getDoctor() != null
                && !office.getDoctor().getId().equals(requestingDoctorId)) {
            throw new RelationshipConflictException(
                    "Office is already assigned to another doctor");
        }
        return office;
    }
}
