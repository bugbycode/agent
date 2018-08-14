package com.jing.cloud.agent.client.thread;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	
	private String clientId;
	
	private String secret;
	
	private ChannelFuture future;
	
	private Map<String,NettyClient> nettyClientMap;
	
	public StartupRunnable(String host, int port,String clientId,String secret,Map<String,NettyClient> nettyClientMap) {
		this.host = host;
		this.port = port;
		this.clientId = clientId;
		this.secret = secret;
		this.nettyClientMap = nettyClientMap;
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
	}
	
	public synchronized void writeAndFlush(Object msg) {
		future.channel().writeAndFlush(msg);
	}
}
