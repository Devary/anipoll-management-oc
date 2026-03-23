package com.anipoll.management.read;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.anipoll.management.document.AnimeDocument;
import com.anipoll.management.dto.AnimeResponse;
import com.anipoll.management.dto.PaginatedResponse;
import com.anipoll.management.mapper.ElasticDocumentMapper;
import com.anipoll.management.search.AnimeIndexService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@ApplicationScoped
public class AnimeReadService {

    public static final String INDEX = "animes-read";

    @Inject
    ElasticsearchClient elasticsearchClient;

    @Inject
    ElasticDocumentMapper mapper;

    @Inject
    AnimeIndexService animeIndexService;

    public Uni<PaginatedResponse<AnimeResponse>> list(int page, int size, String name) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        return Uni.createFrom().item(() -> {
            try {
                return search(safePage, safeSize, name);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public Uni<AnimeResponse> get(Long id) {
        return Uni.createFrom().item(() -> {
                    try {
                        return getDocument(id);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .map(mapper::toAnimeResponse);
    }

    private PaginatedResponse<AnimeResponse> search(int page, int size, String name) throws IOException {
        try {
            SearchResponse<AnimeDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX)
                            .from(page * size)
                            .size(size)
                            .query(buildNameQuery(name)),
                    AnimeDocument.class);

            List<AnimeResponse> items = response.hits().hits().stream()
                    .map(hit -> mapper.toAnimeResponse(hit.source()))
                    .toList();

            long total = response.hits().total() == null ? items.size() : response.hits().total().value();
            return new PaginatedResponse<>(items, page, size, total);
        } catch (ElasticsearchException e) {
            if (isIndexNotFound(e)) {
                animeIndexService.ensureIndexExists();
                return new PaginatedResponse<>(List.of(), page, size, 0);
            }
            throw e;
        }
    }

    private AnimeDocument getDocument(Long id) throws IOException {
        try {
            GetResponse<AnimeDocument> response = elasticsearchClient.get(g -> g.index(INDEX).id(String.valueOf(id)), AnimeDocument.class);
            if (!response.found() || response.source() == null) {
                throw new NotFoundException("Anime not found: " + id);
            }
            return response.source();
        } catch (ElasticsearchException e) {
            if (isIndexNotFound(e)) {
                animeIndexService.ensureIndexExists();
                throw new NotFoundException("Anime not found: " + id);
            }
            throw e;
        }
    }

    private Query buildNameQuery(String name) {
        if (name == null || name.isBlank()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.bool(b -> b
                .should(s -> s.match(m -> m.field("name").query(name)))
                .should(s -> s.match(m -> m.field("description").query(name)))
                .minimumShouldMatch("1")));
    }

    private boolean isIndexNotFound(ElasticsearchException e) {
        return e.response() != null
                && e.response().error() != null
                && "index_not_found_exception".equals(e.response().error().type());
    }
}
