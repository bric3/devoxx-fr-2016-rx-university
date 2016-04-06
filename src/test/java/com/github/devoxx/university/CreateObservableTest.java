package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

public class CreateObservableTest {

    public int compute() {
        return 0;
    }

    @Test
    public void should_just() {

        Observable.just(compute())
                .subscribe();
    }

    @Test
    public void should_just2() {

        int result = compute();
        Observable.just(result)
                .subscribe();
    }

    @Test
    public void should_defer() {
        Observable.defer(() -> Observable.just(compute()))
                .subscribe();
    }

    @Test
    public void should_from_callable() {
        Observable.fromCallable(() -> compute())
                .subscribe();
    }


    public static class DB {
        public Object query(String query) {
            return null;
        }

        public void closeDb() {

        }
    }

    public DB openDB() {
        return new DB();
    }

    @Test
    public void should_using() {

        Observable.using(
                () -> new DB(),
                db -> Observable.just(db),
                db -> db.closeDb())
                .subscribe();

    }


    @Test
    public void should_create_abstract() {

        // Observable.create(subscriber -> {
        // subscriber.add(/* ... */);

        // subscriber.setProducer(n -> {
        //    subscriber.onNext(/* ... */);
        // ...
        //    subscriber.onCompleted();
        //  });
        //}).subscribe();
    }

    @Test
    public void should_create1() throws InterruptedException {
        Observable.create(subscriber -> {
            subscriber.onNext(compute());
            subscriber.onCompleted();
        }).subscribe();
    }

    @Test
    public void should_create1bis() throws InterruptedException {
        Observable.create(subscriber -> {
            for (int i = 0; i < 10; i++) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(compute());
                }
            }
            subscriber.onCompleted();
        }).subscribe();
    }

    @Test
    public void should_create2() throws InterruptedException {
        Observable.create(subscriber -> {
            DB db = new DB();
            subscriber.add(Subscriptions.create(() -> db.closeDb()));
            subscriber.onNext(db);
            subscriber.onCompleted();
        }).subscribe();

    }

    @Test
    public void should_create3() throws InterruptedException {
        Observable.create(subscriber -> {
            DB db = new DB();
            subscriber.add(Subscriptions.create(() -> db.closeDb()));
            // subscriber.setProducer(n ->);

        }).subscribe();

    }
}
