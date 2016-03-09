package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;
import rx.observables.AsyncOnSubscribe;

public class ObservableFactoryTest {

    @Test
    public void should_create() throws InterruptedException {
        Observable.create(AsyncOnSubscribe.<Long>createStateless((l, b) -> {
            System.out.println(l);
            b.onNext(Observable.just(l));
        })).subscribe(System.out::println);

        Thread.sleep(1000);
    }
}
