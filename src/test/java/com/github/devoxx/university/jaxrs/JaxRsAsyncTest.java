package com.github.devoxx.university.jaxrs;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Test;
import com.github.devoxx.university.server.JaxRsServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JaxRsAsyncTest {
    JaxRsServer jaxRsServer = new JaxRsServer(8080).application(AsyncJaxRsApp.class).start();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(false).build();
    ExecutorService executor = Executors.newFixedThreadPool(100);


    @Test
    public void do_http() throws Exception {
        Request httpQuery = new Request.Builder().url("http://localhost:8080/undertow/jax-rs/wait/20")
                                                 .build();

        Response response = okHttpClient.newCall(httpQuery)
                                        .execute();

        assertEquals(200, response.code());
        assertEquals("Waited 20ms", response.body().string());
    }

    @Test
    public void do_http_longer() throws Exception {
        Request httpQuery = new Request.Builder().url("http://localhost:8080/undertow/jax-rs/wait/100")
                                                 .build();

        Response response = okHttpClient.newCall(httpQuery)
                                        .execute();

        assertEquals(503, response.code());
        assertEquals("42", response.header("Retry-After"));
    }

    @Test
    public void do_several_http_with_latency() throws Exception {
        Request httpQuery = new Request.Builder().url("http://localhost:8080/undertow/jax-rs/wait/20")
                                                 .build();

        IntStream.range(0, 3).forEach(queryNumber -> {
            executor.submit(() -> okHttpClient.newCall(httpQuery).execute());
        });
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
    }


    @Path("wait")
    public static class WaitResource {
        ExecutorService executor = Executors.newFixedThreadPool(100);

        @GET
        @Path("{wait_time}")
        public void wait(@Suspended final AsyncResponse response, @PathParam("wait_time") int waitTime) throws InterruptedException {
            response.setTimeout(50, MILLISECONDS);
            Future<?> future = executor.submit(() -> {
                try {
                    MILLISECONDS.sleep(waitTime);
                    response.resume(format("Waited %dms", waitTime));
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    System.out.println("got interrupted");
                    response.resume(e);
                }
            });
            response.setTimeoutHandler(asyncResponse -> {
                asyncResponse.cancel(42);
//                asyncResponse.resume(javax.ws.rs.core.Response.status(Status.SERVICE_UNAVAILABLE).header("", "").build());
                future.cancel(true);
            });
        }
    }

    @ApplicationPath("/jax-rs/*")
    private static class AsyncJaxRsApp extends ResourceConfig {
        public AsyncJaxRsApp() {
            registerClasses(WaitResource.class);
        }
    }

    @After
    public void stop_server() throws Exception {
        jaxRsServer.stop();
    }
}
