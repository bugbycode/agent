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
		logger.info("�յ�������������Ϣ:" + msg);
		Message message = (Message)msg;
		String token = message.getToken();
		int type = message.getType();
		if(type == MessageCode.REGISTER_ERROR) {
			ctx.close();
			logger.info("��֤ʧ�ܹر�����......");
		}else if(type == MessageCode.REGISTER_SUCCESS) {
			logger.info("��֤�ɹ�......");
		} else if(type == MessageCode.CLOSE_CONNECTION) {//�ر�����
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.close();
			}
		} else if(type == MessageCode.TRANSFER_DATA) { //ͨ�Ŷ���������
			//�����ݴ�����Ŀ���豸
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				byte[] data = (byte[]) message.getData();
				ByteBuf buf = ctx.alloc().buffer(data.length);
				buf.writeBytes(data);
				client.writeAndFlush(buf);
			}
		} else if(type == MessageCode.CONNECTION) { //��������
			NettyClient client = new NettyClient(ctx.channel(), nettyClientMap);
			client.connection(message);
		}
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//logger.info("���ӷ���ɹ�");
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		//logger.info("����ͻ�����Ϣ��ȡ���......");
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
        //logger.info("���ӹر�");
        super.channelInactive(ctx);
        
    }

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				/* ����ʱ */
				logger.debug("===�����===(READER_IDLE ����ʱ)");
			} else if (event.state() == IdleState.WRITER_IDLE) {
				/* д��ʱ */
				//logger.debug("��������");
				Message msg = new Message();
				msg.setType(MessageCode.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			} else if (event.state() == IdleState.ALL_IDLE) {
				/* �ܳ�ʱ */
				//logger.debug("===�����===(ALL_IDLE �ܳ�ʱ)");
			}
		}
	}
}
