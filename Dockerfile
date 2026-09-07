# ==========================================
# ETAPA 1: Compilación y ejecución Liquibase
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# 1. Copiar pom.xml y descargar dependencias para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copiar el código fuente y recursos (changelogs, openapi spec)
COPY src ./src

# 3. Compilar, generar DTOs con OpenAPI Generator, correr Liquibase y empaquetar el JAR
# (Si no hay base de datos disponible durante el docker build estricto, usa: -DskipTests)
RUN mvn clean install -DskipTests

# ==========================================
# ETAPA 2: Contenedor de ejecución / artefacto
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos el JAR generado desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Variables de entorno con valores por defecto (compatibles con application.yml)
ENV DB_HOST=postgres \
    DB_PORT=5432 \
    DB_NAME=pos_db \
    DB_USER=postgres \
    DB_PASSWORD=postgres

# Comando para ejecutar el JAR (corre Liquibase y finaliza o se mantiene activo si es Runner)
ENTRYPOINT ["java", "-jar", "app.jar"]
