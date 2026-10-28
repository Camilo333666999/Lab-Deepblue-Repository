# DeepBlue Rescue UI

Frontend en **React 19 + Vite 8 + JavaScript + TanStack Query 5** para consumir el API REST de DeepBlue Rescue. Forma parte del Laboratorio 4 y vive separado del backend Java.

## Requisitos

- Node.js 22+ y npm.
- Backend Spring Boot en `http://localhost:8080`.
- PostgreSQL con el esquema creado por Flyway.
- Los datos de demostración de `../scripts/demo-data.sql`.

## Puesta en marcha

Desde la raíz del repositorio:

```bash
# 1. Arrancar el backend (crea el esquema con Flyway)
mvn spring-boot:run
```

En otra terminal, aplicar los datos de demostración:

```bash
psql -U postgres -h localhost -d deepblue -f scripts/demo-data.sql
```

Después, arrancar el frontend:

```bash
cd deepblue-ui
npm install
npm run dev
```

Abrir <http://localhost:5173>.

Si PostgreSQL usa otras credenciales, ajusta las variables `DB_URL`, `DB_USER` y `DB_PASSWORD` del backend y el comando `psql`. El seed es idempotente: se puede ejecutar varias veces sin duplicar los registros base, pero no revierte cambios de estado ni tratamientos añadidos durante las pruebas.

## Scripts

| Comando | Descripción |
|---|---|
| `npm run dev` | Servidor de desarrollo Vite con HMR. |
| `npm run build` | Build de producción en `dist/`. |
| `npm run lint` | Análisis estático con Oxlint. |
| `npm run preview` | Sirve el build de producción localmente. |

## Configuración

### Proxy `/api`

`vite.config.js` redirige las peticiones `/api` al backend:

```js
server: {
  proxy: {
    '/api': 'http://localhost:8080',
  },
},
```

El navegador siempre llama rutas relativas (`/api/...`); Vite se encarga del destino. Esto evita CORS durante el desarrollo y permite cambiar el puerto del backend sin tocar los componentes.

### QueryClient

`src/main.jsx` configura:

- `retry: false`: no reintenta errores de validación o de negocio.
- `refetchOnWindowFocus: false`: evita recargas al volver a la pestaña.
- `staleTime: 30_000`: los datos se consideran frescos durante 30 segundos.

`staleTime` es la única adaptación respecto a los bloques literales del laboratorio. Permite comprobar en *Network* que al cambiar de filtro o de caso y volver, dentro de esos 30 segundos, TanStack Query reutiliza la caché en lugar de repetir la petición.

## Estructura

```text
src/
├── api/
│   ├── client.js              # fetch + ApiError
│   ├── rescueCases.js         # listar y cambiar estado
│   ├── animals.js             # animal, tratamientos, elegibilidad
│   └── treatments.js          # registrar tratamiento
├── hooks/
│   ├── useRescueCases.js      # useQuery de listas por estado
│   ├── useAnimal.js           # tres useQuery con enabled
│   └── useRegisterTreatment.js# useMutation + invalidación
├── components/
│   ├── ErrorMessage.jsx
│   ├── CaseList.jsx
│   ├── CaseDetail.jsx
│   ├── ChangeStatus.jsx
│   ├── TreatmentList.jsx
│   └── TreatmentForm.jsx
├── App.jsx
├── index.css
└── main.jsx
```

## Flujo funcional

1. `CaseList` consulta `/api/rescue-cases?status=...` con una `queryKey` que incluye el estado.
2. Al seleccionar un caso, `CaseDetail` consulta en paralelo:
   - `/api/animals/{animalCode}`
   - `/api/animals/{animalCode}/treatments`
   - `/api/animals/{animalCode}/treatment-eligibility`
3. `ChangeStatus` usa `useMutation` para `PATCH /api/rescue-cases/{caseCode}/status`.
4. Al cambiar el estado, invalida la clave prefijo `['rescue-cases']`, por lo que se refrescan todas las listas.
5. `TreatmentForm` usa `useReducer` y `POST /api/treatments`.
6. Al registrar un tratamiento, invalida `['animal', animalCode]`, lo que refresca el detalle, los tratamientos y la elegibilidad.

## Datos de demostración

Códigos incluidos:

| Caso | Estado | Animal |
|---|---|---|
| `RES-2026-001` | `ADMITTED` | `AN-2026-001` |
| `RES-2026-002` | `IN_REHABILITATION` | `AN-2026-002` |
| `RES-2026-003` | `RELEASED` | `AN-2026-003` |
| `RES-2026-004` | `ADMITTED` | `AN-2026-004` |
| `RES-2026-005` | `UNDER_EVALUATION` | `AN-2026-005` |

Especialistas: `SPEC-001` (activo) y `SPEC-002` (inactivo). `AN-2026-002` ya tiene un tratamiento inicial.

Recorrido recomendado:

- `RES-2026-002` o `RES-2026-005` para ver elegibilidad verdadera, tratamientos y registrar datos válidos.
- `RES-2026-004` para provocar `409` con `ADMITTED → RELEASED`.
- `RES-2026-001` para ver un animal sin tratamientos.
- `SPEC-002` o `SP-999` para probar errores de especialista.

## Verificación

Comprobaciones realizadas:

- `npm run build`: correcto.
- `npm run lint`: correcto; una advertencia de Fast Refresh por `export const STATUSES` en `CaseList.jsx`, presente en el código literal del laboratorio.
- E2E con Playwright + Chrome:
  - lista, estado vacío y caché de listas;
  - detalle, elegibilidad, tratamientos y caché del detalle;
  - `ApiError` `404` para un animal inexistente;
  - `409` para transición inválida y `200` para transición válida;
  - `409` para tratamiento no elegible (`AN-2026-001`) y `201` para tratamiento válido (`AN-2026-005`);
  - `400` por descripción corta, especialista vacío, fecha futura y fecha vacía;
  - `404` para especialista inexistente;
  - `201` con actualización de la tabla y reset del formulario;
  - mensaje de error `500` cuando el backend no responde.

## Regla de tratamiento

Solo los animales cuyos casos están en `UNDER_EVALUATION` o `IN_REHABILITATION` pueden recibir tratamiento. `AN-2026-001` está en `ADMITTED`: la UI muestra elegibilidad negativa y `POST /api/treatments` devuelve `409`.

Para el camino exitoso se puede usar `AN-2026-002` (`IN_REHABILITATION`) o `AN-2026-005` (`UNDER_EVALUATION`).
