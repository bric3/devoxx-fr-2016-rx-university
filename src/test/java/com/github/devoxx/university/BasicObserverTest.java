package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;
import rx.Observer;

public class BasicObserverTest {

    @Test
    public void should_do() {

        Observable.just(1, 2, 3)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer v) {
                        System.out.println("Valeur : " + v);
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Fin");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("Erreur");
                    }
                });
    }

    @Test
    public void should_do_other() {

        Observable.just(1, 2, 3)
                .map(i -> i * 2)
                .take(2)
                .defaultIfEmpty(0)
                .subscribe();
    }

    @Test
    public void should_do_exemple() {
        Observable.just("bjr", "DevoXx")
                .map(str -> str.toLowerCase())
                .filter(str -> str.length() >= 3)
                .subscribe();
    }
}
