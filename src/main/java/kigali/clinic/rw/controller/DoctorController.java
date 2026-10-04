package kigali.clinic.rw.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.service.DoctorService;

@RestController
@RequestMapping("/api/doctors" )
public class DoctorController {
    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Doctor> create(@Valid @RequestBody Doctor doctor) {
        Doctor saved = service.create(doctor);
        return ResponseEntity
                .created(URI.create("/api/doctors/" + saved.getId()))
                .body(saved);
    }

    @GetMapping
    public List<Doctor> findAll() {
        return service.findAll();
    }

    // The fixed paths below do not clash with "/{id}": Spring prefers the literal path.

    // B1: GET /api/doctors/by-specialization?name=cardiology
    @GetMapping("/by-specialization")
    public List<Doctor> bySpecialization(@RequestParam String name) {
        return service.findBySpecialization(name);
    }

    // B2: GET /api/doctors/without-office
    @GetMapping("/without-office")
    public List<Doctor> withoutOffice() {
        return service.findWithoutOffice();
    }

    @GetMapping("/{id}")
    public Doctor findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Doctor update(@PathVariable UUID id,
                         @RequestBody Doctor doctor) {
        return service.update(id, doctor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
