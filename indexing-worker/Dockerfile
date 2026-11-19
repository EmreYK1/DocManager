FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
COPY document-service/pom.xml document-service/
COPY ocr-worker/pom.xml ocr-worker/
COPY indexing-worker/pom.xml indexing-worker/
RUN mvn -B -q -pl indexing-worker -am dependency:go-offline

COPY indexing-worker/src indexing-worker/src
RUN mvn -B -q -pl indexing-worker -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/indexing-worker/target/indexing-worker-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
