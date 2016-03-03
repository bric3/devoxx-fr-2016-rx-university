package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SchedulersTest {


    private static final int NUMBER_OF_TASKS = 500_000;

    @Test
    public void should_push_a_lot_into_a_scheduler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_TASKS);
        for (int i = 0; i < NUMBER_OF_TASKS; i++) {
            Observable.timer(0, TimeUnit.MILLISECONDS, Schedulers.io())
                    .map(m -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return m;
                    }).subscribe(new Subscriber<Long>() {
                @Override
                public void onCompleted() {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onNext(Long aLong) {

                }
            });
        }

        latch.await();
    }
}
