package com.anipoll.management.resource;

import com.anipoll.management.dto.AnimeRequest;
import com.anipoll.management.dto.AnimeResponse;
import com.anipoll.management.dto.DeleteResponse;
import com.anipoll.management.dto.PaginatedResponse;
import com.anipoll.management.service.AnimeService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/animes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Anime", description = "CRUD operations for anime management")
public class AnimeResource {

    @Inject
    AnimeService animeService;

    @GET
    @Operation(summary = "List animes", description = "Returns paginated anime records. Supports filtering by name.")
    @APIResponse(responseCode = "200", description = "Anime page returned", content = @Content(schema = @Schema(implementation = PaginatedResponse.class)))
    public Uni<PaginatedResponse<AnimeResponse>> list(
            @Parameter(description = "Zero-based page index") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("10") int size,
            @Parameter(description = "Optional name filter") @QueryParam("name") String name) {
        return animeService.list(page, size, name);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get anime by id", description = "Returns a single anime by its identifier.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Anime found", content = @Content(schema = @Schema(implementation = AnimeResponse.class))),
            @APIResponse(responseCode = "404", description = "Anime not found")
    })
    public Uni<AnimeResponse> get(@PathParam("id") Long id) {
        return animeService.get(id);
    }

    @POST
    @Operation(summary = "Create anime", description = "Creates a new anime record.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Anime created", content = @Content(schema = @Schema(implementation = AnimeResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid request")
    })
    public Uni<AnimeResponse> create(AnimeRequest request) {
        return animeService.create(request);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update anime", description = "Updates an existing anime record.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Anime updated", content = @Content(schema = @Schema(implementation = AnimeResponse.class))),
            @APIResponse(responseCode = "404", description = "Anime not found")
    })
    public Uni<AnimeResponse> update(@PathParam("id") Long id, AnimeRequest request) {
        return animeService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete anime", description = "Deletes an anime by its identifier.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Deletion result returned", content = @Content(schema = @Schema(implementation = DeleteResponse.class))),
            @APIResponse(responseCode = "404", description = "Anime not found")
    })
    public Uni<DeleteResponse> delete(@PathParam("id") Long id) {
        return animeService.delete(id).map(DeleteResponse::new);
    }
}
