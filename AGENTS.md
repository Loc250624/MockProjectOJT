# AGENTS.md

## 1. Project overview

This repository is a Spring Boot E-Learning platform with three primary roles:

- `STUDENT`
- `TEACHER`
- `ADMIN`

Main functional areas include authentication and authorization, user profiles,
course management, enrollment and payment, online learning, learning progress,
assessments, certificates, notifications, and administration.

The agent must preserve the existing architecture, naming conventions, UI design
system, security model, and role separation.

---

## 2. Instructions hierarchy

Before changing code:

1. Read this `AGENTS.md`.
2. Read `README.md` and any feature-specific Markdown files referenced by the task.
3. Inspect the current implementation before proposing new classes or endpoints.
4. Check `pom.xml`, application configuration, database migrations, tests, and
   relevant Git history when necessary.
5. Prefer extending existing code over creating a parallel implementation.

If a nested directory contains another `AGENTS.md` or `AGENTS.override.md`, follow
the more specific instructions for files inside that directory.

---

## 3. Repository structure

Inspect the repository and confirm the actual structure before editing. Expected
areas may include:

- `src/main/java/com/ojtsu26/elearning/controller`
- `src/main/java/com/ojtsu26/elearning/service`
- `src/main/java/com/ojtsu26/elearning/service/impl`
- `src/main/java/com/ojtsu26/elearning/repository`
- `src/main/java/com/ojtsu26/elearning/entity`
- `src/main/java/com/ojtsu26/elearning/dto`
- `src/main/java/com/ojtsu26/elearning/config`
- `src/main/java/com/ojtsu26/elearning/security`
- `src/main/java/com/ojtsu26/elearning/exception`
- `src/main/resources/templates`
- `src/main/resources/static`
- `src/main/resources/application.properties`
- `src/test/java`

Do not assume every folder exists. Reuse the actual package and directory layout.

---

## 4. Required working process

For every task:

1. Restate the goal in one or two sentences.
2. Inspect all directly related backend, frontend, security, database, and test files.
3. Produce a short implementation plan before making broad changes.
4. Make the smallest coherent change that completes the requested behavior.
5. Add or update tests.
6. Run the relevant build and test commands.
7. Review the final diff for regressions, security problems, duplicated logic,
   unused code, and accidental formatting changes.
8. Report the files changed, commands run, results, assumptions, and manual test steps.

Do not begin by generating many new files. First determine what already exists.

---

## 5. Build and test commands

Prefer the Maven Wrapper when it exists.

### Windows

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### Git Bash, Linux, or macOS

```bash
./mvnw clean test
./mvnw spring-boot:run
```

If the repository does not contain Maven Wrapper files, use:

```bash
mvn clean test
mvn spring-boot:run
```

Useful targeted test command:

```bash
./mvnw -Dtest=ClassNameTest test
```

Before reporting completion, run at least the relevant tests. For wide or risky
changes, run the full test suite.

Do not claim tests passed unless the command actually completed successfully.

---

## 6. Java and Spring Boot conventions

- Use the Java version and Spring Boot version declared in `pom.xml`.
- Preserve the existing package structure.
- Use constructor injection. Do not introduce field injection.
- Keep controllers thin.
- Put business rules and authorization-sensitive decisions in services.
- Use repositories only for persistence queries.
- Reuse existing DTOs, mappers, enums, exceptions, and response formats.
- Do not return JPA entities directly from public APIs unless this is already an
  intentional project-wide convention.
- Use `@Transactional` at the service layer for multi-step writes.
- Validate request data with Jakarta Validation when the project already uses it.
- Use the project's global exception handling mechanism.
- Do not catch broad exceptions only to hide errors.
- Avoid duplicated business logic across controllers or services.
- Avoid N+1 queries. Use projections, fetch joins, entity graphs, or aggregate
  queries when appropriate.
- Do not add a production dependency unless it is necessary and compatible with
  the current stack.

---

## 7. Security and role rules

Backend authorization is mandatory. Hiding a button in the UI is not sufficient.

- Obtain the current user from the authenticated security context.
- Do not trust `userId`, `studentId`, `teacherId`, role, price, score, completion
  state, or payment state supplied by the client.
- `STUDENT` may only access their own enrollment, progress, attempts, submissions,
  payments, notifications, and certificates.
- `TEACHER` may only manage courses that they own or are assigned to.
- `TEACHER` must not access another teacher's course by changing an ID.
- `ADMIN` access must follow the existing administration policy.
- Protect against IDOR on every endpoint accepting an entity ID.
- Validate entity relationships. For example, confirm that a lesson belongs to the
  requested course and an enrollment belongs to the authenticated student.
- Never expose passwords, password hashes, access tokens, refresh tokens, OAuth
  secrets, private keys, or unnecessary personal information.
- Do not add endpoints that let the frontend mark a payment as successful, set a
  quiz score, force course completion, or issue a certificate directly.
- Sanitize user-provided rich text before rendering.
- Apply upload validation for file type, size, filename, and storage path.
- Do not weaken Spring Security rules merely to make a test or page work.

---

## 8. Role-separated frontend rules

The three portals must remain separate:

- Student pages must not expose Teacher or Admin actions.
- Teacher pages must not expose Admin-only actions.
- Admin pages must not accidentally reuse Student or Teacher permissions.
- Preserve the existing navigation, header, sidebar, typography, spacing, colors,
  and responsive conventions.
- Reuse existing CSS classes and JavaScript modules.
- Do not redesign unrelated pages.
- All changed pages must support desktop, tablet, and mobile layouts.
- Include loading, empty, success, validation, forbidden, and error states when relevant.
- Do not use mock data when the relevant backend API already exists.
- Do not duplicate inline CSS or JavaScript when a shared asset is appropriate.

---

## 9. Database rules

- Inspect current entities, schema, SQL scripts, and migration approach before changes.
- Prefer backward-compatible migrations.
- Do not drop or rename existing tables or columns without explicit task requirements
  and an impact analysis.
- Add foreign keys, unique constraints, and indexes when they enforce real business
  invariants or support frequent queries.
- Use database constraints in addition to service validation for important uniqueness.
- Avoid destructive seed scripts.
- Do not commit real credentials or machine-specific database values.
- Keep secrets in an ignored secret configuration or environment variables.
- When changing an enum or status workflow, review all queries, templates, tests, and
  existing stored values that depend on it.

---

## 10. Business consistency rules

For stateful features:

- Define valid status transitions explicitly.
- Reject invalid transitions instead of silently forcing a state.
- Make retryable operations idempotent.
- Consider duplicate requests and concurrent requests.
- Use server-side data as the source of truth.
- Store timestamps consistently using the project's timezone policy.
- Preserve audit information when the project already supports auditing.

Important examples:

- One active enrollment per student and course.
- Paid enrollment is activated only from a verified backend payment flow.
- Learning progress cannot be changed for another student.
- Quiz answers and correct options must not leak before allowed.
- Quiz scoring occurs on the server.
- A certificate is issued only after server-side eligibility validation.
- Notifications are scoped to their recipient.
- Teacher reports only include students from the teacher's course.

---

## 11. Testing requirements

For every changed feature, cover the relevant cases:

- Successful behavior.
- Validation failure.
- Anonymous access.
- Wrong role.
- Ownership violation or cross-user ID access.
- Missing resource.
- Invalid status transition.
- Duplicate request.
- Concurrent request when the feature can be triggered simultaneously.
- Empty data.
- Boundary values.
- Existing behavior that could regress.

Use the testing style already present in the repository. Prefer focused unit tests
for business rules and integration/controller tests for security and persistence.

Do not delete or weaken an existing test to make new code pass unless the old test
is demonstrably incorrect and the reason is documented.

---

## 12. Git and scope rules

- Do not commit secrets, generated build output, IDE metadata, logs, or uploaded
  runtime files.
- Do not modify `target/`.
- Do not reformat unrelated files.
- Do not rename broad package structures during a feature task.
- Do not alter another feature merely because its code could be improved.
- Keep changes reviewable and limited to the requested feature and direct dependencies.
- Do not run destructive Git commands such as `reset --hard`, history rewriting,
  force push, or mass file deletion unless the user explicitly requests it.
- Do not create commits unless the user asks.
- Before finishing, inspect `git diff` and `git status`.

---

## 13. Definition of done

A task is complete only when:

- The requested behavior is implemented end to end.
- Backend authorization and validation are present.
- Database integrity is preserved.
- Existing architecture and UI conventions are followed.
- Relevant tests have been added or updated.
- The relevant tests or build have been run.
- No unrelated changes are included.
- The final diff has been reviewed.
- Manual verification steps are provided.

---

## 14. Required final report

At the end of every task, report:

1. Summary of the implementation.
2. Files added, modified, or removed.
3. Database or configuration changes.
4. Security and authorization decisions.
5. Tests added or updated.
6. Commands executed and their actual results.
7. Manual test steps.
8. Remaining assumptions, limitations, or blocked checks.

If something could not be tested, state exactly what was not tested and why.
