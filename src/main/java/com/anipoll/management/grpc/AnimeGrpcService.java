package com.anipoll.management.grpc;

import com.anipoll.management.entity.Anime;
import com.anipoll.management.entity.Sharacter;
import com.anipoll.management.proto.anime.AnimeIdRequest;
import com.anipoll.management.proto.anime.AnimeListResponse;
import com.anipoll.management.proto.anime.AnimeMessage;
import com.anipoll.management.proto.anime.AnimeServiceGrpc;
import com.anipoll.management.proto.anime.CreateAnimeRequest;
import com.anipoll.management.proto.anime.DeleteAnimeResponse;
import com.anipoll.management.proto.anime.SharacterMessage;
import com.anipoll.management.proto.anime.UpdateAnimeRequest;
import com.anipoll.management.service.AnimeService;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@GrpcService
public class AnimeGrpcService extends AnimeServiceGrpc.AnimeServiceImplBase {

    @Inject
    AnimeService animeService;

    @Override
    public void getAnime(AnimeIdRequest request, StreamObserver<AnimeMessage> responseObserver) {
        animeService.getEntityForGrpc(request.getId())
                .map(this::toMessage)
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void listAnimes(Empty request, StreamObserver<AnimeListResponse> responseObserver) {
        animeService.listAllForGrpc()
                .map(items -> AnimeListResponse.newBuilder()
                        .addAllItems(items.stream().map(this::toMessage).toList())
                        .build())
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void createAnime(CreateAnimeRequest request, StreamObserver<AnimeMessage> responseObserver) {
        animeService.createForGrpc(request.getName(), request.getDescription())
                .flatMap(anime -> animeService.getEntityForGrpc(anime.id))
                .map(this::toMessage)
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void updateAnime(UpdateAnimeRequest request, StreamObserver<AnimeMessage> responseObserver) {
        animeService.updateForGrpc(request.getId(), request.getName(), request.getDescription())
                .map(this::toMessage)
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void deleteAnime(AnimeIdRequest request, StreamObserver<DeleteAnimeResponse> responseObserver) {
        animeService.delete(request.getId())
                .map(deleted -> DeleteAnimeResponse.newBuilder().setDeleted(deleted).build())
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    private AnimeMessage toMessage(Anime anime) {
        return AnimeMessage.newBuilder()
                .setId(anime.id == null ? 0L : anime.id)
                .setName(nullSafe(anime.name))
                .setDescription(nullSafe(anime.description))
                .addAllSharacters(anime.sharacters == null ? java.util.List.of() : anime.sharacters.stream().map(this::toSharacterMessage).toList())
                .build();
    }

    private SharacterMessage toSharacterMessage(Sharacter sharacter) {
        return SharacterMessage.newBuilder()
                .setId(sharacter.id == null ? 0L : sharacter.id)
                .setName(nullSafe(sharacter.name))
                .setDescription(nullSafe(sharacter.description))
                .setAnimeId(sharacter.anime != null && sharacter.anime.id != null ? sharacter.anime.id : 0L)
                .build();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private <T> void completeUnary(StreamObserver<T> observer, T item) {
        observer.onNext(item);
        observer.onCompleted();
    }

    private Throwable toGrpcError(Throwable failure) {
        if (failure instanceof NotFoundException) {
            return Status.NOT_FOUND.withDescription(failure.getMessage()).asRuntimeException();
        }
        return Status.INTERNAL.withDescription(failure.getMessage()).withCause(failure).asRuntimeException();
    }
}
