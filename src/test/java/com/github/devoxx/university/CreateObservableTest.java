package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;

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

        Observable.using(() -> new DB(), db -> Observable.just(db), db -> db.closeDb())
                .subscribe();

    }
}
