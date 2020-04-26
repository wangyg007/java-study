package com.kone.nettycombat.core.netty;

import com.kone.nettycombat.core.codec.json.JSONDecoder;
import com.kone.nettycombat.core.codec.json.JSONEncoder;
import com.kone.nettycombat.entity.Request;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.SocketAddress;

/**
 * @author wangyg
 * @time 2020/4/23 18:15
 * @note
 **/

@Slf4j
@Component
public class NettyClient implements InitializingBean {

    public NettyClient() {

    }

    private EventLoopGroup group = new NioEventLoopGroup(1);
    private Bootstrap bootstrap = new Bootstrap();

    @Autowired
    NettyClientHandler handler;
    @Autowired
    ChanelManager chanelManager;

    @Value("${rpc.client.address}")
    private String clientAddr;

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }


    private void start(){
        try {
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

            String[] split = clientAddr.split(":");
            if (split.length==2){
                String host=split[0];
                int port=Integer.parseInt(split[1]);
                bootstrap.bind(host,port).sync();
                log.info("client start success,bind:"+port);

            }
        }catch (Exception e){
            log.error("client start error:{}",e);
        }
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


    public void send(Request request) throws InterruptedException {
        Channel channel = chanelManager.chooseChannel();

        if (null!=channel && channel.isActive()){
            handler.sendReq(request, channel);
           return;
        }
    }


}
