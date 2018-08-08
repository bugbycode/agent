package com.jing.cloud.forward.handler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.forward.client.NettyClient;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ForwardHandler.class);
	
	private Channel serverChannel;
	
	private String token;
	
	private Map<String,NettyClient> nettyClientMap;
	
	public ForwardHandler(String token,Channel serverChannel,Map<String,NettyClient> nettyClientMap) {
		this.serverChannel = serverChannel;
		this.token = token;
		this.nettyClientMap = nettyClientMap;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		Message message = new Message();
		message.setType(MessageCode.TRANSFER_DATA);
		message.setToken(token);
		
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		message.setData(data);
		
		serverChannel.writeAndFlush(message);
	}

	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
		Message message = new Message();
		message.setType(MessageCode.CLOSE_CONNECTION);
		message.setToken(token);
		serverChannel.writeAndFlush(message);
		super.channelInactive(ctx);
		
		NettyClient client = nettyClientMap.get(token);
		if(client != null) {
			client.close();
		}
		nettyClientMap.remove(token);
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		logger.error("Exception caught {}.", cause.getMessage());
        super.exceptionCaught(ctx, cause);
    }
}
