package com.anipoll.management.repository;

import com.anipoll.management.entity.Anime;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AnimeRepository implements PanacheRepository<Anime> {

    public Uni<List<Anime>> listPage(int page, int size, String name) {
        return findFiltered(name)
                .page(Page.of(page, size))
                .list();
    }

    public Uni<Long> countFiltered(String name) {
        if (name == null || name.isBlank()) {
            return count();
        }
        return count("lower(name) like ?1", like(name));
    }

    public Uni<Anime> findByIdWithSharacters(Long id) {
        return find("select distinct a from Anime a left join fetch a.sharacters s where a.id = ?1", id)
                .firstResult();
    }

    public Uni<List<Anime>> listAllWithSharacters() {
        return find("select distinct a from Anime a left join fetch a.sharacters order by a.id")
                .list();
    }

    private io.quarkus.hibernate.reactive.panache.PanacheQuery<Anime> findFiltered(String name) {
        if (name == null || name.isBlank()) {
            return find("order by id");
        }
        return find("lower(name) like ?1 order by id", like(name));
    }

    private String like(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
