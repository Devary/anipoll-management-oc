package com.anipoll.management.repository;

import com.anipoll.management.entity.Sharacter;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SharacterRepository implements PanacheRepository<Sharacter> {

    public Uni<List<Sharacter>> listPage(int page, int size, String name, Long animeId) {
        return findFiltered(name, animeId)
                .page(Page.of(page, size))
                .list();
    }

    public Uni<Long> countFiltered(String name, Long animeId) {
        if ((name == null || name.isBlank()) && animeId == null) {
            return count();
        }
        if (animeId == null) {
            return count("lower(name) like ?1", like(name));
        }
        if (name == null || name.isBlank()) {
            return count("anime.id = ?1", animeId);
        }
        return count("lower(name) like ?1 and anime.id = ?2", like(name), animeId);
    }

    public Uni<Sharacter> findByIdWithAnime(Long id) {
        return find("select s from Sharacter s join fetch s.anime where s.id = ?1", id)
                .firstResult();
    }

    public Uni<List<Sharacter>> listAllWithAnime() {
        return find("select s from Sharacter s join fetch s.anime order by s.id")
                .list();
    }

    public Uni<List<Sharacter>> listByAnimeIdWithAnime(Long animeId) {
        return find("select s from Sharacter s join fetch s.anime where s.anime.id = ?1 order by s.id", animeId)
                .list();
    }

    private io.quarkus.hibernate.reactive.panache.PanacheQuery<Sharacter> findFiltered(String name, Long animeId) {
        if ((name == null || name.isBlank()) && animeId == null) {
            return find("select s from Sharacter s join fetch s.anime order by s.id");
        }
        if (animeId == null) {
            return find("select s from Sharacter s join fetch s.anime where lower(s.name) like ?1 order by s.id", like(name));
        }
        if (name == null || name.isBlank()) {
            return find("select s from Sharacter s join fetch s.anime where s.anime.id = ?1 order by s.id", animeId);
        }
        return find("select s from Sharacter s join fetch s.anime where lower(s.name) like ?1 and s.anime.id = ?2 order by s.id", like(name), animeId);
    }

    private String like(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
