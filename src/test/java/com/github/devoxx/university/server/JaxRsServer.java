package com.github.devoxx.university.server;

import static java.util.stream.Collectors.toList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.jersey.servlet.ServletContainer;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ListenerInfo;

public class JaxRsServer {
    private Undertow server;
    private Class<?> appClass;
    private Collection<Class<? extends EventListener>> listeners = new ArrayList<>();
    private int port;

    public JaxRsServer(int port) {
        this.port = port;
    }

    public static void main(final String[] args) {
        new JaxRsServer(8080).application(JaxRsApp.class)
                             .registerShutdownHook()
                             .start();
    }

    public JaxRsServer application(Class<?> appClass) {
        this.appClass = Objects.requireNonNull(appClass);
        return this;
    }

    public JaxRsServer start() {
        checkNotNull(appClass, "appClass");
        DeploymentInfo servletBuilder = Servlets.deployment()
                                                .setClassLoader(this.getClass().getClassLoader())
                                                .setContextPath("/undertow")
                                                .setDeploymentName("rxjava.war")
                                                .addListeners(
                                                        listeners.stream()
                                                                 .map(ListenerInfo::new)
                                                                 .collect(toList())
                                                )
                                                .addServlets(
                                                        Servlets.servlet("MessageServlet", MessageServlet.class)
                                                                .addInitParam("message", "Hello World")
                                                                .addMapping("/*"),
                                                        Servlets.servlet("JaxRsServlet", ServletContainer.class)
                                                                .setLoadOnStartup(1)
                                                                .addInitParam("javax.ws.rs.Application", appClass.getName())
                                                                .setAsyncSupported(true)
                                                                .addMapping("/jax-rs/*")
                                                );

        DeploymentManager manager = Servlets.defaultContainer()
                                            .addDeployment(servletBuilder);
        manager.deploy();
        try {
            PathHandler pathHandler = Handlers.path(Handlers.redirect("/undertow"))
                                              .addPrefixPath("/undertow", manager.start());
            server = Undertow.builder()
                             .addHttpListener(port, "localhost")
                             .setHandler(pathHandler)
                             .build();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        System.out.format("Starting undertow server on %d%n", port);
        server.start();
        return this;
    }

    public void stop() {
        System.out.println("Stopping undertow server");
        server.stop();
    }

    private <T> T checkNotNull(T arg, String argName) {
        if (arg == null) {
            throw new IllegalArgumentException(String.format("%s cannot be null", argName));
        }
        return arg;
    }

    public JaxRsServer registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                JaxRsServer.this.stop();
            }
        });
        return this;
    }

    public JaxRsServer registerListener(Class<? extends EventListener> listenerClass) {
        listeners.add(Objects.requireNonNull(listenerClass));
        return this;
    }

    private static class MessageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.getWriter().print(getServletConfig().getInitParameter("message"));
        }
    }
}
