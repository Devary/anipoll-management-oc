package com.anipoll.management.mapper;

import com.anipoll.management.document.AnimeDocument;
import com.anipoll.management.document.AnimeSummaryDocument;
import com.anipoll.management.document.SharacterDocument;
import com.anipoll.management.document.SharacterSummaryDocument;
import com.anipoll.management.dto.AnimeResponse;
import com.anipoll.management.dto.AnimeSummaryResponse;
import com.anipoll.management.dto.SharacterResponse;
import com.anipoll.management.dto.SharacterSummaryResponse;
import com.anipoll.management.entity.Anime;
import com.anipoll.management.entity.Sharacter;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ElasticDocumentMapper {

    public AnimeDocument toAnimeDocument(Anime anime) {
        AnimeDocument document = new AnimeDocument();
        document.id = anime.id;
        document.name = anime.name;
        document.description = anime.description;
        document.sharacters = anime.sharacters == null ? List.of() : anime.sharacters.stream().map(this::toSharacterSummaryDocument).toList();
        return document;
    }

    public SharacterDocument toSharacterDocument(Sharacter sharacter) {
        SharacterDocument document = new SharacterDocument();
        document.id = sharacter.id;
        document.name = sharacter.name;
        document.description = sharacter.description;
        document.anime = sharacter.anime == null ? null : toAnimeSummaryDocument(sharacter.anime);
        return document;
    }

    public AnimeResponse toAnimeResponse(AnimeDocument document) {
        AnimeResponse response = new AnimeResponse();
        response.id = document.id;
        response.name = document.name;
        response.description = document.description;
        response.sharacters = document.sharacters == null ? List.of() : document.sharacters.stream().map(this::toSharacterSummaryResponse).toList();
        return response;
    }

    public SharacterResponse toSharacterResponse(SharacterDocument document) {
        SharacterResponse response = new SharacterResponse();
        response.id = document.id;
        response.name = document.name;
        response.description = document.description;
        response.anime = document.anime == null ? null : toAnimeSummaryResponse(document.anime);
        return response;
    }

    private AnimeSummaryDocument toAnimeSummaryDocument(Anime anime) {
        AnimeSummaryDocument document = new AnimeSummaryDocument();
        document.id = anime.id;
        document.name = anime.name;
        document.description = anime.description;
        return document;
    }

    private SharacterSummaryDocument toSharacterSummaryDocument(Sharacter sharacter) {
        SharacterSummaryDocument document = new SharacterSummaryDocument();
        document.id = sharacter.id;
        document.name = sharacter.name;
        document.description = sharacter.description;
        document.animeId = sharacter.anime == null ? null : sharacter.anime.id;
        return document;
    }

    private AnimeSummaryResponse toAnimeSummaryResponse(AnimeSummaryDocument document) {
        AnimeSummaryResponse response = new AnimeSummaryResponse();
        response.id = document.id;
        response.name = document.name;
        response.description = document.description;
        return response;
    }

    private SharacterSummaryResponse toSharacterSummaryResponse(SharacterSummaryDocument document) {
        SharacterSummaryResponse response = new SharacterSummaryResponse();
        response.id = document.id;
        response.name = document.name;
        response.description = document.description;
        response.animeId = document.animeId;
        return response;
    }
}
