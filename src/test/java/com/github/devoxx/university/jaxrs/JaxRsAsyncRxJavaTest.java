package com.github.devoxx.university.jaxrs;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import rx.Observable;
import rx.schedulers.Schedulers;

public class JaxRsAsyncRxJavaTest {
    JaxRsServer jaxRsServer = new JaxRsServer().application(RxJavaAsyncJaxRsApp.class).start();
    OkHttpClient okHttpClient = new OkHttpClient();
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
        @GET
        @Path("{wait_time}")
        public void wait(@Suspended final AsyncResponse response, @PathParam("wait_time") int waitTime) {
            response.setTimeout(50, MILLISECONDS);
            Observable.just(waitTime)
                      .observeOn(Schedulers.computation())
                      .map(this::longStuff)
                      .subscribe(response::resume, response::resume);

        }

        private String longStuff(int waitTime) {
            try {
                MILLISECONDS.sleep(waitTime);
                return format("Waited %dms", waitTime);
            } catch (InterruptedException ignored) {
                Thread.interrupted();
                return "got interrupted";
            }
        }

    }

    @ApplicationPath("/jax-rs/*")
    private static class RxJavaAsyncJaxRsApp extends ResourceConfig {
        public RxJavaAsyncJaxRsApp() {
            registerClasses(WaitResource.class);
        }
    }

    @After
    public void stop_server() throws Exception {
        jaxRsServer.stop();
    }
}
