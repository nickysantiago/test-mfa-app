# test-mfa-app

A Spring Boot demo application exploring two authentication flows side by side: a custom multi-factor authentication (MFA) implementation using Spring Security filters, and Single Sign-On (SSO) via Keycloak using OAuth2 / OIDC. Built as a hands-on learning project for Spring Security, identity federation, and DevSecOps practices.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Authentication Flows](#authentication-flows)
  - [Form Login + MFA](#form-login--mfa)
  - [SSO via Keycloak](#sso-via-keycloak)
  - [Logout](#logout)
- [Key Components](#key-components)
- [Configuration](#configuration)
- [Running Locally](#running-locally)
- [Running with Docker](#running-with-docker)
- [CI/CD Pipeline](#cicd-pipeline)
- [Keycloak Setup](#keycloak-setup)
- [Security Notes](#security-notes)

---

## Overview

This project demonstrates two distinct authentication paths protected by Spring Security:

- **Form Login + MFA** — users authenticate with a username and password, then must provide a second factor (a hardcoded TOTP code for demo purposes) before gaining access.
- **SSO via Keycloak** — users authenticate through a self-hosted Keycloak instance using the OAuth2 Authorization Code flow with OIDC. GitHub is configured as a federated identity provider within Keycloak.

Both flows land on the same protected `/home` page, which identifies how the user authenticated.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 2.7.18 |
| Security | Spring Security 5.8 |
| SSO Protocol | OAuth2 / OpenID Connect (OIDC) |
| Identity Provider | Keycloak 26.x (self-hosted) |
| Federated IdP | GitHub (via Keycloak broker) |
| Templates | Thymeleaf |
| Build Tool | Maven 3.9.x |
| Container | Docker (Amazon Corretto 17 Alpine) |
| CI/CD | Jenkins |
| Artifact Repository | Nexus 3 |

---

## Project Structure

```
test-mfa-app/
├── src/
│   └── main/
│       ├── java/com/example/mfademo/
│       │   ├── MfaDemoApplication.java                # Spring Boot entry point
│       │   ├── SecurityConfig.java                    # Security filter chain, auth providers
│       │   ├── MainController.java                    # Route handlers (/, /login, /home, /mfa)
│       │   ├── MfaService.java                        # MFA interface contract
│       │   ├── SimpleMfaService.java                  # Demo MFA implementation (hardcoded code)
│       │   ├── MfaAuthenticationFilter.java           # Custom filter handling POST /mfa/verify
│       │   ├── MfaAuthenticationProvider.java         # Validates MFA token and code
│       │   ├── MfaAuthenticationToken.java            # Custom authentication token for MFA step
│       │   ├── MfaSuccessHandler.java                 # Redirects to /home after MFA success
│       │   ├── CustomAuthenticationSuccessHandler.java # Post-login: routes to MFA or home
│       │   └── KeycloakLogoutHandler.java             # Handles RP-initiated logout with Keycloak
│       └── resources/
│           ├── application.properties                 # App config, OAuth2/OIDC settings
│           └── templates/
│               ├── login.html                         # Login page (form + SSO button)
│               ├── mfa.html                           # MFA code entry page
│               └── home.html                          # Protected landing page
├── Dockerfile                                         # Container image definition
├── Jenkinsfile                                        # CI/CD pipeline definition
└── pom.xml                                            # Maven dependencies
```

---

## Authentication Flows

### Form Login + MFA

This flow uses a custom Spring Security filter chain to enforce a two-step authentication process.

```
User submits username + password (POST /login)
        ↓
DaoAuthenticationProvider validates credentials
        ↓
CustomAuthenticationSuccessHandler checks MFA status
        ↓
  MFA enabled?
  ├── YES → SecurityContext cleared, username stored in session
  │          → redirect to /mfa
  │              ↓
  │         User submits MFA code (POST /mfa/verify)
  │              ↓
  │         MfaAuthenticationFilter picks up the request
  │              ↓
  │         MfaAuthenticationProvider validates the code
  │              ↓
  │         MfaSuccessHandler → redirect to /home
  │
  └── NO  → redirect to /home directly
```

**Demo credentials:**

| Field | Value |
|---|---|
| Username | `user1` |
| Password | `password` |
| MFA Code | `1234` |

> MFA is enabled only for `user1`. The code is hardcoded in `SimpleMfaService` — this is intentional for demo purposes only.

---

### SSO via Keycloak

This flow uses Spring Security's built-in OAuth2 client support. The app delegates authentication entirely to Keycloak using the Authorization Code flow.

```
User clicks "Login with Keycloak (SSO)"
        ↓
Spring redirects → Keycloak login page
        ↓
User authenticates on Keycloak
(optionally via GitHub as a federated IdP)
        ↓
Keycloak redirects back → /login/oauth2/code/keycloak
        ↓
Spring exchanges authorization code for tokens
        ↓
OidcUser is created from the ID token
        ↓
User redirected → /home
```

The `/home` page distinguishes between the two login methods and displays the appropriate username:

```java
if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
    // SSO user — username pulled from OIDC token
} else {
    // Form login user — username pulled from SecurityContext
}
```

---

### Logout

Logout is handled differently depending on how the user authenticated:

```
User clicks "Log Out" (POST /logout)
        ↓
Spring invalidates local session + clears JSESSIONID cookie
        ↓
KeycloakLogoutHandler.onLogoutSuccess() is called
        ↓
  Is the user an OidcUser (SSO)?
  ├── YES → redirect to Keycloak end_session endpoint
  │          (with id_token_hint + post_logout_redirect_uri)
  │              ↓
  │          Keycloak ends its SSO session
  │              ↓
  │          Keycloak redirects back → /login
  │
  └── NO  → redirect directly to /login
```

This ensures SSO users are fully logged out at the Keycloak level (RP-initiated logout), preventing silent re-authentication on next login.

---

## Key Components

### `SecurityConfig.java`

The central security configuration. Notable design decisions:

- A **local `ProviderManager`** is built with `DaoAuthenticationProvider` and `MfaAuthenticationProvider` and passed only to the MFA filter. This intentionally avoids overriding Spring's global `AuthenticationManager`, which is required for the OAuth2 login filter to function correctly.
- **`oauth2Login()`** is wired separately and relies on Spring's auto-configured manager to handle `OAuth2LoginAuthenticationToken`.
- **`KeycloakLogoutHandler`** replaces the simple `logoutSuccessUrl` to support RP-initiated logout for SSO users while still redirecting form login users normally.

### `MfaAuthenticationFilter.java`

Extends `AbstractAuthenticationProcessingFilter` and intercepts `POST /mfa/verify`. It reads the pre-auth username from the session (stored by `CustomAuthenticationSuccessHandler`) and the submitted code from the request, then delegates to `MfaAuthenticationProvider`.

### `CustomAuthenticationSuccessHandler.java`

Fires after successful username/password authentication. If MFA is required for the user, it clears the `SecurityContext` (preventing premature full authentication) and stores the username in session before redirecting to `/mfa`. If MFA is not required, it redirects to the originally requested URL or `/home`.

### `KeycloakLogoutHandler.java`

Implements `LogoutSuccessHandler`. Checks whether the principal is an `OidcUser` — if so, constructs Keycloak's `end_session` URL with `id_token_hint` and `post_logout_redirect_uri` and redirects there. Falls back to a direct redirect to `/login` for form login users. Both URLs are injected from `application.properties` via `@Value` to avoid hardcoded environment-specific values.

---

## Configuration

All environment-specific values live in `application.properties`:

```properties
server.port=8080
spring.main.banner-mode=off

# Required when running behind a reverse proxy (Nginx, Cloudflare, etc.)
# Allows {baseUrl} placeholder to resolve to the public HTTPS URL
server.forward-headers-strategy=native

# Application URLs (externalized to avoid hardcoding in Java)
app.keycloak.logout-url=https://auth.your-domain.com/realms/your-realm/protocol/openid-connect/logout
app.base-url=https://your-app-domain.com

# Keycloak OAuth2 / OIDC registration
spring.security.oauth2.client.registration.keycloak.client-id=your-client-id
spring.security.oauth2.client.registration.keycloak.client-secret=your-client-secret
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/keycloak

# Keycloak provider — Spring auto-discovers all endpoints via the OIDC discovery document
spring.security.oauth2.client.provider.keycloak.issuer-uri=https://auth.your-domain.com/realms/your-realm
```

> **Important:** Never commit real client secrets to source control. Use environment variables or a secrets manager in production.

---

## Running Locally

**Prerequisites:** Java 17, Maven 3.9+, a running Keycloak instance

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

The app will be available at `http://localhost:8080`.

For local development without Keycloak, the form login + MFA flow works independently. The SSO button will fail unless `issuer-uri` points to a reachable Keycloak instance.

---

## Running with Docker

The app is packaged as a Docker image based on Amazon Corretto 17 Alpine.

```bash
# Build the JAR first
mvn clean package

# Build the Docker image
docker build -t mfa-demo .

# Run the container
docker run -p 8080:8080 mfa-demo
```

**Dockerfile:**

```dockerfile
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY mfa-demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## CI/CD Pipeline

The `Jenkinsfile` defines a four-stage pipeline:

```
Build → Upload to Nexus → Test → Deploy
```

| Stage | Description |
|---|---|
| **Build** | Checks out source and runs `mvn clean package` |
| **Upload Artifacts to Nexus** | Pushes the JAR and POM to a Nexus 3 repository |
| **Test** | Runs `mvn test` |
| **Deploy** | SCPs the JAR and Dockerfile to the target host via SSH, then executes `deploy.sh` remotely |

`TARGET_HOST` and `TARGET_USER` are injected as Jenkins environment variables, keeping host details out of the repository.

---

## Keycloak Setup

Keycloak is run as a Docker container behind a reverse proxy handling TLS termination.

```bash
docker run -d \
  -v keycloak-data:/opt/keycloak/data \
  -p 8777:8080 \
  --name keycloak \
  -e KC_HOSTNAME=auth.your-domain.com \
  -e KC_HTTP_ENABLED=true \
  -e KC_PROXY_HEADERS=xforwarded \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=your-password \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

`KC_PROXY_HEADERS=xforwarded` is required when running behind a reverse proxy — it tells Keycloak to trust `X-Forwarded-Proto` headers so it correctly identifies the public scheme as HTTPS and sets session cookies with the `Secure` flag.

**Required Keycloak configuration:**

1. Create a realm (e.g. `test-mfa-app`)
2. Set **Realm Settings → General → Frontend URL** to your public Keycloak URL
3. Create a confidential OIDC client with:
   - Valid Redirect URI: `https://your-app-domain.com/login/oauth2/code/keycloak`
   - Valid Post Logout Redirect URI: `https://your-app-domain.com/login`
   - Web Origins: `https://your-app-domain.com`
4. Copy the client secret from the **Credentials** tab into `application.properties`
5. *(Optional)* Add GitHub as a federated Identity Provider under **Identity Providers → GitHub**, then update the GitHub OAuth App callback URL to:
   ```
   https://auth.your-domain.com/realms/your-realm/broker/github/endpoint
   ```

---

## Security Notes

This is a **demo application** and intentionally uses several configurations that are not suitable for production:

| What | Why it's demo-only | Production alternative |
|---|---|---|
| `NoOpPasswordEncoder` | Stores passwords in plaintext | `BCryptPasswordEncoder` |
| Hardcoded MFA code (`1234`) | No real TOTP validation | TOTP library (e.g. `java-otp`) integrated with an authenticator app |
| In-memory user store | Users lost on restart | Database-backed `UserDetailsService` |
| Hardcoded `user1` in `SimpleMfaService` | Not scalable | Per-user MFA enrollment stored in a database |
| `start-dev` mode in Keycloak | Not production-safe | `start` mode with PostgreSQL backend and HA configuration |
| Client secret in `application.properties` | Risk of accidental commit | Environment variable, AWS Secrets Manager, or Vault |
