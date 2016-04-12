package com.github.devoxx.university.server;

import java.io.IOException;
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

public class JaxRsServer {
    private Undertow server;
    private Class<?> appClass;


    public static void main(final String[] args) {
        new JaxRsServer().application(JaxRsApp.class)
                         .registerShutdownHook()
                         .start();
    }

    public JaxRsServer application(Class<?> appClass) {
        this.appClass = appClass;
        return this;
    }

    public JaxRsServer start() {
        checkNotNull(appClass, "appClass");
        DeploymentInfo servletBuilder = Servlets.deployment()
                                                .setClassLoader(this.getClass().getClassLoader())
                                                .setContextPath("/undertow")
                                                .setDeploymentName("rxjava.war")
                                                .addServlets(
                                                        Servlets.servlet("MessageServlet", MessageServlet.class)
                                                                .addInitParam("message", "Hello World")
                                                                .addMapping("/*"),
                                                        Servlets.servlet("JaxRsServlet", ServletContainer.class)
                                                                .setLoadOnStartup(1)
                                                                .addInitParam("javax.ws.rs.Application", appClass.getName())
                                                                .addMapping("/jax-rs/*")
                                                        );

        DeploymentManager manager = Servlets.defaultContainer()
                                            .addDeployment(servletBuilder);
        manager.deploy();
        try {
            PathHandler pathHandler = Handlers.path(Handlers.redirect("/undertow"))
                                              .addPrefixPath("/undertow", manager.start());
            server = Undertow.builder()
                             .addHttpListener(8080, "localhost")
                             .setHandler(pathHandler)
                             .build();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Starting server");
        server.start();
        return this;
    }

    public void stop() {
        System.out.println("Stopping server");
        server.stop();
    }

    private <T> T checkNotNull(T arg, String argName) {
        if (arg == null) {
            throw new IllegalArgumentException(String.format("%s cannot be null", argName));
        }
        return arg;
    }

    private JaxRsServer registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                JaxRsServer.this.stop();
            }
        });
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
