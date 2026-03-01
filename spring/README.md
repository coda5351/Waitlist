# Waitlist

A Spring Boot application with PostgreSQL and Flyway database migrations.

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 12+

## Setup

### 1. Create PostgreSQL Database

```bash
psql -U postgres
CREATE DATABASE waitlist;
\q
```

### 2. Update Database Configuration

Edit `src/main/resources/application.properties` with your PostgreSQL credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/waitlist
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### 3. Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

## API Endpoints

- `GET /api/hello` - Returns "Hello, World!"

## Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`. Each migration file follows the naming convention `V{version}__{description}.sql`.

Example:
- `V1__Initial_schema.sql` - Creates the users table

## Project Structure

```
src/
├── main/
│   ├── java/com/waitlist/
│   │   ├── WaitlistApplication.java
│   │   └── controller/
│   │       └── HelloController.java
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__Initial_schema.sql
└── test/
```
