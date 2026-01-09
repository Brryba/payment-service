FROM maven AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /payment-service
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]