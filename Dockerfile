# To build, run the following command from the top level project directory:
#
# docker build -t nroduit/karnak:latest -f Dockerfile .

# Based on build image containing maven, jdk and git
FROM maven:3.9-eclipse-temurin-25-noble AS builder
WORKDIR /app

# Build the Spring Boot application with layers
COPY pom.xml .
COPY src ./src
RUN mvn -B package -P production
WORKDIR /app/bin
RUN cp ../target/karnak*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Build the final deployment image
FROM eclipse-temurin:25-jdk-noble
WORKDIR /app

COPY --from=builder /app/bin/extracted/dependencies/ ./
RUN true
COPY --from=builder /app/bin/extracted/spring-boot-loader/ ./
RUN true
COPY --from=builder /app/bin/extracted/snapshot-dependencies/ ./
RUN true
COPY --from=builder /app/bin/extracted/application/ ./
RUN true
COPY tools/docker-entrypoint.sh .

EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
