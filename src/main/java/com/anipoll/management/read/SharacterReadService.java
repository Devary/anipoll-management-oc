package com.anipoll.management.read;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.anipoll.management.document.SharacterDocument;
import com.anipoll.management.dto.PaginatedResponse;
import com.anipoll.management.dto.SharacterResponse;
import com.anipoll.management.mapper.ElasticDocumentMapper;
import com.anipoll.management.search.SharacterIndexService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SharacterReadService {

    public static final String INDEX = "sharacters-read";

    @Inject
    ElasticsearchClient elasticsearchClient;

    @Inject
    ElasticDocumentMapper mapper;

    @Inject
    SharacterIndexService sharacterIndexService;

    public Uni<PaginatedResponse<SharacterResponse>> list(int page, int size, String name, Long animeId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        return Uni.createFrom().item(() -> {
            try {
                return search(safePage, safeSize, name, animeId);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public Uni<SharacterResponse> get(Long id) {
        return Uni.createFrom().item(() -> {
                    try {
                        return getDocument(id);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .map(mapper::toSharacterResponse);
    }

    private PaginatedResponse<SharacterResponse> search(int page, int size, String name, Long animeId) throws IOException {
        try {
            SearchResponse<SharacterDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX)
                            .from(page * size)
                            .size(size)
                            .query(buildQuery(name, animeId)),
                    SharacterDocument.class);

            List<SharacterResponse> items = response.hits().hits().stream()
                    .map(hit -> mapper.toSharacterResponse(hit.source()))
                    .toList();

            long total = response.hits().total() == null ? items.size() : response.hits().total().value();
            return new PaginatedResponse<>(items, page, size, total);
        } catch (ElasticsearchException e) {
            if (isIndexNotFound(e)) {
                sharacterIndexService.ensureIndexExists();
                return new PaginatedResponse<>(List.of(), page, size, 0);
            }
            throw e;
        }
    }

    private SharacterDocument getDocument(Long id) throws IOException {
        try {
            GetResponse<SharacterDocument> response = elasticsearchClient.get(g -> g.index(INDEX).id(String.valueOf(id)), SharacterDocument.class);
            if (!response.found() || response.source() == null) {
                throw new NotFoundException("Sharacter not found: " + id);
            }
            return response.source();
        } catch (ElasticsearchException e) {
            if (isIndexNotFound(e)) {
                sharacterIndexService.ensureIndexExists();
                throw new NotFoundException("Sharacter not found: " + id);
            }
            throw e;
        }
    }

    private Query buildQuery(String name, Long animeId) {
        List<Query> filters = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            filters.add(Query.of(q -> q.bool(b -> b
                    .should(s -> s.match(m -> m.field("name").query(name)))
                    .should(s -> s.match(m -> m.field("description").query(name)))
                    .minimumShouldMatch("1"))));
        }
        if (animeId != null) {
            filters.add(Query.of(q -> q.term(t -> t.field("anime.id").value(animeId))));
        }
        if (filters.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.bool(b -> b.filter(filters)));
    }

    private boolean isIndexNotFound(ElasticsearchException e) {
        return e.response() != null
                && e.response().error() != null
                && "index_not_found_exception".equals(e.response().error().type());
    }
}
