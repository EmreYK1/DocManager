FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Alle POMs, weil Maven das ganze Projekt einliest, aber nur die benötigten Quellen
COPY pom.xml .
COPY messaging-contract/pom.xml messaging-contract/
COPY document-service/pom.xml document-service/
COPY ocr-worker/pom.xml ocr-worker/
COPY indexing-worker/pom.xml indexing-worker/
COPY genai-worker/pom.xml genai-worker/
COPY messaging-contract/src messaging-contract/src
COPY ocr-worker/src ocr-worker/src
# Das Maven-Repository bleibt zwischen den Builds im Cache
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl ocr-worker -am package -DskipTests

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
