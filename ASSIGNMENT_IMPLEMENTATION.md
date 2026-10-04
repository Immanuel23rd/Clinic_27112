# Clinic assignment implementation notes

This file records what was added for the class exercises on top of the base CRUD layer
(see `CRUD_GUIDE.md` for the base layer). It covers two pieces of work:

1. Practical Exercise 4: specializations and five appointment queries.
2. The JPA & JPQL in-class quiz: 12 requirements plus 2 bonus items.

## Implemented

- Doctor CRUD, with specializations assigned by ID through `DoctorService`.
- Specialization CRUD.
- Doctor–Specialization many-to-many join table `doctor_specialization` (owned by `Doctor`).
- Five appointment queries from Practical Exercise 4, exposed under `/api/queries`.
- Twelve quiz requirements (derived queries, JPQL joins, aggregates, bulk update) and two bonus items.
- `AppointmentStatus` has five values: `PENDING`, `SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`.
  `PENDING` was added for Exercise 4. The quiz uses the other four. A new appointment with no status is saved as `SCHEDULED`.
- JSON recursion guards: `@JsonIgnore` on the reverse side of every bidirectional relationship
  (`Doctor.appointments`, `Patient.appointments`, `Office.doctor`, `Specialization.doctors`).

## Base CRUD endpoints

All five resources follow the same shape: `POST /api/<resource>`, `GET /api/<resource>`,
`GET /api/<resource>/{id}`, `PUT /api/<resource>/{id}`, `DELETE /api/<resource>/{id}`.

| Resource | Base path |
|---|---|
| Doctor | `/api/doctors` |
| Specialization | `/api/specializations` |
| Office | `/api/offices` |
| Patient | `/api/patients` |
| Appointment | `/api/appointments` |

Doctor create/update accepts existing specialization and office IDs, for example:

```json
{
  "firstName": "Aline",
  "lastName": "Uwase",
  "dateOfBirth": "1995-04-12",
  "office": { "id": "EXISTING-OFFICE-UUID" },
  "specializations": [
    { "id": "EXISTING-SPECIALIZATION-UUID" }
  ]
}
```

Create a specialization first with `POST /api/specializations` and `{ "name": "Cardiology" }`, then use its
returned UUID in the doctor payload. The office ID must refer to an existing office, and an office can
belong to only one doctor. The service resolves every ID to a managed entity instead of persisting the
request-body copy.

## Exercise 4 query endpoints (`/api/queries`)

| # | Endpoint | Query type | Behaviour |
|---|---|---|---|
| 1 | `GET /api/queries/doctors/{doctorId}/pending` | Derived | `PENDING` appointments of one doctor, earliest date first |
| 2 | `GET /api/queries/past-pending` | JPQL | `PENDING` appointments dated before today. Returns `Appointment` entities, not a DTO, as requested |
| 3 | `GET /api/queries/appointment-counts` | JPQL aggregate | Appointment count per doctor, highest count first (interface projection) |
| 4 | `GET /api/queries/specializations/{name}/appointments` | JPQL join | Appointments of doctors holding that specialization (case-insensitive) |
| 5 | `GET /api/queries/doctors/{doctorId}/appointments?page=1&size=10` | Derived + `Pageable` | Page of one doctor's appointments sorted by date. **`page` is 1-based here** |

The bonus paging endpoint of the quiz (`/api/appointments/page`) is **0-based**, because it passes the
`page` parameter to `PageRequest.of` unchanged. The two endpoints behave differently on purpose.

## Quiz endpoints

Every requirement has three pieces: a repository method, a service method and a controller endpoint.
A requirement marked DERIVED uses a method name only. A requirement marked JPQL uses `@Query`.

### Part A: derived queries

| ID | Endpoint | Repository method | Behaviour |
|---|---|---|---|
| A1 | `GET /api/patients/by-last-name?lastName=uwase` | `PatientRepository.findByLastNameIgnoreCaseOrderByFirstNameAsc` | Case-insensitive match, sorted by first name A to Z, empty list when nobody matches |
| A2 | `GET /api/appointments/by-status?status=SCHEDULED` | `AppointmentRepository.findByStatusOrderByAppointmentDateAsc` | Enum parameter, earliest date first. An unknown status returns 400 |
| A3 | `GET /api/appointments/between?start=2026-10-01&end=2026-10-31` | `AppointmentRepository.findByAppointmentDateBetweenOrderByAppointmentDateAsc` | Both dates inclusive, ordered by date |
| A4 | `POST /api/appointments/save` | `AppointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusNot` | Rejects a second non-cancelled appointment for the same doctor and date |

Details that are easy to get wrong:

- **Dates (A3 and every date parameter).** The controller parses the text with `LocalDate.parse` and then
  converts it with `java.sql.Date.valueOf`, because the `Appointment` entity still stores `java.sql.Date`.
  A malformed date returns `400` with `{"error": "Dates must use the format yyyy-MM-dd"}`.
- **A4 response.** When the doctor is already booked, `/save` returns HTTP `409` with the plain-text body
  `Doctor is already booked on that date`. Otherwise it returns `201` with the saved appointment.
- **A4 coverage.** `AppointmentService.create` runs the same check, so the older `POST /api/appointments`
  is also protected, but it reports the conflict as JSON (`{"error": "..."}`) through `GlobalExceptionHandler`.
- **A4 scope.** The check runs on creation only. `PUT /api/appointments/{id}` does not re-check double booking.

### Part B: JPQL with joins

| ID | Endpoint | Repository method | Behaviour |
|---|---|---|---|
| B1 | `GET /api/doctors/by-specialization?name=cardiology` | `DoctorRepository.findBySpecializationName` | `JOIN d.specializations s` with `LOWER(s.name) = LOWER(:name)` |
| B2 | `GET /api/doctors/without-office` | `DoctorRepository.findDoctorsWithoutOffice` | `d.office IS NULL`, sorted by last name |
| B3 | `GET /api/specializations/unused` | `SpecializationRepository.findUnused` | `s.doctors IS EMPTY` (tests the collection, no `COUNT`) |
| B4 | `GET /api/patients/of-doctor/{doctorId}` | `PatientRepository.findPatientsOfDoctor` | `SELECT DISTINCT p ... JOIN p.appointments a WHERE a.doctor.id = :doctorId` |

B4 answers an unknown doctor ID with HTTP `404` and the plain-text body
`The doctor with that id does not exist`. `PatientService.findByDoctor` returns an empty `Optional` for
"doctor not found" and an `Optional` holding an empty list for "doctor exists, no patients", so the
controller can tell the two cases apart.

### Part C: aggregates and bulk updates

| ID | Endpoint | Repository method | Behaviour |
|---|---|---|---|
| C1 | `GET /api/appointments/stats/by-status` | `AppointmentRepository.countAppointmentsByStatus` | `List<Object[]>`, one `[status, count]` row per status that has appointments |
| C2 | `GET /api/patients/frequent?min=3` | `PatientRepository.findFrequentPatients` | `GROUP BY p HAVING COUNT(a) >= :min ORDER BY COUNT(a) DESC`, returns `Patient` entities |
| C3 | `GET /api/offices/busiest` | `OfficeRepository.findOfficesByAppointmentCount` | Appointment → Doctor → Office, grouped per office, top row only (`PageRequest.of(0, 1)`) |
| C4 | `PATCH /api/appointments/cancel-day?doctorId=...&date=2026-10-20` | `AppointmentRepository.cancelDay` | One `@Modifying` UPDATE, returns `"N appointments cancelled"` |

- **C3 response.** A JSON array such as `["Room 101", 101, 5]` (name, number, count), or the plain text
  `No appointments yet` with HTTP 200 when no appointment belongs to a doctor with an office. Ties are broken
  by the lower office number.
- **C4 semantics.** The UPDATE sets `CANCELLED` on every appointment of that doctor on that date whose status
  is not `COMPLETED`, exactly as the quiz states. Rows that were already `CANCELLED` are updated again
  and counted, so the number is the number of rows the statement touched, not the number whose status changed.
  The repository method uses `clearAutomatically = true` so stale entities do not remain in the persistence context.

### Bonus

| Endpoint | Repository method | Behaviour |
|---|---|---|
| `GET /api/appointments/page?page=0&size=5&sort=appointmentDate,desc` | inherited `findAll(Pageable)` | The controller builds the `Pageable` from the three parameters. The response contains `content`, `totalPages` and `totalElements`. Defaults: page 0, size 5, sort `appointmentDate,asc` |
| `DELETE /api/appointments/cancelled-before?date=2026-10-15` | `AppointmentRepository.deleteByStatusBefore` | One JPQL DELETE of every `CANCELLED` appointment dated strictly before the date. Returns `"N appointments deleted"` |

An invalid sort direction or a non-positive page size returns 400. A `sort` property that is not an
`Appointment` field is not handled specially and produces a server error from Spring Data.

### Read-only appointment rule and the bulk queries

`AppointmentService.update` and `delete` refuse to touch `COMPLETED` or `CANCELLED` appointments (409).
The bulk queries work at the SQL level and do not go through that check:

- C4 skips `COMPLETED` rows but re-writes `CANCELLED` ones with the same status.
- The bonus DELETE removes `CANCELLED` appointments in bulk, which the single-record `DELETE /api/appointments/{id}` would refuse.

## Seed data for the quiz

Enter this data through the save endpoints, in this order (details and JSON bodies are in the
`0 Seed data` folder of `clinic-quiz.postman_collection.json`):

| Entity | Rows |
|---|---|
| Specialization | Cardiology, Pediatrics, Dermatology, Neurology (nobody holds Neurology) |
| Office | Room 101, Room 102, Room 103 (103 stays unassigned) |
| Doctor | Aline Mukamana (office 101, Cardiology + Pediatrics), Eric Habimana (office 102, Dermatology), Grace Ingabire (no office, Cardiology) |
| Patient | Jean Uwase, Marie Uwase, Eric Mugisha, Claire Niyonsenga, David Kamanzi |
| Appointment | 10 appointments from 2026-10-05 to 2026-11-20 covering all four quiz statuses |

Order matters for A4: for Aline Mukamana on 2026-10-20 the `CANCELLED` appointment must be saved before the
`CONFIRMED` one. Saving a second non-cancelled appointment for the same doctor and date is rejected with 409.

Expected results with this data:

| Check | Expected |
|---|---|
| C1 status counts | SCHEDULED 4, CONFIRMED 2, COMPLETED 2, CANCELLED 2 |
| A1 `lastName=uwase` | Jean, then Marie |
| B1 `name=cardiology` | Aline and Grace |
| B2 | Grace |
| B3 | Neurology |
| B4 for Aline | Jean, Marie, Eric, David (four distinct patients) |
| C2 `min=3` | Jean Uwase |
| C3 | `["Room 101", 101, 5]` |
| C4 for Aline on 2026-10-20 | `2 appointments cancelled` |
| Bonus delete before 2026-10-15 | `1 appointments deleted` |

Run C4 and the bonus DELETE last, because they change the data the other checks rely on.

## Notes

- `AppointmentStatus.PENDING` was added because the Exercise 4 queries filter by `PENDING`.
- Specialization associations are sent as IDs; the service resolves those IDs to managed entities.
- The project uses the PostgreSQL settings in `application.properties` (database `clinic`, port `8081`).
  Adjust the credentials for your local database before running.
- Verification status: the quiz queries were written and reviewed against the quiz sheet. Run the Postman
  collection against a live PostgreSQL database, and check the SQL that `spring.jpa.show-sql=true` prints,
  before relying on them.