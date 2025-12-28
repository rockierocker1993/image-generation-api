# =========================
# Stage 1: Build
# =========================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests


# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:17-jre-jammy

# Install base tools
RUN apt-get update && apt-get install -y \
    ca-certificates \
    wget \
    gnupg \
    software-properties-common \
    potrace \
    && rm -rf /var/lib/apt/lists/*

# -------------------------
# Install Inkscape CLI (>=1.3)
# -------------------------
RUN add-apt-repository ppa:inkscape.dev/stable \
    && apt-get update \
    && apt-get install -y inkscape \
    && rm -rf /var/lib/apt/lists/*

# Verify inkscape version (debug safety)
RUN inkscape --version

# -------------------------
# Create non-root user
# -------------------------
RUN groupadd -r appuser \
    && useradd -r -g appuser -m -d /home/appuser appuser

WORKDIR /app

# Copy Spring Boot jar
COPY --from=build /app/target/*.jar app.jar

# Copy vtracer binary
COPY ./tools/vtracer /usr/local/bin/vtracer
RUN chmod +x /usr/local/bin/vtracer

# App directories
RUN mkdir -p /app/output /app/temp \
    && chown -R appuser:appuser /app /home/appuser

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]