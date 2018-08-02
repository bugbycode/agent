package com.jing.cloud.agent.client.thread;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.agent.client.handler.ClientHandler;
import com.jing.cloud.agent.forward.handler.RemoteHandler;
import com.jing.cloud.config.IdleConfig;
import com.jing.cloud.handler.MessageDecoder;
import com.jing.cloud.handler.MessageEncoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
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
	
	public StartupRunnable(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void run() {
		Bootstrap client = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		client.group(group).channel(NioSocketChannel.class);
		client.option(ChannelOption.TCP_NODELAY, true);// ����Ϣ�����̷���
		client.option(ChannelOption.SO_KEEPALIVE, true);// ���ֳ�����
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT,
						IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				 // ��ʼ������������������������
				 ch.pipeline().addLast(new MessageDecoder());
				 ch.pipeline().addLast(new MessageEncoder());
				 ch.pipeline().addLast(new ClientHandler());
			}
			
		});
		
		try {
			future = client.connect(host, port).sync();
			if (future.isSuccess()) {
				 // �õ��ܵ�������ͨ��
				logger.info("����ͻ��˿����ɹ�...");
			 }
			 else{
				 logger.error("����ͻ��˿���ʧ��...");
			 }
			 future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
			group.shutdownGracefully();
		} finally {
			new Thread(this).start();
		}
	}

	
	public void writeAndFlush(Object msg) {
		future.channel().writeAndFlush(msg);
	}
}
