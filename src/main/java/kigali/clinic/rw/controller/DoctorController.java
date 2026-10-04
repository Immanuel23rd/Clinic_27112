package kigali.clinic.rw.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.service.DoctorService;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorService service;
    public DoctorController(DoctorService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Doctor> create(@RequestBody Doctor doctor) {
        Doctor saved = service.create(doctor);
        return ResponseEntity.created(URI.create("/api/doctors/" + saved.getId())).body(saved);
    }
    @GetMapping public List<Doctor> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public Doctor findById(@PathVariable UUID id) { return service.findById(id); }
    @PutMapping("/{id}") public Doctor update(@PathVariable UUID id, @RequestBody Doctor doctor) { return service.update(id, doctor); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
