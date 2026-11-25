package com.docmanager.indexing.messaging;

import com.docmanager.indexing.service.SearchIndex;
import com.docmanager.messaging.IndexMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexingListenerTest {

    @Mock
    private SearchIndex searchIndex;

    @InjectMocks
    private IndexingListener listener;

    private final UUID id = UUID.randomUUID();

    @Test
    void upsertAndDelete_areForwardedToTheIndex() {
        listener.handle(IndexMessage.upsert(id, "a.pdf", "Hello"));
        listener.handle(IndexMessage.delete(id));

        verify(searchIndex).index(id, "a.pdf", "Hello");
        verify(searchIndex).delete(id);
    }

    @Test
    void indexError_isRethrownForRetry() {
        doThrow(new IllegalStateException("ES weg")).when(searchIndex).delete(id);

        assertThatThrownBy(() -> listener.handle(IndexMessage.delete(id))).hasMessage("ES weg");
    }
}
