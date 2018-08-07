package com.jing.cloud.forward.client;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.forward.handler.ForwardHandler;
import com.jing.cloud.module.ConnectionInfo;
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

public class NettyClient {
	
	private final Logger logger = LogManager.getLogger(NettyClient.class);
	
	private String token;
	
	private ChannelFuture future;
	
	private Channel serverChannel;
	
	private Bootstrap remoteClient;
	
	private EventLoopGroup remoteGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	public NettyClient(Channel serverChannel,
			Map<String,NettyClient> nettyClientMap) {
		this.serverChannel = serverChannel;
		this.remoteClient = new Bootstrap();
		this.remoteGroup = new NioEventLoopGroup();
		this.nettyClientMap = nettyClientMap;
	}
	
	public void connection(Message message) {
		this.token = message.getToken();
		ConnectionInfo conn = (ConnectionInfo) message.getData();
		this.remoteClient.group(remoteGroup).channel(NioSocketChannel.class);
		this.remoteClient.option(ChannelOption.TCP_NODELAY, true);// 有消息后立刻发送
		this.remoteClient.option(ChannelOption.SO_KEEPALIVE, true);// 保持长连接
		this.remoteClient.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new ForwardHandler(token,serverChannel,nettyClientMap));
			}
		});
		
		future = this.remoteClient.connect(conn.getHost(), conn.getPort()).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message();
				message.setToken(token);
				if(future.isSuccess()) {
					message.setType(MessageCode.CONNECTION_SUCCESS);
					nettyClientMap.put(token, NettyClient.this);
					logger.info("Connection to " + conn.getHost() + ":" + conn.getPort() + " successfully.");
				}else {
					message.setType(MessageCode.CONNECTION_ERROR);
					logger.info("Connection to " + conn.getHost() + ":" + conn.getPort() + " failed.");
				}
				serverChannel.writeAndFlush(message);
			}
		});
	}
	
	public void writeAndFlush(Object msg) {
		future.channel().writeAndFlush(msg);
	}
	
	public void close() {
		future.channel().close();
		remoteGroup.shutdownGracefully();
	}
}
