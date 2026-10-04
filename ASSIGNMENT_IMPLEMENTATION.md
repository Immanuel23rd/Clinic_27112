# Clinic assignment implementation notes

## Implemented
- Doctor CRUD endpoints and specialization assignment through the existing `DoctorService`.
- Specialization CRUD endpoints.
- Doctor–Specialization many-to-many join table `doctor_specialization`.
- Appointment repository queries from Practical Exercise 4 and a small controller exposing them.
- `PENDING` appointment status.
- JSON recursion guards for bidirectional relationships.

## CRUD endpoints
- `POST /api/doctors`, `GET /api/doctors`, `GET /api/doctors/{id}`, `PUT /api/doctors/{id}`, `DELETE /api/doctors/{id}`
- `POST /api/specializations`, `GET /api/specializations`, `GET /api/specializations/{id}`, `PUT /api/specializations/{id}`, `DELETE /api/specializations/{id}`

Doctor create/update accepts existing specialization IDs, for example:
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
Create a specialization first using `POST /api/specializations` with `{ "name": "Cardiology" }`, then use its returned UUID in the doctor payload. Office ID must refer to an existing office.

## Exercise 4 query endpoints
1. `GET /api/queries/doctors/{doctorId}/pending` — derived query, pending appointments ascending by appointment date.
2. `GET /api/queries/past-pending` — JPQL query for pending appointments dated before today. As requested, it returns Appointment entities rather than a DTO.
3. `GET /api/queries/appointment-counts` — aggregate count by doctor, descending by count.
4. `GET /api/queries/specializations/{name}/appointments` — joins appointments to doctors and specializations.
5. `GET /api/queries/doctors/{doctorId}/appointments?page=1&size=10` — page is 1-based in the URL; results sort by appointment date ascending.

## Notes
- `AppointmentStatus.PENDING` was added because the slide's queries explicitly filter by PENDING.
- Specialization associations are sent as IDs; the service resolves those IDs to managed entities rather than trying to persist request-body copies.
- The project uses the PostgreSQL connection settings in `application.properties`; adjust credentials for your local database before running.
