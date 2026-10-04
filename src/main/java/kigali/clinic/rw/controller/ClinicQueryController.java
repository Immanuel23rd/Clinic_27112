package kigali.clinic.rw.controller;

import java.sql.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.repository.AppointmentRepository;

@RestController
@RequestMapping("/api/queries")
public class ClinicQueryController {
    private final AppointmentRepository appointments;
    public ClinicQueryController(AppointmentRepository appointments) { this.appointments = appointments; }

    // 1. GET /api/queries/doctors/{doctorId}/pending
    @GetMapping("/doctors/{doctorId}/pending")
    public List<Appointment> pendingForDoctor(@PathVariable UUID doctorId) {
        return appointments.findByDoctorIdAndStatusOrderByAppointmentDateAsc(doctorId, AppointmentStatus.PENDING);
    }

    // 2. GET /api/queries/past-pending
    @GetMapping("/past-pending")
    public List<Appointment> pastPending() {
        return appointments.findPastAppointmentsByStatus(AppointmentStatus.PENDING, new Date(System.currentTimeMillis()));
    }

    // 3. GET /api/queries/appointment-counts
    @GetMapping("/appointment-counts")
    public List<AppointmentRepository.DoctorAppointmentCount> countsPerDoctor() {
        return appointments.countAppointmentsPerDoctor();
    }

    // 4. GET /api/queries/specializations/{name}/appointments
    @GetMapping("/specializations/{name}/appointments")
    public List<Appointment> bySpecialization(@PathVariable String name) {
        return appointments.findAppointmentsByDoctorSpecialization(name);
    }

    // page is 1-based for the URL; Spring Data's page index is 0-based.
    // 5. GET /api/queries/doctors/{doctorId}/appointments?page=1&size=10
    @GetMapping("/doctors/{doctorId}/appointments")
    public Page<Appointment> doctorAppointments(@PathVariable UUID doctorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 1 || size < 1) throw new IllegalArgumentException("page and size must be positive");
        return appointments.findByDoctorId(doctorId,
            PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "appointmentDate")));
    }
}
