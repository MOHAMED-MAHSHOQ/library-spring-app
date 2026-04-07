# Authentication & Login Flow - Complete Mastery Guide

This document explains how username and password authentication works in the Student Library application, from login endpoint to JWT token generation and validation.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Login Endpoint](#login-endpoint)
3. [Authentication Manager](#authentication-manager)
4. [Password Encoding & Verification](#password-encoding--verification)
5. [JWT Token Generation](#jwt-token-generation)
6. [JWT Token Validation](#jwt-token-validation)
7. [Security Filter Chain](#security-filter-chain)
8. [Complete Flow Diagram](#complete-flow-diagram)
9. [Key Components Deep Dive](#key-components-deep-dive)

---

## Architecture Overview

The authentication system uses:
- **Spring Security** for authentication framework
- **JWT (JSON Web Tokens)** for stateless authorization
- **BCrypt** for password hashing
- **JWT Library (io.jsonwebtoken)** for token creation and parsing

**Key Classes Involved:**
- `AuthController` - REST endpoint for login/register
- `AuthService` - Business logic for authentication
- `SecurityConfig` - Spring Security configuration
- `JwtAuthFilter` - Filter to validate JWT tokens on each request
- `JwtUtil` - Utility class for JWT operations
- `AppUser` - User entity implementing UserDetails
- `AppUserDetailsService` - Custom UserDetailsService implementation

---

## Login Endpoint

### **File:** `AuthController.java`

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(request));
    }
}
```

### How It Works:

1. **Client sends POST request** to `/api/v1/auth/login` with JSON body:
   ```json
   {
       "email": "user@example.com",
       "password": "password123"
   }
   ```

2. **@Valid annotation** validates the `LoginRequest` using javax.validation constraints

3. **AuthService.login()** is called with the request object

---

## LoginRequest DTO

### **File:** `LoginRequest.java`

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

### Validation Details:
- `@NotBlank` - ensures field is not null or empty
- `@Email` - validates proper email format
- If validation fails, Spring returns 400 Bad Request with validation errors

---

## AuthService - Core Authentication Logic

### **File:** `AuthService.java`

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthResponse login(LoginRequest request) {
        // STEP 1: Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        // STEP 2: Fetch user from database
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // STEP 3: Generate JWT token
        String token = jwtUtil.generateToken(user);
        
        // STEP 4: Build and return response
        return buildResponse(user, token);
    }

    private AuthResponse buildResponse(AppUser user, String token) {
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .expiresIn(expirationMs)
                .build();
    }
}
```

### Login Process Flow:

**STEP 1: Authentication Manager**
```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
)
```
- Creates an `UsernamePasswordAuthenticationToken` with email and password
- Passes it to `AuthenticationManager` for verification
- If credentials are invalid, throws `BadCredentialsException`

**STEP 2: User Lookup**
```java
AppUser user = appUserRepository.findByEmail(request.getEmail())
    .orElseThrow(...)
```
- Fetches user from database using email
- The email acts as the username in this system

**STEP 3: Token Generation**
```java
String token = jwtUtil.generateToken(user)
```
- Generates JWT token (explained in detail below)

**STEP 4: Response**
```java
return buildResponse(user, token)
```
- Returns `AuthResponse` containing token, user info, and expiration time

---

## Authentication Manager

### **File:** `SecurityConfig.java`

```java
@Bean
public AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return new ProviderManager(provider);
}
```

### What It Does:

1. **Creates DaoAuthenticationProvider** - loads user details from database
2. **Sets PasswordEncoder** - uses BCrypt to verify passwords
3. **Returns ProviderManager** - orchestrates authentication

### Authentication Flow Inside AuthenticationManager:

When `authenticate(UsernamePasswordAuthenticationToken)` is called:

1. **Load User Details:**
   ```
   DaoAuthenticationProvider calls AppUserDetailsService
       → AppUserDetailsService.loadUserByUsername(email)
       → AppUserRepository.findByEmail(email)
       → Returns AppUser (implements UserDetails)
   ```

2. **Verify Password:**
   ```
   PasswordEncoder (BCrypt).matches(rawPassword, hashedPassword)
   - rawPassword: "password123" (from login request)
   - hashedPassword: encrypted password from database
   - Returns: true/false
   ```

3. **Return Result:**
   - If password matches → return authenticated token ✓
   - If password doesn't match → throw `BadCredentialsException` ✗

---

## Password Encoding & Verification

### **File:** `SecurityConfig.java`

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### BCrypt Process:

#### During Registration (Encoding):
```java
// From AuthService.register()
AppUser user = AppUser.builder()
    .password(passwordEncoder.encode(request.getPassword()))
    // ...
    .build();
```

**What happens:**
1. Raw password: `"myPassword123"`
2. BCryptPasswordEncoder generates random salt
3. Password + salt → hashed using bcrypt algorithm
4. Stored in database: `$2a$10$N9qo8uLOickgx2ZMRZoMye...` (encrypted)

#### During Login (Verification):
```java
// Inside DaoAuthenticationProvider
boolean matches = passwordEncoder.matches(
    rawPassword,          // "myPassword123" from login
    hashedPassword        // "$2a$10$N9qo8uLOickgx2ZMRZoMye..." from DB
);
```

**Why BCrypt is secure:**
- **One-way hashing** - cannot reverse to get original password
- **Salt included** - prevents rainbow table attacks
- **Adaptive** - can increase complexity as computers get faster

---

## JWT Token Generation

### **File:** `JwtUtil.java`

```java
@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    JwtUtil(@Value("${app.jwt.secret}") String secret, 
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities()
            .stream()
            .map(a -> a.getAuthority())
            .toList());
        return buildToken(claims, userDetails.getUsername());
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .setClaims(claims)                          // Add custom claims
            .setSubject(subject)                        // Set email as subject
            .setIssuedAt(new Date())                    // Current time
            .setExpiration(new Date(
                System.currentTimeMillis() + expirationMs))  // Expiration time
            .signWith(key, SignatureAlgorithm.HS256)   // Sign with secret key
            .compact();                                  // Convert to string
    }
}
```

### JWT Token Structure:

A JWT has 3 parts separated by dots: `header.payload.signature`

**Example Token:**
```
eyJhbGciOiJIUzI1NiJ9.
eyJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXSwic3ViIjoiZW1haWxAZXhhbXBsZS5jb20iLCJpYXQiOjE3MTIzNDU2NzAsImV4cCI6MTcxMjQzMjA3MH0.
abcd1234...
```

#### Part 1: Header
```json
{
  "alg": "HS256",    // Algorithm (HMAC-SHA256)
  "typ": "JWT"       // Type
}
```

#### Part 2: Payload (Claims)
```json
{
  "authorities": ["ROLE_USER"],           // User roles/permissions
  "sub": "user@example.com",              // Subject (username/email)
  "iat": 1712345670,                      // Issued At (current time)
  "exp": 1712432070                       // Expiration (24 hours later)
}
```

#### Part 3: Signature
```
HMAC256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret_key
)
```

### Configuration from `application.properties`:
```properties
app.jwt.secret=change-me-in-production-must-be-at-least-64-characters-long-xxx
app.jwt.expiration-ms=86400000  # 24 hours in milliseconds
```

### Response to Client:

```java
private AuthResponse buildResponse(AppUser user, String token) {
    return AuthResponse.builder()
        .token(token)                    // JWT token
        .email(user.getEmail())          // User email
        .fullName(user.getFullName())    // User full name
        .role(user.getRole().name())     // User role
        .expiresIn(expirationMs)         // Token expiration time
        .build();
}
```

**JSON Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXX0...",
  "email": "user@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "expiresIn": 86400000
}
```

---

## JWT Token Validation

### **File:** `JwtUtil.java`

```java
public boolean isTokenValid(String token, UserDetails userDetails) {
    final String userName = extractUsername(token);
    return userName.equals(userDetails.getUsername()) && !isExpired(token);
}

private boolean isExpired(String token) {
    return parseClaims(token).getExpiration().before(new Date());
}

public String extractUsername(String token) {
    return parseClaims(token).getSubject();
}

private Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)              // Use same secret key
        .build()
        .parseClaimsJws(token)           // Parse and verify signature
        .getBody();
}
```

### Validation Checks:

1. **Signature Verification**
   - Uses secret key to verify token wasn't tampered
   - If altered, throws `SignatureException`

2. **Expiration Check**
   - Compares token's expiration time with current time
   - If expired, returns false

3. **Username Match**
   - Extracts username from token
   - Compares with current user
   - Ensures token belongs to the right user

---

## Security Filter Chain

### **File:** `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()           // ✓ No auth needed
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // ✓ CORS preflight
                .anyRequest().authenticated()                             // ✗ All others need auth
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### Configuration Breakdown:

| Setting | Purpose |
|---------|---------|
| `.cors()` | Enable CORS (Cross-Origin Resource Sharing) |
| `.csrf(csrf -> csrf.disable())` | Disable CSRF (not needed for stateless JWT) |
| `SessionCreationPolicy.STATELESS` | No server-side sessions (JWT is stateless) |
| `/api/v1/auth/**` permitAll | Allow login/register without authentication |
| `OPTIONS` permitAll | Allow CORS preflight requests |
| `.anyRequest().authenticated()` | All other endpoints require valid JWT |
| `addFilterBefore(jwtAuthFilter)` | Add JWT filter before standard auth filter |

---

## JWT Auth Filter - Request Processing

### **File:** `JwtAuthFilter.java`

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain chain) throws ServletException, IOException {
        
        // STEP 1: Extract token from header
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // STEP 2: Get token without "Bearer " prefix
        final String token = authHeader.substring(7);
        final String userName;
        
        // STEP 3: Extract username from token
        try {
            userName = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            chain.doFilter(request, response);
            return;
        }

        // STEP 4: If username exists and not already authenticated
        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // STEP 5: Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
            
            // STEP 6: Validate token
            if(jwtUtil.isTokenValid(token, userDetails)){
                
                // STEP 7: Create authentication token
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                // STEP 8: Add request details
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // STEP 9: Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // STEP 10: Continue filter chain
        chain.doFilter(request, response);
    }
}
```

### Request Processing Flow:

```
Client Request with JWT
    ↓
[STEP 1] Extract "Authorization" header
    ↓
[STEP 2] Check if starts with "Bearer "
    ↓ (if not, continue chain without auth)
    ↓
[STEP 3] Extract token (remove "Bearer " prefix)
    ↓
[STEP 4] Extract username from token payload
    ↓ (if error, continue chain without auth)
    ↓
[STEP 5] Load UserDetails from database
    ↓
[STEP 6] Validate token (signature, expiration, username)
    ↓ (if invalid, continue chain without auth)
    ↓
[STEP 7-8] Create authenticated token with authorities
    ↓
[STEP 9] Set in SecurityContextHolder
    ↓
[STEP 10] Continue filter chain (request is now authenticated)
    ↓
Request reaches Controller (with authentication context)
```

### Example Request:

```
GET /api/v1/students HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXX0...
```

### AppUserDetailsService - User Loading

**File:** `AppUserDetailsService.java`

```java
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
    }
}
```

- Implements Spring's `UserDetailsService` interface
- `loadUserByUsername()` is called with email (used as username)
- Returns `AppUser` which implements `UserDetails`
- Throws `UsernameNotFoundException` if user doesn't exist

---

## AppUser Entity - UserDetails Implementation

### **File:** `AppUser.java`

```java
@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean enabled;

    @PrePersist
    void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.enabled = true;
    }

    // UserDetails Implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;  // Email is the username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public enum Role {
        USER, ADMIN
    }
}
```

### UserDetails Contract:

| Method | Purpose |
|--------|---------|
| `getAuthorities()` | Returns user roles (e.g., `ROLE_USER`, `ROLE_ADMIN`) |
| `getPassword()` | Returns encrypted password from database |
| `getUsername()` | Returns email (acts as username) |
| `isAccountNonExpired()` | Is account still valid? (always true here) |
| `isAccountNonLocked()` | Is account locked? (always false here) |
| `isCredentialsNonExpired()` | Are credentials still valid? (always true here) |
| `isEnabled()` | Is account enabled? (checked against `enabled` field) |

---

## Database Schema

```sql
CREATE TABLE app_users (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- Encrypted with BCrypt
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,       -- USER or ADMIN
    created_at TIMESTAMP NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true
);
```

---

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AUTHENTICATION FLOW                              │
└─────────────────────────────────────────────────────────────────────────┘

                          ╔════════════════════════╗
                          ║    CLIENT REQUEST      ║
                          ║  POST /api/v1/auth/    ║
                          ║  login                 ║
                          ║ {email, password}      ║
                          ╚════════════════════════╝
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  AuthController      │
                          │  .login()            │
                          └──────────────────────┘
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  AuthService         │
                          │  .login(request)     │
                          └──────────────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
          ┌──────────────────┐ ┌──────────────┐ ┌──────────────┐
          │ Authenticate     │ │ Fetch User   │ │ Generate JWT │
          │ Credentials      │ │ from DB      │ │              │
          └──────────────────┘ └──────────────┘ └──────────────┘
                    │                │                │
              [1]   │          [2]   │          [3]   │
          .authenticate()    .findByEmail()   .generateToken()
                    │                │                │
                    ▼                ▼                ▼
          ┌──────────────────┐ ┌──────────────┐ ┌──────────────┐
          │ AuthenticationMgr │ │ Repository   │ │ JwtUtil      │
          │                   │ │ Database     │ │ Sign JWT     │
          │ DaoAuthProvider   │ │              │ │              │
          └──────────────────┘ └──────────────┘ └──────────────┘
                    │                │                │
          [1.1]     ▼                │          [3.1] │
        .loadUserByUsername()        │        Create Claims
                    │                │          Set Expiry
                    ▼                │          Sign with Key
          ┌──────────────────┐       │                │
          │ AppUserDetailsSvc│       │                │
          └──────────────────┘       │                │
                    │                │                │
          [1.2]     ▼                ▼                ▼
        .findByEmail()      ┌──────────────┐ ┌──────────────┐
                    │       │ AppUser      │ │ JWT Token    │
                    │       │ (UserDetails)│ │ (String)     │
                    │       └──────────────┘ └──────────────┘
                    │                │                │
          [1.3]     ▼                │                │
        BCrypt Password       [2.1] Return         [3.2]
        Verification              User              Return
                    │                │          JWT to Client
                    │                └────────────┬─────────────┘
                    │                             │
                    └─────────────────────────────┘
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  AuthResponse        │
                          │  {token, email,      │
                          │   fullName, role,    │
                          │   expiresIn}         │
                          └──────────────────────┘
                                     │
                                     ▼
                          ╔════════════════════════╗
                          ║   RESPONSE TO CLIENT   ║
                          ║   HTTP 200 OK          ║
                          ║   {JWT Token}          ║
                          ╚════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────┐
│                    SUBSEQUENT REQUEST WITH JWT                           │
└─────────────────────────────────────────────────────────────────────────┘

                ╔════════════════════════════════════╗
                ║  CLIENT REQUEST WITH JWT TOKEN     ║
                ║  GET /api/v1/students              ║
                ║  Authorization: Bearer <JWT>       ║
                ╚════════════════════════════════════╝
                             │
                             ▼
                ┌──────────────────────────────┐
                │  SecurityFilterChain         │
                │  Applies configured rules    │
                └──────────────────────────────┘
                             │
                             ▼
                ┌──────────────────────────────┐
                │  JwtAuthFilter               │
                │  .doFilterInternal()         │
                └──────────────────────────────┘
                             │
                ┌────────────┼────────────┐
                ▼            ▼            ▼
        [1] Extract    [2] Check    [3] Extract
        Bearer Token    "Bearer"    Username from JWT
                │            │            │
                └────────────┼────────────┘
                             ▼
                ┌──────────────────────────────┐
                │  AppUserDetailsService       │
                │  .loadUserByUsername()       │
                └──────────────────────────────┘
                             │
                             ▼
                ┌──────────────────────────────┐
                │  Load UserDetails from DB    │
                └──────────────────────────────┘
                             │
                             ▼
                ┌──────────────────────────────┐
                │  JwtUtil.isTokenValid()      │
                │  - Check Signature          │
                │  - Check Expiration         │
                │  - Match Username           │
                └──────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                ✓ VALID          ✗ INVALID
                    │                 │
                    ▼                 ▼
        Set Auth in    Continue without
        SecurityContext   Authentication
                    │                 │
                    └────────┬────────┘
                             ▼
                    Pass control to
                    Controller
                             │
                    ┌────────┴────────┐
                    │                 │
                ✓ Authenticated  ✗ Unauthenticated
                    │                 │
                    ▼                 ▼
          Controller executes   401 Unauthorized
          & Returns 200 OK         Response
```

---

## Complete Authentication Example

### Step 1: Client Login Request

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "securePassword123"
  }'
```

### Step 2: Server-Side Processing

```
1. AuthController receives request
2. AuthService.login() called with:
   {
     "email": "john@example.com",
     "password": "securePassword123"
   }

3. authenticationManager.authenticate(token) executes:
   a. AppUserDetailsService.loadUserByUsername("john@example.com")
      ↓
      AppUserRepository.findByEmail("john@example.com")
      ↓
      Returns AppUser from database:
      {
        id: 1,
        email: "john@example.com",
        password: "$2a$10$N9qo8uLOi...", (encrypted)
        fullName: "John Doe",
        role: USER,
        enabled: true
      }

   b. BCryptPasswordEncoder.matches(
        "securePassword123",                           (provided)
        "$2a$10$N9qo8uLOi..."             (from DB)
      )
      ↓
      Result: ✓ TRUE (password matches)

4. User found ✓

5. jwtUtil.generateToken(user) creates JWT:
   Header: {
     "alg": "HS256",
     "typ": "JWT"
   }
   
   Payload: {
     "authorities": ["ROLE_USER"],
     "sub": "john@example.com",
     "iat": 1712345670,
     "exp": 1712432070
   }
   
   Signature: HMAC256(header.payload, secretKey)
   ↓
   Token: "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXX0..."

6. buildResponse() returns:
   {
     "token": "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXX0...",
     "email": "john@example.com",
     "fullName": "John Doe",
     "role": "USER",
     "expiresIn": 86400000
   }
```

### Step 3: Server Response

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXX0...",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "expiresIn": 86400000
}
```

### Step 4: Client Stores Token

Client (Frontend) stores the JWT token in:
- LocalStorage
- SessionStorage
- Memory
- Cookie

### Step 5: Client Makes Authenticated Request

```bash
curl -X GET http://localhost:8080/api/v1/students \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXX0..."
```

### Step 6: Server Validates Token

```
1. JwtAuthFilter intercepts request
2. Extracts token from "Authorization: Bearer <TOKEN>"
3. jwtUtil.extractUsername(token) → "john@example.com"
4. AppUserDetailsService loads UserDetails
5. jwtUtil.isTokenValid(token, userDetails):
   a. Verifies signature using secret key ✓
   b. Checks expiration (not expired) ✓
   c. Confirms username matches ✓
6. Creates UsernamePasswordAuthenticationToken
7. Sets in SecurityContextHolder
8. Request continues to controller with authentication context
9. Controller method executes and returns 200 OK
```

---

## Security Best Practices Implemented

| Feature | Purpose | Implementation |
|---------|---------|-----------------|
| **Password Hashing** | Never store plain passwords | BCrypt with salt |
| **JWT Signing** | Prevent token tampering | HMAC-SHA256 algorithm |
| **Token Expiration** | Limit token validity window | 24 hours default |
| **Stateless Auth** | No session storage needed | JWT contains all info |
| **CORS Configuration** | Control cross-origin access | Whitelist specific origins |
| **CSRF Disabled** | Not needed for JWT/stateless | Explicitly disabled |
| **Role-Based Access** | Different permissions per user | ROLE_USER, ROLE_ADMIN |
| **Validation** | Input sanitization | @Valid annotations |

---

## Common Scenarios & Solutions

### Scenario 1: Invalid Credentials

**Request:**
```json
{
  "email": "john@example.com",
  "password": "wrongPassword"
}
```

**Flow:**
1. AuthService.login() called
2. authenticationManager.authenticate() → BCrypt.matches() → FALSE
3. authenticationManager throws BadCredentialsException

**Response:**
```
HTTP/1.1 401 Unauthorized
{
  "error": "Unauthorized",
  "message": "Bad credentials"
}
```

### Scenario 2: User Not Found

**Request:**
```json
{
  "email": "nonexistent@example.com",
  "password": "anyPassword"
}
```

**Flow:**
1. authenticationManager.authenticate() throws UsernameNotFoundException

**Response:**
```
HTTP/1.1 401 Unauthorized
```

### Scenario 3: Expired Token on Protected Endpoint

**Request:**
```
GET /api/v1/students
Authorization: Bearer <EXPIRED_TOKEN>
```

**Flow:**
1. JwtAuthFilter extracts token
2. jwtUtil.isTokenValid() checks expiration
3. Expiration date is before current date → returns FALSE
4. Authentication NOT set in SecurityContext
5. Controller is protected, returns 401

**Response:**
```
HTTP/1.1 401 Unauthorized
```

### Scenario 4: No Token on Protected Endpoint

**Request:**
```
GET /api/v1/students
(No Authorization header)
```

**Flow:**
1. JwtAuthFilter checks for "Authorization" header
2. Header is null → returns early
3. Filter chain continues without setting authentication
4. Controller requires authentication
5. Returns 401 Unauthorized

**Response:**
```
HTTP/1.1 401 Unauthorized
```

### Scenario 5: Modified Token (Tampered)

**Scenario:** Client changes token payload but keeps same signature

**Request:**
```
GET /api/v1/students
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.MODIFIED_PAYLOAD.signature
```

**Flow:**
1. JwtAuthFilter extracts token
2. jwtUtil.parseClaims() verifies signature
3. Signature doesn't match modified payload
4. Throws SignatureException
5. Authentication NOT set

**Response:**
```
HTTP/1.1 401 Unauthorized
```

---

## Key Concepts to Master

### 1. **Stateless Authentication**
- No server-side sessions stored
- JWT contains all necessary information
- Highly scalable (no session replication needed)
- Each request is self-contained

### 2. **JWT vs Sessions**
| Aspect | Session | JWT |
|--------|---------|-----|
| Storage | Server-side | Client-side |
| Scalability | Difficult | Easy |
| Microservices | Hard to share | Easy to share |
| Mobile-friendly | Limited | Perfect |
| CSRF | Vulnerable | Not vulnerable |

### 3. **Why BCrypt?**
- **Irreversible** - cannot decrypt to original password
- **Salted** - randomness prevents rainbow tables
- **Adaptive** - can increase work factor over time
- **Industry standard** - widely used and trusted

### 4. **Why Sign JWT?**
- **Integrity** - proves token hasn't been altered
- **Non-repudiation** - server can verify it created the token
- **Detectability** - any tampering is immediately detected

### 5. **AuthenticationManager Role**
- **Central orchestrator** of authentication process
- **Delegates to providers** (DaoAuthenticationProvider)
- **Abstracts** authentication complexity
- **Pluggable** - can support multiple authentication methods

---

## Debugging Tips

### Enable SQL Logging
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Check JWT Token Contents
Use https://jwt.io/ to decode and inspect JWT tokens

### Log Authentication Details
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
    logger.info("Login attempt for email: {}", request.getEmail());
    AuthResponse response = authService.login(request);
    logger.info("Login successful for email: {}", request.getEmail());
    return ResponseEntity.status(HttpStatus.OK).body(response);
}
```

### Trace Filter Chain
Add debug logs in JwtAuthFilter:
```java
logger.debug("Processing request for path: {}", request.getRequestURI());
logger.debug("Authorization header: {}", authHeader);
logger.debug("Extracted username: {}", userName);
logger.debug("Token valid: {}", isValid);
```

---

## Summary

The authentication flow in this application follows industry best practices:

1. **Login Request** → Credentials sent to server
2. **Credential Verification** → BCrypt password comparison
3. **JWT Generation** → Token created with user info and expiration
4. **Token Response** → Client receives JWT
5. **Protected Requests** → Client includes JWT in Authorization header
6. **Token Validation** → Server verifies signature, expiration, and username
7. **Request Processing** → If valid, request is authenticated and processed

This design ensures:
- ✓ Secure (BCrypt + JWT signing)
- ✓ Scalable (stateless)
- ✓ Mobile-friendly (JWT tokens)
- ✓ Microservice-ready (tokens can be verified by any service)
- ✓ CSRF-safe (stateless, not vulnerable)

Master this flow, and you'll understand most modern API authentication systems!

---

---

# 🏗️ DEEP ARCHITECTURE - INTERFACES & IMPLEMENTATIONS

## For Your Clients: Understanding the System Architecture

This section explains the architecture like you're teaching a beginner who has never seen Spring Security before. We'll break down:
- What interfaces Spring provides
- What our code customizes
- How everything connects together
- What happens at each layer

---

## 1️⃣ THE INTERFACE-BASED ARCHITECTURE (Spring Security Pattern)

### Understanding Interfaces

An **Interface** is like a contract that says: *"Any class implementing me must have these specific methods."*

Spring Security uses interfaces to make the framework flexible and customizable:

```
┌─────────────────────────────────────────────────────────────────┐
│                     SPRING SECURITY INTERFACES                   │
│                   (What Spring provides)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  UserDetails Interface                                           │
│  ├─ getUsername()           - What's the username?              │
│  ├─ getPassword()           - What's the encrypted password?    │
│  ├─ getAuthorities()        - What roles does user have?        │
│  ├─ isEnabled()             - Is user active?                   │
│  ├─ isAccountNonExpired()   - Is account not expired?           │
│  └─ isAccountNonLocked()    - Is account not locked?            │
│                                                                   │
│  UserDetailsService Interface                                    │
│  └─ loadUserByUsername(String username)                         │
│     └─ Purpose: Load user from database using username          │
│                                                                   │
│  AuthenticationManager Interface                                 │
│  └─ authenticate(Authentication auth)                           │
│     └─ Purpose: Verify credentials (username + password)        │
│                                                                   │
│  AuthenticationProvider Interface                                │
│  ├─ authenticate(Authentication auth)                           │
│  └─ supports(Class<?> auth)                                      │
│     └─ Purpose: Handle specific authentication logic             │
│                                                                   │
│  PasswordEncoder Interface                                       │
│  ├─ encode(String rawPassword)                                   │
│  └─ matches(String rawPassword, String encodedPassword)         │
│     └─ Purpose: Hash passwords & verify them                    │
│                                                                   │
│  Filter Interface (from Servlet)                                │
│  └─ doFilter(ServletRequest, ServletResponse, FilterChain)      │
│     └─ Purpose: Intercept every HTTP request                    │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ OUR CUSTOM IMPLEMENTATIONS

### What We Built (Custom Classes)

```
┌─────────────────────────────────────────────────────────────────┐
│                   OUR CUSTOM IMPLEMENTATIONS                      │
│              (What your development team built)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ✓ AppUser (implements UserDetails)                             │
│    ├─ Custom: Maps database user to Spring's UserDetails        │
│    ├─ Methods: getUsername(), getPassword(), getAuthorities()  │
│    └─ Data: email, password, fullName, role, enabled           │
│                                                                   │
│  ✓ AppUserDetailsService (implements UserDetailsService)        │
│    ├─ Custom: Loads users from OUR database                     │
│    ├─ Method: loadUserByUsername(email)                         │
│    └─ Uses: AppUserRepository to query database                 │
│                                                                   │
│  ✓ JwtAuthFilter (extends OncePerRequestFilter)                │
│    ├─ Custom: Our JWT token validation logic                    │
│    ├─ Method: doFilterInternal(request, response, chain)        │
│    └─ Does: Extracts JWT, validates, sets authentication        │
│                                                                   │
│  ✓ AuthService (business logic)                                 │
│    ├─ Custom: Our login/register logic                          │
│    ├─ Methods: login(), register()                              │
│    └─ Uses: All the above components                            │
│                                                                   │
│  ✓ AuthController (REST endpoint)                               │
│    ├─ Custom: Our login HTTP endpoint                           │
│    ├─ Method: POST /api/v1/auth/login                           │
│    └─ Uses: AuthService to process login                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3️⃣ SPRING'S DEFAULT IMPLEMENTATIONS

### What Spring Provides (Built-in)

```
┌─────────────────────────────────────────────────────────────────┐
│              SPRING SECURITY DEFAULT IMPLEMENTATIONS             │
│              (What Spring gives us for free)                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  AuthenticationManager (I)  ← Implemented by ProviderManager    │
│  ├─ Provided: ProviderManager                                   │
│  ├─ Purpose: Coordinates authentication (delegates to           │
│  │           AuthenticationProviders)                            │
│  └─ We use it to verify email + password combo                 │
│                                                                   │
│  AuthenticationProvider (I) ← Implemented by DaoAuthProvider    │
│  ├─ Provided: DaoAuthenticationProvider                         │
│  ├─ Purpose: Checks if password matches (using UserDetails      │
│  │           & PasswordEncoder)                                  │
│  └─ We configure it in SecurityConfig                           │
│                                                                   │
│  PasswordEncoder (I) ← Implemented by BCryptPasswordEncoder     │
│  ├─ Provided: BCryptPasswordEncoder                             │
│  ├─ Purpose: Hashes passwords & verifies them                   │
│  └─ We use it to hash passwords during registration             │
│                                                                   │
│  UserDetailsService (I) ← Typically implemented by us           │
│  ├─ We created: AppUserDetailsService                           │
│  ├─ Default: InMemoryUserDetailsManager                         │
│  ├─ Purpose: Loads users from somewhere (DB, LDAP, etc)         │
│  └─ We load from PostgreSQL database                            │
│                                                                   │
│  Filter (I) ← Implemented by OncePerRequestFilter               │
│  ├─ Spring Base: OncePerRequestFilter                           │
│  ├─ We extended: JwtAuthFilter                                  │
│  ├─ Purpose: Intercepts requests, runs logic, continues chain   │
│  └─ We use it to validate JWT tokens                            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4️⃣ COMPLETE COMPONENT INTERACTION DIAGRAM

### How Everything Connects

```
┌──────────────────────────────────────────────────────────────────┐
│                    REQUEST → RESPONSE FLOW                        │
└──────────────────────────────────────────────────────────────────┘

CLIENT LAYER
════════════════════════════════════════════════════════════════════
  Browser/Mobile App
  │
  │ POST /api/v1/auth/login
  │ {"email": "john@example.com", "password": "secret"}
  │
  ▼

REST LAYER (HTTP Entry Point)
════════════════════════════════════════════════════════════════════
  AuthController (@RestController)
  │
  ├─ Annotation: @PostMapping("/login")
  ├─ Method: login(@Valid @RequestBody LoginRequest request)
  │
  ├─ Validates: @Valid checks email format & required fields
  │
  ├─ Calls: authService.login(request)
  │
  ▼

BUSINESS LOGIC LAYER
════════════════════════════════════════════════════════════════════
  AuthService (Service)
  │
  ├─ Step 1: authenticationManager.authenticate(
  │           new UsernamePasswordAuthenticationToken(
  │               email, password
  │           )
  │         )
  │
  │ ┌───────────────────────────────────────────────────────┐
  │ │ STEP 1 EXPLODES INTO MULTIPLE CALLS:                 │
  │ │                                                        │
  │ │ authenticationManager (from SecurityConfig)          │
  │ │   └─ Type: ProviderManager (Spring impl)            │
  │ │     └─ Delegates to: DaoAuthenticationProvider       │
  │ │        │                                              │
  │ │        ├─ Calls: userDetailsService                  │
  │ │        │          .loadUserByUsername(email)         │
  │ │        │                                              │
  │ │        │  ┌────────────────────────────────────┐    │
  │ │        │  │ loadUserByUsername() chains to:    │    │
  │ │        │  │                                    │    │
  │ │        │  │ AppUserDetailsService (our impl)  │    │
  │ │        │  │  └─ appUserRepository.findByEmail │    │
  │ │        │  │     └─ Queries PostgreSQL DB      │    │
  │ │        │  │        └─ Returns AppUser entity  │    │
  │ │        │  │           (implements UserDetails)│    │
  │ │        │  └────────────────────────────────────┘    │
  │ │        │                                              │
  │ │        ├─ Has UserDetails now, calls:               │
  │ │        │  passwordEncoder.matches(                 │
  │ │        │      rawPassword from login,              │
  │ │        │      encodedPassword from AppUser         │
  │ │        │  )                                         │
  │ │        │  └─ Returns: true/false                    │
  │ │        │                                              │
  │ │        └─ If match: Returns authenticated token    │            │
  │ │           If no match: Throws BadCredentialsEx     │            │
  │ └───────────────────────────────────────────────────────┘
  │
  │
  ├─ Step 2: appUserRepository.findByEmail(request.getEmail())
  │           └─ Gets AppUser from DB
  │
  ├─ Step 3: jwtUtil.generateToken(user)
  │           └─ Creates JWT token
  │
  ├─ Step 4: buildResponse(user, token)
  │           └─ Packages data into AuthResponse DTO
  │
  ▼

REPOSITORY LAYER (Database Access)
════════════════════════════════════════════════════════════════════
  AppUserRepository (extends JpaRepository)
  │
  ├─ Method: findByEmail(String email)
  │  └─ Returns: Optional<AppUser>
  │
  ├─ Queries: SELECT * FROM app_users WHERE email = ?
  │
  ├─ Maps: Database row → AppUser entity object
  │
  ▼

ENTITY/MODEL LAYER
════════════════════════════════════════════════════════════════════
  AppUser (implements UserDetails)
  │
  ├─ Implements Spring's UserDetails interface
  ├─ Override Methods:
  │  ├─ getUsername() → returns email (acts as username)
  │  ├─ getPassword() → returns encrypted password from DB
  │  ├─ getAuthorities() → returns List of SimpleGrantedAuthority
  │  │                     (e.g., "ROLE_USER", "ROLE_ADMIN")
  │  ├─ isEnabled() → checks enabled field
  │  ├─ isAccountNonExpired() → returns true
  │  ├─ isAccountNonLocked() → returns true
  │  └─ isCredentialsNonExpired() → returns true
  │
  ├─ Fields:
  │  ├─ email: String
  │  ├─ password: String (encrypted with BCrypt)
  │  ├─ fullName: String
  │  ├─ role: Enum (USER or ADMIN)
  │  ├─ enabled: boolean
  │  └─ createdAt: LocalDateTime
  │
  ▼

JWT UTILITY LAYER
════════════════════════════════════════════════════════════════════
  JwtUtil (Component)
  │
  ├─ generateToken(UserDetails user)
  │  ├─ Creates Header: {"alg":"HS256","typ":"JWT"}
  │  ├─ Creates Payload: Claims with authorities, subject, timestamps
  │  ├─ Signs with secret key                      │            │
  │  └─ Returns: JWT token string                        │            │
  │
  ├─ extractUsername(String token)
  │  ├─ Parses JWT
  │  ├─ Returns: username/email from "sub" claim
  │
  ├─ isTokenValid(String token, UserDetails user)
  │  ├─ Verifies: Signature (using secret key)
  │  ├─ Checks: Expiration timestamp
  │  ├─ Matches: Username in token vs current user
  │  ├─ Returns: true if all valid, false otherwise
  │
  ▼

RESPONSE LAYER
════════════════════════════════════════════════════════════════════
  AuthResponse (DTO)
  │
  ├─ Contains:
  │  ├─ token: String (JWT)
  │  ├─ email: String
  │  ├─ fullName: String
  │  ├─ role: String
  │  └─ expiresIn: long (milliseconds)
  │
  ├─ Returned as JSON:
  │  {
  │    "token": "eyJhbGciOi...",
  │    "email": "john@example.com",
  │    "fullName": "John Doe",
  │    "role": "USER",
  │    "expiresIn": 86400000
  │  }
  │
  ▼

CLIENT RECEIVES RESPONSE
════════════════════════════════════════════════════════════════════
  HTTP 200 OK
  {JWT Token stored for future requests}
```

---

## 5️⃣ SPRING SECURITY FILTER CHAIN ARCHITECTURE

### Understanding How Requests are Processed

When a request comes in, it goes through multiple filters BEFORE reaching your controller:

```
CLIENT REQUEST
       │
       ▼
┌─────────────────────────────────────────────────────────────────┐
│           SPRING SECURITY FILTER CHAIN (in order)               │
└─────────────────────────────────────────────────────────────────┘
       │
       ├─→ Filter 1: CorsFilter
       │   └─ Purpose: Handle cross-origin requests
       │   └─ Spring Default
       │
       ├─→ Filter 2: CsrfFilter
       │   └─ Purpose: Prevent CSRF attacks
       │   └─ We DISABLED this (it's disabled in SecurityConfig)
       │
       ├─→ Filter 3: AuthenticationFilter
       │   └─ Purpose: Handle form-based login
       │   └─ Spring Default (we don't use it)
       │
       ├─→ Filter 4: ⭐ JwtAuthFilter (OUR CUSTOM FILTER)
       │   └─ Position: BEFORE UsernamePasswordAuthenticationFilter
       │   └─ Purpose: Validate JWT tokens
       │   └─ Method: doFilterInternal()
       │   │
       │   │  WHAT IT DOES:
       │   │  1. Reads "Authorization: Bearer <JWT>" header
       │   │  2. Extracts JWT token
       │   │  3. Calls jwtUtil.extractUsername(token)
       │   │  4. Calls userDetailsService.loadUserByUsername(email)
       │   │  5. Calls jwtUtil.isTokenValid(token, userDetails)
       │   │  6. If valid, sets SecurityContext with authentication
       │   │  7. If invalid, continues without authentication
       │   │  8. Calls filterChain.doFilter() to continue
       │   │
       │
       ├─→ Filter 5: ExceptionTranslationFilter
       │   └─ Purpose: Handle security exceptions
       │   └─ Spring Default
       │
       ├─→ Filter 6: FilterSecurityInterceptor
       │   └─ Purpose: Enforce access control rules
       │   └─ Checks: Is user authenticated? Does user have permission?
       │
       ▼
   REQUEST CONTINUES
       │
       ▼
    SECURITY CONFIG RULES
       │
       ├─ Rule 1: "/api/v1/auth/**" → permitAll()
       │  └─ Meaning: Anyone can access login/register (no auth needed)
       │
       ├─ Rule 2: "OPTIONS /**" → permitAll()
       │  └─ Meaning: CORS preflight requests don't need auth
       │
       ├─ Rule 3: "/**" → anyRequest().authenticated()
       │  └─ Meaning: All other endpoints REQUIRE authentication
       │  └─ If not authenticated → 401 Unauthorized
       │
       ▼
   REACH YOUR CONTROLLER
       │
       ├─ AuthController.login()     (public, no auth needed)
       ├─ StudentController.getAll() (private, auth required)
       └─ BookController.getAll()    (private, auth required)
       │
       ▼
   RETURN RESPONSE TO CLIENT
```

---

## 6️⃣ THE AUTHENTICATION MANAGER - DEEP DIVE

### What is AuthenticationManager and How Does It Work?

**In Simple Terms:** AuthenticationManager is like a security guard who verifies your identity. When you give him your username and password, he checks if they're correct.

```java
// This is what happens in AuthService.login()
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        request.getEmail(),      // username
        request.getPassword()    // password (not encrypted yet)
    )
);
```

**What Happens Inside authenticationManager:**

```
┌──────────────────────────────────────────────────────────────┐
│              authenticationManager.authenticate()             │
│                    (ProviderManager impl)                     │
└──────────────────────────────────────────────────────────────┘

Input: UsernamePasswordAuthenticationToken {
  principal: "john@example.com",
  credentials: "securePassword123"     (NOT ENCRYPTED YET)
}

Step 1: Check which providers can handle this
        ├─ Ask: "Can anyone handle UsernamePasswordAuthenticationToken?"
        └─ Answer: "Yes! DaoAuthenticationProvider can!"

Step 2: Delegate to DaoAuthenticationProvider.authenticate(token)
        │
        ├─ Get username from token: "john@example.com"
        │
        ├─ Call UserDetailsService.loadUserByUsername("john@example.com")
        │  │
        │  └─→ AppUserDetailsService.loadUserByUsername(email)
        │     │
        │     └─→ appUserRepository.findByEmail(email)
        │        │
        │        └─→ Query: SELECT * FROM app_users WHERE email = ?
        │           └─→ Database returns: AppUser {
        │               email: "john@example.com",
        │               password: "$2a$10$N9qo8uLOi...",  (ENCRYPTED)
        │               fullName: "John Doe",
        │               role: USER,
        │               enabled: true
        │             }
        │
        ├─ Get password from UserDetails (encrypted)
        │
        ├─ Get password from token (plain text, from login)
        │
        ├─ Call PasswordEncoder.matches(
        │     "securePassword123",              (plain from login)
        │     "$2a$10$N9qo8uLOi..."             (encrypted from DB)
        │  )
        │  └─ BCrypt checks: Does plain password match encrypted?
        │     └─ Returns: true ✓ or false ✗
        │
        ├─ If match:
        │  └─ Return: UsernamePasswordAuthenticationToken {
        │     principal: AppUser,
        │     credentials: null,
        │     authenticated: true,
        │     authorities: [ROLE_USER]
        │  }
        │
        ├─ If no match:
        │  └─ Throw: BadCredentialsException
        │
        ▼

Output: 
  SUCCESS: Authentication {
    principal: AppUser,
    authenticated: true,
    authorities: [ROLE_USER]
  }
  
  OR
  
  FAILURE: BadCredentialsException
           ("Bad credentials")
```

---

## 7️⃣ SPRING'S UserDetailsService - Why We Customize It

### Default Spring Behavior vs Our Custom Behavior

**SPRING'S DEFAULT:**
```java
// Spring provides in-memory user storage
@Configuration
public class SecurityConfig {
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password("password")
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```
❌ Problems:
- Users hardcoded in code
- Can't persist users
- No database integration
- Only for testing

---

**OUR CUSTOM IMPLEMENTATION:**
```java
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
```
✓ Advantages:
- Users stored in PostgreSQL database
- Can register new users
- Users persist across app restarts
- Database as source of truth

**How Spring Calls It:**

```
Spring Flow:
  AuthenticationManager
    └─→ DaoAuthenticationProvider
        └─→ userDetailsService.loadUserByUsername(email)
            └─→ AppUserDetailsService.loadUserByUsername(email)
                └─→ appUserRepository.findByEmail(email)
                    └─→ SELECT * FROM app_users WHERE email = ?
                        └─→ Return AppUser (implements UserDetails)
```

---

## 8️⃣ PASSWORD ENCODER - Why We Use BCrypt

### Default Spring Behavior vs Our Implementation

**WHAT SPRING PROVIDES (NoOpPasswordEncoder - for testing only):**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();  // ❌ NOT SECURE - don't use!
}
```
❌ Problems:
- Doesn't encrypt passwords
- Just stores passwords plain text
- Anyone with DB access can read passwords
- NOT for production

---

**WHAT WE USE (BCryptPasswordEncoder):**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();  // ✓ Secure
}
```

**How BCrypt Works:**

```
ENCODING (During Registration)
═════════════════════════════════════════════════════════════════

Input: Plain password "myPassword123"

BCrypt Process:
  ├─ Step 1: Generate random salt (prevents rainbow tables)
  │  └─ Salt: "N9qo8uLOickgx2ZMRZoMye"
  │
  ├─ Step 2: Combine password + salt
  │  └─ Combined: "myPassword123" + salt
  │
  ├─ Step 3: Run through bcrypt algorithm (very slow intentionally)
  │  └─ Applies SHA-512 multiple times (2^10 = 1024 rounds)
  │
  ├─ Step 4: Output
  │  └─ Result: "$2a$10$N9qo8uLOickgx2ZMRZoMyeiuL5O/x3R/h5fBv2u2i5VQNT6vq6O1O"
  │     Parts:
  │     ├─ $2a$ = BCrypt version
  │     ├─ $10$ = Cost (work factor, can increase)
  │     └─ rest = Salt + Encrypted Password

Output: NEVER STORE PLAIN "myPassword123"!
        ALWAYS STORE: "$2a$10$N9qo8uLOickgx2ZMRZoMyeiuL5O/x3R/h5fBv2u2i5VQNT6vq6O1O"

Database: app_users.password = "$2a$10$N9qo8uLOickgx2ZMRZoMye..."


VERIFICATION (During Login)
═════════════════════════════════════════════════════════════════

User enters: Plain password "myPassword123"

BCrypt Check:
  ├─ Input: 
  │  ├─ Plain password from login: "myPassword123"
  │  └─ Encrypted password from DB: "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
  │
  ├─ Process:
  │  ├─ Step 1: Extract salt from encrypted password
  │  │  └─ Salt: "N9qo8uLOickgx2ZMRZoMye"
  │  │
  │  ├─ Step 2: Apply same salt to plain password
  │  │  └─ Encrypt "myPassword123" with extracted salt
  │  │
  │  ├─ Step 3: Compare results
  │  │  ├─ New hash: "$2a$10$N9qo8uLOickgx2ZMRZoMyeiuL5O..."
  │  │  ├─ Old hash: "$2a$10$N9qo8uLOickgx2ZMRZoMyeiuL5O..."
  │  │  └─ Match? YES ✓
  │
  └─ Output: true/false

Code in DaoAuthenticationProvider:
  passwordEncoder.matches(
      "myPassword123",                              // plain
      "$2a$10$N9qo8uLOickgx2ZMRZoMye..."           // from DB
  )  // Returns: true

WHY BCRYPT IS SECURE:
  ✓ One-way: Can't reverse encrypted → plain
  ✓ Salted: Different salt = different hash (prevents rainbow tables)
  ✓ Adaptive: Cost factor (10 here) can increase as computers speed up
  ✓ Standard: Used industry-wide, heavily tested
```

---

## 9️⃣ JWT TOKEN FLOW - SPRING PROVIDES THE BASE

### What Spring Provides vs What We Built

**SPRING PROVIDES (io.jsonwebtoken library):**

Spring Security doesn't provide JWT out of the box. We added this library:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
```

---

**WHAT WE BUILT (JwtUtil.java):**

```java
@Component
public class JwtUtil {
    // Spring provides: Jwts, Keys, SignatureAlgorithm
    // We built: Custom generateToken, isTokenValid logic
    
    public String generateToken(UserDetails userDetails) {
        // We created the "what goes in token" logic
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        // We created the "how to validate token" logic
    }
}
```

---

## 🔟 FILTER ARCHITECTURE - OncePerRequestFilter Explained

### Why We Extend OncePerRequestFilter

**What Spring Provides:**
```java
// Base class that Spring provides
public abstract class OncePerRequestFilter extends GenericFilterBean {
    protected abstract void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain
    ) throws ServletException, IOException;
}
```

**What We Implement:**
```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    // We override: doFilterInternal()
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain chain) 
        throws ServletException, IOException {
        
        // Our custom logic here
        
        // IMPORTANT: Must call chain.doFilter() to continue
        chain.doFilter(request, response);
    }
}
```

**Why OncePerRequestFilter?**

```
PROBLEM: Multiple filters could process same request
  └─ Request comes in
     └─ Filter processes it
     └─ Calls chain.doFilter()
     └─ Another filter processes same request
     └─ Could create conflicts

SOLUTION: OncePerRequestFilter guarantees
  └─ Your doFilterInternal() runs ONCE per request
  └─ Even if request is forwarded internally
  └─ Prevents duplicate processing
```

---

## 1️⃣1️⃣ COMPLETE METHOD CALL CHAIN

### Every Method Call from Login to Response

```
Timeline: When user hits POST /api/v1/auth/login
═══════════════════════════════════════════════════════════════

T1: AuthController.login(LoginRequest request)
    ├─ Input Validation: @Valid checks constraints
    │  ├─ Email must be valid format
    │  └─ Password must not be blank
    │
    ├─ Call: authService.login(request)
    │
    └─ Returns: ResponseEntity<AuthResponse>

T2: AuthService.login(LoginRequest request)
    ├─ Line 1: authenticationManager.authenticate(
    │  │
    │  └─→ Goes to Authentication Manager (ProviderManager)
    │      │
    │      ├─ Create: UsernamePasswordAuthenticationToken
    │      │  ├─ principal: request.getEmail()
    │      │  └─ credentials: request.getPassword()
    │      │
    │      ├─ Find Provider: Which provider can handle this?
    │      │  └─ DaoAuthenticationProvider says: "I can!"
    │      │
    │      ├─ Call: DaoAuthenticationProvider.authenticate(token)
    │      │  │
    │      │  ├─ Call: userDetailsService.loadUserByUsername(email)
    │      │  │  │
    │      │  │  └─→ AppUserDetailsService.loadUserByUsername(email)
    │      │  │     │
    │      │  │     ├─ Call: appUserRepository.findByEmail(email)
    │      │  │     │  │
    │      │  │     │  └─→ JpaRepository finds user in database
    │      │  │     │      └─ Returns: Optional<AppUser>
    │      │  │     │
    │      │  │     └─ Return: AppUser (implements UserDetails)
    │      │  │
    │      │  ├─ Call: passwordEncoder.matches(
    │      │  │           plainPassword,
    │      │  │           appUser.getPassword()
    │      │  │        )
    │      │  │  └─ BCrypt checks if match ✓
    │      │  │
    │      │  └─ Return: Authenticated token (if match)
    │      │     Or Throw: BadCredentialsException (if no match)
    │      │
    │      └─ Return: Authentication (authenticated: true)
    │
    │  EXCEPTION HANDLING: 
    │  ├─ BadCredentialsException → 401 Unauthorized
    │  ├─ UsernameNotFoundException → 401 Unauthorized
    │  └─ Other exceptions → 500 Server Error
    │
    │ (If exception thrown, authService.login() stops here)
    │
    ├─ Line 2: appUserRepository.findByEmail(request.getEmail())
    │  │
    │  └─→ AppUserRepository.findByEmail(email)
    │      └─ Query: SELECT * FROM app_users WHERE email = ?
    │         └─ Returns: Optional<AppUser>
    │
    ├─ Line 3: jwtUtil.generateToken(user)
    │  │
    │  └─→ JwtUtil.generateToken(UserDetails user)
    │      │
    │      ├─ Create claims map
    │      │  └─ Add authorities: ["ROLE_USER"]
    │      │
    │      ├─ Call: buildToken(claims, user.getUsername())
    │      │  │
    │      │  └─→ JwtUtil.buildToken(claims, subject)
    │      │     │
    │      │     ├─ Jwts.builder()
    │      │     ├─ .setClaims(claims)
    │      │     ├─ .setSubject(email)
    │      │     ├─ .setIssuedAt(new Date())
    │      │     ├─ .setExpiration(new Date(now + 24hrs))
    │      │     ├─ .signWith(secretKey, HS256)
    │      │     └─ .compact()  ← Returns JWT string
    │      │
    │      └─ Return: JWT Token String
    │
    ├─ Line 4: buildResponse(user, token)
    │  │
    │  └─→ AuthService.buildResponse(AppUser, String)
    │     │
    │     ├─ Create: AuthResponse.builder()
    │     ├─ .token(token)
    │     ├─ .email(user.getEmail())
    │     ├─ .fullName(user.getFullName())
    │     ├─ .role(user.getRole().name())
    │     ├─ .expiresIn(expirationMs)
    │     └─ .build()
    │
    └─ Return: AuthResponse

T3: AuthController packages response
    ├─ ResponseEntity.status(HttpStatus.OK)
    ├─ .body(authResponse)
    │
    └─ Return: ResponseEntity<AuthResponse>

T4: Spring converts to JSON and sends to client
    ├─ Serializes: AuthResponse → JSON
    │
    ├─ Response:
    │  HTTP/1.1 200 OK
    │  Content-Type: application/json
    │  {
    │    "token": "eyJhbGciOi...",
    │    "email": "john@example.com",
    │    "fullName": "John Doe",
    │    "role": "USER",
    │    "expiresIn": 86400000
    │  }
    │
    └─ Complete!


═══════════════════════════════════════════════════════════════
SUBSEQUENT REQUESTS WITH JWT TOKEN
═══════════════════════════════════════════════════════════════

T1: Client sends request with JWT
    GET /api/v1/students
    Authorization: Bearer eyJhbGciOi...

T2: SpringSecurityFilterChain processes request
    ├─ Request passes through multiple filters
    │
    ├─→ JwtAuthFilter.doFilterInternal() runs
    │  │
    │  ├─ Get: request.getHeader("Authorization")
    │  │  └─ Returns: "Bearer eyJhbGciOi..."
    │  │
    │  ├─ Check: Starts with "Bearer "?
    │  │  └─ YES ✓
    │  │
    │  ├─ Extract: token = authHeader.substring(7)
    │  │  └─ Result: "eyJhbGciOi..." (without "Bearer ")
    │  │
    │  ├─ Call: jwtUtil.extractUsername(token)
    │  │  │
    │  │  └─→ JwtUtil.extractUsername(token)
    │  │     │
    │  │     ├─ Call: parseClaims(token)
    │  │     │  │
    │  │     │  └─→ JwtUtil.parseClaims(token)
    │  │     │     │
    │  │     │     ├─ Jwts.parserBuilder()
    │  │     │     ├─ .setSigningKey(secretKey)
    │  │     │     ├─ .build()
    │  │     │     ├─ .parseClaimsJws(token)  ← Verifies signature!
    │  │     │     │  └─ If signature invalid → throws exception
    │  │     │     └─ .getBody()  ← Returns Claims object
    │  │     │
    │  │     └─ Return: claims.getSubject() (the email)
    │  │
    │  ├─ Call: userDetailsService.loadUserByUsername(email)
    │  │  │
    │  │  └─→ AppUserDetailsService.loadUserByUsername(email)
    │  │     └─→ appUserRepository.findByEmail(email)
    │  │        └─ Returns: AppUser
    │  │
    │  ├─ Call: jwtUtil.isTokenValid(token, userDetails)
    │  │  │
    │  │  └─→ JwtUtil.isTokenValid(token, userDetails)
    │  │     │
    │  │     ├─ extractUsername(token) → email
    │  │     ├─ Compare: email == userDetails.getUsername()?
    │  │     │  └─ YES ✓
    │  │     │
    │  │     ├─ Check: !isExpired(token)?
    │  │     │  │
    │  │     │  └─→ JwtUtil.isExpired(token)
    │  │     │     │
    │  │     │     ├─ parseClaims(token)
    │  │     │     ├─ Get expiration date
    │  │     │     └─ Compare: expDate.before(new Date())?
    │  │     │        └─ NO (not expired) ✓
    │  │     │
    │  │     └─ Return: true ✓ Token is valid!
    │  │
    │  ├─ If valid:
    │  │  ├─ Create: UsernamePasswordAuthenticationToken(
    │  │  │            userDetails,
    │  │  │            null,
    │  │  │            userDetails.getAuthorities()
    │  │  │          )
    │  │  │
    │  │  ├─ Set: authToken.setDetails(...)
    │  │  │
    │  │  └─ SecurityContextHolder.getContext()
    │  │     .setAuthentication(authToken)
    │  │     └─ This stores authentication for current request!
    │  │
    │  ├─ Call: chain.doFilter(request, response)
    │  │  └─ Continue to next filter and eventually controller
    │  │
    │  └─ Return from doFilterInternal()
    │
    ├─ Other filters process
    │
    └─ Reach StudentController

T3: StudentController method executes
    ├─ SecurityContextHolder has authentication
    ├─ User is authenticated with role ROLE_USER
    ├─ Authorization checks pass
    ├─ Method runs and returns 200 OK

T4: Response sent to client
```

---

## 1️⃣2️⃣ SECURITY CONFIG - THE ORCHESTRATOR

### SecurityConfig.java - Ties Everything Together

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    // BEAN 1: PasswordEncoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    // Purpose: Spring's DaoAuthenticationProvider will use this
    // to verify passwords during login

    // BEAN 2: AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = 
            new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }
    // Purpose: Created by AuthService to authenticate credentials
    // This bean is auto-injected into AuthService

    // BEAN 3: Security Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) 
            throws Exception {
        http
            // Disable CSRF (not needed for stateless JWT)
            .csrf(csrf -> csrf.disable())
            
            // Set session management to STATELESS
            // No server-side sessions created
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
            
            // Define authorization rules
            .authorizeHttpRequests(auth -> auth
                // Login/Register endpoints - anyone can access
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // CORS preflight requests - anyone can access
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // All other endpoints - MUST be authenticated
                .anyRequest().authenticated()
            )
            
            // Add our JWT filter BEFORE the standard auth filter
            .addFilterBefore(
                jwtAuthFilter, 
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    // Purpose: Configures Spring Security for the entire app
}
```

**How SecurityConfig Ties Everything Together:**

```
SecurityConfig
├─ Defines PasswordEncoder bean
│  ├─ Used by: DaoAuthenticationProvider
│  └─ Called when: Verifying passwords during login
│
├─ Creates AuthenticationManager bean
│  ├─ Uses: PasswordEncoder + UserDetailsService
│  └─ Injected into: AuthService
│
├─ Builds Filter Chain
│  ├─ Adds: JwtAuthFilter
│  │  └─ Runs on: Every HTTP request
│  │
│  ├─ Sets: Authorization rules
│  │  ├─ Rule 1: /api/v1/auth/** → permitAll
│  │  ├─ Rule 2: OPTIONS /** → permitAll
│  │  └─ Rule 3: /** → authenticated required
│  │
│  └─ Sets: SessionCreationPolicy.STATELESS
│     └─ No server-side sessions
│
└─ Result: Complete security infrastructure ready to use
```

---

## 1️⃣3️⃣ NEWBIE EXPLANATION - THE SIMPLE VERSION

### If You're Explaining to a Non-Technical Client

```
🎯 THE BIG PICTURE
═════════════════════════════════════════════════════════════════

Imagine a nightclub with a bouncer (Spring Security):

1. FIRST TIME VISITING (Registration)
   └─ You: "I want to join your club!"
   └─ Bouncer: "OK, give me your email and password"
   └─ You: "Email: john@example.com, Password: mySecret123"
   └─ Bouncer: 
      └─ Encrypts your password with a special formula
      └─ Stores: email → encrypted password in a list
      └─ Gives you: A membership card (JWT token) with your name

2. RETURNING VISIT (Login)
   └─ You: "I want to enter!"
   └─ You give: Email and password
   └─ Bouncer:
      └─ Looks in the list for your email
      └─ Gets your encrypted password from the list
      └─ Checks: Does your password match the one we encrypted?
      └─ YES ✓ 
      └─ Gives you: A new membership card (JWT token)

3. USING YOUR MEMBERSHIP CARD (Protected Requests)
   └─ You: "Can I enter VIP area?"
   └─ You show: Membership card (JWT token)
   └─ Bouncer:
      └─ Checks: Is this card real and not expired?
      └─ Checks: Does it belong to you?
      └─ YES ✓
      └─ Lets you in!

4. IF CARD IS INVALID/EXPIRED
   └─ You: "Can I enter?"
   └─ You show: Invalid card
   └─ Bouncer: "NO! 401 Access Denied!"


🏗️ THE ARCHITECTURE IN SIMPLE TERMS
═════════════════════════════════════════════════════════════════

FOUR MAIN PIECES:

PIECE 1: DATA (What we store)
├─ AppUser entity
├─ Stored in: PostgreSQL database
├─ Fields: email, encrypted_password, name, role
└─ Like: Contact list with passwords

PIECE 2: RECEIVING REQUESTS (Entry point)
├─ AuthController
├─ Receives: POST /api/v1/auth/login with email + password
├─ Does: Calls AuthService to process
└─ Like: Receptionist taking your information

PIECE 3: VERIFICATION LOGIC (The checking)
├─ AuthService
├─ Does:
│  ├─ Asks: "Is this email + password correct?"
│  ├─ Uses: AuthenticationManager (the validator)
│  ├─ Gets: User from database
│  ├─ Creates: JWT membership card
│  └─ Returns: Card to client
└─ Like: Bouncer checking the list

PIECE 4: PROTECTING REQUESTS (Guard duty)
├─ JwtAuthFilter
├─ Runs on: Every request to protected endpoints
├─ Does:
│  ├─ Checks: Do you have a membership card?
│  ├─ Validates: Is card real and not expired?
│  ├─ If YES: Let request through
│  ├─ If NO: Send 401 Unauthorized
│  └─ Like: Bouncer checking cards at the door
└─ Stored in: SecurityContext (request's memory)


🔄 FLOW IN SIMPLE WORDS
═════════════════════════════════════════════════════════════════

LOGIN FLOW:
  User sends email + password
    ↓
  AuthController receives it
    ↓
  AuthService checks password
    ↓
  Looks up email in database
    ↓
  Compares password (encrypted vs new)
    ↓
  Match? YES ✓
    ↓
  Create JWT card with user info
    ↓
  Send card to client
    ↓
  Client stores card


USING PROTECTED ENDPOINT WITH JWT:
  Client sends request with card
    ↓
  JwtAuthFilter sees card
    ↓
  Checks: Is card real? Not expired? Belongs to user?
    ↓
  All YES ✓
    ↓
  Puts user info in request memory
    ↓
  Let request go through
    ↓
  Controller runs and returns data


🛡️ WHY THIS DESIGN IS GOOD
═════════════════════════════════════════════════════════════════

PROBLEM: Storing passwords
└─ Solution: Encrypt with BCrypt
   └─ Why: If database stolen, passwords still secret
   └─ How: BCrypt uses one-way encryption
      └─ Can't reverse it back

PROBLEM: Every request need to verify user?
└─ Solution: Give JWT card after login
   └─ Why: Card has everything inside
   └─ How: Card is signed, can't be faked
      └─ If someone changes it, signature breaks

PROBLEM: Multiple servers, which remembers the user?
└─ Solution: Card remembers, not server
   └─ Why: Any server can read the card
   └─ How: Card has secret recipe, hard to fake

PROBLEM: How long is card valid?
└─ Solution: Card expires in 24 hours
   └─ Why: Security - limits damage if card stolen
   └─ How: Timestamp on card tells when it expires


💡 KEY INTERFACES EXPLAINED SIMPLY
═════════════════════════════════════════════════════════════════

INTERFACE: Contract that says "You MUST do these things"

Spring provides:
├─ UserDetails interface
│  └─ "You must tell me: username, password, roles"
│  └─ We use: AppUser implements this
│
├─ UserDetailsService interface
│  └─ "You must give me a user by username"
│  └─ We use: AppUserDetailsService implements this
│
├─ AuthenticationManager interface
│  └─ "You must verify username + password"
│  └─ We use: Spring's ProviderManager + DaoAuthenticationProvider
│
├─ PasswordEncoder interface
│  └─ "You must encrypt and verify passwords"
│  └─ We use: BCryptPasswordEncoder
│
└─ Filter interface
   └─ "You must check every request"
   └─ We use: JwtAuthFilter implements OncePerRequestFilter


🎓 WHAT'S SPRING VS WHAT'S CUSTOM
═════════════════════════════════════════════════════════════════

SPRING PROVIDES (Framework):
├─ Interfaces: UserDetails, UserDetailsService, etc.
├─ Classes: BCryptPasswordEncoder, ProviderManager
├─ Filters: Security filter chain infrastructure
├─ Beans: Auto-wiring, dependency injection
└─ Why: So developers don't build from scratch

WE CUSTOMIZED (Your Business):
├─ AppUser: Our user entity with our fields
├─ AppUserDetailsService: Load users from OUR database
├─ JwtAuthFilter: Our JWT validation logic
├─ JwtUtil: Our JWT creation/parsing logic
├─ AuthService: Our business logic for login/register
└─ SecurityConfig: Our security rules for the app


📊 CORE vs CUSTOM
═════════════════════════════════════════════════════════════════

CORE BEHAVIOR (Same in any Spring Security app):
├─ How password encoding works
├─ How authentication manager verifies credentials
├─ How filter chain processes requests
├─ How UserDetails interface works
└─ Why: Spring handles this automatically

CUSTOM BEHAVIOR (Specific to your app):
├─ What fields are in your user (email, fullName, role)
├─ Where users are stored (PostgreSQL database)
├─ How to load users (by email)
├─ What JWT contains (authorities, subject, timestamps)
├─ How long JWT lives (24 hours)
├─ Which endpoints are protected
└─ Why: Your business needs determine this
```

---

## 1️⃣4️⃣ DEPENDENCY INJECTION - How Beans Connect

### Understanding @RequiredArgsConstructor and Dependency Injection

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
}
```

**What @RequiredArgsConstructor Does:**

```
MANUAL WAY (without Lombok):
════════════════════════════════════════════════════════════════
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    
    // Constructor - manually written
    public AuthService(AppUserRepository repo, JwtUtil jwt) {
        this.appUserRepository = repo;
        this.jwtUtil = jwt;
    }
}

AUTOMATIC WAY (with @RequiredArgsConstructor):
════════════════════════════════════════════════════════════════
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    
    // Lombok generates the constructor automatically!
    // Same result as manual way

HOW SPRING USES IT:
════════════════════════════════════════════════════════════════
1. Spring sees: @Service on AuthService
   └─ Creates bean of AuthService

2. Spring sees: AuthService needs dependencies
   ├─ AppUserRepository (bean)
   ├─ JwtUtil (bean)
   ├─ PasswordEncoder (bean)
   └─ AuthenticationManager (bean)

3. Spring finds: Beans matching those types
   ├─ Looks for @Repository annotated AppUserRepository
   ├─ Looks for @Component annotated JwtUtil
   ├─ Looks for @Bean PasswordEncoder
   └─ Looks for @Bean AuthenticationManager

4. Spring injects: Passes them to constructor
   └─ new AuthService(repo, jwt, encoder, manager)

5. Result: AuthService has all dependencies ready to use!


WHY THIS PATTERN?
════════════════════════════════════════════════════════════════
✓ Loose coupling: AuthService doesn't create its dependencies
✓ Testability: Can inject mock objects for testing
✓ Flexibility: Can swap implementations easily
✓ Clean code: No "new" keyword scattered around
```

---

## 1️⃣5️⃣ THE COMPLETE PICTURE - All Parts Together

```
┌─────────────────────────────────────────────────────────────────┐
│                    COMPLETE SYSTEM DIAGRAM                       │
│           (Every component and how it connects)                  │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ CLIENT APP   │
│ (Browser/    │
│  Mobile)     │
└──────┬───────┘
       │ HTTP Request
       │ POST /api/v1/auth/login
       │ {"email": "john@example.com", "password": "secret"}
       ▼
┌────────────────────────────────────────────────────────────────┐
│                    SPRING FRAMEWORK LAYER                        │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [1] HTTP Request comes in                                      │
│      ↓                                                           │
│  [2] SecurityFilterChain processes                              │
│      ├─ Filter 1: CORS                                          │
│      ├─ Filter 2: CSRF (disabled)                               │
│      ├─ ...more filters...                                      │
│      └─ Our Filter: JwtAuthFilter                               │
│         └─ If token present:                                    │
│            ├─ Extract JWT                                       │
│            ├─ Validate it                                       │
│            └─ Set authentication context                        │
│      ↓                                                           │
│  [3] Authorization check                                        │
│      ├─ Is endpoint public (/api/v1/auth/*)? → Allow           │
│      ├─ Is endpoint protected? → Need authentication            │
│      └─ Has user valid JWT? → Allow or 401                     │
│      ↓                                                           │
│  [4] Route to correct controller                                │
└────────────────────────────────────────────────────────────────┘
       ▼
┌────────────────────────────────────────────────────────────────┐
│              YOUR APPLICATION CODE LAYER                         │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  AuthController (REST Entry Point)                              │
│  ├─ @PostMapping("/login")                                      │
│  ├─ Receives: LoginRequest (validated)                          │
│  ├─ Calls: authService.login(request)                           │
│  └─ Returns: ResponseEntity<AuthResponse>                       │
│      ↓                                                           │
│  AuthService (Business Logic)                                   │
│  ├─ Calls: authenticationManager.authenticate(...)              │
│  │  │                                                            │
│  │  └─→ ┌─────────────────────────────────────────┐            │
│  │     │ Spring's Authentication Manager          │            │
│  │     │ (ProviderManager + DaoAuthProvider)      │            │
│  │     │                                          │            │
│  │     ├─ Calls: userDetailsService.loadUser...() │            │
│  │     │  └─→ AppUserDetailsService               │            │
│  │     │     └─ Calls: appUserRepository.findBy..│            │
│  │     │        └─→ Repository Layer              │            │
│  │     │           └─ SQL Query to database       │            │
│  │     │              Returns: AppUser            │            │
│  │     │                                          │            │
│  │     ├─ Calls: passwordEncoder.matches(...)     │            │
│  │     │  └─ BCrypt verification                  │            │
│  │     │  └─ Returns: true/false                  │            │
│  │     │                                          │            │
│  │     └─ Returns: Authenticated token            │            │
│  │        OR throws: BadCredentialsException      │            │
│  │                                                 │            │
│  ├─ Calls: appUserRepository.findByEmail(...)      │            │
│  │  └─ Get AppUser from database                  │            │
│  │                                                 │            │
│  ├─ Calls: jwtUtil.generateToken(user)            │            │
│  │  ├─ Creates JWT payload                        │            │
│  │  ├─ Signs with secret key                      │            │
│  │  └─ Returns: JWT string                        │            │
│  │                                                 │            │
│  ├─ Calls: buildResponse(user, token)             │            │
│  │  └─ Creates: AuthResponse DTO                  │            │
│  │     ├─ token                                   │            │
│  │     ├─ email                                   │            │
│  │     ├─ fullName                                │            │
│  │     ├─ role                                    │            │
│  │     └─ expiresIn                               │            │
│  │                                                 │            │
│  └─ Returns: AuthResponse (converted to JSON)     │            │
│      ↓                                             │            │
│  SecurityConfig (Configuration)                   │            │
│  ├─ @Bean PasswordEncoder                         │            │
│  │  └─ BCryptPasswordEncoder                      │            │
│  ├─ @Bean AuthenticationManager                   │            │
│  │  └─ Uses: UserDetailsService + PasswordEncoder │            │
│  ├─ @Bean SecurityFilterChain                     │            │
│  │  └─ Defines: Authorization rules               │            │
│  └─ Registers: JwtAuthFilter                      │            │
│                                                   │            │
└────────────────────────────────────────────────────────────────┘
       ▼
┌────────────────────────────────────────────────────────────────┐
│                    DATABASE LAYER                                │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  PostgreSQL Database                                             │
│  ├─ Table: app_users                                            │
│  │  ├─ id: BIGINT                                               │
│  │  ├─ email: VARCHAR (unique)                                  │
│  │  ├─ password: VARCHAR (encrypted)                            │
│  │  ├─ full_name: VARCHAR                                       │
│  │  ├─ role: VARCHAR (USER/ADMIN)                               │
│  │  ├─ enabled: BOOLEAN                                         │
│  │  └─ created_at: TIMESTAMP                                    │
│  │                                                              │
│  └─ Accessed by: AppUserRepository (JPA)                        │
│     └─ Methods: findByEmail(), existsByEmail(), save()          │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
       ▼
┌────────────────────────────────────────────────────────────────┐
│                    RESPONSE TO CLIENT                            │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  HTTP 200 OK                                                    │
│  Content-Type: application/json                                 │
│                                                                  │
│  {                                                              │
│    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ...",                      │
│    "email": "john@example.com",
│    "fullName": "John Doe",
│    "role": "USER",
│    "expiresIn": 86400000                                        │
│  }                                                              │
│                                                                  │
│  (Client stores JWT for future requests)                        │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

