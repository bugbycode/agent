package com.jing.cloud.agent.client.thread;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bugbycode.https.HttpsClient;
import com.jing.cloud.agent.client.handler.ClientHandler;
import com.jing.cloud.config.IdleConfig;
import com.jing.cloud.forward.client.NettyClient;
import com.jing.cloud.handler.MessageDecoder;
import com.jing.cloud.handler.MessageEncoder;
import com.jing.cloud.module.Authentication;
import com.jing.cloud.module.HandlerConst;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class StartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(StartupRunnable.class);
	
	private String host;
	
	private int port;
	
	private int proxyPort;
	
	private String clientId;
	
	private String secret;
	
	private String oauthUri; 
	
	private String consoleUri;
	
	private HttpsClient httpsClient;
	
	private ChannelFuture future;
	
	private Map<String,NettyClient> nettyClientMap;
	
	public StartupRunnable(String oauthUri, String consoleUri,
			String clientId,String secret,Map<String,NettyClient> nettyClientMap,
			HttpsClient httpsClient,int proxyPort) {
		this.clientId = clientId;
		this.secret = secret;
		this.nettyClientMap = nettyClientMap;
		this.oauthUri = oauthUri;
		this.consoleUri = consoleUri;
		this.httpsClient = httpsClient;
		this.proxyPort = proxyPort;
	}

	public void run() {
		Bootstrap client = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		client.group(group).channel(NioSocketChannel.class);
		client.option(ChannelOption.TCP_NODELAY, true);
		client.option(ChannelOption.SO_KEEPALIVE, true);
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT,
						IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				 ch.pipeline().addLast(new MessageDecoder(HandlerConst.MAX_FRAME_LENGTH, HandlerConst.LENGTH_FIELD_OFFSET, 
							HandlerConst.LENGTH_FIELD_LENGTH, HandlerConst.LENGTH_AD_JUSTMENT, 
							HandlerConst.INITIAL_BYTES_TO_STRIP));
				 ch.pipeline().addLast(new MessageEncoder());
				 ch.pipeline().addLast(new ClientHandler(StartupRunnable.this,nettyClientMap));
			}
			
		});
		
		try {
			String tokenResult = httpsClient.getToken(oauthUri, "client_credentials", clientId, secret, "agent");
			Map<String,Object> tokenMap = jsonToMap(new JSONObject(tokenResult));
			if(tokenMap.containsKey("error")) {
				logger.info("Agent auth failed.");
				return;
			}
			String token = tokenMap.get("access_token").toString();
			String url = consoleUri;
			Map<String,Object> data = new HashMap<String,Object>();
			String result = httpsClient.getResource(url, token, data);
			JSONObject json = new JSONObject(result);
			if(json.length() > 0) {
				host = json.getString("ip");
				port = json.getInt("port");
				
				if("127.0.0.1".equals(host) || "localhost".equals(host)) {
					URL authUrl = new URL(oauthUri);
					host = authUrl.getHost();
					port = this.proxyPort;
				}
				
				future = client.connect(host, port).addListener(new ChannelFutureListener() {
					
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (future.isSuccess()) {
							logger.info("Connection to " + host + ":" + port + " success...");
							Message msg = new Message();
							msg.setType(MessageCode.REGISTER);
							Authentication authInfo = new Authentication(secret, clientId);
							msg.setData(authInfo);
							future.channel().writeAndFlush(msg);
						 } else{
							 logger.error("Connection to " + host + ":" + port + " failed...");
							 group.shutdownGracefully();
							 Thread.sleep(5000);
							 new Thread(StartupRunnable.this).start();
						 }
					}
				});
			} else {
				Thread.sleep(5000);
				run();
			}
		} catch (Exception e) {
			//e.printStackTrace();
			logger.error(e.getMessage());
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			run();
		}
	}
	
	public synchronized void writeAndFlush(Object msg) {
		future.channel().writeAndFlush(msg);
	}
	
	private Map<String,Object> jsonToMap(JSONObject json){
		Map<String,Object> map = new HashMap<String,Object>();
		@SuppressWarnings("unchecked")
		Iterator<String> it = json.keys();
		while(it.hasNext()) {
			String key = it.next();
			try {
				if("authorities".equals(key)) {
					JSONArray arr = json.getJSONArray(key);
					int len =arr.length();
					Collection<String> collection = new ArrayList<String>();
					for(int index = 0;index < len;index++) {
						collection.add(arr.getString(index));
					}
					map.put(key, collection);
				}else {
					map.put(key, json.get(key));
				}
			}catch (JSONException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		return map;
	}
}
