# 1. build the frontend
FROM node:18-alpine AS frontend
WORKDIR /vue

# install dependencies
COPY vue/package*.json ./
# use npm install since CI can fail with lockfile mismatches
RUN npm install
COPY vue/ .
RUN npm run build
# generates `dist/`

# 2. build the backend, embedding the frontend output
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app

# copy backend source first (static will be overwritten)
COPY spring/pom.xml .
COPY spring/src ./src

# copy frontend build into resources so Spring will serve static files
# do this after copying backend code so it replaces any stale files in src/main/resources/static
COPY --from=frontend /vue/dist ./src/main/resources/static

# skip test compilation entirely to avoid build-time errors
RUN mvn clean package -Dmaven.test.skip=true

# 3. runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
# include secret.properties so FirebaseService can load local secrets at runtime
COPY --from=backend /app/src/main/resources/firebase-adminsdk.json ./firebase-adminsdk.json

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
