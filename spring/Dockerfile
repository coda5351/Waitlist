# 1. build the frontend
FROM node:18-alpine AS frontend
WORKDIR /vue

# install dependencies
COPY vue/package*.json ./
RUN npm ci
COPY vue/ .
RUN npm run build
# generates `dist/`

# 2. build the backend, embedding the frontend output
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app

# copy frontend build into resources so Spring will serve static files
COPY --from=frontend /vue/dist ./src/main/resources/static

COPY spring/pom.xml .
COPY spring/src ./src
RUN mvn clean package -DskipTests

# 3. runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
