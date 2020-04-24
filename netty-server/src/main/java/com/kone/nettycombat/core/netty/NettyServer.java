package com.kone.nettycombat.core.netty;

import com.kone.nettycombat.common.anotation.MyService;
import com.kone.nettycombat.core.codec.json.JSONDecoder;
import com.kone.nettycombat.core.codec.json.JSONEncoder;
import com.kone.nettycombat.core.zookeeper.ServerRegister;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wangyg
 * @time 2020/4/23 14:51
 * @note
 **/
@Slf4j
@Component
public class NettyServer implements ApplicationContextAware, InitializingBean {

    private static final EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    private static final EventLoopGroup workGroup = new NioEventLoopGroup(4);

    private Map<String,Object> serviceMap=new HashMap<>();

    @Value("${rpc.server.address}")
    private String serverAddress;

    @Autowired
    ServerRegister serverRegister;

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(MyService.class);
        for (Object bean:beans.values()){
            Class<?> clazz = bean.getClass();
            Class<?>[] interfaces = clazz.getInterfaces();

            for (Class<?> inter:interfaces){
                String interName = inter.getName();
                log.info((String.format("加载服务类: %s",interName)));
                serviceMap.put(interName,bean);
            }
        }
    }

    public void start(){
        final NettyServiceHander nettyServiceHander = new NettyServiceHander(serviceMap);

        new Thread(()->{
           try {
               ServerBootstrap bootstrap = new ServerBootstrap();
               bootstrap.group(bossGroup,workGroup)
                       .channel(NioServerSocketChannel.class)
                       .option(ChannelOption.SO_BACKLOG,1024)
                       .childOption(ChannelOption.SO_KEEPALIVE,true)
                       .childOption(ChannelOption.TCP_NODELAY,true)
                       .childHandler(new ChannelInitializer<SocketChannel>() {
                           @Override
                           protected void initChannel(SocketChannel ch) throws Exception {
                               ChannelPipeline pipeline = ch.pipeline();
                               pipeline.addLast(new IdleStateHandler(0,0,60));
                               pipeline.addLast(new JSONEncoder());
                               pipeline.addLast(new JSONDecoder());
                               pipeline.addLast(nettyServiceHander);
                           }
                       });

               String[] ipPort = serverAddress.split(":");
               String ip=ipPort[0];
               int port=Integer.parseInt(ipPort[1]);

               ChannelFuture channelFuture = bootstrap.bind(ip, port).sync();
               log.info("server start success,bind:"+port);
               serverRegister.register(serverAddress);

               channelFuture.channel().closeFuture().sync();
           }catch (Exception e){
                e.printStackTrace();
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully();
           }


        }).start();
    }

}
