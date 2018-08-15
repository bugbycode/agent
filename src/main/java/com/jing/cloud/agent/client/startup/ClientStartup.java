package com.jing.cloud.agent.client.startup;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.bugbycode.https.HttpsClient;
import com.jing.cloud.agent.client.thread.StartupRunnable;
import com.jing.cloud.forward.client.NettyClient;

@Component
@Configuration
public class ClientStartup implements ApplicationRunner {

	@Value("${spring.oauth.oauthUri}")
	private String oauthUri;
	
	@Value("${console.uri}")
	private String consoleUri;
	
	@Value("${spring.netty.clientId}")
	private String clientId;
	
	@Value("${spring.netty.secret}")
	private String secret;
	
	@Value("${server.keystorePath}")
	private String keystorePath;
	
	@Value("${server.keystorePassword}")
	private String keystorePassword;
	
	@Autowired
	private Map<String,NettyClient> nettyClientMap;
	
	public void run(ApplicationArguments arg0) throws Exception {
		HttpsClient client = new HttpsClient(keystorePath, keystorePassword);
		StartupRunnable run = new StartupRunnable(oauthUri,consoleUri,clientId,secret,nettyClientMap,client);
		new Thread(run).start();
	}

}
