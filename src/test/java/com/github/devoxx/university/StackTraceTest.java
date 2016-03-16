package com.github.devoxx.university;

import org.junit.Test;

public class StackTraceTest {

    /*

exemple de stack trace qui n'aide pas

java.net.ConnectException: Connection refused
	at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method) ~[?:1.8.0_74]
	at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:717) ~[?:1.8.0_74]
	at org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor.processEvent(DefaultConnectingIOReactor.java:173) ~[httpcore-nio-4.3.2.jar:4.3.2]
	at org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor.processEvents(DefaultConnectingIOReactor.java:147) ~[httpcore-nio-4.3.2.jar:4.3.2]
	at org.apache.http.impl.nio.reactor.AbstractMultiworkerIOReactor.execute(AbstractMultiworkerIOReactor.java:348) ~[httpcore-nio-4.3.2.jar:4.3.2]
	at org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager.execute(PoolingNHttpClientConnectionManager.java:189) ~[httpasyncclient-4.0.2.jar:4.0.2]
	at org.apache.http.impl.nio.client.CloseableHttpAsyncClientBase.doExecute(CloseableHttpAsyncClientBase.java:67) ~[httpasyncclient-4.0.2.jar:4.0.2]
	at org.apache.http.impl.nio.client.CloseableHttpAsyncClientBase.access$000(CloseableHttpAsyncClientBase.java:38) ~[httpasyncclient-4.0.2.jar:4.0.2]
	at org.apache.http.impl.nio.client.CloseableHttpAsyncClientBase$1.run(CloseableHttpAsyncClientBase.java:57) ~[httpasyncclient-4.0.2.jar:4.0.2]
	at java.lang.Thread.run(Thread.java:745) [?:1.8.0_74]

 */

    @Test
    public void should_doNothing() {

    }
}
