package com.jing.cloud.agent.forward.thread;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.agent.client.handler.ClientHandler;
import com.jing.cloud.agent.forward.handler.RemoteHandler;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;

public class ClientStartupRunnable implements Runnable {
	
	private final Logger logger = LogManager.getLogger(ClientStartupRunnable.class);
	
	private Channel serverChannel;
	
	private EventLoopGroup group;
	
	private ChannelFuture future;
	
	private String token;
	
	private String host;
	
	private int port;
	
	private Map<String,ClientStartupRunnable> clientMap;
	
	public ClientStartupRunnable() {
		
	}
	
	public ClientStartupRunnable(Channel serverChannel,String token,String host,int port,
			Map<String,ClientStartupRunnable> clientMap) {
		this.serverChannel = serverChannel;
		this.token = token;
		this.host = host;
		this.port = port;
		this.clientMap = clientMap;
	}
	
	public void run() {
		Bootstrap client = new Bootstrap();
		group = new NioEventLoopGroup();
		client.group(group).channel(NioSocketChannel.class);
		client.option(ChannelOption.TCP_NODELAY, true);// ����Ϣ�����̷���
		client.option(ChannelOption.SO_KEEPALIVE, true);// ���ֳ�����
		client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				 ch.pipeline().addLast(new RemoteHandler(serverChannel,token));
			}
			
		});

		Message message = new Message();
		message.setToken(token);
		future = client.connect(host, port).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					 // �õ��ܵ�������ͨ��
					logger.info("����Ŀ�����ɹ�...");
					
					message.setType(MessageCode.CONNECTION_SUCCESS);
					
					clientMap.put(token, ClientStartupRunnable.this);
					
				 } else{
					 logger.error("����Ŀ�����ʧ��...");
					 
					 message.setType(MessageCode.CONNECTION_ERROR);
				 }

				//����������Ϣ 
				serverChannel.writeAndFlush(message);
			}
		});
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
	
	public static void main(String[] args) {
		new ClientStartupRunnable().run();
	}
}
