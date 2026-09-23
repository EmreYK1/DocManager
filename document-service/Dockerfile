FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
COPY document-service/pom.xml document-service/
RUN mvn -B -q -pl document-service -am dependency:go-offline

COPY document-service/src document-service/src
RUN mvn -B -q -pl document-service -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/document-service/target/document-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
