FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY ./stu_backend/pom.xml ./stu_backend/pom.xml
COPY ./stu_backend/src ./stu_backend/src

RUN mvn -f ./stu_backend/pom.xml -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /build/stu_backend/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
