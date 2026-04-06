# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack **dine-in** restaurant order management system (brand name: **Savoria**) with three roles: Guest/Customer (table-based ordering via QR code), Staff (real-time order dashboard with session management), Admin (menu/category CRUD, order management).

- **Backend**: Spring Boot 3.2.3 (Java 17), MySQL 8.0, JWT, WebSocket (STOMP)
- **Frontend**: Vue 3, Vite, Tailwind CSS, Pinia, Vue Router, Axios, @stomp/stompjs, @phosphor-icons/vue, qrcode

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

- **`model/`** — JPA entities: `User`, `Category`, `MenuItem`, `Order` (`@ManyToOne TableSession`), `OrderItem`, `Payment`, `TableSession`. Enums in `model/enums/`: `Role`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `TableSessionStatus`.
- **`repository/`** — Spring Data JPA repositories, one per entity.
- **`dto/`** — Request/response records grouped by domain: `dto/auth/`, `dto/menu/`, `dto/order/`.
- **`security/`** — `JwtUtil` (jjwt 0.12.x), `JwtAuthenticationFilter`, `UserDetailsServiceImpl`.
- **`config/`** — `SecurityConfig`, `WebSocketConfig`, `WebConfig` (serves `/uploads/**`), `DataInitializer` (seeds admin/staff + sample menu), `GlobalExceptionHandler`.
- **`service/`** — `AuthService`, `MenuService`, `OrderService` (WebSocket broadcast; `@Lazy` setter injection for `TableSessionService` to break circular dependency), `TableSessionService`, `FileUploadService`.
- **`controller/`** — `AuthController` (`/api/auth/**`), `MenuController` (public), `OrderController` (`/api/orders`), `StaffController` (`/api/staff/**`, STAFF or ADMIN), `AdminController` (`/api/admin/**`, ADMIN), `FileUploadController`, `TableSessionController` (`/api/table-sessions/**`, public).

### Security / Auth Flow

JWT passed as `Authorization: Bearer <token>`. Public endpoints work without a token. Login uses **username** (not email).

Guest ordering: `POST /api/orders` is `permitAll`. Controller checks `@AuthenticationPrincipal`; if null, uses `guestName`/`guestPhone` from request body.

### Payment Flow

Orders are placed as `PENDING` immediately. Staff **cannot** advance an order from `PENDING → CONFIRMED` until `payment.status == PAID`.

1. Customer places order → `POST /api/orders` → order status `PENDING`, payment status `PENDING`
2. Customer goes to `/payment/:orderNumber` — chooses QR / Card / Cash
3. Customer clicks confirm → `POST /api/orders/{orderNumber}/pay` → payment marked `PAID`
4. Staff can now confirm the order

`confirmPayment` in `OrderService` checks `payment.status == PAID` (not the old `AWAITING_PAYMENT` status). The `AWAITING_PAYMENT` order status exists in the enum but is no longer used.

### WebSocket (Real-time)

- Endpoint: `/ws` with SockJS fallback.
- Vite config requires `define: { global: 'globalThis' }` to fix `global is not defined` from sockjs-client.
- Staff subscribes to `/topic/orders` — receives every order update.
- Customers subscribe to `/topic/orders/{orderNumber}` — receives updates for their specific order.
- Server pushes via `SimpMessagingTemplate` inside `OrderService.updateOrderStatus()` and `placeOrder()` and `confirmPayment()`.

### Table Session Feature

Multiple customers at the same table place individual orders that accumulate under one `TableSession`.

- **`TableSession`** entity: `tableNumber`, `status` (OPEN/PAID/EXPIRED), `openedAt`, `lastActivityAt`, `closedAt`, `paymentMethod`, `@OneToMany orders`.
- **`TableSessionStatus`** enum: `OPEN, PAID, EXPIRED`.
- **Flow**: On order placement, `OrderService` calls `TableSessionService.getOrCreateSession(tableNumber)`.
- **Expiry**: `@Scheduled(fixedDelay=60_000)` marks sessions EXPIRED if `lastActivityAt` older than timeout (default 60 min, `table.session.timeout-minutes`).
- **Staff close**: `POST /api/staff/tables/{tableNumber}/close` marks session EXPIRED immediately — frees the table for the next customer.
- **Payment**: `POST /api/table-sessions/{tableNumber}/pay` marks session PAID and all orders DELIVERED.
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
- **`stores/auth.js`** — Pinia store. Persists token + user to `localStorage`.
- **`stores/cart.js`** — Cart state, persisted to `localStorage`.
- **`stores/table.js`** — `tableNumber` in `sessionStorage` (clears on tab close). `setTable(n)` / `clearTable()`.
- **`stores/orders.js`** — Persists `[{ orderNumber, tableNumber }]` to `localStorage`. `addOrder(orderNumber, tableNumber)`, `getNumbersForTable(tableNumber)`, `removeOrder(orderNumber)`. Scopes order history to the current table.
- **`composables/useWebSocket.js`** — Wraps `@stomp/stompjs` Client. Auto-disconnects on unmount.
- **`composables/useDialog.js`** — Module-level singleton for custom alert/confirm dialogs. Returns promises. `showAlert(message, title)` and `showConfirm(message, title, variant)`. Replaces all native `alert()`/`confirm()` calls.
- **`components/AppDialog.vue`** — The dialog UI. Mounted in `App.vue` via `<Teleport to="body">`. Reads from `useDialog` state. Two variants: `alert` (OK button) and `confirm` (Cancel + Confirm). `danger` variant uses red styling.
- **`components/Modal.vue`** — Reusable modal for admin CRUD forms.
- **`components/OrderStatusBadge.vue`** — Colored badge. Dark mode class strings written in full in the config object so Tailwind scanner picks them up.
- **`components/ImageUpload.vue`** — Drag & drop / click / URL paste image uploader. v-model compatible.
- **`components/MenuCard.vue`** — Equal-height cards. Description uses `line-clamp-2` with CSS tooltip on hover.
- **`router/index.js`** — Route guards. Includes `/my-orders`, `/payment/:orderNumber`, `/table/:tableNumber/bill`.
- **`views/PaymentView.vue`** — Three payment tabs: QR Code (generated with `qrcode` library from order ref), Card (Luhn validation), Cash at Cashier. Redirects to `/my-orders` after confirmation.
- **`views/MyOrdersView.vue`** — Shows order history scoped to `tableStore.tableNumber`. On load, checks if the table session is still OPEN (via `tableSessionApi.getSession`). If session is ended, clears orders for that table and resets the table store. Shows three states: no table selected / no orders placed / order list.
- **`views/OrderTrackingView.vue`** — Live stepper: PENDING→CONFIRMED→PREPARING→READY→DELIVERED.
- **`views/TableBillView.vue`** — Table bill with payment method selection (CASH/CARD).
- **`views/staff/StaffDashboard.vue`** — Full redesign: status tabs (Active/Pending/Confirmed/Preparing/Ready/All) with live counts, horizontal-scrolling active table cards, color-coded order cards with elapsed time and full-width action buttons.

### My Orders / Session Scoping

- `ordersStore` stores `{ orderNumber, tableNumber }` pairs in `localStorage`.
- `MyOrdersView` and `Navbar` only show/count orders matching `tableStore.tableNumber`.
- When `MyOrdersView` loads and there are stored orders, it calls `tableSessionApi.getSession(tableNumber)`. A 404 means the session was ended by staff → clears all orders for that table and resets `tableStore`.
- The Navbar polls every 30s and does the same check.
- If table is set but no orders exist yet (no session), shows "No orders have been placed" without clearing the table.

### Running Bill Banner

Shown on `HomeView` when `tableStore.tableNumber` is set and `activeSession` has at least one order with `payment.status !== 'PAID'`. Disappears once all orders are paid. Condition: `hasUnpaidOrders` computed.

### Staff Dashboard Design

- **Header**: search bar, refresh button, username, logout — all in the top bar.
- **Active Tables**: horizontal scroll row, compact cards (T5, order count, total, End Session).
- **Status tabs**: Active | Pending | Confirmed | Preparing | Ready | All — each with a live count badge. Replaces the old "Active Only / All Orders" toggle.
- **Order cards**: colored top border + table number badge, customer + elapsed time, bold item quantities, full-width action button colored by next stage (blue→Confirm, purple→Start Cooking, green→Mark Ready, orange→Mark Served). Loading spinner inside button while updating.
- Sessions reload on every WebSocket order event (new orders create sessions) and on Refresh.

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

### Order Status Flow

`PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `DELIVERED`

**Key rules:**
- Orders placed → immediately `PENDING`, staff notified via WebSocket
- `PENDING → CONFIRMED` blocked until `payment.status == PAID`
- Staff cannot cancel (enforced in `StaffController`); admins can
- On DELIVERED, any remaining PENDING cash payment is auto-marked PAID
