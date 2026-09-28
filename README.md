# QY Candle - JWT Authentication with Nimbus JOSE + JWT

## Assignment

Demo JWT theo bài giảng Spring Boot 3 + Security 6, thay toàn bộ JJWT 0.12.6 bằng Nimbus JOSE + JWT. Website mang chủ đề QY Candle - tiệm nến thơm tại Quy Nhơn.

## Technologies

- Java 17, Maven, Spring Boot 3.4.3, Spring Security 6
- Spring Web, Data JPA, Validation, Thymeleaf
- MySQL, BCrypt, Bootstrap 5, Fetch API
- Nimbus JOSE + JWT 10.9.1 (không dùng JJWT)

## Architecture

```text
Browser -> Fetch API -> REST Controller -> AuthenticationService
        -> Nimbus JwtService -> JwtAuthenticationFilter -> SecurityContext
        -> UserRepository -> MySQL
```

## JWT Library and Flow

Original lecture: **JJWT 0.12.6**. This project: **Nimbus JOSE + JWT**.

`Signup -> Login -> Nimbus signs HS256 JWT -> Bearer token -> JwtAuthenticationFilter -> Nimbus verifies signature, subject and expiration -> SecurityContext -> protected API`

JWT contains `sub`, `iat`, `exp`, `authorities`, `userId`, and `fullName`. It never contains a password or secret. The implementation directly uses `SignedJWT`, `JWTClaimsSet`, `JWSHeader`, `MACSigner`, and `MACVerifier`.

## Database setup

```sql
CREATE DATABASE jwt_qycandle CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Hibernate creates/updates the `users` table. Email is unique and passwords are BCrypt hashes. No sample credential is inserted automatically.

## Environment

PowerShell example (replace the database password):

```powershell
$env:DB_PASSWORD = "your_mysql_password"
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Never commit `DB_PASSWORD`, `JWT_SECRET`, a `.env` file, `application-local.properties`, or a real token.

For a GitHub-safe reference, see `.env.example`. It contains placeholders only; Spring Boot reads the real values from environment variables.

## Run

```powershell
mvn clean test
mvn spring-boot:run
```

The project compiles for Java 17. A JDK 17 or newer can run Maven.

## URLs

- Home: <http://localhost:8080/>
- Login: <http://localhost:8080/login>
- Register: <http://localhost:8080/register>
- Profile: <http://localhost:8080/user/profile>

The profile HTML shell is public because a normal browser navigation cannot attach a token stored in localStorage. Its actual data comes exclusively from protected `GET /users/me`; an absent or invalid token redirects to login.

## API

| Method | URL | Access |
|---|---|---|
| POST | `/auth/signup` | Public |
| POST | `/auth/login` | Public |
| GET | `/users/me` | Bearer JWT |
| GET | `/users` | Bearer JWT |

### cURL demo

```bash
curl -X POST http://localhost:8080/auth/signup -H "Content-Type: application/json" -d '{"email":"phuc@qycandle.vn","password":"Password123!","fullName":"Nguyen Hoang Phuc"}'
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"phuc@qycandle.vn","password":"Password123!"}'
curl http://localhost:8080/users/me -H "Authorization: Bearer TOKEN"
curl http://localhost:8080/users -H "Authorization: Bearer TOKEN"
```

## Testing

Automated tests use an isolated H2 database in MySQL compatibility mode; production still uses MySQL. Tests cover:

- signup and BCrypt password storage
- login and Nimbus JWT generation
- HS256 header plus `sub`, `iat`, `exp`
- missing token -> 401
- valid token -> 200
- modified signature -> 401
- expired token -> 401
- wrong login -> 401
- password fields absent from API responses

Dependency audit:

```powershell
mvn dependency:tree
mvn dependency:tree | Select-String "io.jsonwebtoken|jjwt"
```

The second command must return no matches.

## GitHub preparation

Before pushing, verify that no credential is tracked:

```powershell
git status --short
git grep -n -I -E "DB_PASSWORD=.+|JWT_SECRET=.+|spring.datasource.password=.+"
```

The repository uses `main` as its default branch. Do not add `.env`, `application-local.properties`, database dumps, JWTs, or Maven `target/` output.

## Security Notes

- BCrypt hashes all passwords; APIs use safe response DTOs.
- HS256 secrets must contain at least 32 decoded bytes.
- JWTs are short-lived and signature, algorithm, subject, and expiry are all checked.
- The application is stateless and creates no authentication session.
- LocalStorage is used only to follow the classroom demo. In production, consider an HttpOnly Secure SameSite cookie because localStorage is exposed to XSS.
- Use HTTPS in production and add refresh-token/revocation policy if the application grows.

## 3-5 minute demo script

1. Open `pom.xml`: show `nimbus-jose-jwt` and the absence of JJWT.
2. Open `JwtService`: show `SignedJWT`, `JWTClaimsSet`, `MACSigner`, and `MACVerifier`.
3. Register `phuc@qycandle.vn`, then show the BCrypt value in MySQL `users`.
4. Login and decode the returned token: header `alg=HS256`, `typ=JWT`; payload `sub`, `iat`, `exp`.
5. Call `/users/me` without a token (401), with the token (200), and after changing one signature character (401).
6. Run the expired-token integration test, then demonstrate browser login, profile rendering, and logout clearing localStorage.
