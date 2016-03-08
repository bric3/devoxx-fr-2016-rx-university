package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;

public class ObservableTypeTest {


    @Test
    public void should_compare_observable() {

        Observable<Void> empty = Observable.empty();
        empty.concatMap(o -> Observable.just(1))
                .subscribe(System.out::println);

        // TODO: compare with Complete & Single
    }
}
