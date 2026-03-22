# Waitlist Project

This repository holds the source code for the **Waitlist** application, a full-stack project combining a Spring Boot backend with a Vue.js frontend.

## Structure

```
Entry.java                  # simple Java entry script
V1__create_entries_table.sql # initial database migration
spring/                     # backend application
  ├─ pom.xml                # Maven configuration
  ├─ src/main/java/com/waitlist # Java sources
  │  ├─ config/             # configuration classes
  │  ├─ controller/         # REST controllers
  │  ├─ dto/                # data transfer objects
  │  ├─ exception/          # custom exceptions
  │  ├─ handler/            # exception handlers
  │  ├─ model/              # JPA entity definitions
  │  ├─ repository/         # Spring Data repositories
  │  ├─ security/           # security config
  │  ├─ service/            # business logic services
  │  └─ util/               # utility classes
  └─ src/main/resources/    # application resources
      ├─ application*.properties
      └─ db/migration/      # Flyway migrations

vue/                        # frontend application (Vue 3 + Vite)
  ├─ package.json           # npm configuration
  ├─ vite.config.ts         # Vite build config
  ├─ src/                   # frontend source code
  │   ├─ components/        # reusable components (e.g. DataTable)
  │   ├─ pages/             # route-level views
  │   ├─ router.ts          # Vue Router setup
  │   ├─ store.ts           # Pinia store (or Vuex)
  │   └─ utils/             # helper utilities
  └─ public/                # static assets

```

## Getting Started

### Backend

1. Navigate to `spring/` and run with Maven:
   ```bash
   mvn spring-boot:run
   ```
2. The application uses Flyway for database migrations; the SQL files in `spring/src/main/resources/db/migration` are executed automatically.

### Frontend

1. Change to the `vue/` directory:
   ```bash
   cd vue
   npm install
   npm run dev
   ```
2. The development server runs on the default Vite port (5173); frontend calls the backend via configured API endpoints.

## Database

- Initial schema defined in `V1__create_entries_table.sql` (and duplicated under the Spring resources). Update migrations there when changing DB structure.

## Additional Details

- A `spring/docker-compose.yml` and `Dockerfile` exist for containerized setups.
- The frontend uses scoped CSS occasionally with `:deep()` for deep selectors.
- Test classes are located under `spring/src/test/java`.

### Dev email testing

The backend is configured to send mail using Spring's `JavaMailSender`. When the `dev` profile is active the application points at a local SMTP server. We recommend using **smtp4dev** (https://github.com/rnwood/smtp4dev) to inspect outgoing messages:

1. Start smtp4dev on your machine (docker, npm or windows installer).
   ```bash
   # container listens on port 25 for SMTP
   docker run -p 2525:25 -p 5000:80 rnwood/smtp4dev
   ```
2. Enable the `dev` profile (`SPRING_PROFILES_ACTIVE=dev` or run `mvn spring-boot:run -Dspring-boot.run.profiles=dev`).
3. Reset-password and registration emails will be caught by smtp4dev; open `http://localhost:5000` to view them.

In production you'll want to supply real SMTP credentials in `application-prod.properties` or via environment variables.

## Firebase configuration

The backend uses Firebase only for push notifications (FCM) and not for authentication. The application expects the service account JSON key file to be present at `spring/src/main/resources/firebase-adminsdk.json`.

1. Create a Firebase project in the Firebase console.
2. Enable Cloud Messaging for your project.
3. Download a service account JSON file from Project Settings > Service accounts.
4. Save it to `spring/src/main/resources/firebase-adminsdk.json`

### Optional Firebase properties (environment variables)

The app can still read optional Firebase settings from environment variables, which are mapped in `spring/src/main/resources/application.properties`.

- `FIREBASE_API_KEY` → `firebase.apiKey`
- `FIREBASE_APP_ID` → `firebase.appId`
- `FIREBASE_MESSAGING_SENDER_ID` → `firebase.messagingSenderId`
- `FIREBASE_PROJECT_ID` → `firebase.projectId`
- `FIREBASE_STORAGE_BUCKET` → `firebase.storageBucket`

> Warning: never commit service account JSONs or secret keys into source control. Keep them in secure vaults or environment secrets.

> Warning: never commit service account JSONs or secret keys into source control. Keep them in secure vaults or environment secrets.
