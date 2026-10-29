# ==============================================================================
# GrammarLab — Multi-Stage Production Dockerfile (Option B: Monolithic Container)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build the React + Vite Frontend
# ------------------------------------------------------------------------------
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

# Cache npm dependencies
COPY frontend/package*.json ./
RUN npm ci

# Copy frontend source and build static production bundle
COPY frontend/ ./
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Build the Spring Boot Backend with embedded Frontend static assets
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /app/backend

# Cache Maven dependencies
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy backend source code
COPY backend/src ./src

# Inject built React static assets into Spring Boot's classpath static folder
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static/

# Compile and package production fat JAR
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 3: Minimal Java 21 Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Run as non-privileged user for container security
RUN addgroup -S grammarlab && adduser -S grammarlab -G grammarlab
USER grammarlab:grammarlab

# Copy the standalone fat JAR from backend builder
COPY --from=backend-builder --chown=grammarlab:grammarlab /app/backend/target/grammarlab-backend-1.0.0-SNAPSHOT.jar app.jar

# Dynamic port binding (defaults to 8080, compatible with Render, Railway, AWS, Fly.io)
ENV PORT=8080
EXPOSE 8080

# Configure JVM optimizations for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
