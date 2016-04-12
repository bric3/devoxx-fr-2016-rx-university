package com.github.devoxx.university.server;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

public class ServletServer {
    private Undertow server;

    public static void main(final String[] args) throws ServletException {
        new ServletServer().registerShutdownHook()
                           .start();
    }

    public void stop() {
        server.stop();
    }

    public void start() throws ServletException {
        DeploymentInfo servletBuilder = Servlets.deployment()
                                                .setClassLoader(this.getClass().getClassLoader())
                                                .setContextPath("/undertow")
                                                .setDeploymentName("rxjava.war")
                                                .addServlets(
                                                        Servlets.servlet("MessageServlet", MessageServlet.class)
                                                                .addInitParam("message", "Hello World")
                                                                .addMapping("/*"));

        DeploymentManager manager = Servlets.defaultContainer()
                                            .addDeployment(servletBuilder);
        manager.deploy();
        PathHandler pathHandler = Handlers.path(Handlers.redirect("/undertow"))
                                          .addPrefixPath("/undertow", manager.start());

        server = Undertow.builder()
                         .addHttpListener(8080, "localhost")
                         .setHandler(pathHandler)
                         .build();
        System.out.println("Starting server");
        server.start();
    }

    private ServletServer registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Stopping server");
                ServletServer.this.stop();
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
