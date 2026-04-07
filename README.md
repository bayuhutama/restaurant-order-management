# Savoria — Restaurant Order Management System

A full-stack **dine-in** restaurant order management system. Customers scan a QR code at their table, browse the menu, and place orders. Staff manage orders in real time. Admins control the menu, categories, and tables.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [User Roles](#user-roles)
- [Features](#features)
- [Architecture](#architecture)
- [Database (RDBMS)](#database-rdbms)
- [API Reference](#api-reference)
- [Frontend Routes](#frontend-routes)
- [Flows](#flows)
- [Design Patterns](#design-patterns)

---

## Tech Stack

| Layer | Technologies |
|---|---|
| Backend | Spring Boot 3.2.3, Java 17, MySQL 8.0, JWT (jjwt 0.12.5), WebSocket (STOMP) |
| Frontend | Vue 3, Vite, Tailwind CSS, Pinia, Vue Router, Axios, @stomp/stompjs |

---

## Getting Started

### Prerequisites

Make sure the following are installed before running the project:

- **Java 17+**
- **Node.js 18+**
- **MySQL 8.0**
- **Maven** (or use the included `./mvnw` wrapper)

### 1. Clone the repository

```bash
git clone https://github.com/bayuhutama/restaurant-order-management.git
cd restaurant-order-management
```

### 2. Set up the database

Log in to MySQL and create the database:

```sql
CREATE DATABASE `restaurant-order-management`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. Configure the backend

Create a local properties file (this file is gitignored):

```bash
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties
```

Then edit `application-local.properties` and set your values:

```properties
spring.datasource.password=your_mysql_password
jwt.secret=your-secret-key-at-least-32-characters-long
```

> See [Configuration](#configuration) for all available options.

### 4. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts on **http://localhost:8080**.

On first run, `DataInitializer` automatically seeds:
- Default admin and staff accounts
- Sample menu categories and items

> **Security note:** Default account credentials are defined in `DataInitializer.java`. Change them before any non-local deployment.

### 5. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**.

### 6. Open the app

| Role | URL |
|---|---|
| Customer | http://localhost:5173 |
| Staff | http://localhost:5173/staff/login |
| Admin | http://localhost:5173/admin/login |

---

## Configuration

All backend configuration is in `backend/src/main/resources/`.

| Property | Env Variable | Default | Description |
|---|---|---|---|
| `jwt.secret` | `JWT_SECRET` | *(required)* | JWT signing secret, min 32 chars |
| `jwt.expiration` | `JWT_EXPIRATION` | `28800000` (8h) | Token lifetime in milliseconds |
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
| **Customer / Guest** | Public | Scans QR code, browses menu, places orders, pays, tracks order status |
| **Staff** | Authenticated | Views and manages orders in real time, manages table sessions |
| **Admin** | Authenticated | Full access — menu CRUD, category CRUD, order management, QR generation |

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

- JWT passed as `Authorization: Bearer <token>`, signed with HS256
- Public endpoints: menu, order placement, order tracking, payment confirmation
- Staff endpoints: require `ROLE_STAFF` or `ROLE_ADMIN`
- Admin endpoints: require `ROLE_ADMIN`
- Invalid tokens are rejected and logged as `WARN`

### Real-time (WebSocket)

- Endpoint: `/ws` with SockJS fallback
- Staff subscribes to `/topic/orders` — receives all order updates
- Customers subscribe to `/topic/orders/{orderNumber}` — receives updates for their order
- Server broadcasts via `SimpMessagingTemplate` on every order state change

### Table Sessions

Each table has a `TableSession` that accumulates orders from multiple customers. Sessions expire automatically after 60 minutes of inactivity (configurable). Staff can also close a session manually.

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
| `role` | ENUM(`CUSTOMER`, `STAFF`, `ADMIN`) | NOT NULL |
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
| POST | `/api/auth/register` | Public | Register a customer account |
| POST | `/api/auth/login` | Public | Login and receive JWT |

### Orders — `/api/orders`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | Public | Place a new order |
| GET | `/api/orders/track/{orderNumber}` | Public | Get order details by order number |
| POST | `/api/orders/{orderNumber}/pay` | Public | Confirm payment for an order |

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

1. Customer places order → redirected to `/payment/:orderNumber`
2. Customer selects: **QR Code**, **Card**, or **Cash at Cashier**
3. Customer confirms → payment marked `PAID`
4. Staff can now confirm the order
5. For whole-table settlement → `/table/:tableNumber/bill`

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
