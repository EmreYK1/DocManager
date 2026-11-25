package com.docmanager.document;

import com.docmanager.messaging.IndexMessage;
import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.OcrRequestMessage;
import com.docmanager.messaging.OcrResultMessage;
import com.docmanager.messaging.SummaryRequestMessage;
import com.docmanager.messaging.SummaryResultMessage;
import com.fasterxml.jackson.databind.JsonNode;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Der Weg eines Dokuments durch das echte Backend mit PostgreSQL, RabbitMQ und MinIO. Der Test spielt
 * selbst den ocr-worker und den genai-worker und liest mit, was der indexing-worker bekäme. Braucht Docker.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentFlowIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("cgr.dev/chainguard/minio:latest")
            .withCommand("server", "/data")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    @DynamicPropertySource
    static void minioUrl(DynamicPropertyRegistry registry) {
        registry.add("app.minio.url", () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    }

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private MinioClient minio;

    @Test
    void documentLifecycle() {
        String folderId = post("/folders", Map.of("name", "Ablage")).get("id").asText();
        String id = upload(folderId);

        OcrRequestMessage request = receive(MessagingTopology.OCR_REQUEST_QUEUE, new ParameterizedTypeReference<>() {});
        assertThat(request.documentId()).hasToString(id);

        // Ergebnis kommt doppelt an: Es darf nur einmal wirken
        OcrResultMessage result = OcrResultMessage.succeeded(UUID.fromString(id), "Hello World");
        rabbit.convertAndSend(MessagingTopology.EXCHANGE, MessagingTopology.OCR_RESULT_KEY, result);
        rabbit.convertAndSend(MessagingTopology.EXCHANGE, MessagingTopology.OCR_RESULT_KEY, result);
        await().untilAsserted(() ->
                assertThat(get("/documents/" + id).get("status").asText()).isEqualTo("OCR_DONE"));
        assertThat(nextIndexMessage()).isEqualTo(IndexMessage.upsert(request.documentId(), "a.pdf", "Hello World"));

        SummaryRequestMessage summaryRequest =
                receive(MessagingTopology.SUMMARY_REQUEST_QUEUE, new ParameterizedTypeReference<>() {});
        assertThat(summaryRequest).isEqualTo(new SummaryRequestMessage(request.documentId(), "Hello World"));

        rabbit.convertAndSend(
                MessagingTopology.EXCHANGE,
                MessagingTopology.SUMMARY_RESULT_KEY,
                SummaryResultMessage.succeeded(request.documentId(), "Kurzfassung"));
        await().untilAsserted(() ->
                assertThat(get("/documents/" + id).get("status").asText()).isEqualTo("SUMMARY_DONE"));
        assertThat(get("/documents/" + id).get("summary").asText()).isEqualTo("Kurzfassung");

        patch(id, Map.of("filename", "b.pdf"));
        assertThat(nextIndexMessage().filename()).isEqualTo("b.pdf");

        // Lazy-Beziehungen funktionieren ohne open-in-view, ein belegter Ordner bleibt bestehen
        assertThat(get("/documents").get(0).get("folderId").asText()).isEqualTo(folderId);
        post("/documents/" + id + "/comments", Map.of("author", "Max", "content", "Gut"));
        assertThat(get("/documents/" + id + "/comments")
                        .get(0)
                        .get("documentId")
                        .asText())
                .isEqualTo(id);
        assertThat(delete("/folders/" + folderId)).isEqualTo(HttpStatus.CONFLICT);

        assertThat(delete("/documents/" + id)).isEqualTo(HttpStatus.OK);
        assertThat(nextIndexMessage()).isEqualTo(IndexMessage.delete(request.documentId()));
        assertThat(minio.listObjects(
                        ListObjectsArgs.builder().bucket("documents").build()))
                .isEmpty();
        assertThat(delete("/folders/" + folderId)).isEqualTo(HttpStatus.OK);
    }

    private String upload(String folderId) {
        var form = new LinkedMultiValueMap<String, Object>();
        form.add("file", new ByteArrayResource("%PDF-1.4".getBytes()) {
            @Override
            public String getFilename() {
                return "a.pdf";
            }
        });
        form.add("folderId", folderId);
        return http.postForObject("/documents/upload", form, JsonNode.class)
                .get("id")
                .asText();
    }

    private JsonNode get(String path) {
        return http.getForObject(path, JsonNode.class);
    }

    private JsonNode post(String path, Object body) {
        return http.postForObject(path, body, JsonNode.class);
    }

    private void patch(String id, Object body) {
        // Der Standard-Client von TestRestTemplate kann kein PATCH
        http.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        http.patchForObject("/documents/" + id, body, JsonNode.class);
    }

    private HttpStatus delete(String path) {
        return HttpStatus.valueOf(http.exchange(path, HttpMethod.DELETE, null, String.class)
                .getStatusCode()
                .value());
    }

    private IndexMessage nextIndexMessage() {
        return receive(MessagingTopology.INDEX_QUEUE, new ParameterizedTypeReference<>() {});
    }

    private <T> T receive(String queue, ParameterizedTypeReference<T> type) {
        return rabbit.receiveAndConvert(queue, Duration.ofSeconds(15).toMillis(), type);
    }
}
