FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
COPY document-service/pom.xml document-service/
COPY ocr-worker/pom.xml ocr-worker/
COPY indexing-worker/pom.xml indexing-worker/
RUN mvn -B -q -pl ocr-worker -am dependency:go-offline

COPY ocr-worker/src ocr-worker/src
RUN mvn -B -q -pl ocr-worker -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

# Nicht als root laufen
RUN groupadd --system app && useradd --system --gid app --no-create-home app

RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-deu \
    tesseract-ocr-eng \
    ghostscript \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/ocr-worker/target/ocr-worker-*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
