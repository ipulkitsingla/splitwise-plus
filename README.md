# Splitwise++ — Smart Expense Splitter

A production-ready, full-stack expense management system built with Java Spring Boot and React.

---

## Architecture Overview

```
splitwise-plus/
├── backend/                         # Spring Boot application
│   └── src/main/java/com/splitwiseplusplus/
│       ├── controller/              # REST controllers (Auth, Group, Expense, Settlement, etc.)
│       ├── service/                 # Business logic layer
│       ├── repository/              # Spring Data JPA repositories
│       ├── model/                   # JPA entities
│       ├── dto/                     # Request/response DTOs
│       ├── security/                # JWT filter, UserDetailsService
│       ├── config/                  # Security, Swagger, Firebase, WebSocket configs
│       ├── scheduler/               # Recurring expenses, reminders, monthly reports
│       ├── websocket/               # STOMP WebSocket notification handler
│       ├── util/                    # Debt simplification algorithm
│       └── exception/               # Custom exceptions + global handler
├── frontend/                        # React + Vite application
│   └── src/
│       ├── pages/                   # Dashboard, Group, Expenses, Balances, Analytics, Notifications
│       ├── components/              # Layout, modals
│       ├── api.js                   # Axios client + all API methods
│       ├── store.js                 # Zustand auth + notification stores
│       └── index.css                # Design system tokens + utility classes
├── docker-compose.yml               # Full stack Docker deployment
└── docs/                            # Additional documentation
```

---

## Tech Stack

| Layer       | Technology                                      |
|-------------|------------------------------------------------|
| Backend     | Java 17, Spring Boot 3.2, Spring Security 6    |
| Auth        | JWT (HS512), BCrypt password hashing           |
| Database    | MySQL 8.0, Hibernate JPA                       |
| Real-time   | WebSockets (STOMP/SockJS)                      |
| Email       | Spring Mail (SMTP), HTML templates             |
| Push        | Firebase Cloud Messaging (FCM)                 |
| OCR         | Tesseract 4 via Tess4J                         |
| Currency    | ExchangeRate-API (cached)                      |
| Frontend    | React 18, Vite, Tailwind CSS, Recharts         |
| State       | Zustand                                        |
| HTTP Client | Axios (with JWT interceptors + auto-refresh)   |
| Docs        | Swagger / SpringDoc OpenAPI 3                  |
| Deploy      | Docker, Docker Compose                         |

---

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.0 (or Docker)
- Tesseract OCR (`apt install tesseract-ocr`)

### 1. Clone and configure

```bash
git clone <repo-url>
cd splitwise-plus
cp .env.example .env
# Edit .env with your credentials
```

### 2. Start with Docker Compose (recommended)

```bash
docker-compose up -d
```

This starts:
- MySQL at `localhost:3306`
- Spring Boot API at `http://localhost:8080/api/v1`
- React frontend at `http://localhost:3000`
- Swagger UI at `http://localhost:8080/api/v1/swagger-ui.html`

### 3. Manual setup (development)

**Backend:**
```bash
cd backend
# Configure application.properties or set env vars
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

---

## Environment Variables

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/splitwise_plus
DB_USERNAME=splitwise
DB_PASSWORD=your_password

# JWT (use a long random string, min 64 chars)
JWT_SECRET=your-super-secret-jwt-key-minimum-64-characters-long
JWT_EXPIRATION_MS=86400000       # 24 hours
JWT_REFRESH_EXPIRATION_MS=604800000  # 7 days

# Email (Gmail example)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password   # Use App Password, not real password

# Firebase (optional)
FIREBASE_ENABLED=false
FIREBASE_CREDENTIALS=./firebase-service-account.json

# CORS
CORS_ORIGINS=http://localhost:3000
```

---

## API Documentation

Full interactive docs: `http://localhost:8080/api/v1/swagger-ui.html`

### Authentication

| Method | Endpoint          | Description                  | Auth |
|--------|-------------------|------------------------------|------|
| POST   | /auth/register    | Register new user            | No   |
| POST   | /auth/login       | Login, returns JWT           | No   |
| POST   | /auth/refresh     | Refresh access token         | No   |

**Login request:**
```json
POST /auth/login
{ "email": "user@example.com", "password": "password123" }
```
**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": { "id": 1, "name": "Alice", "email": "user@example.com" }
  }
}
```

### Groups

| Method | Endpoint                     | Description                     |
|--------|------------------------------|---------------------------------|
| POST   | /groups                      | Create group                    |
| GET    | /groups/my                   | Get current user's groups       |
| GET    | /groups/{id}                 | Get group details               |
| POST   | /groups/{id}/members         | Add member (admin only)         |
| DELETE | /groups/{id}/members/{uid}   | Remove member (admin only)      |
| POST   | /groups/join                 | Join via invite code            |
| POST   | /groups/{id}/invite          | Invite user by email            |

### Expenses

| Method | Endpoint                                 | Description                          |
|--------|------------------------------------------|--------------------------------------|
| POST   | /expenses                                | Create expense                       |
| GET    | /expenses/group/{groupId}                | List expenses (paginated + filtered) |
| GET    | /expenses/{id}                           | Get expense by ID                    |
| PUT    | /expenses/{id}                           | Update expense                       |
| DELETE | /expenses/{id}                           | Delete expense                       |
| POST   | /expenses/{id}/receipt                   | Attach receipt image                 |
| POST   | /expenses/scan-receipt                   | OCR scan receipt image               |
| GET    | /expenses/group/{groupId}/suggestions    | Smart split suggestions              |

**Create expense (equal split):**
```json
POST /expenses
{
  "description": "Team lunch",
  "amount": 120.00,
  "currency": "USD",
  "groupId": 1,
  "paidById": 2,
  "splitType": "EQUAL",
  "category": "FOOD",
  "expenseDate": "2025-01-15",
  "participantIds": [1, 2, 3, 4]
}
```

**Create expense (percentage split):**
```json
POST /expenses
{
  "description": "Hotel booking",
  "amount": 300.00,
  "splitType": "PERCENTAGE",
  "splitData": { "1": 50, "2": 30, "3": 20 }
}
```

### Settlements & Balances

| Method | Endpoint                              | Description                                |
|--------|---------------------------------------|--------------------------------------------|
| POST   | /settlements                          | Record payment                             |
| GET    | /settlements/group/{id}/balances      | Get simplified balances (debt algorithm)   |
| GET    | /settlements/group/{id}               | Settlement history                         |

**Balances response shows minimized transactions:**
```json
{
  "simplifiedTransactions": [
    { "fromUserId": 3, "fromUserName": "Carol", "toUserId": 1, "toUserName": "Alice", "amount": 40.00 }
  ],
  "netBalances": { "1": 80.00, "2": -40.00, "3": -40.00 }
}
```

### Analytics

| Method | Endpoint                  | Query params            |
|--------|---------------------------|-------------------------|
| GET    | /analytics/group/{id}     | startDate, endDate      |

### Notifications

| Method | Endpoint                    | Description            |
|--------|-----------------------------|------------------------|
| GET    | /notifications              | Paginated notifications|
| GET    | /notifications/unread-count | Unread count           |
| PUT    | /notifications/{id}/read    | Mark one as read       |
| PUT    | /notifications/read-all     | Mark all as read       |

---

## Debt Simplification Algorithm

The core algorithm minimizes the number of transactions needed to settle all debts:

```
Input:  Net balances per user (positive = owed, negative = owes)
Output: Minimum set of payment transactions

Algorithm (Greedy, O(n log n)):
1. Split users into creditors (net > 0) and debtors (net < 0)
2. Use max-heap for creditors, min-heap for debtors
3. While both heaps non-empty:
   a. Pop largest creditor C (owed most) and largest debtor D (owes most)
   b. settle = min(C.amount, D.amount)
   c. Record transaction: D pays C settle amount
   d. Carry over remainder back into appropriate heap
4. Return transaction list

Example:
  4 people, total $500 spent, each owes $125:
  A paid $300 → net +$175
  B paid $100 → net -$25
  C paid $50  → net -$75
  D paid $50  → net -$75

  Raw transactions needed (naive): 3
  After algorithm: 3 (optimal for 4 people)
  Reduction from naive O(n²): significant for larger groups
```

---

## WebSocket (Real-time Notifications)

Connect using STOMP over SockJS:

```javascript
const client = new Client({
  webSocketFactory: () => new SockJS('/api/v1/ws'),
  onConnect: () => {
    client.subscribe(`/user/${userId}/queue/notifications`, (msg) => {
      const notification = JSON.parse(msg.body)
      // { id, title, message, type, read, createdAt }
    })
  }
})
client.activate()
```

---

## Scheduled Tasks

| Task                    | Schedule                    | Description                          |
|-------------------------|-----------------------------|--------------------------------------|
| Recurring Expenses      | Daily at 00:05              | Creates new expense from templates   |
| Payment Reminders       | Every Monday at 09:00       | Emails/pushes outstanding debts      |
| Monthly Reports         | 1st of each month at 08:00  | Sends analytics summary by email     |

---

## Database Schema

```sql
users           (id, name, email, password, phone, profile_image_url, preferred_currency, role, ...)
groups          (id, name, description, type, currency, invite_code, created_by, ...)
group_members   (id, group_id, user_id, role, status, joined_at)
expenses        (id, description, amount, currency, amount_in_base_currency, split_type, category,
                 expense_date, group_id, paid_by, created_by, recurring, recurrence_type, ...)
expense_splits  (id, expense_id, user_id, owed_amount, percentage, is_settled)
settlements     (id, group_id, payer_id, receiver_id, amount, currency, payment_method, settled_at)
notifications   (id, user_id, title, message, type, reference_id, is_read, created_at)
device_tokens   (id, user_id, token, device_type, is_active)
```

---

## Deployment on Railway / Render

**Backend (Spring Boot):**
1. Set environment variables in dashboard
2. Build command: `mvn clean package -DskipTests`
3. Start command: `java -jar target/*.jar`
4. Add MySQL plugin / external DB

**Frontend (React):**
1. Build command: `npm install && npm run build`
2. Publish directory: `dist`
3. Set `VITE_API_URL` env var

---

## Security

- JWT HS512 tokens with configurable expiration
- BCrypt password hashing (strength 12)
- Role-based authorization (USER / ADMIN) via `@PreAuthorize`
- SQL injection prevention via JPA parameterized queries
- Input validation via Jakarta Bean Validation (`@Valid`)
- CORS configured for specific origins only
- Stateless session management

---

## Running Tests

```bash
cd backend
mvn test

# Run specific test
mvn test -Dtest=DebtSimplificationAlgorithmTest
```

---

## Sample Data (SQL)

```sql
INSERT INTO users (name, email, password, preferred_currency, role) VALUES
('Alice Johnson', 'alice@example.com', '$2a$12$...', 'USD', 'USER'),
('Bob Smith',     'bob@example.com',   '$2a$12$...', 'USD', 'USER'),
('Carol Lee',     'carol@example.com', '$2a$12$...', 'EUR', 'USER');

INSERT INTO `groups` (name, type, currency, invite_code, created_by) VALUES
('Goa Trip 2025', 'TRIP', 'USD', 'GOATRIP1', 1),
('Apartment',     'HOME', 'USD', 'APT12345', 1);
```
