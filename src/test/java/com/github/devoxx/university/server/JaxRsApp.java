package com.github.devoxx.university.server;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/jax-rs/*")
public class JaxRsApp extends ResourceConfig {
    public JaxRsApp() {
        packages(true, "com.github.devoxx.university.server");
    }
}
