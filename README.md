# Savoria — Restaurant Order Management

A full-stack **dine-in** restaurant order management system. Customers scan a QR code at their table to browse the menu and place orders. Staff manage orders in real time. Admins control the menu, categories, and tables.

## Tech Stack

| Layer | Technologies |
|---|---|
| Backend | Spring Boot 3.2.3, Java 17, MySQL 8.0, JWT, WebSocket (STOMP) |
| Frontend | Vue 3, Vite, Tailwind CSS, Pinia, Vue Router, Axios |

## Features

- **QR table ordering** — Customers scan a QR code, browse the menu, and place orders without an account
- **Payment flow** — After ordering, customers choose QR code, card, or cash at cashier; staff can only process orders after payment is confirmed
- **Real-time updates** — Orders and table sessions appear instantly on the staff dashboard via WebSocket
- **My Orders** — Customers see their order history and live status scoped to their current table session
- **Order tracking** — Customers can track individual order progress with a live stepper
- **Table sessions** — Multiple orders per table accumulate into one bill; customers pay at the end via the bill page
- **Staff dashboard** — Status-tab navigation (Pending / Confirmed / Preparing / Ready / All), active table session management, one-click order progression
- **Table session management** — Staff can end a table session to free the table for the next customer; customer history clears automatically
- **Admin panel** — Full CRUD for menu items and categories, image uploads, QR code generation, order management
- **Responsive** — Works on mobile, tablet, and desktop
- **Dark mode** — Automatically follows the OS/browser preference
- **Search** — Live search on the menu page, staff dashboard, and admin views
- **Custom dialogs** — All alerts and confirmations use styled in-app modals (no browser `alert`/`confirm`)

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- MySQL 8.0

### Database setup

```sql
CREATE DATABASE restaurant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Backend

```bash
cd backend
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# Edit application-local.properties and set your DB password
./mvnw spring-boot:run
```

The backend starts on **http://localhost:8080**.

On first run, `DataInitializer` seeds two accounts and sample menu data automatically.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**.

## Default Accounts

| Role  | Username | Password  |
|-------|----------|-----------|
| Admin | admin    | Admin123! |
| Staff | staff    | Staff123! |

## Routes

| Path | Description |
|---|---|
| `/` | Customer menu (scan `/?table=N` to set table) |
| `/checkout` | Place order |
| `/payment/:orderNumber` | Payment page (QR / Card / Cash) |
| `/track/:orderNumber` | Order tracking stepper |
| `/my-orders` | Customer order history for current table |
| `/table/:tableNumber/bill` | Table bill & payment |
| `/staff/login` | Staff login |
| `/staff` | Staff order dashboard |
| `/admin/login` | Admin login |
| `/admin/menu` | Menu item management |
| `/admin/categories` | Category management |
| `/admin/orders` | All orders view |
| `/admin/tables` | QR code generator |

## Order Status Flow

```
PENDING → CONFIRMED → PREPARING → READY → DELIVERED
```

Orders are placed as `PENDING` and immediately visible to staff. Staff can only advance an order to `CONFIRMED` after the customer's payment is confirmed.

## Payment Flow

1. Customer places order → redirected to `/payment/:orderNumber`
2. Customer selects payment method: **QR Code**, **Card** (Luhn-validated), or **Cash at Cashier**
3. Customer confirms payment → order payment marked PAID
4. Staff sees the order as payable and can confirm it
5. For whole-table settlement, staff or customer visits `/table/:tableNumber/bill`
