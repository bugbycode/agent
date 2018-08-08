package com.jing.cloud.agent.client.handler;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.agent.client.thread.StartupRunnable;
import com.jing.cloud.forward.client.NettyClient;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private StartupRunnable run;
	
	private Map<String,NettyClient> nettyClientMap;
	
	public ClientHandler(StartupRunnable run,
			Map<String,NettyClient> nettyClientMap) {
		this.run = run;
		this.nettyClientMap = nettyClientMap;
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message)msg;
		String token = message.getToken();
		int type = message.getType();
		if(type == MessageCode.REGISTER_ERROR) {
			ctx.close();
		}else if(type == MessageCode.REGISTER_SUCCESS) {
			logger.info("Agent auth successfully......");
		} else if(type == MessageCode.CLOSE_CONNECTION) {
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.close();
			}
		} else if(type == MessageCode.TRANSFER_DATA) {
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				byte[] data = (byte[]) message.getData();
				ByteBuf buf = ctx.alloc().buffer(data.length);
				buf.writeBytes(data);
				client.writeAndFlush(buf);
			}
		} else if(type == MessageCode.CONNECTION) {
			NettyClient client = new NettyClient(ctx.channel(), nettyClientMap);
			client.connection(message);
		}
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error(cause.getMessage());
        closeAllNettyClient();
    }
	
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        closeAllNettyClient();
        new Thread(run).start();
    }

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				logger.debug("Heartbeat timeout.");
			} else if (event.state() == IdleState.WRITER_IDLE) {
				Message msg = new Message();
				msg.setType(MessageCode.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			}
		}
	}
	
	private void closeAllNettyClient() {
		Set<Entry<String,NettyClient>> set = nettyClientMap.entrySet();
		synchronized (nettyClientMap) {
			Iterator<Entry<String,NettyClient>> it = set.iterator();
			while(it.hasNext()) {
				Entry<String,NettyClient> entry = it.next();
				NettyClient client = entry.getValue();
				client.close();
			}
			nettyClientMap.clear();
		}
	}
}
