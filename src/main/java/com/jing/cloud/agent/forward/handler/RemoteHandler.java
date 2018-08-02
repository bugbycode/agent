package com.jing.cloud.agent.forward.handler;

import com.jing.cloud.agent.forward.thread.ClientStartupRunnable;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoteHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private ClientStartupRunnable run;
	
	public RemoteHandler(ClientStartupRunnable run) {
		this.run = run;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		
		Message message = new Message();
		message.setData(data);
		message.setToken("");
		message.setType(MessageCode.TRANSFER_DATA);
		run.writeAndFlush(message);
	}

	
}
