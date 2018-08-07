package com.jing.cloud.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jing.cloud.forward.client.NettyClient;

@Configuration
public class NettyConfig {
	
	@Bean("nettyClientMap")
	public Map<String,NettyClient> getNettyClientMap(){
		return Collections.synchronizedMap(new HashMap<String,NettyClient>());
	}
}
