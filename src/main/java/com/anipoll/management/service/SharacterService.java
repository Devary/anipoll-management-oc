package com.anipoll.management.service;

import com.anipoll.management.dto.PaginatedResponse;
import com.anipoll.management.dto.SharacterRequest;
import com.anipoll.management.dto.SharacterResponse;
import com.anipoll.management.entity.Sharacter;
import com.anipoll.management.mapper.ManagementMapper;
import com.anipoll.management.read.SharacterReadService;
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
public class SharacterService {

    @Inject
    SharacterRepository sharacterRepository;

    @Inject
    AnimeRepository animeRepository;

    @Inject
    ManagementMapper mapper;

    @Inject
    SharacterReadService sharacterReadService;

    @Inject
    SharacterIndexService sharacterIndexService;

    @Inject
    AnimeIndexService animeIndexService;

    public Uni<PaginatedResponse<SharacterResponse>> list(int page, int size, String name, Long animeId) {
        return sharacterReadService.list(page, size, name, animeId);
    }

    public Uni<SharacterResponse> get(Long id) {
        return sharacterReadService.get(id);
    }

    @WithTransaction
    public Uni<SharacterResponse> create(SharacterRequest request) {
        return animeRepository.findById(request.animeId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + request.animeId))
                .flatMap(anime -> {
                    Sharacter sharacter = new Sharacter();
                    sharacter.name = request.name;
                    sharacter.description = request.description;
                    sharacter.anime = anime;
                    return sharacterRepository.persistAndFlush(sharacter).replaceWith(sharacter);
                })
                .flatMap(sharacter -> sharacterRepository.findByIdWithAnime(sharacter.id))
                .flatMap(sharacter -> sharacterIndexService.index(sharacter)
                        .flatMap(ignored -> synchronizeParentAnime(sharacter).replaceWith(sharacter)))
                .map(mapper::toSharacterResponse);
    }

    @WithTransaction
    public Uni<SharacterResponse> update(Long id, SharacterRequest request) {
        return sharacterRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Sharacter not found: " + id))
                .flatMap(sharacter -> animeRepository.findById(request.animeId)
                        .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + request.animeId))
                        .invoke(anime -> {
                            sharacter.name = request.name;
                            sharacter.description = request.description;
                            sharacter.anime = anime;
                        })
                        .replaceWith(sharacter))
                .flatMap(sharacter -> sharacterRepository.flush().replaceWith(sharacter))
                .flatMap(sharacter -> sharacterRepository.findByIdWithAnime(sharacter.id))
                .flatMap(sharacter -> sharacterIndexService.index(sharacter)
                        .flatMap(ignored -> synchronizeParentAnime(sharacter).replaceWith(sharacter)))
                .map(mapper::toSharacterResponse);
    }

    @WithTransaction
    public Uni<Boolean> delete(Long id) {
        return sharacterRepository.findByIdWithAnime(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Sharacter not found: " + id))
                .flatMap(sharacter -> {
                    Long animeId = sharacter.anime != null ? sharacter.anime.id : null;
                    return sharacterRepository.deleteById(id)
                            .flatMap(deleted -> sharacterIndexService.delete(id)
                                    .flatMap(ignored -> animeId == null ? Uni.createFrom().item(deleted) : synchronizeAnimeById(animeId).replaceWith(deleted)));
                });
    }

    public Uni<List<Sharacter>> listAllForGrpc() {
        return sharacterRepository.listAllWithAnime();
    }

    public Uni<Sharacter> getEntityForGrpc(Long id) {
        return sharacterRepository.findByIdWithAnime(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Sharacter not found: " + id));
    }

    @WithTransaction
    public Uni<Sharacter> createForGrpc(String name, String description, Long animeId) {
        return animeRepository.findById(animeId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + animeId))
                .flatMap(anime -> {
                    Sharacter sharacter = new Sharacter();
                    sharacter.name = name;
                    sharacter.description = description;
                    sharacter.anime = anime;
                    return sharacterRepository.persistAndFlush(sharacter).replaceWith(sharacter);
                })
                .flatMap(sharacter -> sharacterRepository.findByIdWithAnime(sharacter.id))
                .flatMap(sharacter -> sharacterIndexService.index(sharacter)
                        .flatMap(ignored -> synchronizeParentAnime(sharacter).replaceWith(sharacter)));
    }

    @WithTransaction
    public Uni<Sharacter> updateForGrpc(Long id, String name, String description, Long animeId) {
        return sharacterRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Sharacter not found: " + id))
                .flatMap(sharacter -> animeRepository.findById(animeId)
                        .onItem().ifNull().failWith(() -> new NotFoundException("Anime not found: " + animeId))
                        .invoke(anime -> {
                            sharacter.name = name;
                            sharacter.description = description;
                            sharacter.anime = anime;
                        })
                        .replaceWith(sharacter))
                .flatMap(sharacter -> sharacterRepository.flush().replaceWith(sharacter))
                .flatMap(sharacter -> sharacterRepository.findByIdWithAnime(sharacter.id))
                .flatMap(sharacter -> sharacterIndexService.index(sharacter)
                        .flatMap(ignored -> synchronizeParentAnime(sharacter).replaceWith(sharacter)));
    }

    private Uni<Void> synchronizeParentAnime(Sharacter sharacter) {
        return synchronizeAnimeById(sharacter.anime == null ? null : sharacter.anime.id);
    }

    private Uni<Void> synchronizeAnimeById(Long animeId) {
        if (animeId == null) {
            return Uni.createFrom().voidItem();
        }
        return animeRepository.findByIdWithSharacters(animeId)
                .flatMap(anime -> anime == null ? Uni.createFrom().voidItem() : animeIndexService.index(anime));
    }
}
