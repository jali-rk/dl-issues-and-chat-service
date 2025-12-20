FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle

COPY src ./src

RUN ./gradlew clean bootJar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8900

ENTRYPOINT ["java","-jar","/app/app.jar"]
