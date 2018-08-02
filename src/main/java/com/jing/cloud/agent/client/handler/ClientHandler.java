package com.jing.cloud.agent.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	public ClientHandler() {
		
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info("收到服务器返回消息:" + msg);
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("连接服务成功");
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.info("消息读取完毕......");
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error(cause.getMessage());
    }
	
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("连接关闭");
        super.channelInactive(ctx);
    }

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				/* 读超时 */
				logger.debug("===服务端===(READER_IDLE 读超时)");
			} else if (event.state() == IdleState.WRITER_IDLE) {
				/* 写超时 */
				logger.debug("发送心跳");
				Message msg = new Message();
				msg.setType(MessageCode.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			} else if (event.state() == IdleState.ALL_IDLE) {
				/* 总超时 */
				logger.debug("===服务端===(ALL_IDLE 总超时)");
			}
		}
	}
}
