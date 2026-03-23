package com.anipoll.management.service;

import com.anipoll.management.dto.AnimeRequest;
import com.anipoll.management.dto.AnimeResponse;
import com.anipoll.management.dto.PaginatedResponse;
import com.anipoll.management.entity.Anime;
import com.anipoll.management.mapper.ManagementMapper;
import com.anipoll.management.read.AnimeReadService;
import com.anipoll.management.repository.AnimeRepository;
import com.anipoll.management.repository.SharacterRepository;
import com.anipoll.management.search.AnimeIndexService;
import com.anipoll.management.search.SharacterIndexService;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class AnimeService {

    @Inject
    AnimeRepository animeRepository;

    @Inject
    SharacterRepository sharacterRepository;

    @Inject
    ManagementMapper mapper;

    @Inject
    AnimeReadService animeReadService;

    @Inject
    AnimeIndexService animeIndexService;

    @Inject
    SharacterIndexService sharacterIndexService;

    public Uni<PaginatedResponse<AnimeResponse>> list(int page, int size, String name) {
        return animeReadService.list(page, size, name);
    }

    public Uni<AnimeResponse> get(Long id) {
        return animeReadService.get(id);
    }

    @WithTransaction
    public Uni<AnimeResponse> create(AnimeRequest request) {
        Anime anime = new Anime();
        anime.name = request.name;
        anime.description = request.description;
        return animeRepository.persistAndFlush(anime)
                .replaceWith(anime)
                .flatMap(saved -> animeRepository.findByIdWithSharacters(saved.id))
                .flatMap(saved -> animeIndexService.index(saved).replaceWith(saved))
                .map(mapper::toAnimeResponse);
    }

    @WithTransaction
    public Uni<AnimeResponse> update(Long id, AnimeRequest request) {
        return animeRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + id))
                .invoke(anime -> {
                    anime.name = request.name;
                    anime.description = request.description;
                })
                .flatMap(anime -> animeRepository.flush().replaceWith(anime))
                .flatMap(anime -> animeRepository.findByIdWithSharacters(anime.id))
                .flatMap(anime -> animeIndexService.index(anime)
                        .flatMap(ignored -> synchronizeSharactersForAnime(anime.id).replaceWith(anime)))
                .map(mapper::toAnimeResponse);
    }

    @WithTransaction
    public Uni<Boolean> delete(Long id) {
        return animeRepository.findByIdWithSharacters(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + id))
                .flatMap(anime -> animeRepository.deleteById(id)
                        .flatMap(deleted -> animeIndexService.delete(id).replaceWith(deleted)));
    }

    public Uni<List<Anime>> listAllForGrpc() {
        return animeRepository.listAllWithSharacters();
    }

    public Uni<Anime> getEntityForGrpc(Long id) {
        return animeRepository.findByIdWithSharacters(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + id));
    }

    @WithTransaction
    public Uni<Anime> createForGrpc(String name, String description) {
        Anime anime = new Anime();
        anime.name = name;
        anime.description = description;
        return animeRepository.persistAndFlush(anime)
                .replaceWith(anime)
                .flatMap(saved -> animeRepository.findByIdWithSharacters(saved.id))
                .flatMap(saved -> animeIndexService.index(saved).replaceWith(saved));
    }

    @WithTransaction
    public Uni<Anime> updateForGrpc(Long id, String name, String description) {
        return animeRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + id))
                .invoke(anime -> {
                    anime.name = name;
                    anime.description = description;
                })
                .flatMap(anime -> animeRepository.flush().replaceWith(anime))
                .flatMap(anime -> animeRepository.findByIdWithSharacters(anime.id))
                .flatMap(anime -> animeIndexService.index(anime)
                        .flatMap(ignored -> synchronizeSharactersForAnime(anime.id).replaceWith(anime)));
    }

    private Uni<Void> synchronizeSharactersForAnime(Long animeId) {
        return sharacterRepository.listByAnimeIdWithAnime(animeId)
                .flatMap(items -> {
                    Uni<Void> chain = Uni.createFrom().voidItem();
                    for (var sharacter : items) {
                        chain = chain.flatMap(ignored -> sharacterIndexService.index(sharacter));
                    }
                    return chain;
                });
    }
}
