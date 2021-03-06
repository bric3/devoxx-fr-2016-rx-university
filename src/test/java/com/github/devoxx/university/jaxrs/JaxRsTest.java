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
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Test;
import com.github.devoxx.university.server.JaxRsServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class JaxRsTest {

    JaxRsServer jaxRsServer = new JaxRsServer(8080).application(PlainOldJaxRsApp.class).start();
    OkHttpClient okHttpClient = new OkHttpClient();
    ExecutorService executor = Executors.newFixedThreadPool(100);


    @Test
    public void do_http() throws Exception {
        Request httpQuery = new Request.Builder().url("http://localhost:8080/undertow/jax-rs/wait/20")
                                                 .build();

        okhttp3.Response response = okHttpClient.newCall(httpQuery)
                                                .execute();
        assertEquals(200, response.code());
        assertEquals("Waited 20ms", response.body().string());
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
        public Response wait(@PathParam("wait_time") int waitTime) throws InterruptedException {

            System.out.format("heavy work of : %dms%n", waitTime);
            MILLISECONDS.sleep(waitTime); // hard and long work
            return Response.ok(format("Waited %dms", waitTime)).build();
        }
    }

    @ApplicationPath("/jax-rs/*")
    private static class PlainOldJaxRsApp extends ResourceConfig {
        public PlainOldJaxRsApp() {
            registerClasses(WaitResource.class);
        }
    }

    @After
    public void stop_server() throws Exception {
        jaxRsServer.stop();
    }
}
