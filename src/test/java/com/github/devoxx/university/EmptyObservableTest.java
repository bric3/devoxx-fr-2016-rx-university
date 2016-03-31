package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;

import java.util.Arrays;

public class EmptyObservableTest {


    @Test
    public void should_combine() throws InterruptedException {

        Observable<Integer> a = Observable.just(1);
        Observable<String> b = Observable.<String>empty()
                .defaultIfEmpty(null);

        Observable<Long> c = Observable.just(3l);

        Observable.zip(a, b, c, (w, x, v) -> Arrays.asList(w, x, v)).subscribe(System.out::println);

        Thread.sleep(1000);
    }

    @Test
    public void should_be_empty() {

        empty().doOnNext(r -> System.err.println(r)).subscribe();

    }

    private Observable<Void> empty() {
        return Observable.empty();
    }
}
