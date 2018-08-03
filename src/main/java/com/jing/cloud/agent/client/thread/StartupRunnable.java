package com.jing.cloud.agent.client.thread;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.agent.client.handler.ClientHandler;
import com.jing.cloud.agent.forward.handler.RemoteHandler;
import com.jing.cloud.agent.forward.thread.ClientStartupRunnable;
import com.jing.cloud.config.IdleConfig;
import com.jing.cloud.handler.MessageDecoder;
import com.jing.cloud.handler.MessageEncoder;
import com.jing.cloud.module.Authentication;
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
	
	private ChannelFuture future;
	
	private Map<String,ClientStartupRunnable> clientMap;
	
	public StartupRunnable(String host, int port,Map<String,ClientStartupRunnable> clientMap) {
		this.host = host;
		this.port = port;
		this.clientMap = clientMap;
	}

	public void run() {
		Bootstrap client = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		client.group(group).channel(NioSocketChannel.class);
		client.option(ChannelOption.TCP_NODELAY, true);// 有消息后立刻发送
		client.option(ChannelOption.SO_KEEPALIVE, true);// 保持长连接
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT,
						IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				 // 初始化编码器，解码器，处理器
				 ch.pipeline().addLast(new MessageDecoder());
				 ch.pipeline().addLast(new MessageEncoder());
				 ch.pipeline().addLast(new ClientHandler(StartupRunnable.this,clientMap));
			}
			
		});
		
		future = client.connect(host, port).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("连接代理服务器成功...");
					Message msg = new Message();
					msg.setType(MessageCode.REGISTER);
					Authentication authInfo = new Authentication("fort", "fort");
					msg.setData(authInfo);
					future.channel().writeAndFlush(msg);
				 } else{
					 logger.error("连接代理服务器失败...");
					 group.shutdownGracefully();
					 new Thread(StartupRunnable.this).start();
				 }
			}
		});
		
	}

	
	public void writeAndFlush(Object msg) {
		future.channel().writeAndFlush(msg);
	}
}
