# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack **dine-in** restaurant order management system (brand name: **Savoria**) with three roles: Guest/Customer (table-based ordering via QR code), Staff (real-time order dashboard with session management), Admin (menu/category CRUD, order management).

- **Backend**: Spring Boot 3.2.3 (Java 17), MySQL 8.0, JWT, WebSocket (STOMP)
- **Frontend**: Vue 3, Vite, Tailwind CSS, Pinia, Vue Router, Axios, @stomp/stompjs, @phosphor-icons/vue, qrcode

This is a **dine-in only** restaurant system — no delivery, no takeaway. Customers sit at a table, scan a QR code, place orders, and food is brought to the table by staff.

## Code Style

### Comments

Every source file must have explanatory comments. Apply this to all new and modified code:

- **Backend (Java):** Every class must have a Javadoc `/** ... */` comment explaining its role in the system. Every public method must have a Javadoc comment describing what it does, its parameters, and any important side-effects (e.g. WebSocket broadcasts, transactions, locking). Non-obvious fields must have inline comments. Enum values must have comments explaining their lifecycle meaning.
- **Frontend (Vue/JS):** Every component `<script setup>` block must open with a JSDoc `/** ... */` comment explaining the component's purpose. Non-trivial functions, computed properties, and watchers must have a comment explaining what they do and why. Store files must document each piece of state and action.
- **Don't comment the obvious** (`// increment counter` above `i++`). Focus on *why* — business rules, concurrency rationale, security constraints, and non-obvious choices.

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

- **`model/`** — JPA entities: `User`, `Category`, `MenuItem`, `Order` (`@ManyToOne TableSession`), `OrderItem`, `Payment`, `TableSession`. Enums in `model/enums/`: `Role`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `TableSessionStatus`.
- **`repository/`** — Spring Data JPA repositories, one per entity.
- **`dto/`** — Request/response records grouped by domain: `dto/auth/`, `dto/menu/`, `dto/order/`.
- **`security/`** — `JwtUtil` (jjwt 0.12.x), `JwtAuthenticationFilter`, `UserDetailsServiceImpl`.
- **`config/`** — `SecurityConfig`, `WebSocketConfig`, `WebConfig` (serves `/uploads/**`), `DataInitializer` (seeds admin/staff + sample menu), `GlobalExceptionHandler`.
- **`service/`** — `AuthService`, `MenuService`, `OrderService` (WebSocket broadcast; `@Lazy` setter injection for `TableSessionService` to break circular dependency), `TableSessionService`, `FileUploadService`.
- **`controller/`** — `AuthController` (`/api/auth/**`), `MenuController` (public), `OrderController` (`/api/orders`), `StaffController` (`/api/staff/**`, STAFF or ADMIN), `AdminController` (`/api/admin/**`, ADMIN), `FileUploadController`, `TableSessionController` (`/api/table-sessions/**`, public).

### Security / Auth Flow

JWT passed as `Authorization: Bearer <token>`. Public endpoints work without a token. Login uses **username** (not email).

Token lifetime is **role-based** — see `JwtUtil.generateToken(User)`. STAFF/ADMIN get `jwt.expiration.staff` (default **13h**, enough for a 10AM–10PM shift). CUSTOMER gets `jwt.expiration` (default 8h). The `generateToken(UserDetails)` overload is kept for callers without a concrete `User` and uses the default lifetime.

Guest ordering: `POST /api/orders` is `permitAll`. Controller checks `@AuthenticationPrincipal`; if null, uses `guestName`/`guestPhone` from request body.

### Payment Flow

Orders are placed as `PENDING` immediately. Staff **cannot** advance an order from `PENDING → CONFIRMED` until `payment.status == PAID`.

1. Customer places order → `POST /api/orders` → order status `PENDING`, payment status `PENDING`, **response includes `payment.paymentToken` (UUID, one-time)**
2. Frontend stores the token in `ordersStore` (localStorage) via `addOrder(orderNumber, tableNumber, paymentToken)`
3. Customer goes to `/payment/:orderNumber` — chooses QR / Card / Cash
4. Customer clicks confirm → frontend calls `ordersStore.getTokenForOrder(orderNumber)` → `POST /api/orders/{orderNumber}/pay` with `{ paymentToken }` body → payment marked `PAID`
5. Staff can now confirm the order

**`paymentToken` security design:**
- Generated as `UUID.randomUUID()` in `placeOrder()` and stored on `Payment.paymentToken` (unique, nullable for backward compat)
- Returned **only** in the initial `placeOrder()` HTTP response — `mapToResponse()` always passes `null` for the token so it never appears in WebSocket broadcasts or subsequent reads
- `confirmPayment(orderNumber, paymentToken)` validates the token; `null` DB tokens (legacy orders) skip validation
- Prevents an unauthenticated client watching `/topic/orders` from harvesting an order number and marking a stranger's order as PAID

`confirmPayment` also checks `payment.status == PAID` (not the old `AWAITING_PAYMENT` status). The `AWAITING_PAYMENT` order status exists in the enum but is no longer used.

### WebSocket (Real-time)

- Endpoint: `/ws` with SockJS fallback.
- Vite config requires `define: { global: 'globalThis' }` to fix `global is not defined` from sockjs-client.
- Server pushes via `SimpMessagingTemplate` through the private `broadcast()` helper in `OrderService`, called from `placeOrder()`, `confirmPayment()`, and `updateOrderStatus()`.

| Topic | Subscriber | Purpose |
|---|---|---|
| `/topic/orders` | Staff dashboard | Every order event across all tables |
| `/topic/orders/{orderNumber}` | Customer order tracking page | Updates for one specific order |
| `/topic/table/{tableNumber}` | Customer "My Orders" page | All events for a table — keeps every device at the same table in sync |

### Table Session Feature

Multiple customers at the same table place individual orders that accumulate under one `TableSession`.

- **`TableSession`** entity: `tableNumber`, `status` (OPEN/PAID/EXPIRED), `openedAt`, `lastActivityAt`, `closedAt`, `paymentMethod`, `@OneToMany orders`.
- **`TableSessionStatus`** enum: `OPEN, PAID, EXPIRED`.
- **Flow**: On order placement, `OrderService` calls `TableSessionService.getOrCreateSession(tableNumber)`.
- **Expiry**: `@Scheduled(fixedDelay=60_000)` uses a **single atomic bulk UPDATE** (`UPDATE table_sessions SET status='EXPIRED' WHERE status='OPEN' AND last_activity_at < :cutoff`) instead of a read-then-save loop. This prevents the race where the scheduler could overwrite a concurrently committed `PAID` status with `EXPIRED`.
- **Staff close**: `POST /api/staff/tables/{tableNumber}/close` marks session EXPIRED immediately — frees the table for the next customer.
- **Payment**: `POST /api/table-sessions/{tableNumber}/pay` marks session PAID. Payments are batch-saved with `saveAll()` (one round-trip regardless of order count).
- **Circular dependency**: broken with `@Lazy` setter injection for `TableSessionService` in `OrderService`.

### Staff Endpoints

`StaffController` (`/api/staff/**`, requires STAFF or ADMIN):

| Method | Path | Description |
|---|---|---|
| GET | `/api/staff/orders` | List orders (`?activeOnly=false`) |
| PATCH | `/api/staff/orders/{id}/status` | Advance order status (no cancel) |
| GET | `/api/staff/tables` | List all OPEN table sessions |
| POST | `/api/staff/tables/{tableNumber}/close` | End a table session (marks EXPIRED) |

### QR Code / Table Flow

- Admin generates QR codes via `/admin/tables`. Each QR encodes `/?table=<number>`.
- Scanning sets `tableStore.tableNumber` (sessionStorage). Table shown as fixed badge on menu.
- At checkout, locked if from QR scan; otherwise customer enters manually. Manual entry also sets `tableStore`.
- After checkout, table number is saved alongside the order number in `ordersStore` (localStorage).

### Frontend Structure (`frontend/src/`)

- **`api/index.js`** — All Axios calls. `staffApi` includes `getOpenSessions()` and `closeSession(tableNumber)`. `tableSessionApi` has `getSession(tableNumber)` and `paySession(tableNumber, { paymentMethod })`.
- **`stores/auth.js`** — Pinia store. Persists token + user to `sessionStorage` (not `localStorage`), so staff/admin sessions end automatically when the browser or tab closes — important for shared devices like kitchen tablets. Page refreshes within the same tab still restore the session.
- **`api/index.js`** — the shared Axios instance also installs a **global 401 response interceptor**: when the backend rejects a request with 401 (expired/revoked token), it wipes `sessionStorage` and redirects to the login page matching the current area (`/admin/login`, `/staff/login`, or `/`). 401s from `/auth/login` and `/auth/register` are excluded so wrong-password errors can be shown inline on the form instead of triggering a redirect.
- **`stores/cart.js`** — Cart state, persisted to `localStorage`.
- **`stores/table.js`** — `tableNumber` in `sessionStorage` (clears on tab close). `setTable(n)` / `clearTable()`.
- **`stores/orders.js`** — Persists `[{ orderNumber, tableNumber, paymentToken }]` to `localStorage`. `addOrder(orderNumber, tableNumber, paymentToken)`, `getNumbersForTable(tableNumber)`, `removeOrder(orderNumber)`, `getTokenForOrder(orderNumber)`. Scopes order history to the current table. Token is stored here so `PaymentView` can retrieve it without a server round-trip.
- **`composables/useWebSocket.js`** — Wraps `@stomp/stompjs` Client. Auto-disconnects on unmount. Guards against duplicate clients on rapid remounts (`connect()` is a no-op if a client already exists). `subscribe()` tracks each subscription in an internal array, parses JSON safely (try/catch), and unsubscribes everything on `disconnect()`. **All views must use the composable's `subscribe()` — never call `client.subscribe()` directly**, as that bypasses the tracked-cleanup mechanism.
- **`composables/useDialog.js`** — Module-level singleton for custom alert/confirm dialogs. Returns promises. `showAlert(message, title)` and `showConfirm(message, title, variant)`. Replaces all native `alert()`/`confirm()` calls.
- **`components/AppDialog.vue`** — The dialog UI. Mounted in `App.vue` via `<Teleport to="body">`. Reads from `useDialog` state. Two variants: `alert` (OK button) and `confirm` (Cancel + Confirm). `danger` variant uses red styling.
- **`components/Modal.vue`** — Reusable modal for admin CRUD forms.
- **`components/OrderStatusBadge.vue`** — Colored badge. Dark mode class strings written in full in the config object so Tailwind scanner picks them up.
- **`components/ImageUpload.vue`** — Drag & drop / click / URL paste image uploader. v-model compatible. URL input validates protocol (only `http://` and `https://` allowed — blocks `javascript:`, `data:`, and other dangerous schemes).
- **`components/MenuCard.vue`** — Equal-height cards. Description uses `line-clamp-2` with CSS tooltip on hover.
- **`router/index.js`** — Route guards. Includes `/my-orders`, `/payment/:orderNumber`, `/table/:tableNumber/bill`.
- **`views/PaymentView.vue`** — Three payment tabs: QR Code (generated with `qrcode` library from order ref), Card (Luhn validation), Cash at Cashier. Retrieves `paymentToken` from `ordersStore` and sends it in the confirm request body. Redirects to `/my-orders` after confirmation.
- **`views/MyOrdersView.vue`** — Shows order history scoped to `tableStore.tableNumber`. On load, checks if the table session is still OPEN (via `tableSessionApi.getSession`). If session is ended, clears orders for that table and resets the table store. Shows three states: no table selected / no orders placed / order list.
- **`views/OrderTrackingView.vue`** — Live stepper: PENDING→CONFIRMED→PREPARING→READY→DELIVERED.
- **`views/TableBillView.vue`** — Table bill with payment method selection (CASH/CARD).
- **`views/staff/StaffDashboard.vue`** — Full redesign: status tabs (Active/Pending/Confirmed/Preparing/Ready/All) with live counts, horizontal-scrolling active table cards, color-coded order cards with elapsed time and full-width action buttons.

### My Orders / Session Scoping

- `ordersStore` stores `{ orderNumber, tableNumber, paymentToken }` tuples in `localStorage`.
- `MyOrdersView` and `Navbar` only show/count orders matching `tableStore.tableNumber`.
- When `MyOrdersView` loads and there are stored orders, it calls `tableSessionApi.getSession(tableNumber)`. A 404 means the session was ended by staff → clears all orders for that table and resets `tableStore`.
- The Navbar polls every 30s and does the same check.
- If table is set but no orders exist yet (no session), shows "No orders have been placed" without clearing the table.

### Running Bill Banner

Shown on `HomeView` when `tableStore.tableNumber` is set and `activeSession` has at least one order with `payment.status !== 'PAID'`. Disappears once all orders are paid. Condition: `hasUnpaidOrders` computed.

### Staff Dashboard Design

- **Header**: search bar, refresh button, username, logout — all in the top bar.
- **Active Tables**: horizontal scroll row, compact cards (T5, order count, total, End Session).
- **Status tabs**: Active | Pending | Confirmed | Preparing | Ready | All — each with a live count badge. Counts are computed in a **single pass** over the orders array (`statusCounts` computed) rather than six separate `.filter()` calls. Replaces the old "Active Only / All Orders" toggle.
- **Order cards**: colored top border + table number badge, customer + elapsed time, bold item quantities, full-width action button colored by next stage (blue→Confirm, purple→Start Cooking, green→Mark Ready, orange→Mark Served). Loading spinner inside button while updating.
- **Search** is debounced 300 ms (`debouncedSearch` ref + watcher) so `displayedOrders` doesn't re-filter on every keystroke.
- Sessions reload **only when a new order arrives** on the WebSocket (idx === -1), not on every status-update message. A `setInterval(loadSessions, 30_000)` (cleared on unmount) covers the gap where another staff client closes a session.
- The refresh button calls `Promise.all([loadOrders(), loadSessions()])` — both requests run in parallel.

### Responsive Design

Tailwind CSS breakpoints (`sm`/`lg`/`xl`). Admin sidebar hidden on mobile (hamburger toggle). Tables wrapped in `overflow-x-auto` with `min-w-[Npx]`. Touch targets ≥36px.

### Dark Mode

`darkMode: 'media'` in `tailwind.config.js`. Auto-detects OS preference. Global classes in `main.css` include `dark:` variants. No manual toggle.

### Search

Client-side `computed` filtering (reactive, no network calls):

| View | Searches by |
|---|---|
| `HomeView.vue` | Item name or description + category filter |
| `StaffDashboard.vue` | Order number, customer name, phone, table number |
| `admin/OrdersView.vue` | Order number, customer name, phone, table number |
| `admin/MenuManagement.vue` | Item name or description + category/availability filters |

### Performance

#### Backend
- `spring.jpa.properties.hibernate.default_batch_fetch_size=30` — lazy collections (e.g. `Order.items`, `TableSession.orders`) are loaded in batches of up to 30 with `IN (...)` queries instead of one query per entity (eliminates most N+1 patterns on list endpoints).
- `@EntityGraph(attributePaths = {"category"})` on all `MenuItemRepository` finders — JOIN FETCHes the `Category` association so `mapMenuItem()` doesn't trigger a separate query per item.
- `@EntityGraph(attributePaths = {"items", "payment", "user"})` on **all** `OrderRepository` finders that drive `mapToResponse()` — `findByOrderNumber`, `findByTableSessionId`, `findAllByOrderByCreatedAtDesc`, `findByUserIdOrderByCreatedAtDesc`, `findByStatusInOrderByCreatedAtDesc`, `findByStatusNotInOrderByCreatedAtDesc`. The locking queries (`findByOrderNumberForUpdate`, `findByIdForUpdate`) intentionally omit EntityGraph to avoid interference with row-level lock scope.
- `@Index` on `Order`: composite `(status, created_at)`, `table_number`, `table_session_id`.
- `@Index` on `TableSession`: composite `(table_number, status)` (hot path for `getOrCreateSession`), `status`.

#### Frontend
- **`cartMap`** computed in `cart.js` — `Map<id, quantity>` for O(1) quantity lookups in `MenuCard` instead of `Array.find()` per render.
- **Debounced `persist()`** in `cart.js` and `orders.js` — 300 ms debounce coalesces rapid mutations into one `localStorage.setItem`.
- **Debounced search** in `HomeView` and `StaffDashboard` — 300 ms debounce; clearing the field is applied immediately.
- **Single-pass status counts** in `StaffDashboard` — `statusCounts` computed iterates orders once vs. 6 separate `.filter()` calls.

### Database

MySQL 8.0. `spring.jpa.hibernate.ddl-auto=update`. Connection defaults overridable via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` env vars. Create DB manually before first run:

```sql
CREATE DATABASE restaurant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Validation

All DTOs use `@Valid`. Key constraints: username 3–50 chars, password ≥8 chars with uppercase + number, tableNumber max 20, notes max 500, price 0.01–99999999.99.

### Default Seeded Accounts

| Role  | Username | Password   |
|-------|----------|------------|
| Admin | admin    | Admin123!  |
| Staff | staff    | Staff123!  |

Seeding only runs if the user doesn't exist (`existsByUsername`). Prices are in Indonesian Rupiah (IDR).

### Error Handling

#### Backend — `GlobalExceptionHandler`
- **Business-rule violations** (messages matching a known safe-prefix allowlist) are returned to the client as-is with 400 Bad Request. "Not found" errors return 404.
- **Unexpected RuntimeExceptions** (SQL errors, null pointers, etc.) are logged with full stack traces but the client receives only `"An unexpected error occurred"` with 500 status. This prevents internal details from leaking.
- `BadCredentialsException` → 401, generic message (prevents username enumeration).
- `AccessDeniedException` → 403, generic message.
- `MethodArgumentNotValidException` → 400 with field-level error map.
- `MethodArgumentTypeMismatchException` → 400 with `"Invalid parameter: <name>"`. Catches bad path variables (e.g. `/api/menu/abc` when the endpoint expects a Long), preventing the generic 500 that would otherwise leak the internal class-cast detail.

#### Backend — File Upload Security (`FileUploadService`)
- Declared content-type must be an allowed image MIME type.
- Actual file magic bytes are inspected (JPEG/PNG/GIF/WebP signatures) to prevent content-type spoofing.
- The InputStream used for magic-byte detection is closed via try-with-resources to avoid file descriptor leaks.
- Saved filename is a random UUID — user-supplied name is never used.
- Resolved target path is verified to stay inside `uploadDir` (path traversal guard).

#### Frontend — WebSocket Resilience
- `useWebSocket.connect()` guards against duplicate clients on rapid remounts.
- `useWebSocket.subscribe()` wraps `JSON.parse` in try/catch so malformed messages don't crash handlers.
- Views (StaffDashboard, MyOrdersView, OrderTrackingView) subscribe via the composable's `subscribe()` so each subscription is registered in the tracked array and torn down on unmount.
- `disconnect()` resets `client.value` to null and clears the subscriptions array, allowing clean reconnection.

### Order Status Flow

`PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `DELIVERED`

**Key rules:**
- Orders placed → immediately `PENDING`, staff notified via WebSocket
- `PENDING → CONFIRMED` blocked until `payment.status == PAID`
- Staff cannot cancel (enforced in `StaffController`); admins can
- On DELIVERED, any remaining PENDING cash payment is auto-marked PAID
