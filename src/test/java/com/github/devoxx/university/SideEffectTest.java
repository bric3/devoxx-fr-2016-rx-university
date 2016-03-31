package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class SideEffectTest {

    private static final Logger LOGGER = Logger.getLogger("logger");

    @Test
    public void should_do_wrong_side_effects() {

        List<Integer> result = new ArrayList<>();

        Observable.just(1, 2, 3)
                .map(i -> result.add(i))
                .subscribe();
    }

    @Test
    public void should_do_good_side_effects() {

        List<Integer> result = new ArrayList<>();

        Observable.just(1, 2, 3)
                .doOnNext(i -> result.add(i)) // explicite
                .subscribe();
    }

    @Test
    public void should_do_good_side_effects3() {

        // attention ! état partagé !
        AtomicLong errors = new AtomicLong(0);

        Observable.just(1, 2, 3)
                .doOnError(ex -> errors.incrementAndGet())
                .subscribe();
    }


    @Test
    public void should_do_good_side_effects2() {

        List<Integer> result = new ArrayList<>();

        Observable.just(1, 2, 3)
                // explicite
                .doOnNext(i -> LOGGER.info("Valeur :" + i))
                .subscribe();

        Observable.just(1, 2, 3)
                .doOnSubscribe(() -> LOGGER.info("Subscribe"))
                .doOnNext(i -> LOGGER.info("Valeur :" + i))
                .doOnTerminate(() -> LOGGER.info("Terminate"))
                .subscribe();
    }

    @Test
    public void should_do_good_side_effects1() {
        Observable.just(1, 2, 3)
                .collect(() -> new ArrayList<>(), (l, value) -> l.add(value))
                .subscribe(result -> {/* ... */});

        Observable.just(1, 2, 3)
                .toList()
                .subscribe(result -> {/* ... */});
    }

    @Test
    public void should_do_wrong_side_effects2() {

        Observable.just(1, 2, 3)
                .map(i -> {
                    LOGGER.info("value : " + i);
                    return i;
                })
                .subscribe();
    }

    @Test
    public void should_create() throws InterruptedException {

        Observable.defer(() -> Observable.just("TOTO" + Thread.currentThread().getName()));

        Observable.empty()
                .first()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe((x) -> {
                    System.out.println(x + " => " + Thread.currentThread().getName());
                });

        Thread.sleep(1000);

        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                throw new RuntimeException("ouos");
            }
        }).subscribe(new Observer<Object>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Object o) {

            }
        });
    }

    private class WebService {
        public Observable<String> call() {
            return Observable.empty();
        }

        public Observable<String> call(int arg) {
            return Observable.empty();
        }
    }

    private final WebService webservice = new WebService();

    @Test
    public void should_do_wrong_side_effects3() {

        Observable.just(1, 2, 3)
                .flatMap(i -> {
                    LOGGER.info("début de l'appel");
                    return webservice.call();
                })
                .subscribe();
    }

    @Test
    public void should_123() {
        Observable.just(1, 2, 3).mergeWith(Observable.just(4, 5, 6)).subscribe(System.out::println);
    }


    @Test
    public void monad1() {

        Observable.just(1, 2, 3)
                .subscribe(arg -> {
                    webservice.call(arg).subscribe(System.out::println);
                });
    }

}
