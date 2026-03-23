package com.anipoll.management.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.anipoll.management.entity.Anime;
import com.anipoll.management.mapper.ElasticDocumentMapper;
import com.anipoll.management.read.AnimeReadService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UncheckedIOException;

@ApplicationScoped
public class AnimeIndexService {

    @Inject
    ElasticsearchClient elasticsearchClient;

    @Inject
    ElasticDocumentMapper mapper;

    @PostConstruct
    void init() {
        ensureIndexExists();
    }

    public Uni<Void> index(Anime anime) {
        return Uni.createFrom().item(() -> {
            try {
                elasticsearchClient.index(i -> i.index(AnimeReadService.INDEX)
                        .id(String.valueOf(anime.id))
                        .document(mapper.toAnimeDocument(anime)));
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public Uni<Void> delete(Long id) {
        return Uni.createFrom().item(() -> {
            try {
                elasticsearchClient.delete(d -> d.index(AnimeReadService.INDEX).id(String.valueOf(id)));
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void ensureIndexExists() {
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(AnimeReadService.INDEX)).value();
            if (!exists) {
                elasticsearchClient.indices().create(c -> c
                        .index(AnimeReadService.INDEX)
                        .settings(IndexSettings.of(s -> s.numberOfShards("1").numberOfReplicas("0"))));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize anime read index", e);
        }
    }
}
