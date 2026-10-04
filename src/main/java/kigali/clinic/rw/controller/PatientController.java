package kigali.clinic.rw.controller;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import kigali.clinic.rw.domain.Patient;
import kigali.clinic.rw.service.PatientService;

@RestController
@RequestMapping("/api/patients" )
public class PatientController {
    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Patient> create(@Valid @RequestBody Patient patient) {
        Patient saved = service.create(patient);
        return ResponseEntity
                .created(URI.create("/api/patients/" + saved.getId()))
                .body(saved);
    }

    @GetMapping
    public List<Patient> findAll() {
        return service.findAll();
    }

    // The fixed paths below do not clash with "/{id}": Spring prefers the literal path.

    // A1: GET /api/patients/by-last-name?lastName=uwase
    @GetMapping("/by-last-name")
    public List<Patient> byLastName(@RequestParam String lastName) {
        return service.findByLastName(lastName);
    }

    // B4: GET /api/patients/of-doctor/{doctorId}
    // ResponseEntity<?> because the body is either a List<Patient> or a plain-text message.
    @GetMapping("/of-doctor/{doctorId}")
    public ResponseEntity<?> ofDoctor(@PathVariable UUID doctorId) {
        Optional<List<Patient>> patients = service.findByDoctor(doctorId);
        if (patients.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("The doctor with that id does not exist");
        }
        return ResponseEntity.ok(patients.get());
    }

    // C2: GET /api/patients/frequent?min=3
    @GetMapping("/frequent")
    public List<Patient> frequent(@RequestParam long min) {
        return service.findFrequent(min);
    }

    @GetMapping("/{id}")
    public Patient findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable UUID id,
                         @Valid @RequestBody Patient patient) {
        return service.update(id, patient);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
