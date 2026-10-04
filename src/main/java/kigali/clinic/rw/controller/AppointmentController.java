package kigali.clinic.rw.controller;

import java.net.URI;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.service.AppointmentService;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<Appointment> create(@RequestBody Appointment appointment) {
        Appointment saved = appointmentService.create(appointment);
        return ResponseEntity
                .created(URI.create("/api/appointments/" + saved.getId()))
                .body(saved);
    }

    // A4: POST /api/appointments/save
    // Plain-text 409 with the exact message from the quiz when the doctor is double booked.
    // Returns ResponseEntity<?> because the body is either an Appointment or a String.
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Appointment appointment) {
        if (appointmentService.isDoctorBooked(appointment)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(AppointmentService.DOUBLE_BOOKED_MESSAGE);
        }
        Appointment saved = appointmentService.create(appointment);
        return ResponseEntity
                .created(URI.create("/api/appointments/" + saved.getId()))
                .body(saved);
    }

    @GetMapping
    public List<Appointment> findAll() {
        return appointmentService.findAll();
    }

    // The fixed paths below ("by-status", "between", "page", ...) do not clash with "/{id}":
    // Spring always prefers the more specific literal path over a path variable.

    // A2: GET /api/appointments/by-status?status=SCHEDULED
    // Spring converts the text into the AppointmentStatus enum; an unknown value gives HTTP 400.
    @GetMapping("/by-status")
    public List<Appointment> byStatus(@RequestParam AppointmentStatus status) {
        return appointmentService.findByStatus(status);
    }

    // A3: GET /api/appointments/between?start=2026-10-01&end=2026-10-31
    @GetMapping("/between")
    public List<Appointment> between(@RequestParam String start, @RequestParam String end) {
        return appointmentService.findBetween(toSqlDate(start), toSqlDate(end));
    }

    // C1: GET /api/appointments/stats/by-status  ->  [["SCHEDULED",4],["COMPLETED",2],...]
    @GetMapping("/stats/by-status")
    public List<Object[]> statsByStatus() {
        return appointmentService.countByStatus();
    }

    // Bonus: GET /api/appointments/page?page=0&size=5&sort=appointmentDate,desc
    // The Pageable is built here from the three plain request parameters.
    @GetMapping("/page")
    public Page<Appointment> page(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "5") int size,
                                  @RequestParam(defaultValue = "appointmentDate,asc") String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromString(parts[1].trim())   // "asc" / "desc", otherwise IllegalArgumentException -> 400
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, parts[0].trim()));
        // Page<> serialises with content, totalPages and totalElements.
        return appointmentService.findPage(pageable);
    }

    @GetMapping("/{id}")
    public Appointment findById(@PathVariable UUID id) {
        return appointmentService.findById(id);
    }

    @PutMapping("/{id}")
    public Appointment update(@PathVariable UUID id,
                              @RequestBody Appointment appointment) {
        return appointmentService.update(id, appointment);
    }

    // C4: PATCH /api/appointments/cancel-day?doctorId=...&date=2026-10-20
    @PatchMapping("/cancel-day")
    public String cancelDay(@RequestParam UUID doctorId, @RequestParam String date) {
        return appointmentService.cancelDay(doctorId, toSqlDate(date));
    }

    // Bonus: DELETE /api/appointments/cancelled-before?date=2026-10-15
    @DeleteMapping("/cancelled-before")
    public String deleteCancelledBefore(@RequestParam String date) {
        return appointmentService.deleteCancelledBefore(toSqlDate(date));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // The quiz asks for LocalDate parsing in the controller. The Appointment entity stores
    // java.sql.Date, so the parsed LocalDate is converted before it reaches the repository.
    // A malformed date throws DateTimeParseException, which GlobalExceptionHandler turns into 400.
    private static Date toSqlDate(String text) {
        LocalDate parsed = LocalDate.parse(text);
        return Date.valueOf(parsed);
    }
}