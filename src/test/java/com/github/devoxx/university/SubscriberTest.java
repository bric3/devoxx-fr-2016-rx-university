package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;

public class SubscriberTest {

    public static class Timer {
        public void onTick(Action0 action) {

        }

        public void start() {

        }
    }

    @Test
    public void should_be_hot() {

        Timer timer = new Timer();
        Observable.create(subscriber -> {
            timer.onTick(() -> subscriber.onNext("tick"));

        }).subscribe();
    }


    @Test
    public void should_be_cold() {
        Observable.create(subscriber -> {
            Timer timer = new Timer();
            timer.onTick(() -> subscriber.onNext("tick"));

        }).subscribe();
    }

    public static class WebService {
        public Observable<String> call() {
            return Observable.empty();
        }
    }

    @Test
    public void should_cold() {
        WebService ws = new WebService();

        Observable<String> response = ws.call();

        response
                .map(str -> str.toLowerCase())
                .subscribe(System.out::println);

        response
                .subscribe(System.out::println);


    }
}
