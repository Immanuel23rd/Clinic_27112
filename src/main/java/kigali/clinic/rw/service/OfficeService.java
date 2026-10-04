package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kigali.clinic.rw.domain.Office;
import kigali.clinic.rw.repository.OfficeRepository;

@Service
@Transactional
public class OfficeService {
    private final OfficeRepository officeRepository;

    public OfficeService(OfficeRepository officeRepository) {
        this.officeRepository = officeRepository;
    }

    public Office create(Office input) {
        ensureOfficeNumberIsAvailable(input.getOfficeNumber(), null);
        return officeRepository.save(input);
    }

    /**
     * Backwards-compatible method for the original /api/office/save endpoint.
     * New controllers should call create() and return the saved resource.
     */
    public String saveOffice(Office input) {
        create(input);
        return "saved successfully";
    }

    @Transactional(readOnly = true)
    public List<Office> findAll() {
        return officeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Office findById(UUID id) {
        return officeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Office not found: " + id));
    }

    // C3: [name, officeNumber, appointmentCount] of the top office, or empty when there are no
    // appointments (the controller then answers "No appointments yet").
    @Transactional(readOnly = true)
    public Optional<Object[]> findBusiest() {
        List<Object[]> rows = officeRepository.findOfficesByAppointmentCount(PageRequest.of(0, 1));
        return rows.stream().findFirst();
    }

    public Office update(UUID id, Office input) {
        Office existing = findById(id);
        ensureOfficeNumberIsAvailable(input.getOfficeNumber(), id);
        existing.setName(input.getName());
        existing.setOfficeNumber(input.getOfficeNumber());
        return officeRepository.save(existing);
    }

    public void delete(UUID id) {
        Office office = findById(id);
        if (office.getDoctor() != null) {
            throw new RelationshipConflictException(
                    "Office cannot be deleted while assigned to a doctor");
        }
        officeRepository.delete(office);
    }

    private void ensureOfficeNumberIsAvailable(int officeNumber, UUID currentOfficeId) {
        officeRepository.findByOfficeNumber(officeNumber).ifPresent(existing -> {
            if (!existing.getId().equals(currentOfficeId)) {
                throw new RelationshipConflictException(
                        "Office number is already in use: " + officeNumber);
            }
        });
    }
}
