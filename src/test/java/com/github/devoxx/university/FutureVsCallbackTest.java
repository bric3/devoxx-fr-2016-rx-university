package com.github.devoxx.university;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class FutureVsCallbackTest {


    @Test
    public void should_do_a_future() throws ExecutionException, InterruptedException {
        Future<String> future = new FutureTask(() -> "Hello");
        future.get(); // bloquant !
    }
}
