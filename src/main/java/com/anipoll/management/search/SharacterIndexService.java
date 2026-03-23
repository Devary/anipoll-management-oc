package com.anipoll.management.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.anipoll.management.entity.Sharacter;
import com.anipoll.management.mapper.ElasticDocumentMapper;
import com.anipoll.management.read.SharacterReadService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UncheckedIOException;

@ApplicationScoped
public class SharacterIndexService {

    @Inject
    ElasticsearchClient elasticsearchClient;

    @Inject
    ElasticDocumentMapper mapper;

    @PostConstruct
    void init() {
        ensureIndexExists();
    }

    public Uni<Void> index(Sharacter sharacter) {
        return Uni.createFrom().item(() -> {
            try {
                elasticsearchClient.index(i -> i.index(SharacterReadService.INDEX)
                        .id(String.valueOf(sharacter.id))
                        .document(mapper.toSharacterDocument(sharacter)));
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public Uni<Void> delete(Long id) {
        return Uni.createFrom().item(() -> {
            try {
                elasticsearchClient.delete(d -> d.index(SharacterReadService.INDEX).id(String.valueOf(id)));
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void ensureIndexExists() {
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(SharacterReadService.INDEX)).value();
            if (!exists) {
                elasticsearchClient.indices().create(c -> c
                        .index(SharacterReadService.INDEX)
                        .settings(IndexSettings.of(s -> s.numberOfShards("1").numberOfReplicas("0"))));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize sharacter read index", e);
        }
    }
}
