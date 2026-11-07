# Slotly API

Plataforma de reservas para negocios pequeños (barberías, consultorios, estudios).
El requisito central: **un mismo profesional nunca puede quedar reservado dos veces
en horarios que se solapan**, aunque las peticiones lleguen al mismo tiempo.

Las decisiones de diseño están en [ADR-0001](docs/adr/ADR-0001.md).

## Concurrencia: antes y después

El test `BookSlotRaceTest` lanza 20 reservas simultáneas para el mismo turno.

### Antes: validar solo en la aplicación

El caso de uso preguntaba "¿está libre?" y luego guardaba. Entre ambos pasos hay un
hueco: varias peticiones pasan la validación antes de que ninguna guarde.

```text
[bookings saved for the same slot]
expected: 1
 but was: 10
```

Se guardaron 10 reservas para el mismo turno: una por cada conexión del pool.

### Después: dos mecanismos

1. **Bloqueo por profesional** (`SELECT ... FOR UPDATE` sobre `provider`): las reservas
   del mismo profesional se procesan una a la vez; las de profesionales distintos,
   en paralelo.
2. **Exclusion constraint** `booking_no_overlap` en PostgreSQL: la base de datos
   rechaza cualquier solapamiento, aunque algún camino del código olvide el bloqueo.

Resultado: **1 reserva guardada y 19 rechazadas** con `SlotUnavailableException` (HTTP 409).

Solo con el constraint, la carrera producía deadlocks (`40P01`): la regla seguía
protegiendo los datos, pero el cliente recibía un error genérico en vez de un 409.

## Cómo correr los tests

Requiere Java 21 y Docker en ejecución (los tests de integración usan Testcontainers).

```bash
./mvnw test
```