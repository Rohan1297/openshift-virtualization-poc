package com.example.vmhello;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class VmHelloApplication {
    public static void main(String[] args) {
        SpringApplication.run(VmHelloApplication.class, args);
    }

    @GetMapping("/api/hello")
    public Map<String, String> hello(@RequestParam(defaultValue = "Workshop participant") String name) {
        String displayName = name.strip();
        if (displayName.isEmpty()) displayName = "Workshop participant";
        if (displayName.length() > 80) displayName = displayName.substring(0, 80);
        return Map.of("message", "Hello, " + displayName + "! Welcome to the OpenShift Virtualization workshop.");
    }

    @GetMapping("/api/info")
    public Map<String, Object> info() {
        String hostname;
        try { hostname = InetAddress.getLocalHost().getHostName(); }
        catch (UnknownHostException e) { hostname = "unknown"; }
        return Map.of("application", "VM Hello", "hostname", hostname,
            "javaVersion", System.getProperty("java.version"),
            "uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000,
            "serverTime", Instant.now().toString());
    }
}
