package com.kone.nettycombat.core.netty;

import com.alibaba.fastjson.JSON;
import com.kone.nettycombat.entity.Request;
import com.kone.nettycombat.entity.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.SynchronousQueue;

/**
 * @author wangyg
 * @time 2020/4/24 15:13
 * @note
 **/
@Component
@Slf4j
@ChannelHandler.Sharable
public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    NettyClient client;

    @Autowired
    ChanelManager chanelManager;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("success connect to server:{}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        log.info("disconnect to server:{}", address);
        ctx.channel().close();
        chanelManager.removeChannel(ctx.channel());
    }

    /**
     * 获取请求返回
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        Response response = JSON.parseObject(msg.toString(), Response.class);
        String requestId = response.getRequestId();

    }

    /**
     * 发送消息
     * @param request
     * @param channel
     * @return
     */
    public void sendReq(Request request, Channel channel) {

        channel.writeAndFlush(request).addListener(
                new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if (future.isSuccess()){
                            log.info("response success:{}",future.get());
                        }else {
                            Response response = new Response();
                            response.setCode(1);
                            response.setError_msg(null==future.get()?"no response from sever":future.get().toString());
                            log.info("response success:{}",future.get());
                        }
                    }
                }
        );
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("interval 30s,heartbeat...");
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event= (IdleStateEvent) evt;
            if (event.state()==IdleState.ALL_IDLE){
                Request request = new Request();
                request.setMethodName("heartBeat");
                ctx.channel().writeAndFlush(request);
            }
        }else {
            super.userEventTriggered(ctx,evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("communication error:",cause);
        ctx.channel().close();
    }
}
