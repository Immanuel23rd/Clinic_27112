package kigali.clinic.rw.controller;
import java.net.URI; import java.util.List; import java.util.UUID;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import kigali.clinic.rw.domain.Specialization; import kigali.clinic.rw.service.SpecializationService;
@RestController @RequestMapping("/api/specializations")
public class SpecializationController {
 private final SpecializationService service; public SpecializationController(SpecializationService service){this.service=service;}
 @PostMapping public ResponseEntity<Specialization> create(@RequestBody Specialization input){Specialization saved=service.create(input); return ResponseEntity.created(URI.create("/api/specializations/"+saved.getId())).body(saved);}
 @GetMapping public List<Specialization> findAll(){return service.findAll();}
 @GetMapping("/{id}") public Specialization findById(@PathVariable UUID id){return service.findById(id);}
 @PutMapping("/{id}") public Specialization update(@PathVariable UUID id,@RequestBody Specialization input){return service.update(id,input);}
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id){service.delete(id); return ResponseEntity.noContent().build();}
}
