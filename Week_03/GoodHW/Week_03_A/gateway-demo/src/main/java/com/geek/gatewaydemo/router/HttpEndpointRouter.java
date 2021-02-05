package com.geek.gatewaydemo.router;

import java.util.List;

public interface HttpEndpointRouter {

    String name();
    
    String route(List<String> endpoints);
    
    // Load Balance
    // Random
    // RoundRibbon 
    // Weight
    // - server01,20
    // - server02,30
    // - server03,50
    
}
