# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack **dine-in** restaurant order management system (brand name: **Savoria**) with three roles: Guest/Customer (table-based ordering via QR code), Staff (real-time order dashboard), Admin (menu/category CRUD, order management).

- **Backend**: Spring Boot 3.2.3 (Java 17), SQLite, JWT, WebSocket (STOMP)
- **Frontend**: Vue 3, Vite, Tailwind CSS, Pinia, Vue Router, Axios, @stomp/stompjs

This is a **dine-in only** restaurant system — no delivery, no takeaway. Customers sit at a table, scan a QR code, place orders, and food is brought to the table by staff.

## Commands

### Backend
```bash
cd backend
./mvnw spring-boot:run          # Start backend on :8080
./mvnw clean package            # Build JAR
./mvnw test                     # Run tests
```

> Use `./mvnw` (Maven wrapper), not `mvn` — Maven may not be in PATH.

### Frontend
```bash
cd frontend
npm install                  # Install dependencies (first time)
npm run dev                  # Start dev server on :5173
npm run build                # Production build
```

## Architecture

### Backend Package Structure (`backend/src/main/java/com/restaurant/`)

- **`model/`** — JPA entities: `User` (fields: id, name, username, email, password, phone, role, createdAt — `username` is the login identifier, `email` kept for contact), `Category`, `MenuItem`, `Order` (has `@ManyToOne TableSession`), `OrderItem`, `Payment`, `TableSession`. Enums in `model/enums/`: `Role`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `TableSessionStatus`.
- **`repository/`** — Spring Data JPA repositories, one per entity.
- **`dto/`** — Request/response records grouped by domain: `dto/auth/`, `dto/menu/`, `dto/order/`.
- **`security/`** — `JwtUtil` (token generation/validation with jjwt 0.12.x API), `JwtAuthenticationFilter` (per-request filter), `UserDetailsServiceImpl`.
- **`config/`** — `SecurityConfig` (endpoint authorization + CORS), `WebSocketConfig` (STOMP broker), `WebConfig` (serves `uploads/` dir as static resource at `/uploads/**`), `DataInitializer` (seeds admin/staff users + sample menu on first run), `GlobalExceptionHandler`.
- **`service/`** — Business logic: `AuthService`, `MenuService`, `OrderService` (WebSocket broadcast via `SimpMessagingTemplate`; uses `@Lazy` setter injection for `TableSessionService` to avoid circular dependency), `TableSessionService` (session lifecycle + `@Scheduled` expiry), `FileUploadService` (validates type/size, saves with UUID filename, returns absolute URL).
- **`controller/`** — `AuthController` (`/api/auth/**`), `MenuController` (public GET `/api/categories/**` and `/api/menu/**`), `OrderController` (`/api/orders`), `StaffController` (`/api/staff/**`, requires STAFF or ADMIN), `AdminController` (`/api/admin/**`, requires ADMIN), `FileUploadController` (`POST /api/admin/upload`, requires ADMIN), `TableSessionController` (`/api/table-sessions/**`, public).

### Security / Auth Flow

JWT is passed as `Authorization: Bearer <token>`. `JwtAuthenticationFilter` processes tokens on every request but does not require them — public endpoints work without a token, `@AuthenticationPrincipal` returns `null` for unauthenticated requests. Role authorization is enforced both in `SecurityConfig` (URL-level) and via `@PreAuthorize` on controllers (method-level, enabled by `@EnableMethodSecurity`).

Login uses **username** (not email). `LoginRequest` sends `{ username, password }`; `RegisterRequest` sends `{ name, username, email, password, phone }`. `AuthResponse` returns `username` (not email). `UserDetailsServiceImpl` loads users by `username`.

Guest ordering: `POST /api/orders` is `permitAll`. The controller checks if `@AuthenticationPrincipal` is present; if not, it uses `guestName`/`guestPhone` from the request body.

### WebSocket (Real-time)

- Endpoint: `/ws` with SockJS fallback.
- Vite config requires `define: { global: 'globalThis' }` to fix `global is not defined` error from sockjs-client.
- Staff subscribes to `/topic/orders` — receives every order update.
- Customers subscribe to `/topic/orders/{orderNumber}` — receives updates for their specific order.
- Server pushes via `SimpMessagingTemplate.convertAndSend(...)` inside `OrderService.updateOrderStatus()` and `placeOrder()`.

### Table Session Feature

Multiple customers at the same table can each place individual orders — they accumulate under one `TableSession` per table.

- **`TableSession`** entity: `tableNumber`, `status` (OPEN/PAID/EXPIRED), `openedAt`, `lastActivityAt`, `closedAt`, `paymentMethod`, `@OneToMany orders`.
- **`TableSessionStatus`** enum: `OPEN, PAID, EXPIRED`.
- **Flow**: On order placement, `OrderService` calls `TableSessionService.getOrCreateSession(tableNumber)` to get or create an OPEN session, then links the new order to it. `lastActivityAt` is updated on each order (`touchSession`).
- **Expiry**: `@Scheduled(fixedDelay=60_000)` in `TableSessionService` runs every minute and marks sessions EXPIRED if `lastActivityAt` is older than the configured timeout (default 60 minutes, set via `table.session.timeout-minutes` in `application.properties`).
- **Payment**: `POST /api/table-sessions/{tableNumber}/pay` with `{ paymentMethod }` marks the session PAID and all its orders DELIVERED.
- **Bill page** (`/table/:tableNumber/bill`): shows all orders and items for the table, grand total, and payment form.
- **Running bill banner**: shown on the home page when the scanned table has an active session with orders.
- **Circular dependency**: `OrderService` → `TableSessionService` → `OrderService` is broken with `@Lazy` setter injection for `TableSessionService` in `OrderService`.

### QR Code / Table Flow

- Admin generates QR codes via the Tables admin page (`/admin/tables`). Each QR encodes `/?table=<number>`.
- Scanning sets `tableStore.tableNumber` in the Pinia store (persisted to `localStorage`), and the table number is shown as a fixed badge on the menu page.
- At checkout, if `tableStore.tableNumber` is set, it is shown as locked (from QR scan); otherwise the customer can manually enter a table number.
- `paymentMethod` is hardcoded to `CASH` in `CheckoutView` — the per-table bill payment page handles actual payment method selection.

### Frontend Structure (`frontend/src/`)

- **`api/index.js`** — All Axios calls organized by domain (`authApi`, `menuApi`, `orderApi`, `staffApi`, `adminCategoryApi`, `adminMenuApi`, `adminOrderApi`, `uploadApi`, `tableSessionApi`). JWT token auto-attached via request interceptor. `uploadApi.uploadImage(file)` sends `multipart/form-data`. `tableSessionApi` has `getSession(tableNumber)` and `paySession(tableNumber, { paymentMethod })`.
- **`stores/auth.js`** — Pinia store with `isAuthenticated`, `isAdmin`, `isStaff`, `isCustomer` computed getters. Persists token + user to `localStorage`. `login(username, password)` sends `{ username, password }`.
- **`stores/cart.js`** — Cart state persisted to `localStorage`.
- **`stores/table.js`** — Pinia store for `tableNumber` (set from QR scan query param).
- **`composables/useWebSocket.js`** — Wraps `@stomp/stompjs` Client with `connect(onConnected)` / `subscribe(destination, cb)`. Auto-disconnects on component unmount.
- **`router/index.js`** — Route guards check `auth.isAuthenticated` and role. `meta.requiresRole: 'ADMIN'` or `'STAFF'` enforced before navigation. Includes `/table/:tableNumber/bill` route.
- **`components/ImageUpload.vue`** — Reusable image uploader. Supports drag & drop, click-to-browse, and URL paste. Shows instant local preview via `createObjectURL` while uploading, then replaces with server URL. Emits `update:modelValue` (v-model compatible).
- **`components/MenuCard.vue`** — Equal-height cards (`flex flex-col h-full`). Description uses `line-clamp-2` with a CSS-only tooltip (`peer`/`peer-hover:block`) showing full text on hover.
- **`components/OrderStatusBadge.vue`** — Colored dot badge with pulsing animation for active statuses. No emojis.
- **`views/TableBillView.vue`** — Table bill page: lists all orders and items, shows grand total, payment method selection (CASH/CARD with card validation), calls `paySession`.
- **`views/OrderTrackingView.vue`** — Vertical stepper showing order progress. Steps: PENDING→"Order Received", CONFIRMED→"Order Confirmed", PREPARING→"Being Prepared", READY→"Ready to Serve", DELIVERED→"Served". Active step has pulsing orange ring.
- **`views/PaymentView.vue`** — Per-order payment page. CARD payment has full client-side validation: Luhn algorithm for card number, expiry format/future check, CVV length, cardholder name.

### Database

SQLite file (`restaurant.db`) is created automatically in whichever directory the backend process runs from. `spring.jpa.hibernate.ddl-auto=update` handles schema creation.

**SQLite limitation**: Cannot `ALTER TABLE ADD COLUMN` with a UNIQUE constraint. If a new UNIQUE column is added to an entity, delete `restaurant.db` and restart — the schema will be recreated. Non-unique columns can be added via `ddl-auto=update` without deleting the DB.

### Validation Summary

All request DTOs use Jakarta Bean Validation (`@Valid` on every controller method). Key constraints:

| DTO | Notable constraints |
|-----|-------------------|
| `RegisterRequest` | username 3–50 chars; password min 8, must have uppercase + number; email format |
| `LoginRequest` | username max 50; password max 128 |
| `OrderRequest` | tableNumber max 20; notes max 500; guestEmail validated; paymentMethod not null |
| `OrderItemRequest` | quantity 1–100; notes max 200 |
| `MenuItemRequest` | name max 100; description max 1000; price 0.01–99999999.99; imageUrl max 500 |
| `CategoryRequest` | name max 100; description max 500; imageUrl max 500 |

Uploaded images are stored in an `uploads/` directory alongside `restaurant.db`. The directory is created automatically on first upload. Served at `/uploads/**` as static resources — no auth required so `<img>` tags load correctly from the frontend. `upload.dir` and `app.base-url` in `application.properties` control where files are stored and what URL prefix is returned.

**To migrate to PostgreSQL**, change these three lines in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/restaurant
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```
Then add `postgresql` to `pom.xml` dependencies and remove `sqlite-jdbc` + `hibernate-community-dialects`.

### Default Seeded Accounts

| Role  | Username | Password   |
|-------|----------|------------|
| Admin | admin    | Admin123!  |
| Staff | staff    | Staff123!  |

Seeding only runs if the user does not already exist (checked via `existsByUsername`). Menu prices are in Indonesian Rupiah (IDR).

If passwords were changed, delete `restaurant.db` and restart the backend to re-seed with the new passwords.

### Password Policy (registration)

Enforced via `@Pattern` on `RegisterRequest.password`:
- Minimum **8 characters**, maximum **128**
- At least one **uppercase letter**
- At least one **number**

The seeded admin/staff accounts are saved directly (bypassing the DTO validation), so their passwords are set in `DataInitializer` and must manually comply with the policy.

### Order Status Flow

`PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `DELIVERED`

Orders go directly to `PENDING` on placement — staff sees them immediately via WebSocket. The `AWAITING_PAYMENT` status exists in the enum but is no longer used in the standard dine-in flow.

**Cancellation**: Staff cannot cancel orders (enforced in `StaffController`). Admins can cancel via the admin orders view.

**Payment**: Handled at the table session level via `/table/:tableNumber/bill`. `CARD` is simulated (no real gateway). `CASH` is the default method set at checkout; actual payment method is confirmed on the bill page.

**Staff dashboard** (`/staff`): shows live orders grouped by status. Action buttons: "Confirm Order", "Start Cooking", "Mark Ready", "Mark Served". No cancel button for staff.
