# --- Build Stage ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S cinebh && adduser -S cinebh -G cinebh

COPY --from=build /app/target/*.jar app.jar

COPY src/main/resources/cinebh-keystore.p12 /app/cinebh-keystore.p12

USER cinebh

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]
