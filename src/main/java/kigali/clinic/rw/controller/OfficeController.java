package kigali.clinic.rw.controller;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import kigali.clinic.rw.domain.Office;
import kigali.clinic.rw.service.OfficeService;

@RestController
@RequestMapping("/api/offices" )
public class OfficeController {
    private final OfficeService service;

    public OfficeController(OfficeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Office> create(@Valid @RequestBody Office office) {
        Office saved = service.create(office);
        return ResponseEntity
                .created(URI.create("/api/offices/" + saved.getId()))
                .body(saved);
    }

    @GetMapping
    public List<Office> findAll() {
        return service.findAll();
    }

    // C3: GET /api/offices/busiest  ->  ["Main Clinic",101,5]  or  "No appointments yet"
    // The literal path wins over "/{id}". ResponseEntity<?> because the body is an Object[] or a String.
    @GetMapping("/busiest")
    public ResponseEntity<?> busiest() {
        Optional<Object[]> top = service.findBusiest();
        if (top.isEmpty()) {
            return ResponseEntity.ok("No appointments yet");
        }
        return ResponseEntity.ok(top.get());
    }

    @GetMapping("/{id}")
    public Office findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Office update(@PathVariable UUID id,
                         @Valid @RequestBody Office office) {
        return service.update(id, office);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
