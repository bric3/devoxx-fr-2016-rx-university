package com.github.devoxx.university.jaxrs;


import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Test;
import com.github.devoxx.university.latency.LatencyTracking;
import com.github.devoxx.university.server.JaxRsServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class JaxRsTest {

    private final JaxRsServer jaxRsServer = new JaxRsServer().application(PlainOldJaxRsApp.class).start();
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final LatencyTracking latencyTracking = new LatencyTracking();

    @Test
    public void do_http() throws Exception {
        Request httpQuery = new Request.Builder().url("http://localhost:8080/undertow/jax-rs/wait/20")
                                                 .build();

        okHttpClient.newCall(httpQuery)
                    .execute();
    }

    @Test
    public void do_several_http_with_latency() throws Exception {
        Request httpQuery = new Request.Builder().url("http://localhost:8080/undertow/jax-rs/wait/20")
                                                 .build();

        for(int i = 0; i < 100; i++) {
            latencyTracking.trackLatency(() -> okHttpClient.newCall(httpQuery)
                                                           .execute());
        }

        latencyTracking.printHistogram(System.out);
    }


    @Path("wait")
    public static class WaitResource {

        @GET
        @Path("{wait_time}")
        public Response wait(@PathParam("wait_time") int waitTime) throws InterruptedException {
            MILLISECONDS.sleep(waitTime);
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
