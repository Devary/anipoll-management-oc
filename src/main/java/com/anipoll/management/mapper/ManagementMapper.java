package com.anipoll.management.mapper;

import com.anipoll.management.dto.AnimeResponse;
import com.anipoll.management.dto.AnimeSummaryResponse;
import com.anipoll.management.dto.SharacterResponse;
import com.anipoll.management.dto.SharacterSummaryResponse;
import com.anipoll.management.entity.Anime;
import com.anipoll.management.entity.Sharacter;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ManagementMapper {

    public AnimeResponse toAnimeResponse(Anime anime) {
        AnimeResponse response = new AnimeResponse();
        response.id = anime.id;
        response.name = anime.name;
        response.description = anime.description;
        response.sharacters = anime.sharacters == null ? List.of() : anime.sharacters.stream().map(this::toSharacterSummaryResponse).toList();
        return response;
    }

    public AnimeSummaryResponse toAnimeSummaryResponse(Anime anime) {
        AnimeSummaryResponse response = new AnimeSummaryResponse();
        response.id = anime.id;
        response.name = anime.name;
        response.description = anime.description;
        return response;
    }

    public SharacterResponse toSharacterResponse(Sharacter sharacter) {
        SharacterResponse response = new SharacterResponse();
        response.id = sharacter.id;
        response.name = sharacter.name;
        response.description = sharacter.description;
        response.anime = sharacter.anime == null ? null : toAnimeSummaryResponse(sharacter.anime);
        return response;
    }

    public SharacterSummaryResponse toSharacterSummaryResponse(Sharacter sharacter) {
        SharacterSummaryResponse response = new SharacterSummaryResponse();
        response.id = sharacter.id;
        response.name = sharacter.name;
        response.description = sharacter.description;
        response.animeId = sharacter.anime == null ? null : sharacter.anime.id;
        return response;
    }
}
