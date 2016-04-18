package com.github.devoxx.university.multi.hystrix;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
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
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;

public class UseMultipleServicesHystrixTest {

    JaxRsServer jaxRsServer = new JaxRsServer(8080).application(WaitApp.class).start();
    RxNettyServer rxNettyServer = new RxNettyServer(8081).start();
    JaxRsServer composedServer = new JaxRsServer(8082).registerListener(HystrixCleanup.class).application(ComposeApp.class).start();


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
        System.out.printf("HTTP Status : %d%n", response.getStatus());
        System.out.printf("Response : %s%n", response.readEntity(String.class));
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
                              Observable<String> waitResult = new WaitServiceCommand(lastNumber).observe();
                              Observable<String> workResult = new WorkServiceCommand(lastNumber).observe();
                              return Observable.zip(waitResult,
                                                    workResult,
                                                    (wait, work) -> format("=> Result : %s, %s%n=> Took : %dms", wait, work, (System.nanoTime() - start) / 1000 / 1000));
                          })
                          .subscribe(response::resume,
                                     (error) -> {
//                                         if(error instanceof HystrixRuntimeException){
//                                             ((HystrixRuntimeException) error).getFallbackException().getCause()
//                                         }
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

    @WebListener
    private static class HystrixCleanup implements ServletContextListener {
        @Override
        public void contextInitialized(ServletContextEvent sce) {
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            System.err.println("cleanup hystrix");
            Hystrix.reset(1, TimeUnit.SECONDS);
        }
    }

    private static class WaitServiceCommand extends HystrixObservableCommand<String> {
        private int wait;

        protected WaitServiceCommand(int wait) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("wait group"))
                        .andCommandKey(HystrixCommandKey.Factory.asKey("wait")));
            this.wait = wait;
        }

        @Override
        protected Observable<String> construct() {
            return Observable.fromCallable(() -> waitWS(wait))
                             .flatMap(response -> {
                                 if(response.getStatus() == 503) {
                                     return Observable.error(new ComposeApp.MandatoryServiceUnavailable("wait", Status.SERVICE_UNAVAILABLE));
                                 }
                                 return Observable.just(response.readEntity(String.class));
                             })
                             .retry(2);
        }

        @Override
        protected Observable<String> resumeWithFallback() {
            return Observable.just("Waited too long");
        }
    }

    private static class WorkServiceCommand extends HystrixObservableCommand<String> {
        private int work;

        protected WorkServiceCommand(int work) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("work group"))
                        .andCommandKey(HystrixCommandKey.Factory.asKey("work"))
                        .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(500)));
            this.work = work;
        }

        @Override
        protected Observable<String> construct() {
            return Observable.fromCallable(() -> workWS(work))
                             .map(response -> response.readEntity(String.class));
        }

        @Override
        protected Observable<String> resumeWithFallback() {
            return Observable.just("Worked too long");
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
