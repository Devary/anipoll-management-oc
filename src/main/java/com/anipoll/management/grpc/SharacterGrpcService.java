package com.anipoll.management.grpc;

import com.anipoll.management.entity.Anime;
import com.anipoll.management.entity.Sharacter;
import com.anipoll.management.proto.sharacter.AnimeRefMessage;
import com.anipoll.management.proto.sharacter.CreateSharacterRequest;
import com.anipoll.management.proto.sharacter.DeleteSharacterResponse;
import com.anipoll.management.proto.sharacter.SharacterIdRequest;
import com.anipoll.management.proto.sharacter.SharacterListResponse;
import com.anipoll.management.proto.sharacter.SharacterMessage;
import com.anipoll.management.proto.sharacter.SharacterServiceGrpc;
import com.anipoll.management.proto.sharacter.UpdateSharacterRequest;
import com.anipoll.management.service.SharacterService;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@GrpcService
public class SharacterGrpcService extends SharacterServiceGrpc.SharacterServiceImplBase {

    @Inject
    SharacterService sharacterService;

    @Override
    public void getSharacter(SharacterIdRequest request, StreamObserver<SharacterMessage> responseObserver) {
        sharacterService.getEntityForGrpc(request.getId())
                .map(this::toMessage)
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void listSharacters(Empty request, StreamObserver<SharacterListResponse> responseObserver) {
        sharacterService.listAllForGrpc()
                .map(items -> SharacterListResponse.newBuilder()
                        .addAllItems(items.stream().map(this::toMessage).toList())
                        .build())
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void createSharacter(CreateSharacterRequest request, StreamObserver<SharacterMessage> responseObserver) {
        sharacterService.createForGrpc(request.getName(), request.getDescription(), request.getAnimeId())
                .map(this::toMessage)
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void updateSharacter(UpdateSharacterRequest request, StreamObserver<SharacterMessage> responseObserver) {
        sharacterService.updateForGrpc(request.getId(), request.getName(), request.getDescription(), request.getAnimeId())
                .map(this::toMessage)
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    @Override
    public void deleteSharacter(SharacterIdRequest request, StreamObserver<DeleteSharacterResponse> responseObserver) {
        sharacterService.delete(request.getId())
                .map(deleted -> DeleteSharacterResponse.newBuilder().setDeleted(deleted).build())
                .subscribe().with(
                        item -> completeUnary(responseObserver, item),
                        failure -> responseObserver.onError(toGrpcError(failure))
                );
    }

    private SharacterMessage toMessage(Sharacter sharacter) {
        return SharacterMessage.newBuilder()
                .setId(sharacter.id == null ? 0L : sharacter.id)
                .setName(nullSafe(sharacter.name))
                .setDescription(nullSafe(sharacter.description))
                .setAnime(toAnimeRef(sharacter.anime))
                .build();
    }

    private AnimeRefMessage toAnimeRef(Anime anime) {
        if (anime == null) {
            return AnimeRefMessage.newBuilder().build();
        }
        return AnimeRefMessage.newBuilder()
                .setId(anime.id == null ? 0L : anime.id)
                .setName(nullSafe(anime.name))
                .setDescription(nullSafe(anime.description))
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
