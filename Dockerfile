# Etapa de build (compila o projeto)
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa final (roda o app)
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Instala stunnel
RUN apt-get update && apt-get install -y stunnel4 && rm -rf /var/lib/apt/lists/*
# Cria diretório de configuração do stunnel
RUN mkdir -p /etc/stunnel
# Copia o arquivo de configuração do stunnel
COPY stunnel.conf /etc/stunnel/stunnel.conf

COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]