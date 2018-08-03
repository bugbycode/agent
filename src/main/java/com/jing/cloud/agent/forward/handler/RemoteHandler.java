package com.jing.cloud.agent.forward.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoteHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(RemoteHandler.class);
	
	private Channel serverChannel;
	
	private String token;
	
	public RemoteHandler(Channel serverChannel,String token) {
		this.serverChannel = serverChannel;
		this.token = token;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		logger.info("目标设备返回消息" + msg);
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		
		//System.out.println("recv " + new String(data));
		
		Message message = new Message();
		message.setData(data);
		message.setToken(token);
		message.setType(MessageCode.TRANSFER_DATA);
		serverChannel.writeAndFlush(message);
	}

	
}
