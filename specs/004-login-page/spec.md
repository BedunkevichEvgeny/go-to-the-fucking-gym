# Feature Specification: Login Page

**Feature Branch**: `004-login-page`  
**Created**: 2026-05-04  
**Status**: Draft  
**Input**: User description: "Login page with name/password authentication, redirect on success, error message on failure, protected main page"

**Language Rule**: This specification MUST be written in English.

## Clarifications

### Session 2026-05-04

- Q: What session/auth mechanism should be used? → A: Server-side HttpSession with JSESSIONID cookie
- Q: What authentication implementation should be used? → A: Spring Security with SecurityFilterChain + UserDetailsService + NoOpPasswordEncoder (plain text — MVP simplification)
- Q: What login submission strategy should be used? → A: React sends POST /api/auth/login JSON via fetch/axios; Spring Security processes via custom AuthenticationFilter
- Q: What should happen after successful login (post-login redirect)? → A: Always redirect to the main page `/` regardless of where the user was trying to go
- Q: How should frontend auth state be detected? → A: GET /api/auth/me on app mount; 200 = authenticated, 401 = redirect to login
- Q: Password storage approach for MVP? → A: Store passwords in plain text (no BCrypt hashing); use NoOpPasswordEncoder in Spring Security. **Intentional MVP simplification — NOT for production use.**
- Q: How should logout be implemented? → A: `POST /api/auth/logout` via Spring Security `.logout()` DSL; server invalidates `HttpSession`; frontend redirects to `/login` on success.
- Q: `SecurityFilterChain` permit-list? → A: Only `POST /api/auth/login` and `POST /api/auth/logout` are public. `GET /api/auth/me` is **protected** — returns HTTP 401 when unauthenticated (intended auth-check signal). All other `/api/**` routes require authentication.

*Clarification session complete — 5/5 questions answered.*

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Successful Login (Priority: P1)

A registered user visits the site and is presented with a login page. They enter their username and password, submit the form, and are redirected to the main page of the application.

**Why this priority**: This is the core flow — without it, no user can access the application. It is the foundation all other stories depend on.

**Independent Test**: Can be fully tested by navigating to the site, entering valid credentials, submitting the form, and verifying redirect to the main page.

**Acceptance Scenarios**:

1. **Given** the user is not logged in and navigates to the site, **When** they are presented with the login page, **Then** they see a form with a username field, a password field, and a submit button.
2. **Given** the user enters valid username and password, **When** they submit the form, **Then** the system authenticates them and redirects them to the main page.
3. **Given** the user is already logged in, **When** they navigate to the login page, **Then** they are redirected to the main page directly.

---

### User Story 2 - Failed Login with Error Feedback (Priority: P2)

A user enters incorrect credentials (wrong username or password) and submits the form. The system rejects the login attempt and shows a clear error message on the login page without revealing which field was incorrect.

**Why this priority**: Proper error handling is essential for usability and security — users must know when login fails so they can correct their input.

**Independent Test**: Can be tested by entering invalid credentials, submitting the form, and verifying an error message appears without a redirect.

**Acceptance Scenarios**:

1. **Given** the user enters a non-existent username, **When** they submit the form, **Then** an error message is displayed and the user remains on the login page.
2. **Given** the user enters a valid username but wrong password, **When** they submit the form, **Then** an error message is displayed and the user remains on the login page.
3. **Given** the user submits the form with empty fields, **When** the form is submitted, **Then** validation errors are shown prompting the user to fill in the required fields.

---

### User Story 3 - Protected Main Page + Logout (Priority: P3)

An unauthenticated user attempts to access the main page directly (e.g., by typing the URL). The system detects that the user is not logged in and redirects them to the login page. An authenticated user can log out from the main page, which ends their session and returns them to the login page.

**Why this priority**: Protects application content from unauthorized access. Without this, login is cosmetic and bypassed entirely.

**Independent Test**: Can be tested by clearing session state and navigating directly to the main page URL, then verifying a redirect to the login page occurs. Separately, log in, click the logout button, and verify redirect to `/login` and that subsequent navigation to `/` redirects again.

**Acceptance Scenarios**:

1. **Given** the user is not logged in, **When** they navigate directly to the main page URL, **Then** they are redirected to the login page.
2. **Given** the user completes a successful login, **When** they are redirected to the main page, **Then** the main page loads and is fully accessible.
3. **Given** the user is logged in and on the main page, **When** they click the logout button, **Then** the system invalidates their session and redirects them to the login page.
4. **Given** the user has logged out, **When** they navigate directly to the main page URL, **Then** they are redirected to the login page again.

---

### Edge Cases & Simplicity Checks

- What happens when a user submits the form with only whitespace in the username or password fields? → The system treats these as empty and shows a validation error.
- What happens when multiple failed login attempts occur? → Each attempt returns the same generic error message (brute-force protection is out of scope for this spec).
- What happens if the session expires while the user is on the main page? → The next action that requires authentication redirects the user to the login page.
- How does the system handle the password field to avoid exposure? → The password input masks characters by default.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST present a login form containing a username field, a password field, and a submit button when an unauthenticated user accesses the site.
- **FR-002**: System MUST authenticate users using their username and password.
- **FR-003**: System MUST redirect successfully authenticated users to the main page immediately after login.
- **FR-004**: System MUST display a clear error message on the login page when authentication fails, without specifying which credential was incorrect.
- **FR-005**: System MUST prevent unauthenticated users from accessing the main page and redirect them to the login page.
- **FR-006**: System MUST validate that username and password fields are not empty before submitting credentials to the authentication service.
- **FR-007**: System MUST mask password characters in the password input field.
- **FR-008**: System MUST provide a logout mechanism via `POST /api/auth/logout`; on success the server invalidates the `HttpSession` and the frontend redirects the user to `/login`.
- **FR-009**: The main page MUST display a logout button that triggers `POST /api/auth/logout`.

### Technology Constraints *(mandatory)*

- **TC-001**: Backend services MUST use Java 21.
- **TC-002**: Backend services MUST use Spring Boot conventions and best practices.
- **TC-003**: Web frontend MUST use React.
- **TC-004**: Design MUST prefer simple, explicit logic over clever abstractions.
- **TC-005**: Authentication MUST be implemented using Spring Security with `SecurityFilterChain`, `UserDetailsService`, and `NoOpPasswordEncoder` (plain text comparison). **MVP simplification — not for production.**
- **TC-006**: Session management MUST use server-side `HttpSession` with a `JSESSIONID` cookie (no JWT or token-based auth).
- **TC-007**: The frontend MUST submit login credentials as JSON via `POST /api/auth/login` using `fetch` or `axios`; Spring Security processes the request via a custom `AuthenticationFilter`.
- **TC-008**: Frontend auth state detection MUST use `GET /api/auth/me` on application mount: HTTP 200 indicates an authenticated session; HTTP 401 triggers a redirect to the login page. `GET /api/auth/me` is a **protected** endpoint — the 401 response when unauthenticated is the intended signal.
- **TC-009**: Post-login redirect MUST always navigate to `/` (main page), regardless of the originally requested URL.
- **TC-010**: Logout MUST be implemented using Spring Security's `.logout()` DSL configured in `SecurityFilterChain`; the logout endpoint is `POST /api/auth/logout`; on success the server invalidates the `HttpSession` and returns HTTP 200; the frontend then redirects to `/login`.
- **TC-011**: `SecurityFilterChain` MUST permit only `POST /api/auth/login` and `POST /api/auth/logout` anonymously. All other `/api/**` routes — including `GET /api/auth/me` — MUST require authentication (`.anyRequest().authenticated()`).

### Test Coverage Requirements *(mandatory)*

| Business Rule | Unit Test Reference | Integration Test Reference |
|---|---|---|
| Successful login redirects to main page | LoginPage success redirect unit test | POST /auth/login returns session and redirect |
| Failed login shows error message | LoginPage error display unit test | POST /auth/login with bad credentials returns 401 |
| Empty fields show validation error | LoginPage form validation unit test | Client-side validation (no server call) |
| Main page rejects unauthenticated access | ProtectedRoute unit test | GET /main without session returns 401/redirect |
| Logout invalidates session and redirects to /login | Logout button unit test (calls POST /api/auth/logout) | POST /api/auth/logout invalidates session; subsequent GET /api/auth/me returns 401 |

### Key Entities

- **User**: Represents an authenticated account with a username and plain text password. **Passwords are stored as plain text for MVP simplicity — not for production.** The system uses existing user records — no registration flow is in scope.
- **Session**: A server-side `HttpSession` record indicating the user is authenticated, identified by a `JSESSIONID` cookie. Created on successful login via Spring Security; invalidated on `POST /api/auth/logout`; absent or expired for unauthenticated users.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: *(Performance target — out of scope for MVP; no measurable threshold set.)*
- **SC-002**: 100% of unauthenticated access attempts to the main page are redirected to the login page.
- **SC-003**: Failed login attempts always produce a visible error message on the login page with no redirect.
- **SC-004**: Successful login always produces a redirect to the main page with no error message visible.

### UX Consistency Outcomes *(mandatory when UI changes exist)*

- **UX-001**: The login page uses the same visual design language (colours, typography, spacing) as the rest of the application.
- **UX-002**: Error messages follow the same pattern and styling used for other error states in the application.

### Additional Non-Functional Outcomes

- **NF-001**: *(Performance target — out of scope for MVP; covered by SC-001.)*
- **NF-002**: Password values are never exposed in plain text in the UI, browser history, or network logs.

## Assumptions

- The application already has a user store with existing accounts (username + plain text password for MVP); this feature does not include user registration.
- The "main page" refers to the current primary landing page of the application at `/` that users access after login — its URL and content are not changed by this feature.
- Session management uses server-side `HttpSession` with `JSESSIONID` cookie as established by Spring Security; no JWT or stateless token approach is used.
- Username is a plain text identifier (not necessarily an email address), consistent with the current authentication approach described in the requirements.
- Mobile responsiveness of the login page is assumed to follow the existing site-wide responsive behaviour; no special mobile-only design is required.
- Account lockout after repeated failures is out of scope for this feature.
- Password reset / "forgot password" functionality is out of scope for this feature.
- The frontend does not persist the originally-requested URL; post-login redirect always goes to `/`.




