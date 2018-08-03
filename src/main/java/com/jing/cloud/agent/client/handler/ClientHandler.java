package com.jing.cloud.agent.client.handler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.agent.forward.thread.ClientStartupRunnable;
import com.jing.cloud.module.Authentication;
import com.jing.cloud.module.ConnectionInfo;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.util.RandomUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private boolean registerStatus = false; //客户端注册状态
	
	private Map<String,ClientStartupRunnable> clientMap;
	
	public ClientHandler(Map<String,ClientStartupRunnable> clientMap) {
		this.clientMap = clientMap;
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info("收到服务器返回消息:" + msg);
		Message message = (Message)msg;
		String token = message.getToken();
		int type = message.getType();
		Object data = message.getData();
		if(type == MessageCode.REGISTER_ERROR) {
			ctx.close();
			logger.info("认证失败关闭连接......");
		}else if(type == MessageCode.REGISTER_SUCCESS) {
			logger.info("认证成功......");
			registerStatus = true;
		} else if(type == MessageCode.TRANSFER_DATA) { //通信二进制数据
			ClientStartupRunnable client = clientMap.get(token);
			if(client == null) {
				return;
			}
			byte[] data_buff = (byte[]) data;
			ByteBuf buf = ctx.alloc().buffer(data_buff.length);
			buf.writeBytes(data_buff);
			client.writeAndFlush(buf);//将数据传输至目标设备
		} else if(type == MessageCode.CONNECTION) { //建立连接
			
			ConnectionInfo conn = (ConnectionInfo) message.getData();
			
			ClientStartupRunnable client = new ClientStartupRunnable(ctx.channel(), token, 
					conn.getHost(), conn.getPort(),clientMap);
			new Thread(client).start();
		}
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
				if(registerStatus) {
					msg.setType(MessageCode.HEARTBEAT);
				}else {
					//String token = RandomUtil.GetGuid32();
					msg.setType(MessageCode.REGISTER);
					Authentication authInfo = new Authentication("fort", "fort");
					msg.setData(authInfo);
					//msg.setToken(token);
				}
				ctx.channel().writeAndFlush(msg);
			} else if (event.state() == IdleState.ALL_IDLE) {
				/* 总超时 */
				logger.debug("===服务端===(ALL_IDLE 总超时)");
			}
		}
	}
}
