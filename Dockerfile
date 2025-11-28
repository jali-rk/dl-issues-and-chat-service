FROM amazoncorretto:21-alpine-jdk AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle

COPY src ./src

RUN ./gradlew clean bootJar

FROM amazoncorretto:21-alpine-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8900

ENTRYPOINT ["java","-jar","/app/app.jar"]
