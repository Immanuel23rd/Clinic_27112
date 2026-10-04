package kigali.clinic.rw.domain;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
@Entity
@Table(name="specialization")
public class Specialization {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="name", nullable=false, unique=true) private String name;
 @ManyToMany(mappedBy="specializations") @JsonIgnore private List<Doctor> doctors=new ArrayList<>();
 public UUID getId(){return id;} public String getName(){return name;} public void setName(String name){this.name=name;}
 public List<Doctor> getDoctors(){return doctors;} public void setDoctors(List<Doctor> doctors){this.doctors=doctors;}
}
