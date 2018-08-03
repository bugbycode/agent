package com.jing.cloud.agent.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.module.Authentication;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.util.RandomUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private boolean registerStatus = false; //�ͻ���ע��״̬
	
	public ClientHandler() {
		
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info("�յ�������������Ϣ:" + msg);
		Message message = (Message)msg;
		int type = message.getType();
		Object data = message.getData();
		if(type == MessageCode.REGISTER_ERROR) {
			ctx.close();
			logger.info("��֤ʧ�ܹر�����......");
		}else if(type == MessageCode.REGISTER_SUCCESS) {
			logger.info("��֤�ɹ�......");
			registerStatus = true;
		} else if(type == MessageCode.TRANSFER_DATA) {
			
		} else if(type == MessageCode.CONNECTION) {
			Message result = new Message(message.getToken(), MessageCode.CONNECTION_SUCCESS, data);
			ctx.channel().writeAndFlush(result);
		}
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("���ӷ���ɹ�");
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.info("��Ϣ��ȡ���......");
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error(cause.getMessage());
    }
	
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("���ӹر�");
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
				logger.debug("��������");
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
				/* �ܳ�ʱ */
				logger.debug("===�����===(ALL_IDLE �ܳ�ʱ)");
			}
		}
	}
}
