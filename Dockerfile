# ------------------------------
# Stage 1: Build (con Maven)
# ------------------------------
FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar pom.xml y descargar dependencias (cacheo de dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# ------------------------------
# Stage 2: Run (JRE slim)
# ------------------------------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copiar solo el jar generado del stage anterior
COPY --from=build /app/target/*.jar app.jar

# Puerto expuesto (cámbialo si tu micro cambia de puerto)
EXPOSE 8010

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]
