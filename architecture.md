# Student Library — Full Stack Project

> **Intern project at Capestart** — A production-style Library Management System
> built with Spring Boot (backend) + React (frontend), connected to PostgreSQL.

---

## Table of Contents

1. [What We Are Building](#what-we-are-building)
2. [Tech Stack](#tech-stack)
3. [System Architecture](#system-architecture)
4. [Project Structure](#project-structure)
5. [Backend Deep Dive](#backend-deep-dive)
6. [Frontend Deep Dive](#frontend-deep-dive)
7. [How Everything Connects](#how-everything-connects)
8. [API Reference](#api-reference)
9. [Data Model](#data-model)
10. [Development Roadmap](#development-roadmap)
11. [How to Run Locally](#how-to-run-locally)

---

## What We Are Building

A **Library Management System** for a college. A librarian can:

- Add, edit, and delete **students**
- Add, edit, and delete **books**
- **Assign** a book to a student
- **Remove** a book from a student
- See a **dashboard** with live stats (total students, total books, assigned vs free)
- See which books are **unassigned** (available on the shelf)
- **Search** for any student or book instantly

This is not a toy project. It follows the same patterns used in real production
applications at software companies — layered architecture, DTOs, validation,
error handling, database migrations, and a component-based frontend.

---

## Tech Stack

### Backend

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Programming language |
| Spring Boot | 3.5.11 | Application framework |
| Spring Web | managed by Boot | REST API endpoints |
| Spring Data JPA | managed by Boot | Database access using Java objects |
| Spring Validation | managed by Boot | Input validation (@NotBlank, @Email, etc.) |
| Hibernate | managed by Boot | ORM — maps Java classes to DB tables |
| PostgreSQL | 15+ | Production relational database |
| Liquibase | managed by Boot | Database migration tool (creates tables, seeds data) |
| Lombok | 1.18.34 | Removes boilerplate (generates getters, setters, constructors) |
| MapStruct | 1.6.3 | Auto-generates Entity ↔ DTO conversion code |
| Maven | 3.9.12 | Build tool and dependency manager |

### Frontend

| Technology | Version | Purpose |
|---|---|---|
| React | 18 | UI component library |
| Vite | 5 | Build tool and dev server (fast HMR) |
| React Router | v6 | Client-side page navigation |
| TanStack React Query | v5 | Server state management and API caching |
| Axios | 1.x | HTTP client for calling the Spring API |
| Tailwind CSS | v3 | Utility-first CSS framework |

### Dev Tools

| Tool | Purpose |
|---|---|
| IntelliJ IDEA Ultimate | IDE for both Spring and React |
| pgAdmin / psql | PostgreSQL database GUI / CLI |
| Postman / IntelliJ HTTP client | API testing |
| Git | Version control |

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER (Browser)                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │  opens http://localhost:5173
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                   REACT FRONTEND                                │
│                  (student-library-ui)                           │
│                                                                 │
│   Pages          Hooks              API Layer                   │
│   ─────          ─────              ─────────                   │
│   Dashboard  →   useStudents    →   students.js  ─┐            │
│   Students   →   useBooks       →   books.js     ─┤            │
│   Books      →   useAssignBook  →   axios.js     ─┘            │
│   StudentDetail                                                 │
│   BookDetail                                                    │
│                                                                 │
│   Components: Layout, Sidebar, SearchBar,                       │
│               Modal, Toast, Badge, Skeleton                     │
└─────────────────────────┬───────────────────────────────────────┘
                          │  HTTP requests (JSON)
                          │  GET POST PUT DELETE
                          │  http://localhost:8080/api/v1/...
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SPRING BOOT BACKEND                            │
│                  (student-library)                              │
│                                                                 │
│   Controller Layer   → receives HTTP requests                   │
│   ────────────────                                              │
│   BookController     /api/v1/books/**                           │
│   StudentController  /api/v1/students/**                        │
│                                                                 │
│   Service Layer      → business logic                           │
│   ─────────────                                                 │
│   BookServiceImpl    → validates, processes, delegates          │
│   StudentServiceImpl → duplicate checks, assign/unassign logic  │
│                                                                 │
│   Repository Layer   → database access                          │
│   ────────────────                                              │
│   BookRepository     → JPA queries on books table              │
│   StudentRepository  → JPA queries on students table           │
│                                                                 │
│   Supporting                                                    │
│   ──────────                                                    │
│   MapStruct mappers  → Entity ↔ DTO conversion                 │
│   GlobalExceptionHandler → consistent error responses           │
│   CorsConfig         → allows React to call this API            │
└─────────────────────────┬───────────────────────────────────────┘
                          │  SQL queries via Hibernate
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     POSTGRESQL DATABASE                         │
│                      (studentlibrary)                           │
│                                                                 │
│   students table          books table                           │
│   ─────────────           ────────────                          │
│   id (PK)                 id (PK)                               │
│   name                    title                                 │
│   email (unique)          author                                │
│   phone                   genre                                 │
│   department              isbn (unique)                         │
│                           student_id (FK → students.id)         │
│                                                                 │
│   Managed by Liquibase migrations (runs on app startup)         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Project Structure

### Backend — `student-library/`

```
student-library/
├── pom.xml                          ← Maven dependencies and build config
├── src/
│   └── main/
│       ├── java/com/capestart/studentlibrary/
│       │   ├── StudentLibraryApplication.java   ← main() entry point
│       │   │
│       │   ├── config/
│       │   │   └── CorsConfig.java              ← allows React to call the API
│       │   │
│       │   ├── controller/
│       │   │   ├── BookController.java          ← HTTP endpoints for books
│       │   │   └── StudentController.java       ← HTTP endpoints for students
│       │   │
│       │   ├── service/
│       │   │   ├── BookService.java             ← interface (contract)
│       │   │   ├── StudentService.java          ← interface (contract)
│       │   │   └── impl/
│       │   │       ├── BookServiceImpl.java     ← actual business logic
│       │   │       └── StudentServiceImpl.java  ← actual business logic
│       │   │
│       │   ├── repository/
│       │   │   ├── BookRepository.java          ← JPA DB queries
│       │   │   └── StudentRepository.java       ← JPA DB queries
│       │   │
│       │   ├── entity/
│       │   │   ├── Book.java                    ← maps to books table
│       │   │   └── Student.java                 ← maps to students table
│       │   │
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   ├── BookRequestDto.java      ← what the API accepts
│       │   │   │   └── StudentRequestDto.java   ← what the API accepts
│       │   │   └── response/
│       │   │       ├── BookResponseDto.java     ← what the API returns
│       │   │       ├── BookSummaryDto.java      ← minimal book inside student
│       │   │       └── StudentResponseDto.java  ← what the API returns
│       │   │
│       │   ├── mapper/
│       │   │   ├── BookMapper.java              ← Entity ↔ DTO (MapStruct)
│       │   │   └── StudentMapper.java           ← Entity ↔ DTO (MapStruct)
│       │   │
│       │   └── exception/
│       │       └── GlobalExceptionHandler.java  ← consistent error JSON
│       │
│       └── resources/
│           ├── application.properties           ← DB config, JPA config
│           └── db/changelog/
│               ├── db.changelog-master.xml      ← Liquibase entry point
│               └── changes/
│                   ├── 01-create-student-table.xml
│                   ├── 02-create-book-table.xml
│                   ├── 03-insert-students.xml   ← 10 sample students
│                   └── 04-insert-books.xml      ← 10 sample books
```

### Frontend — `student-library-ui/`

```
student-library-ui/
├── package.json                     ← npm dependencies
├── vite.config.js                   ← Vite build config
├── index.html                       ← single HTML file (React mounts here)
├── tailwind.config.js               ← Tailwind setup
└── src/
    ├── main.jsx                     ← entry point, wraps app in providers
    ├── App.jsx                      ← defines all routes
    ├── index.css                    ← global styles + Tailwind imports
    │
    ├── api/
    │   ├── axios.js                 ← Axios instance with base URL
    │   ├── students.js              ← all student API functions
    │   └── books.js                 ← all book API functions
    │
    ├── hooks/
    │   ├── useStudents.js           ← React Query hooks for students
    │   └── useBooks.js              ← React Query hooks for books
    │
    ├── components/
    │   ├── Layout.jsx               ← sidebar + main area wrapper
    │   ├── Sidebar.jsx              ← navigation links
    │   ├── Modal.jsx                ← reusable modal dialog
    │   ├── Toast.jsx                ← success/error notifications
    │   ├── Badge.jsx                ← status pills (Assigned, Free)
    │   ├── ConfirmDialog.jsx        ← "Are you sure?" before delete
    │   ├── Skeleton.jsx             ← loading placeholder cards
    │   └── EmptyState.jsx           ← zero results illustration
    │
    └── pages/
        ├── Dashboard.jsx            ← stats + activity + unassigned shelf
        ├── Students.jsx             ← list all students, search, add
        ├── StudentDetail.jsx        ← one student + their books
        ├── Books.jsx                ← list all books, genre filter
        ├── BookDetail.jsx           ← one book + assign/unassign
        └── UnassignedShelf.jsx      ← all books without a student
```

---

## Backend Deep Dive

### Why each layer exists

**Controller** — The only layer that knows about HTTP.
It receives a request, calls the service, and returns a response.
It does NOT contain any business logic.

```
POST /api/v1/students
  → StudentController.createStudent()
  → calls StudentService.createStudent()
  → returns 201 Created + StudentResponseDto as JSON
```

**Service** — Where business logic lives.
Checks for duplicates, validates relationships, coordinates between repositories.
It does NOT know about HTTP at all.

**Repository** — The only layer that talks to the database.
Extends JpaRepository which gives you save(), findById(), findAll(), delete()
for free. You only write custom queries when needed.

**Entity** — A Java class that maps directly to a database table.
`@Entity` tells Hibernate "this class is a table".
`@Column` tells Hibernate "this field is a column".

**DTO (Data Transfer Object)** — What goes in and out of the API.
Keeps your internal Entity separate from what you expose.
Request DTOs have validation annotations.
Response DTOs control exactly what fields the frontend receives.

**Mapper (MapStruct)** — Converts Entity → DTO and DTO → Entity.
You write an interface, MapStruct generates the implementation at compile time.

### Why DTOs instead of just returning the Entity?

If you returned the Entity directly:
- You'd expose internal fields you don't want the frontend to see
- Circular references (Student has Books, Book has Student) would crash JSON serialization
- You can't add computed fields like `studentName` on the book response

DTOs solve all three problems.

### How Liquibase works

Every time the Spring app starts, Liquibase checks which changesets have already
run (tracked in the `databasechangelog` table). It only runs new ones.
This means your database schema is version-controlled just like your code.

```
App starts → Liquibase reads db.changelog-master.xml
           → Checks databasechangelog table
           → Runs only changesets not yet applied
           → Creates tables, inserts seed data
           → App continues starting up
```

---

## Frontend Deep Dive

### Why React?

React lets you build UIs as **components** — reusable pieces of HTML + JavaScript.
Instead of writing `document.getElementById` everywhere, you describe *what* the UI
should look like given some data, and React figures out *how* to update the DOM.

```jsx
// A component is just a function that returns HTML-like syntax (JSX)
function StudentCard({ student }) {
  return (
    <div>
      <h2>{student.name}</h2>
      <p>{student.email}</p>
    </div>
  )
}
```

### Why React Router?

Your app has multiple pages (Dashboard, Students, Books).
React Router lets you navigate between them without a full browser refresh.
The URL changes, but only the content area re-renders.

```jsx
// Define routes once
<Routes>
  <Route path="/"         element={<Dashboard />} />
  <Route path="/students" element={<Students />} />
  <Route path="/books"    element={<Books />} />
</Routes>
```

### Why React Query?

Fetching data manually with useEffect is messy. React Query handles:
- Loading states (`isLoading`)
- Error states (`isError`)
- Caching (don't re-fetch if data is fresh)
- Automatic re-fetch after mutations (add a student → list refreshes)
- Background re-fetching

```jsx
// Without React Query (messy)
const [students, setStudents] = useState([])
const [loading, setLoading] = useState(true)
const [error, setError] = useState(null)
useEffect(() => {
  fetch('/api/v1/students')
    .then(r => r.json())
    .then(setStudents)
    .catch(setError)
    .finally(() => setLoading(false))
}, [])

// With React Query (clean)
const { data: students, isLoading, isError } = useQuery({
  queryKey: ['students'],
  queryFn: getStudents,
})
```

### Why Axios over fetch?

- Set the base URL once — no need to repeat `http://localhost:8080/api/v1` everywhere
- Interceptors — run code on every request/response (unwrap data, catch errors)
- Automatically parses JSON — no need to call `.json()` manually

---

## How Everything Connects

### Full request lifecycle — "User clicks Add Student"

```
1. User fills form in AddStudentModal.jsx and clicks Save

2. React calls createStudent(formData) from src/api/students.js

3. Axios sends:
   POST http://localhost:8080/api/v1/students
   Body: { "name": "John", "email": "john@test.com", ... }

4. CorsConfig on Spring allows the request through (same-origin check passes)

5. StudentController.createStudent() receives the request
   → @Valid triggers Bean Validation on StudentRequestDto
   → If validation fails → GlobalExceptionHandler returns 400 with field errors
   → If valid → calls studentService.createStudent()

6. StudentServiceImpl.createStudent()
   → Checks if email already exists (existsByEmail)
   → If duplicate → throws IllegalArgumentException → 400 response
   → If new → studentMapper.toEntity(dto) converts DTO to Entity
   → studentRepository.save(entity) runs INSERT SQL
   → studentMapper.toResponseDto(saved) converts Entity to DTO
   → Returns StudentResponseDto

7. Controller returns 201 Created + StudentResponseDto as JSON

8. Axios interceptor receives the response, returns response.data directly

9. React Query invalidates the ['students'] cache
   → triggers automatic re-fetch of the students list
   → UI updates automatically with the new student

10. Toast notification shows "Student created successfully"
```

---

## API Reference

### Students — base path `/api/v1/students`

| Method | Endpoint | Description | Body | Returns |
|--------|----------|-------------|------|---------|
| GET | `/students` | List all students | — | `StudentResponseDto[]` |
| GET | `/students/{id}` | Get one student | — | `StudentResponseDto` |
| POST | `/students` | Create student | `StudentRequestDto` | `StudentResponseDto` |
| PUT | `/students/{id}` | Update student | `StudentRequestDto` | `StudentResponseDto` |
| DELETE | `/students/{id}` | Delete student | — | `204 No Content` |
| POST | `/students/{studentId}/books/{bookId}` | Assign book | — | `StudentResponseDto` |
| DELETE | `/students/{studentId}/books/{bookId}` | Remove book | — | `StudentResponseDto` |

### Books — base path `/api/v1/books`

| Method | Endpoint | Description | Body | Returns |
|--------|----------|-------------|------|---------|
| GET | `/books` | List all books | — | `BookResponseDto[]` |
| GET | `/books/{id}` | Get one book | — | `BookResponseDto` |
| POST | `/books` | Create book | `BookRequestDto` | `BookResponseDto` |
| PUT | `/books/{id}` | Update book | `BookRequestDto` | `BookResponseDto` |
| DELETE | `/books/{id}` | Delete book | — | `204 No Content` |
| GET | `/books/unassigned` | List unassigned books | — | `BookResponseDto[]` |

### Validation rules

**StudentRequestDto**
- `name` — required, 2–100 characters
- `email` — required, valid email format
- `phone` — optional, exactly 10 digits
- `department` — required, max 100 characters

**BookRequestDto**
- `title` — required, 2–200 characters
- `author` — required, 2–100 characters
- `genre` — required, max 100 characters
- `isbn` — required, must match `ISBN-XXX` format (e.g. ISBN-001)

### Error response format

```json
{
  "timestamp": "2025-03-22T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "messages": {
    "email": "Email must be valid",
    "name": "Name is required"
  }
}
```

---

## Data Model

```
students                          books
────────────────────────          ──────────────────────────────────
id          BIGINT  PK            id          BIGINT  PK
name        VARCHAR(100)          title       VARCHAR(200)
email       VARCHAR(150) UNIQUE   author      VARCHAR(100)
phone       VARCHAR(20)           genre       VARCHAR(100)
department  VARCHAR(100)          isbn        VARCHAR(50)  UNIQUE
                                  student_id  BIGINT  FK → students.id
                                              (nullable — null = unassigned)
```

**Relationship:** One student can have many books. One book belongs to at most
one student (or none if unassigned). This is a `@ManyToOne` / `@OneToMany`
bidirectional relationship in JPA.

**Cascade rule:** When a student is deleted, their books are NOT deleted.
Instead `student_id` is set to `null` — the books return to the unassigned shelf.

---

## Development Roadmap

### Phase 1 — Setup (Steps 1–5)
- [ ] Create React app with Vite
- [ ] Install Axios, React Router, React Query
- [ ] Set up folder structure
- [ ] Create Axios instance with base URL and interceptor
- [ ] Add CorsConfig to Spring Boot

### Phase 2 — Routing and Layout (Steps 6–8)
- [ ] Set up React Router with all routes
- [ ] Build Layout component (sidebar + main area)
- [ ] Create empty page stubs

### Phase 3 — Students Feature (Steps 9–13)
- [ ] Write all student API functions
- [ ] Students list page with React Query
- [ ] Add Student modal with form validation
- [ ] Edit and Delete student
- [ ] Student detail page

### Phase 4 — Books Feature (Steps 14–17)
- [ ] Books list page with genre filter
- [ ] Add / Edit book form
- [ ] Assign book modal
- [ ] Unassigned shelf page

### Phase 5 — Production Polish (Steps 18–22)
- [ ] Dashboard with live stats
- [ ] Toast notifications
- [ ] Global search
- [ ] Loading skeleton components
- [ ] Empty state components

---

## How to Run Locally

### Prerequisites
- Java 21
- Node.js 18+
- PostgreSQL 15+
- IntelliJ IDEA Ultimate

### 1 — Start PostgreSQL and verify the database exists

```bash
psql -U postgres -c "\l"
# You should see studentlibrary in the list
```

### 2 — Start the Spring Boot backend

Open `student-library` in IntelliJ and click the green Run button.
Or from terminal:

```bash
cd student-library
./mvnw spring-boot:run
```

Backend runs at: `http://localhost:8080`
Verify: `http://localhost:8080/api/v1/students` should return JSON

### 3 — Start the React frontend

```bash
cd student-library-ui
npm install      # first time only
npm run dev
```

Frontend runs at: `http://localhost:5173`

### 4 — Both must be running simultaneously

Spring Boot handles all data. React only draws the UI.
They communicate over HTTP on your local machine.

---

## Key Concepts to Understand as a Beginner

| Concept | What it means simply |
|---|---|
| REST API | A set of URLs your frontend calls to get or change data |
| JSON | The format data travels in between frontend and backend |
| Component | A reusable piece of UI in React (like a LEGO brick) |
| State | Data that can change and causes the UI to re-render |
| Hook | A function that gives a component access to React features |
| Query | Fetching data from the server (read operation) |
| Mutation | Changing data on the server (create, update, delete) |
| Cache | Stored copy of data so you don't re-fetch unnecessarily |
| CORS | Browser security rule — backend must allow frontend's origin |
| DTO | A shape/contract for what data looks like going in or out |
| Migration | A versioned script that changes the database schema |

---

*Built step by step as a learning project at Capestart.*
*Every pattern here — layered architecture, DTOs, React Query, Axios interceptors —
is used in production applications at real software companies.*