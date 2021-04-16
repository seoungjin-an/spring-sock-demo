package com.example.jay.spring.sock;

import lombok.Data;

@Data
public class AppSettings {
    public int    listenPort;
    public String targetHost;
    public int    targetPort;
}
