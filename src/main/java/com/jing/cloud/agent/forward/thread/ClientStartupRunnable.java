package com.jing.cloud.agent.forward.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.agent.client.handler.ClientHandler;
import com.jing.cloud.agent.forward.handler.RemoteHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ClientStartupRunnable implements Runnable {
	
	private final Logger logger = LogManager.getLogger(ClientStartupRunnable.class);
	
	private EventLoopGroup group;
	
	private ChannelFuture future;
	
	public void run() {
		Bootstrap client = new Bootstrap();
		group = new NioEventLoopGroup();
		client.group(group).channel(NioSocketChannel.class);
		client.option(ChannelOption.TCP_NODELAY, true);// 有消息后立刻发送
		client.option(ChannelOption.SO_KEEPALIVE, true);// 保持长连接
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				 ch.pipeline().addLast(new RemoteHandler(ClientStartupRunnable.this));
			}
			
		});
		
		try {
			future = client.connect("192.168.1.99", 443).sync();
			if (future.isSuccess()) {
				 // 得到管道，便于通信
				logger.info("连接目标服务成功...");
			 }
			 else{
				 logger.error("连接目标服务失败...");
			 }
			 future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		} finally {
			group.shutdownGracefully();
		}
	}

	public void shutdown() {
		if(future != null) {
			future.channel().close();
		}
		
		if(group != null) {
			group.shutdownGracefully();
		}
	}
	
	public void writeAndFlush(Object msg) {
		future.channel().writeAndFlush(msg);
	}
}
