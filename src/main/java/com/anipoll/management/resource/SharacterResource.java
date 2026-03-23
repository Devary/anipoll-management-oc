package com.anipoll.management.resource;

import com.anipoll.management.dto.DeleteResponse;
import com.anipoll.management.dto.PaginatedResponse;
import com.anipoll.management.dto.SharacterRequest;
import com.anipoll.management.dto.SharacterResponse;
import com.anipoll.management.service.SharacterService;
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

@Path("/api/sharacters")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sharacter", description = "CRUD operations for sharacter management")
public class SharacterResource {

    @Inject
    SharacterService sharacterService;

    @GET
    @Operation(summary = "List sharacters", description = "Returns paginated sharacter records. Supports filtering by name and animeId.")
    @APIResponse(responseCode = "200", description = "Sharacter page returned", content = @Content(schema = @Schema(implementation = PaginatedResponse.class)))
    public Uni<PaginatedResponse<SharacterResponse>> list(
            @Parameter(description = "Zero-based page index") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("10") int size,
            @Parameter(description = "Optional name filter") @QueryParam("name") String name,
            @Parameter(description = "Optional anime id filter") @QueryParam("animeId") Long animeId) {
        return sharacterService.list(page, size, name, animeId);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get sharacter by id", description = "Returns a single sharacter by its identifier.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Sharacter found", content = @Content(schema = @Schema(implementation = SharacterResponse.class))),
            @APIResponse(responseCode = "404", description = "Sharacter not found")
    })
    public Uni<SharacterResponse> get(@PathParam("id") Long id) {
        return sharacterService.get(id);
    }

    @POST
    @Operation(summary = "Create sharacter", description = "Creates a new sharacter and links it to an anime.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Sharacter created", content = @Content(schema = @Schema(implementation = SharacterResponse.class))),
            @APIResponse(responseCode = "404", description = "Referenced anime not found")
    })
    public Uni<SharacterResponse> create(SharacterRequest request) {
        return sharacterService.create(request);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update sharacter", description = "Updates an existing sharacter and its anime relation.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Sharacter updated", content = @Content(schema = @Schema(implementation = SharacterResponse.class))),
            @APIResponse(responseCode = "404", description = "Sharacter or anime not found")
    })
    public Uni<SharacterResponse> update(@PathParam("id") Long id, SharacterRequest request) {
        return sharacterService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete sharacter", description = "Deletes a sharacter by its identifier.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Deletion result returned", content = @Content(schema = @Schema(implementation = DeleteResponse.class))),
            @APIResponse(responseCode = "404", description = "Sharacter not found")
    })
    public Uni<DeleteResponse> delete(@PathParam("id") Long id) {
        return sharacterService.delete(id).map(DeleteResponse::new);
    }
}
