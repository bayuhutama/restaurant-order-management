# Savoria — Restaurant Order Management System

A full-stack **dine-in** restaurant order management system. Customers scan a QR code at their table, browse the menu, and place orders. Staff manage orders in real time. Admins control the menu, categories, and tables.

> **No delivery, no takeaway.** Savoria is built for the in-restaurant experience: sit down, scan, order, eat, pay.

---

## Quick Start (TL;DR)

For developers who just want to run the project locally:

```bash
# 1. Create the MySQL database
mysql -u root -p -e "CREATE DATABASE \`restaurant-order-management\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Configure the backend (edit DB password + JWT secret)
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties

# 3. Run the backend (port 8080)
cd backend && ./mvnw spring-boot:run

# 4. Run the frontend (port 5173) — in a separate terminal
cd frontend && npm install && npm run dev
```

Open **http://localhost:5173** — log in as `admin / Admin123!` or `staff / Staff123!`.

---

## Table of Contents

- [Quick Start (TL;DR)](#quick-start-tldr)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started) — step-by-step setup
- [Configuration](#configuration)
- [User Roles](#user-roles)
- [Features](#features)
- [Architecture](#architecture) — backend + frontend structure, security, WebSocket
- [Database (RDBMS)](#database-rdbms) — ERD and table schemas
- [API Reference](#api-reference)
- [Frontend Routes](#frontend-routes)
- [Flows](#flows) — order, payment, table session
- [Design Patterns](#design-patterns)
- [Troubleshooting](#troubleshooting)

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Spring Boot 3.2.3, Java 17, MySQL 8.0, JWT (jjwt 0.12.5), WebSocket (STOMP), Spring Security, Spring Data JPA, Lombok |
| **Frontend** | Vue 3 (Composition API), Vite, Tailwind CSS, Pinia, Vue Router, Axios, @stomp/stompjs, qrcode, @phosphor-icons/vue |
| **Build** | Maven (`./mvnw` wrapper), npm |

---

## Getting Started

### Prerequisites

Make sure the following are installed before running the project:

| Tool | Version | Check |
|---|---|---|
| Java | 17+ | `java -version` |
| Node.js | 18+ | `node -v` |
| MySQL | 8.0 | `mysql --version` |
| Maven | (optional — `./mvnw` wrapper included) | `mvn -v` |

### 1. Clone the repository

```bash
git clone https://github.com/bayuhutama/restaurant-order-management.git
cd restaurant-order-management
```

### 2. Set up the database

Log in to MySQL and create the database (schema is auto-created by Hibernate on first run):

```sql
CREATE DATABASE `restaurant-order-management`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. Configure the backend

Create a local properties file (gitignored — keeps your credentials out of git):

```bash
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties
```

Edit `application-local.properties` and set at least:

```properties
spring.datasource.password=your_mysql_password
jwt.secret=your-secret-key-at-least-32-characters-long
```

> See [Configuration](#configuration) for the complete list of options.

### 4. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts on **http://localhost:8080**.

On first run, `DataInitializer` automatically seeds:
- Default **admin** account (`admin / Admin123!`)
- Default **staff** account (`staff / Staff123!`)
- Sample menu categories and items (prices in IDR)

> **Security note:** Default credentials are defined in `DataInitializer.java`. **Change them before any non-local deployment.**

### 5. Start the frontend

```bash
cd frontend
npm install         # first time only
npm run dev
```

The frontend starts on **http://localhost:5173**.

### 6. Open the app

| Role | URL | Credentials |
|---|---|---|
| Customer (guest) | http://localhost:5173 | none — scan `/?table=1` to simulate a QR scan |
| Staff | http://localhost:5173/staff/login | `staff / Staff123!` |
| Admin | http://localhost:5173/admin/login | `admin / Admin123!` |

### 7. Try it end-to-end

1. Visit **http://localhost:5173/?table=5** — simulates scanning a QR at table 5.
2. Add items to the cart, go through checkout, and pick a payment method.
3. Open **http://localhost:5173/staff** in another window — you'll see the new order appear in real time via WebSocket.
4. Advance it: Confirm → Preparing → Ready → Delivered.

---

## Configuration

All backend configuration is in `backend/src/main/resources/`.

| Property | Env Variable | Default | Description |
|---|---|---|---|
| `jwt.secret` | `JWT_SECRET` | *(required)* | JWT signing secret, min 32 chars |
| `jwt.expiration.staff` | `JWT_EXPIRATION_STAFF` | `46800000` (13h) | Staff token lifetime — covers a full 10AM–10PM shift |
| `jwt.expiration.admin` | `JWT_EXPIRATION_ADMIN` | `3600000` (1h) | Admin token lifetime — short on purpose |
| `spring.datasource.url` | `DB_URL` | `jdbc:mysql://localhost:3306/...` | Database URL |
| `spring.datasource.username` | `DB_USERNAME` | `root` | Database username |
| `spring.datasource.password` | `DB_PASSWORD` | *(empty)* | Database password |
| `app.base-url` | `APP_BASE_URL` | `http://localhost:8080` | Base URL for file serving |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed CORS origins |
| `upload.dir` | `UPLOAD_DIR` | `uploads` | Directory for uploaded images |
| `table.session.timeout-minutes` | — | `60` | Inactivity timeout for table sessions |

> For local development, override values in `application-local.properties` (gitignored). For production, set environment variables.

---

## User Roles

| Role | Access | Description |
|---|---|---|
| **Guest (customer)** | Public — no account | Scans QR code, browses menu, places orders, pays, tracks order status |
| **Staff** | Authenticated | Views and manages orders in real time, manages table sessions |
| **Admin** | Authenticated | Full access — menu CRUD, category CRUD, order management, QR generation |

> Customers never sign up or log in — the entire customer experience is guest-only, keyed off the table number encoded in the QR code.

---

## Features

### Customer
- Scan QR code at table to set table number automatically
- Browse menu by category with live search
- Add items to cart and place orders without an account
- Choose payment method: QR Code, Card, or Cash at Cashier
- Track order status with a live stepper (PENDING → DELIVERED)
- View all orders for the current table session in My Orders

### Staff
- Real-time order dashboard via WebSocket
- Status tabs: Pending / Confirmed / Preparing / Ready / All
- Advance order status with one click
- View and manage active table sessions
- End a table session to free the table for the next customer

### Admin
- Full CRUD for menu items and categories
- Image upload (file or URL)
- Generate QR codes for each table
- View and manage all orders
- Toggle item availability

### General
- Dark mode (follows OS preference)
- Responsive layout (mobile, tablet, desktop)
- Custom in-app dialogs (no native `alert`/`confirm`)
- Live client-side search on all list views

---

## Architecture

### Backend Package Structure

```
com.restaurant/
├── config/         # SecurityConfig, WebSocketConfig, GlobalExceptionHandler, DataInitializer
├── controller/     # AuthController, OrderController, StaffController, AdminController, ...
├── dto/            # Request/response records grouped by domain (auth/, menu/, order/)
├── model/          # JPA entities: User, Order, OrderItem, Payment, MenuItem, Category, TableSession
│   └── enums/      # Role, OrderStatus, PaymentMethod, PaymentStatus, TableSessionStatus
├── repository/     # Spring Data JPA interfaces, one per entity
├── security/       # JwtUtil, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/        # AuthService, OrderService, MenuService, TableSessionService, FileUploadService
```

### Frontend Structure

```
src/
├── api/            # index.js — all Axios calls grouped by domain
├── components/     # AppDialog, Modal, MenuCard, OrderStatusBadge, ImageUpload
├── composables/    # useWebSocket.js, useDialog.js
├── router/         # index.js with role-based navigation guards
├── stores/         # auth.js, cart.js, table.js, orders.js (Pinia)
└── views/          # HomeView, CheckoutView, PaymentView, MyOrdersView, StaffDashboard, ...
```

### Security

- JWT passed as `Authorization: Bearer <token>`, signed with HS256 (jjwt 0.12.x, `verifyWith()` constrains algorithm to key type)
- **Role-based token lifetime**: admin tokens last **1h** (short to limit blast radius of a stolen admin token), staff tokens last **13h** (covers a full 10AM–10PM shift so floor/kitchen staff aren't logged out mid-service). Customers never authenticate — they order as guests.
- **Single active session per user**: every `User` row has a `tokenVersion` counter that is incremented on each successful login and embedded as a JWT claim. `JwtAuthenticationFilter` rejects any token whose claim doesn't match the current value, so logging in on a new device logs the previous device out on its next request. Bumping the counter server-side also works as a zero-infrastructure force-logout.
- Public endpoints: menu, order placement, order tracking, payment confirmation
- Staff endpoints: require `ROLE_STAFF` or `ROLE_ADMIN`
- Admin endpoints: require `ROLE_ADMIN`
- Invalid tokens are rejected and logged as `WARN`

#### Session lifetime (frontend)

Staff and admin sessions are deliberately short-lived on the client:

- **Token + user profile are stored in `sessionStorage`**, not `localStorage`. When the browser or tab closes, the session is gone — useful for shared devices like a kitchen tablet or front-of-house terminal.
- **Global 401 interceptor** in `api/index.js`: any authenticated request that comes back with 401 (expired, revoked, or otherwise invalid token) clears `sessionStorage` and redirects to the matching login page (`/admin/login`, `/staff/login`, or `/`). Every open tab on the device hits the same interceptor on its next request, so all sessions sharing that token are logged out together.
- 401s from `/auth/login` and `/auth/register` are excluded — they mean "wrong password" and are shown inline on the form rather than triggering a redirect.

### Error Handling

`GlobalExceptionHandler` categorizes all exceptions:

| Exception | HTTP Status | Client Message |
|---|---|---|
| Business-rule violations (known safe messages) | 400 Bad Request | Original message |
| "Not found" business errors | 404 Not Found | Original message |
| Authentication failures | 401 Unauthorized | Generic message (prevents enumeration) |
| Authorization failures | 403 Forbidden | Generic message |
| Validation errors (`@Valid`) | 400 Bad Request | Field-level error map |
| Path variable type mismatch | 400 Bad Request | `"Invalid parameter: <name>"` |
| Unexpected errors | 500 Internal Server Error | `"An unexpected error occurred"` (details logged server-side only) |

### File Upload Security

- Declared content-type must be an allowed image MIME type (JPEG, PNG, GIF, WebP)
- Actual file content is verified via magic byte inspection — prevents content-type spoofing
- File size capped at 10 MB
- Saved filename is a random UUID — user-supplied name is never used
- Target path is validated to stay inside the upload directory (path traversal guard)
- InputStream used for magic-byte detection is properly closed via try-with-resources

### Real-time (WebSocket)

- Endpoint: `/ws` with SockJS fallback
- Server broadcasts via `SimpMessagingTemplate` through a shared `broadcast()` helper in `OrderService`, called on every mutating operation

| Topic | Subscriber | Purpose |
|---|---|---|
| `/topic/orders` | Staff dashboard | Every order event across all tables |
| `/topic/orders/{orderNumber}` | Customer order tracking page | Updates for one specific order |
| `/topic/table/{tableNumber}` | Customer "My Orders" page | All events for a table — keeps every device at the same table in sync |

On the client, subscriptions go through `useWebSocket`'s `subscribe()` helper rather than `client.subscribe()` directly. The helper registers every subscription in an internal array so `disconnect()` can unsubscribe each one explicitly on component unmount — preventing leaks on rapid navigation or remount.

### Table Sessions

Each table has a `TableSession` that accumulates orders from multiple customers. Sessions expire automatically after 60 minutes of inactivity (configurable). Staff can also close a session manually.

### Concurrency

Pessimistic write locks (`SELECT ... FOR UPDATE`) are applied in any transaction where two concurrent requests could corrupt shared state:

| Method | Lock | Reason |
|---|---|---|
| `OrderService.updateOrderStatus` | `findByIdForUpdate(id)` | Prevents two staff from racing on the same order's state transition |
| `OrderService.confirmPayment` | `findByOrderNumberForUpdate(orderNumber)` | Prevents double-payment if two requests arrive simultaneously |
| `TableSessionService.getOrCreateSession` | `findByTableNumberAndStatusForUpdate` | Prevents two simultaneous first-orders at the same table from opening duplicate sessions |
| `TableSessionService.paySession` | `findByTableNumberAndStatusForUpdate` | Prevents concurrent pay requests from both reading OPEN and both writing PAID |
| `TableSessionService.closeSession` | `findByTableNumberAndStatusForUpdate` | Prevents close racing with an in-flight order placement |

The `expireInactiveSessions()` scheduler uses an **atomic bulk UPDATE** (`UPDATE table_sessions SET status='EXPIRED' WHERE status='OPEN' AND last_activity_at < :cutoff`) instead of a read-then-save loop. This eliminates a race where the scheduler could overwrite a concurrently committed `PAID` status with `EXPIRED`.

---

## Database (RDBMS)

**MySQL 8.0** — schema is managed automatically by Hibernate (`ddl-auto=update`).

### Entity Relationship Diagram

```
users ──────────────────< orders >──────────── table_sessions
                           │  │
              ─────────────┘  └──────── payments (1:1)
             │
        order_items >────── menu_items >────── categories
```

### Tables

#### `users`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR | NOT NULL |
| `username` | VARCHAR | NOT NULL, UNIQUE |
| `email` | VARCHAR | NOT NULL, UNIQUE |
| `password` | VARCHAR | NOT NULL (BCrypt hashed) |
| `phone` | VARCHAR | nullable |
| `role` | ENUM(`STAFF`, `ADMIN`) | NOT NULL |
| `token_version` | BIGINT | NOT NULL, default `0` — incremented on each login to invalidate older JWTs |
| `created_at` | DATETIME | set on insert |

#### `categories`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR | NOT NULL |
| `description` | VARCHAR | nullable |
| `image_url` | VARCHAR | nullable |

#### `menu_items`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR | NOT NULL |
| `description` | VARCHAR(1000) | nullable |
| `price` | DECIMAL | NOT NULL |
| `image_url` | VARCHAR | nullable |
| `category_id` | BIGINT | FK → `categories.id`, nullable |
| `available` | BOOLEAN | NOT NULL, default `true` |
| `created_at` | DATETIME | set on insert |

#### `table_sessions`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `table_number` | VARCHAR | NOT NULL |
| `status` | ENUM(`OPEN`, `PAID`, `EXPIRED`) | NOT NULL |
| `opened_at` | DATETIME | NOT NULL, set on insert |
| `last_activity_at` | DATETIME | NOT NULL, updated on each order |
| `closed_at` | DATETIME | nullable, set when closed |
| `payment_method` | ENUM(`CASH`, `CARD`, `QR_CODE`) | nullable, set on payment |

#### `orders`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `order_number` | VARCHAR | NOT NULL, UNIQUE (e.g. `ORD-20250407-A1B2C3`) |
| `user_id` | BIGINT | FK → `users.id`, nullable (null for guest orders) |
| `guest_name` | VARCHAR | nullable |
| `guest_phone` | VARCHAR | nullable |
| `guest_email` | VARCHAR | nullable |
| `status` | ENUM(`PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`) | NOT NULL |
| `notes` | VARCHAR(500) | nullable |
| `table_number` | VARCHAR | NOT NULL |
| `total_amount` | DECIMAL | NOT NULL |
| `table_session_id` | BIGINT | FK → `table_sessions.id` |
| `created_at` | DATETIME | set on insert |
| `updated_at` | DATETIME | updated on every change |

#### `order_items`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `order_id` | BIGINT | FK → `orders.id`, NOT NULL |
| `menu_item_id` | BIGINT | FK → `menu_items.id`, NOT NULL |
| `quantity` | INT | NOT NULL |
| `unit_price` | DECIMAL | NOT NULL (snapshot of price at order time) |
| `notes` | VARCHAR | nullable |

#### `payments`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `order_id` | BIGINT | FK → `orders.id`, NOT NULL, UNIQUE (1:1) |
| `method` | ENUM(`CASH`, `CARD`, `QR_CODE`) | NOT NULL |
| `status` | ENUM(`PENDING`, `PAID`) | NOT NULL |
| `amount` | DECIMAL | NOT NULL |
| `payment_token` | VARCHAR(36) | UNIQUE, nullable — one-time UUID required to confirm payment |
| `transaction_id` | VARCHAR | nullable, set when paid (e.g. `TXN-AB12CD34`) |
| `paid_at` | DATETIME | nullable, set when paid |
| `created_at` | DATETIME | set on insert |

### Relationships

| Relationship | Type | Description |
|---|---|---|
| `orders` → `users` | Many-to-One (nullable) | Registered customer orders; null for guest orders |
| `orders` → `table_sessions` | Many-to-One | All orders at a table share one session |
| `order_items` → `orders` | Many-to-One | Each order has one or more items |
| `order_items` → `menu_items` | Many-to-One | Links ordered item to the menu; `unit_price` snapshots the price |
| `payments` → `orders` | One-to-One | Each order has exactly one payment record |
| `menu_items` → `categories` | Many-to-One (nullable) | Items may belong to a category |

---

## API Reference

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Staff/admin login — returns JWT |

### Orders — `/api/orders`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | Public | Place a new order |
| GET | `/api/orders/track/{orderNumber}` | Public | Get order details by order number |
| POST | `/api/orders/{orderNumber}/pay` | Public | Confirm payment — requires `{ "paymentToken": "..." }` body |

### Staff — `/api/staff`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/staff/orders` | Staff/Admin | List orders (`?activeOnly=true` for in-progress) |
| PATCH | `/api/staff/orders/{id}/status` | Staff/Admin | Advance order status |
| GET | `/api/staff/tables` | Staff/Admin | List all open table sessions |
| POST | `/api/staff/tables/{tableNumber}/close` | Staff/Admin | End a table session |

### Admin — `/api/admin`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/orders` | Admin | List all orders |
| PATCH | `/api/admin/orders/{id}/status` | Admin | Update any order status (including cancel) |
| GET/POST | `/api/admin/categories` | Admin | List / create categories |
| PUT/DELETE | `/api/admin/categories/{id}` | Admin | Update / delete category |
| GET/POST | `/api/admin/menu` | Admin | List / create menu items |
| PUT/DELETE | `/api/admin/menu/{id}` | Admin | Update / delete menu item |
| PATCH | `/api/admin/menu/{id}/toggle` | Admin | Toggle item availability |

### Table Sessions — `/api/table-sessions`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/table-sessions/{tableNumber}` | Public | Get current open session for a table |
| POST | `/api/table-sessions/{tableNumber}/pay` | Public | Pay entire table bill |

---

## Frontend Routes

| Path | Role | Description |
|---|---|---|
| `/` | Customer | Menu page (scan `/?table=N` to set table) |
| `/checkout` | Customer | Cart and order placement |
| `/payment/:orderNumber` | Customer | Payment method selection |
| `/track/:orderNumber` | Customer | Live order status stepper |
| `/my-orders` | Customer | Order history for current table session |
| `/table/:tableNumber/bill` | Customer | Table bill and payment |
| `/staff/login` | Staff | Staff login |
| `/staff` | Staff | Order management dashboard |
| `/admin/login` | Admin | Admin login |
| `/admin/menu` | Admin | Menu item management |
| `/admin/categories` | Admin | Category management |
| `/admin/orders` | Admin | All orders view |
| `/admin/tables` | Admin | QR code generator |

---

## Flows

### Order Flow

```
Customer scans QR → Browses menu → Adds to cart → Checkout
  → Choose payment method → Confirm payment → Staff notified
  → Staff confirms → Preparing → Ready → Delivered
```

### Order Status

```
PENDING → CONFIRMED → PREPARING → READY → DELIVERED
```

- Orders start as `PENDING` and are immediately visible to staff
- Staff **cannot** confirm an order until payment is marked as PAID
- Cash orders are auto-marked PAID when status reaches DELIVERED

### Payment Flow

1. Customer places order → server returns a one-time `paymentToken` in the response body
2. Frontend stores the token in `localStorage` alongside the order number
3. Customer is redirected to `/payment/:orderNumber`, selects: **QR Code**, **Card**, or **Cash at Cashier**
4. Customer confirms → frontend sends `paymentToken` in the request body → payment marked `PAID`
5. Frontend shows a success dialog, then **awaits** `router.push('/my-orders')` so the page transitions cleanly (without the await, a silent router rejection would leave the customer stuck on the payment page)
6. Staff can now confirm the order
7. For whole-table settlement → `/table/:tableNumber/bill`

> **Security note:** The `paymentToken` is a UUID generated at order creation. It is returned only in the `POST /api/orders` response and is never included in WebSocket broadcasts or subsequent read responses. This prevents an unauthenticated third party who learns the order number from the public WebSocket from marking a stranger's order as PAID.

### Table Session Flow

1. First order at a table creates a new `TableSession` (OPEN)
2. All subsequent orders at that table join the same session
3. Session closes when staff ends it manually or after 60 min inactivity
4. Customer's My Orders clears automatically when session ends

---

## Design Patterns

### Architectural Patterns

| Pattern | Where |
|---|---|
| **MVC** | `controller/` → `service/` → `model/` layered separation |
| **Repository** | `OrderRepository`, `UserRepository`, etc. — data access abstracted behind interfaces |
| **Service Layer** | `OrderService`, `AuthService` — business logic isolated from controllers |
| **DTO (Data Transfer Object)** | `dto/order/`, `dto/auth/`, `dto/menu/` — separate request/response shapes from JPA entities |

### GoF Patterns

| Pattern | Where |
|---|---|
| **Builder** | Lombok `@Builder` on all JPA entities (`Order.builder()`, `User.builder()`, etc.) |
| **State Machine** | `VALID_TRANSITIONS` map in `OrderService` — all allowed order status transitions declared explicitly |
| **Chain of Responsibility** | Spring Security filter chain → `JwtAuthenticationFilter` → controllers |
| **Template Method** | `JwtAuthenticationFilter extends OncePerRequestFilter` — Spring defines the template, filter fills in `doFilterInternal` |
| **Observer** | WebSocket broadcast via `SimpMessagingTemplate` — staff dashboard reacts to order events in real time |
| **Singleton** | All Spring `@Component` / `@Service` / `@Repository` beans |
| **Proxy** | Spring AOP — `@Transactional` wraps service methods transparently |
| **Lazy Initialization** | `@Lazy` setter injection in `OrderService` to break circular dependency with `TableSessionService` |

### Frontend Patterns

| Pattern | Where |
|---|---|
| **Store (Flux/Pinia)** | `stores/auth.js`, `stores/cart.js`, `stores/table.js`, `stores/orders.js` — centralized reactive state |
| **Composable / Custom Hook** | `composables/useWebSocket.js`, `composables/useDialog.js` — reusable stateful logic |
| **Singleton** | `useDialog` — module-level state shared as a singleton across all components |
| **Facade** | `api/index.js` — wraps all Axios calls behind named functions (`staffApi`, `tableSessionApi`, etc.) |
| **Router Guard** | `router/index.js` navigation guards — controls route access per role |

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Access denied for user 'root'@'localhost'` on backend start | Wrong DB password | Update `spring.datasource.password` in `application-local.properties` |
| `Unknown database 'restaurant-order-management'` | DB not created | Run the `CREATE DATABASE` statement in [step 2](#2-set-up-the-database) |
| Backend starts but frontend shows CORS errors | Frontend running on a non-default port | Set `app.cors.allowed-origins` to include your origin |
| `global is not defined` in browser console | Vite missing SockJS shim | Already configured via `define: { global: 'globalThis' }` in `vite.config.js` — restart `npm run dev` |
| Staff dashboard doesn't receive live updates | WebSocket blocked or backend not running | Check `ws://localhost:8080/ws` is reachable; verify JWT on the staff session |
| `JWT secret must be at least 32 characters` | Weak `jwt.secret` | Use a longer secret (e.g. generate with `openssl rand -base64 48`) |
| Uploaded image 404s | Wrong `app.base-url` or `upload.dir` | Ensure `upload.dir` exists and `app.base-url` matches the backend origin |
| Port 8080 / 5173 already in use | Another process holds the port | `lsof -i :8080` (macOS/Linux) or `netstat -ano | findstr :8080` (Windows), then kill or change port |
