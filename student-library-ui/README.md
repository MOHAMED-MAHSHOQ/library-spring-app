# Student Library UI

Dark-themed React frontend for the `student-library` Spring Boot backend.

## Features

- JWT login and registration
- Dashboard stats (students, books, assigned, available)
- Students management (list/search/pagination/create/edit/delete)
- Book assignment and removal from students
- Books management (list/search/pagination/create/edit/delete)
- CSV imports for students and books
- Role-aware UI (admin actions hidden for non-admin users)

## Stack

- React + Vite
- React Router
- TanStack React Query
- Axios
- Framer Motion
- Lucide icons

## Backend Contract

This frontend targets:

- Base URL: `http://localhost:8080/api/v1`
- Auth: `/auth/login`, `/auth/register`
- Students: `/students`, `/students/search`, `/students/{id}/books/{bookId}`
- Books: `/books`, `/books/search`, `/books/unassigned`
- Imports: `/import/students`, `/import/books`

## Environment

Copy `.env.example` to `.env` and adjust if needed:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## Run

```powershell
cd C:\Users\mohamed.ms\IdeaProjects\student-library\student-library-ui
npm install
npm run dev
```

## Build

```powershell
cd C:\Users\mohamed.ms\IdeaProjects\student-library\student-library-ui
npm run build
npm run preview
```

## Notes

- Your backend CORS allows `http://localhost:5173`.
- JWT token is stored in local storage (`sl_token`).
- API `401` responses auto-logout the session.

