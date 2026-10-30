# SPB - Task Manager

Spring Boot 4.1 + Spring MVC + JPA/H2 + static web UI + external API call.

## Run
    ./mvnw spring-boot:run          (Windows: mvnw.cmd spring-boot:run)
Open http://localhost:8080  (needs JDK 21 or newer; first run downloads Maven + dependencies)

    ./mvnw test                      run unit + end-to-end tests

Data is saved in ./data (H2 file DB). Delete that folder to reset. Change the port with PORT=9090.

## Features
- Tasks with title, details, priority (low/medium/high), due date, done flag
- Overdue / due-today highlighting, live statistics (total, open, overdue, due today, % done)
- Search, status tabs, priority filter, sorting (newest, due date, priority)
- Edit dialog, bulk "Clear completed", CSV export (respects current filters)
- Validation with clear error messages, consistent JSON errors
- Quote of the day from dummyjson.com via the backend (timeouts, cache, offline fallback)
- Dark mode, responsive layout

## API
| Method | Path | Notes |
|---|---|---|
| GET | /api/tasks | `?status=open\|done\|all&priority=high&q=text&sort=created\|due\|priority` |
| POST | /api/tasks | `{"title":"..","description":"..","priority":"HIGH","dueDate":"2026-12-01"}` -> 201 |
| GET | /api/tasks/{id} | 404 if missing |
| PUT | /api/tasks/{id} | full update |
| PATCH | /api/tasks/{id}/toggle | flip done |
| DELETE | /api/tasks/{id} | 204 |
| DELETE | /api/tasks/completed | `{"deleted": n}` |
| GET | /api/tasks/stats | counts and completion rate |
| GET | /api/tasks/export.csv | same filters as list |
| GET | /api/quote | `?fresh=true` skips cache |

Errors look like `{"status":400,"error":"Bad Request","message":"Validation failed","fieldErrors":{"title":"Title is required"}}`.

## Layout
    task/    entity, repository, service, controller, TaskQuery (pure filter/sort/stats/CSV logic)
    quote/   external API client with cache + fallback
    web/     global error handler
    config/  first-run sample data (app.seed=false to disable)
    static/  index.html, style.css, app.js
