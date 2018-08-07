package com.jing.cloud.agent.client.startup;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.jing.cloud.agent.client.thread.StartupRunnable;
import com.jing.cloud.forward.client.NettyClient;

import io.netty.channel.Channel;

@Component
@Configuration
public class ClientStartup implements ApplicationRunner {

	@Value("${spring.netty.host}")
	private String host;
	
	@Value("${spring.netty.port}")
	private int port;
	
	@Autowired
	private Map<String,NettyClient> nettyClientMap;
	
	public void run(ApplicationArguments arg0) throws Exception {
		StartupRunnable run = new StartupRunnable(host,port,nettyClientMap);
		new Thread(run).start();
	}

}
