package com.github.devoxx.university.server;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.nio.charset.Charset;
import java.util.Objects;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

public class RxNettyServer {

    private final HttpServer<ByteBuf, ByteBuf> httpServer;

    public static void main(String[] args) {
        new RxNettyServer(8080).registerShutdownHook().startAndWait();

    }

    public RxNettyServer(int port) {
        httpServer =
                RxNetty.newHttpServerBuilder(
                        port,
                        new RequestHandler<ByteBuf, ByteBuf>() {
                            @Override
                            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                                if (!Objects.equals(request.getPath(), "/")) {
                                    response.setStatus(HttpResponseStatus.NOT_FOUND);
                                    return response.close(false);
                                }
                                if (request.getHttpMethod() != HttpMethod.POST) {
                                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                                    return response.close(false);
                                }

                                return request.getContent()
                                          .map(bb -> {
                                                  String body = bb.toString(Charset.forName("UTF-8"));
                                                  return Integer.valueOf(body.substring("body=".length()));
                                              })
                                          .map(RxNettyServer::longStuff)
                                          .flatMap(result -> {
                                                  response.setStatus(HttpResponseStatus.OK);
                                                  response.writeString(result);
                                                  return response.close();
                                              }).ignoreElements();
                            }
                        })
                       .pipelineConfigurator(PipelineConfigurators.httpServerConfigurator())
                       .withRequestProcessingThreads(100) // enable more worker thread => for heavy computing
                       .build();
    }

    public void startAndWait() {
        System.out.println("Starting Server");
        httpServer.startAndWait();
    }

    public void start() {
        System.out.println("Starting Server");
        httpServer.start();
    }

    public void stop() throws InterruptedException {
        System.out.println("Stopping server");
        httpServer.shutdown();
    }

    public RxNettyServer registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    RxNettyServer.this.stop();
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
        });
        return this;
    }


    private static String longStuff(int waitTime) {
        try {
            MILLISECONDS.sleep(waitTime);
            return format("Worked for %dms", waitTime);
        } catch (InterruptedException ignored) {
            Thread.interrupted();
            return "got interrupted";
        }
    }

}
