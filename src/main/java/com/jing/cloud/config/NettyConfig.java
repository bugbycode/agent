package com.jing.cloud.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jing.cloud.agent.forward.thread.ClientStartupRunnable;

@Configuration
public class NettyConfig {
	
	@Bean("clientMap")
	public Map<String,ClientStartupRunnable> getClientMap(){
		return Collections.synchronizedMap(new HashMap<String,ClientStartupRunnable>());
	}
}
