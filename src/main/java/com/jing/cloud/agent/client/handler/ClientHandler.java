package com.jing.cloud.agent.client.handler;

import java.util.Map;

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
		logger.info("收到服务器返回消息:" + msg);
		Message message = (Message)msg;
		String token = message.getToken();
		int type = message.getType();
		if(type == MessageCode.REGISTER_ERROR) {
			ctx.close();
			logger.info("认证失败关闭连接......");
		}else if(type == MessageCode.REGISTER_SUCCESS) {
			logger.info("认证成功......");
		} else if(type == MessageCode.CLOSE_CONNECTION) {//关闭连接
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.close();
			}
		} else if(type == MessageCode.TRANSFER_DATA) { //通信二进制数据
			//将数据传输至目标设备
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				byte[] data = (byte[]) message.getData();
				ByteBuf buf = ctx.alloc().buffer(data.length);
				buf.writeBytes(data);
				client.writeAndFlush(buf);
			}
		} else if(type == MessageCode.CONNECTION) { //建立连接
			NettyClient client = new NettyClient(ctx.channel(), nettyClientMap);
			client.connection(message);
		}
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//logger.info("连接服务成功");
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		//logger.info("代理客户端消息读取完毕......");
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error(cause.getMessage());
        cause.printStackTrace();
        new Thread(run).start();
    }
	
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //logger.info("连接关闭");
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
				//logger.debug("发送心跳");
				Message msg = new Message();
				msg.setType(MessageCode.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			} else if (event.state() == IdleState.ALL_IDLE) {
				/* 总超时 */
				//logger.debug("===服务端===(ALL_IDLE 总超时)");
			}
		}
	}
}
