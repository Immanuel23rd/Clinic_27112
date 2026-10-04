package kigali.clinic.rw.service;
import java.util.List; import java.util.UUID;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import kigali.clinic.rw.domain.Specialization; import kigali.clinic.rw.repository.SpecializationRepository;
@Service @Transactional
public class SpecializationService {
 private final SpecializationRepository repository;
 public SpecializationService(SpecializationRepository repository){this.repository=repository;}
 public Specialization create(Specialization input){String name=validName(input.getName()); if(repository.findByNameIgnoreCase(name).isPresent()) throw new RelationshipConflictException("Specialization already exists: "+name); input.setName(name); return repository.save(input);}
 @Transactional(readOnly=true) public List<Specialization> findAll(){return repository.findAll();}
 @Transactional(readOnly=true) public Specialization findById(UUID id){return repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Specialization not found: "+id));}
 public Specialization update(UUID id,Specialization input){Specialization old=findById(id); String name=validName(input.getName()); repository.findByNameIgnoreCase(name).ifPresent(found->{if(!found.getId().equals(id))throw new RelationshipConflictException("Specialization already exists: "+name);}); old.setName(name); return repository.save(old);}
 public void delete(UUID id){Specialization old=findById(id); for (var doctor : old.getDoctors()) { doctor.getSpecializations().remove(old); } old.getDoctors().clear(); repository.delete(old);}
 private String validName(String name){if(name==null||name.isBlank())throw new IllegalArgumentException("Specialization name is required"); return name.trim();}
}
