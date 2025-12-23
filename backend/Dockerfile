# Builder stage
FROM maven:3.9.4-openjdk-17-slim AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -DskipTests package

# Runtime stage
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /workspace/target/personal-1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
