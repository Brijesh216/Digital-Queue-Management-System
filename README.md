# Digital Queue Management System

A full-stack real-time **Digital Queue Management System** built with **Java Spring Boot, React, MySQL, JWT, Spring Security, and WebSocket/STOMP**.

The system allows customers to view available services, join queues, receive unique tokens, and track queue status in real time. Administrators can create and manage services, call the next token, complete tokens, and pause or resume queues.

---

## Features

### Customer Features

- User registration and login
- JWT-based authentication
- Role-based access control
- View available services/queues
- Join a queue and receive a unique token
- One active queue entry per user for a service
- View current serving token
- View number of waiting users
- View estimated waiting time
- Real-time queue status updates
- Join button automatically disabled when a queue is paused
- Authentication state preserved after browser refresh
- Secure logout

### Admin Features

- Admin login
- Admin dashboard
- Create new queues/services
- View available queues
- Switch between multiple services
- View current serving token
- Call the next waiting token
- Complete the current token
- Pause a queue
- Resume a queue
- View waiting token information
- Real-time synchronization across connected users

### Real-Time Features

The system uses **WebSocket/STOMP** to synchronize queue changes instantly.

When an administrator calls or completes a token:

- Customer dashboards receive the update automatically
- Current token information is updated
- Waiting count is updated
- Queue status is synchronized
- No page refresh is required

---

## Technology Stack

### Backend

- Java
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- MySQL
- WebSocket
- STOMP
- Maven

### Frontend

- React
- Vite
- JavaScript
- Axios
- React Router
- Tailwind CSS
- SockJS
- STOMP.js
- React Hot Toast

### Development Tools

- Visual Studio Code
- Git
- GitHub
- MySQL Workbench
- Postman

---

## System Architecture

```text
                    ┌──────────────────────┐
                    │      React UI        │
                    │   Customer / Admin   │
                    └──────────┬───────────┘
                               │
                    REST API / WebSocket
                               │
                               ▼
                    ┌──────────────────────┐
                    │   Spring Boot API    │
                    │                      │
                    │ Controllers          │
                    │ Services             │
                    │ Security             │
                    │ JWT Authentication   │
                    │ WebSocket / STOMP    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │        MySQL         │
                    │                      │
                    │ Users                │
                    │ Queues               │
                    │ Queue Entries/Tokens │
                    └──────────────────────┘
```

---

## User Roles

The application supports role-based access.

### Customer

Customers can:

- Register
- Login
- View available services
- Join queues
- Receive queue tokens
- Track their queue status
- Receive real-time updates

### Admin

Administrators can:

- Login
- Create services
- Manage queues
- Call the next token
- Complete the current token
- Pause queues
- Resume queues
- Monitor queue activity

---

## Authentication and Security

The application uses **JWT authentication with Spring Security**.

### Authentication Flow

```text
User
  │
  │ Login
  ▼
React Frontend
  │
  │ POST /api/auth/login
  ▼
Spring Boot Backend
  │
  │ Validate credentials
  ▼
JWT Token Generated
  │
  ▼
React Frontend
  │
  │ Store JWT
  ▼
Authenticated Requests
```

The JWT is included with protected API requests.

Spring Security validates the token and determines the user's role before allowing access to protected resources.

### Security Features

- Password hashing using BCrypt
- JWT-based authentication
- Spring Security authorization
- Role-based access control
- Protected frontend routes
- Protected backend APIs
- Token validation
- Secure authentication state restoration

---

## Queue Management Flow

### Customer Queue Flow

```text
Login
  ↓
Dashboard
  ↓
Choose Service
  ↓
Join Queue
  ↓
Receive Token
  ↓
Wait
  ↓
Admin Calls Token
  ↓
Token Being Served
  ↓
Admin Completes Token
  ↓
Queue Moves Forward
```

### Admin Queue Flow

```text
Admin Login
  ↓
Admin Dashboard
  ↓
Select Service
  ↓
View Queue
  ↓
Call Next Token
  ↓
Serve Customer
  ↓
Complete Current Token
  ↓
Call Next Token
```

---

## Queue Lifecycle

A typical queue follows this lifecycle:

```text
WAITING
   ↓
CALLED
   ↓
SERVING
   ↓
COMPLETED
```

A queue can also be paused:

```text
ACTIVE
  ↓
PAUSED
  ↓
RESUMED
  ↓
ACTIVE
```

When a queue is paused, customers cannot join that queue until it is resumed.

---

## Real-Time WebSocket Architecture

The system uses **STOMP over WebSocket** with SockJS support on the frontend.

```text
Admin
  │
  │ Call / Complete / Pause / Resume
  ▼
Spring Boot Backend
  │
  │ Queue State Updated
  ▼
WebSocket/STOMP
  │
  ├──────────────► Customer 1
  │
  ├──────────────► Customer 2
  │
  └──────────────► Customer 3
```

This allows all connected users to see queue changes without manually refreshing the browser.

---

## Project Structure

### Backend

The backend follows a layered Spring Boot architecture.

```text
backend/
└── src/
    └── main/
        ├── java/
        │   └── ...
        │       ├── api/
        │       │   ├── controller/
        │       │   └── dto/
        │       │
        │       ├── application/
        │       │   ├── service/
        │       │   ├── event/
        │       │   └── listener/
        │       │
        │       ├── domain/
        │       │   ├── entity/
        │       │   ├── model/
        │       │   └── specification/
        │       │
        │       ├── infrastructure/
        │       │   ├── repository/
        │       │   └── persistence/
        │       │
        │       ├── security/
        │       ├── jwt/
        │       ├── websocket/
        │       ├── config/
        │       └── util/
        │
        └── resources/
            └── application.properties
```

> The exact package names may differ depending on the current backend implementation.

### Frontend

```text
digital-queue-frontend/
└── src/
    ├── components/
    │   ├── ProtectedRoute.jsx
    │   ├── AdminRoute.jsx
    │   └── QueueCard.jsx
    │
    ├── context/
    │   └── AuthContext.jsx
    │
    ├── hooks/
    │   └── useAuth.js
    │
    ├── pages/
    │   ├── Login.jsx
    │   ├── Register.jsx
    │   ├── Dashboard.jsx
    │   └── AdminDashboard.jsx
    │
    ├── services/
    │   ├── api.js
    │   ├── authService.js
    │   └── queueService.js
    │
    ├── utils/
    │   └── storage.js
    │
    ├── App.jsx
    └── main.jsx
```

---

## Database

The application uses **MySQL** for persistent data storage.

The database stores information related to:

- Users
- Roles
- Queues/services
- Queue entries
- Tokens
- Queue status
- Current serving token

A simplified relationship can be represented as:

```text
User
 │
 │ 1
 │
 ├───────────────< Queue Entry
                         │
                         │
                         ▼
                       Queue
```

---

## API Overview

The backend exposes REST APIs for authentication and queue management.

Typical authentication endpoints include:

```text
POST /api/auth/register
POST /api/auth/login
```

Protected queue endpoints handle operations such as:

```text
Create Queue
Get Queues
Join Queue
Call Next Token
Complete Current Token
Pause Queue
Resume Queue
```

The exact endpoint paths should be checked against the current backend implementation.

---

## Getting Started

### Prerequisites

Install the following before running the project:

- Java JDK
- Maven
- Node.js
- npm
- MySQL
- Git

Recommended versions depend on the Spring Boot and frontend package versions used by the project.

---

## Clone the Repository

```bash
git clone https://github.com/Brijesh216/Digital-Queue-Management-System.git
cd Digital-Queue-Management-System
```

---

## Backend Setup

### 1. Open the backend project

Navigate to the Spring Boot backend directory.

```bash
cd backend
```

> Use the actual backend directory name if it differs in the repository.

### 2. Configure MySQL

Create a MySQL database for the application.

Example:

```sql
CREATE DATABASE digital_queue;
```

Update the Spring Boot database configuration in:

```text
src/main/resources/application.properties
```

Example configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/digital_queue
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

Do not commit real database passwords or secrets to GitHub.

### 3. Run the backend

Using Maven:

```bash
mvn spring-boot:run
```

Or on Windows:

```bash
mvnw spring-boot:run
```

The backend will start on the configured Spring Boot port.

---

## Frontend Setup

Open a new terminal and navigate to the frontend directory.

```bash
cd digital-queue-frontend
```

Install dependencies:

```bash
npm install
```

If the application uses an environment file, configure the API URL using Vite variables.

Example:

```env
VITE_API_URL=http://localhost:3000
```

Run the development server:

```bash
npm run dev
```

Vite will provide a local development URL, normally similar to:

```text
http://localhost:5173
```

---

## Running the Full Application

Start the application in this order:

### 1. MySQL

Make sure the MySQL server is running.

### 2. Spring Boot Backend

```bash
mvn spring-boot:run
```

### 3. React Frontend

```bash
npm run dev
```

Then open the frontend URL in your browser.

---

## Example Usage

### Customer

1. Register a new account.
2. Login.
3. Open the customer dashboard.
4. Select a service.
5. Click **Join Queue**.
6. Receive a unique token.
7. Monitor the current serving token and waiting count.
8. Continue watching the dashboard for real-time updates.

### Admin

1. Login with an administrator account.
2. Open the Admin Dashboard.
3. Select a service.
4. View the queue.
5. Click **Call Next Token**.
6. Serve the customer.
7. Click **Complete Current Token**.
8. Call the next customer.
9. Pause or resume the queue when required.

---

## Multi-User Real-Time Testing

The system can be tested with multiple browser sessions.

Example:

```text
Browser 1
Customer A
   │
   └── Joins Queue → Token 1

Browser 2
Customer B
   │
   └── Joins Queue → Token 2

Browser 3
Admin
   │
   └── Calls Next Token
            │
            ▼
     Token 1 is called
            │
            ▼
   Customers receive update
```

The queue state should update automatically across connected clients.

---

## Error Handling

The application handles common queue and authentication scenarios such as:

- Invalid login credentials
- Unauthorized access
- Invalid or expired JWT
- Duplicate queue joining
- Joining a paused queue
- No waiting tokens available
- Invalid queue/service selection
- Protected route access
- Authentication state restoration

---

## Frontend Routing

The frontend uses protected routes to separate customer and administrator access.

Conceptually:

```text
/login
   │
   ├── Customer
   │      └── /dashboard
   │
   └── Admin
          └── /admin
```

Users are redirected according to their authenticated role.

---

## UI

The application uses a modern dark dashboard interface with:

- Responsive layouts
- Service cards
- Queue status indicators
- Token information
- Admin controls
- Customer dashboard
- Real-time notifications
- Responsive buttons and forms
- Active and paused queue states

---

## Testing

The main application flows can be manually tested using:

### Authentication

- Register customer
- Login customer
- Login admin
- Logout
- Refresh browser
- Verify authentication remains associated with the correct user

### Customer Queue

- View services
- Join queue
- Prevent duplicate joining
- Verify generated token
- Verify waiting count
- Verify estimated wait
- Verify paused queue restriction

### Admin Queue

- Create service
- Switch between services
- View queue
- Call next token
- Complete current token
- Pause queue
- Resume queue
- Handle empty queue

### WebSocket

- Open multiple customer sessions
- Login as admin in another session
- Change queue state from admin
- Verify customer dashboards update without refresh

---

## Important Configuration Notes

### Environment Variables

Do not commit sensitive values such as:

```text
Database passwords
JWT secrets
API keys
Production credentials
```

Use environment variables or local configuration files instead.

### `.gitignore`

Recommended entries:

```gitignore
node_modules/
dist/
.env
.env.local
target/
*.log
```

---

## Future Improvements

Possible future improvements include:

- Queue analytics dashboard
- Daily/monthly queue statistics
- Email notifications
- SMS notifications
- QR-code based queue joining
- Appointment scheduling
- Customer notification when their token is approaching
- Operator role
- Multiple branch/location support
- Docker support
- Cloud deployment
- Production monitoring
- Automated testing
- CI/CD pipeline

---

## Project Goals

The project was developed to demonstrate practical implementation of:

- Full-stack web development
- REST API development
- Spring Boot
- Spring Security
- JWT authentication
- Role-based authorization
- React application development
- MySQL database integration
- WebSocket real-time communication
- STOMP messaging
- API integration
- State management
- Protected routing
- Multi-user real-time synchronization

---

## 👨‍💻 Author

**Brijesh Prasad**

🌐 Connect with me: 
- 🔗 [LinkedIn](https://www.linkedin.com/in/brijesh216) 
- 💻 [GitHub](https://github.com/brijesh216)

---

⭐ If you found this project helpful, consider giving it a star on GitHub!

## 📜 License

This project is intended for educational, portfolio, and demonstration purposes.

If you reuse or modify this project, please provide appropriate attribution.

