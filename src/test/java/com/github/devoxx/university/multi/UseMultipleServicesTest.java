package com.github.devoxx.university.multi;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Test;
import com.github.devoxx.university.server.JaxRsServer;
import com.github.devoxx.university.server.RxNettyServer;
import rx.Observable;
import rx.schedulers.Schedulers;

public class UseMultipleServicesTest {

    JaxRsServer jaxRsServer = new JaxRsServer(8080).application(WaitApp.class).start();
    RxNettyServer rxNettyServer = new RxNettyServer(8081).start();
    JaxRsServer composedServer = new JaxRsServer(8082).application(ComposeApp.class).start();


    @Test
    public void do_simple_jaxrs_http() {
        Response response = waitWS(20);

        assertEquals(200, response.getStatus());
        assertEquals("Waited 20ms", response.readEntity(String.class));
    }

    @Test
    public void do_simple_rxnetty_http() {
        Response response = workWS(20);

        assertEquals(200, response.getStatus());
        assertEquals("Worked for 20ms", response.readEntity(String.class));
    }

    @Test
    public void compose_http() {
        Response response = ClientBuilder.newClient()
                                         .target("http://localhost:8082")
                                         .path("undertow/jax-rs/latest/numbers")
                                         .request()
                                         .get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    private static Response workWS(Integer workIndex) {
        return ClientBuilder.newClient()
                            .target("http://localhost:8081")
                            .resolveTemplate("wait_time", 20)
                            .request()
                            .post(Entity.entity(new Form("work", workIndex.toString()), MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    private static Response waitWS(Integer waitTime) {
        return ClientBuilder.newClient()
                            .target("http://localhost:8080")
                            .path("undertow/jax-rs/wait/{wait_time}")
                            .resolveTemplate("wait_time", waitTime)
                            .request()
                            .get();
    }

    @After
    public void stop_servers() throws Exception {
        jaxRsServer.stop();
        rxNettyServer.stop();
        composedServer.stop();
    }


    @ApplicationPath("/jax-rs/*")
    private static class ComposeApp extends ResourceConfig {
        public ComposeApp() {
            registerClasses(ComposedResource.class);
        }

        @Path("latest/numbers")
        public static class ComposedResource {
            @GET
            public void wait(@Suspended final AsyncResponse response) {
                Observable.fromCallable(() -> new SecureRandom().nextInt(1000))
                          .doOnNext(n -> System.out.printf("For number %d%n", n))
                          .flatMap(lastNumber -> {
                              long start = System.nanoTime();
                              Observable<String> waitResult = Observable.fromCallable(() -> waitWS(20))
                                                                        .subscribeOn(Schedulers.io())
                                                                        .flatMap(r -> {
                                                                            if (r.getStatus() != 200) {
                                                                                return Observable.error(new MandatoryServiceUnavailable("wait", Status.SERVICE_UNAVAILABLE));
                                                                            }
                                                                            return Observable.just(r.readEntity(String.class));
                                                                        })
                                                                        .retry(2);
                              Observable<String> workResult = Observable.fromCallable(() -> workWS(lastNumber))
                                                                        .subscribeOn(Schedulers.io())
                                                                        .flatMap(r -> {
                                                                            if (r.getStatus() != 200) {
                                                                                return Observable.error(new MandatoryServiceUnavailable("work", Status.SERVICE_UNAVAILABLE));
                                                                            }
                                                                            return Observable.just(r.readEntity(String.class));
                                                                        })
                                                                        .timeout(500, MILLISECONDS, Observable.error(new MandatoryServiceTimeout("work", Status.GATEWAY_TIMEOUT)));
                              return Observable.zip(waitResult,
                                                    workResult,
                                                    (wait, work) -> format("=> Result : %s, %s%n=> Took : %dms", wait, work, (System.nanoTime() - start) / 1000 / 1000));
                          })
                          .subscribe(response::resume,
                                     (error) -> {
                                         response.resume(Response.status(((HttpServiceException) error).proposedHttpStatus)
                                                                 .entity(error.getMessage())
                                                                 .build());
                                     });
            }
        }

        private static class MandatoryServiceUnavailable extends HttpServiceException {
            public MandatoryServiceUnavailable(String service, Status proposedHttpStatus) {
                super(format("'%s' service not available", service), proposedHttpStatus);
            }
        }
        private static class MandatoryServiceTimeout extends HttpServiceException {
            public MandatoryServiceTimeout(String service, Status proposedHttpStatus) {
                super(format("'%s' service timeout", service), proposedHttpStatus);
            }
        }
        public static class HttpServiceException extends Exception {
            public final Status proposedHttpStatus;
            public HttpServiceException(String message, Status proposedHttpStatus) {
                super(message);
                this.proposedHttpStatus = proposedHttpStatus;
            }
        }
    }


    @ApplicationPath("/jax-rs/*")
    private static class WaitApp extends ResourceConfig {
        public WaitApp() {
            registerClasses(WaitResource.class);
        }

        @Path("wait")
        public static class WaitResource {
            ExecutorService executor = Executors.newFixedThreadPool(100);

            @GET
            @Path("{wait_time}")
            public void wait(@Suspended final AsyncResponse response, @PathParam("wait_time") int waitTime) throws InterruptedException {
                System.out.printf("wait %dms%n", waitTime);
                response.setTimeout(100, MILLISECONDS);
                Future<?> future = executor.submit(() -> {
                    try {
                        MILLISECONDS.sleep(waitTime);
                        response.resume(format("Waited %dms", waitTime));
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        response.resume(e);
                    }
                });
                response.setTimeoutHandler(asyncResponse -> {
                    asyncResponse.cancel(42);
                    future.cancel(true);
                });
            }
        }
    }
}
