package com.github.devoxx.university.rxnetty;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.github.devoxx.university.server.RxNettyServer;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RxNettyTest {
    RxNettyServer rxNettyServer = new RxNettyServer(8080);
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    @Test
    public void do_some_http() throws IOException {
        Request httpQuery = new Request.Builder().url("http://localhost:8080")
                                                 .post(new FormBody.Builder().add("work", "20").build())
                                                 .build();

        Response response = okHttpClient.newCall(httpQuery)
                                        .execute();

        assertEquals(200, response.code());
    }

    @Test
    public void do_unsupported_patch_http() throws IOException {
        Request httpQuery = new Request.Builder().url("http://localhost:8080")
                                                 .patch(new FormBody.Builder().add("work", "20").build())
                                                 .build();

        Response response = okHttpClient.newCall(httpQuery)
                                        .execute();

        assertEquals(400, response.code());
    }

    @Test
    public void do_unsupported_path_http() throws IOException {
        Request httpQuery = new Request.Builder().url("http://localhost:8080/bad")
                                                 .post(new FormBody.Builder().add("work", "20").build())
                                                 .build();

        Response response = okHttpClient.newCall(httpQuery)
                                        .execute();

        assertEquals(404, response.code());
    }

    @Before
    public void start_server() {
        rxNettyServer.start();
    }

    @After
    public void stop_server() throws InterruptedException {
        rxNettyServer.stop();
    }
}
