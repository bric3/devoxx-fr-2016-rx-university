package com.github.devoxx.university;

import org.junit.Test;
import rx.Observable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Java8ToObservable {

    @Test
    public void should_stream() {
        List<String> words = Stream.of("bonJour", "bonjour", "Hello")
                .map(str -> str.toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }

    @Test
    public void should_obs() {
        Observable.just("bonJour", "bonjour", "Hello")
                .map(str -> str.toLowerCase())
                .distinct()
                .subscribe();
    }
}
