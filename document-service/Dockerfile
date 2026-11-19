FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
COPY document-service/pom.xml document-service/
COPY ocr-worker/pom.xml ocr-worker/
COPY indexing-worker/pom.xml indexing-worker/
RUN mvn -B -q -pl document-service -am dependency:go-offline

COPY document-service/src document-service/src
RUN mvn -B -q -pl document-service -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

# Nicht als root laufen
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /build/document-service/target/document-service-*.jar app.jar
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
