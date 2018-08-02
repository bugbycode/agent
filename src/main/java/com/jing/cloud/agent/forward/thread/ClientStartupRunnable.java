package com.jing.cloud.agent.forward.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.agent.client.handler.ClientHandler;

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
	
	public void run() {
		Bootstrap client = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		client.group(group).channel(NioSocketChannel.class);
		client.option(ChannelOption.TCP_NODELAY, true);// ����Ϣ�����̷���
		client.option(ChannelOption.SO_KEEPALIVE, true);// ���ֳ�����
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				 ch.pipeline().addLast(new ClientHandler());
			}
			
		});
		
		try {
			ChannelFuture future= client.connect("192.168.1.99", 443).sync();
			if (future.isSuccess()) {
				 // �õ��ܵ�������ͨ��
				logger.info("����Ŀ�����ɹ�...");
			 }
			 else{
				 logger.error("����Ŀ�����ʧ��...");
			 }
			 future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
			group.shutdownGracefully();
		}
	}

}