package com.github.devoxx.university;

import org.apache.commons.logging.Log;
import org.junit.Test;
import rx.Observable;
import rx.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Java8AndRxTest {

    @Test
    public void should_mix() {

        Observable.just(1, 2, 3, 4)
                .toList()
                .map(list -> {
                    return list.stream()
                            .filter(i -> i % 2 == 0)
                            .collect(Collectors.toList());
                }).subscribe();
    }

    @Test
    public void should_mix2() {

        List<Observable<?>> obs = new ArrayList<>();
        obs.add(Observable.defer(() -> Observable.just(1, 2, 3)));
        obs.add(Observable.just(4, 5, 6));
        obs.add(Observable.fromCallable(() -> 7));

        Observable.merge(obs).subscribe();

    }

    private static class Feed {
        public Feed newRequest() {
            return this;
        }

        public Feed user(Object user) {
            return this;
        }

        public Feed all() {
            return this;
        }

        public Observable<Feed> observe() {
            return Observable.empty();
        }
    }


    @Test
    public void should_mix3() {

        List<Feed> services = new ArrayList<>();
        Object user = null;
        Log log = null;

        Observable<Feed> feeds = Observable.merge(
                services.stream()
                        .map(service -> service.newRequest().user(user).all().observe())
                        .map(feedObservable -> feedObservable
                                .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                                .onErrorResumeNext(Observable.empty()))
                        .collect(Collectors.toList()));

        feeds.subscribe();
    }

    /*
 RxView.clickEvents(_tapBtn)
        .doOnNext(new Action1<ViewClickEvent>() {
            @Override
            public void call(ViewClickEvent onClickEvent){
                Timber.d("--------- GOT A TAP");
            }
        })
        .map(new Func1<ViewClickEvent, Integer>(){
            @Override
            public Integer call(ViewClickEvent onClickEvent){
                return 1;
            }
        }).subscribe();
*/

    @Test
    public void should_toList() throws InterruptedException {
        Subscription subscribe = Observable.interval(10, TimeUnit.MILLISECONDS)
                .toList()
                .subscribe(i -> System.out.println(i));

        Thread.sleep(1000);

        subscribe.unsubscribe();
        Thread.sleep(1000);


    }
}
