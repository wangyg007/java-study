package com.kone.nettycombat.core.netty;

import com.alibaba.fastjson.JSONArray;
import com.kone.nettycombat.core.codec.json.JSONDecoder;
import com.kone.nettycombat.core.codec.json.JSONEncoder;
import com.kone.nettycombat.entity.Request;
import com.kone.nettycombat.entity.Response;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.SocketAddress;
import java.util.concurrent.SynchronousQueue;

/**
 * @author wangyg
 * @time 2020/4/23 18:15
 * @note
 **/

@Slf4j
@Component
public class NettyClient {

    private EventLoopGroup group = new NioEventLoopGroup(1);
    private Bootstrap bootstrap = new Bootstrap();

    @Autowired
    NettyClientHandler handler;
    @Autowired

    ChanelManager chanelManager;

    public NettyClient() {

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true)
                .option(ChannelOption.SO_KEEPALIVE,true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(0,0,30));
                        pipeline.addLast(new JSONEncoder());
                        pipeline.addLast(new JSONDecoder());
                        pipeline.addLast("handler",handler);
                    }
                });
    }

    /**
     * 服务器销毁serverlet前执行
     */
    @PreDestroy
    public void destroy(){
        log.info("client quit,release source....");
        group.shutdownGracefully();
    }

    public Channel doConnect(SocketAddress address) throws InterruptedException {
        ChannelFuture fu = bootstrap.connect(address);
        Channel channel = fu.sync().channel();
        return channel;
    }


    public String send(Request request) throws InterruptedException {
        Channel channel = chanelManager.chooseChannel();

        if (null!=channel && channel.isActive()){
            SynchronousQueue<Object> queue = handler.sendReq(request, channel);
            //Object take = queue.take();
            return "success";
        }

        Response res = new Response();
        res.setCode(1);
        res.setError_msg("have not useable chanel...!");
        return JSONArray.toJSONString(res);
    }

}
