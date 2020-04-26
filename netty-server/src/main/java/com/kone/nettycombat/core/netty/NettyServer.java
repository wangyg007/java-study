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

import java.util.Calendar;
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
                log.info((String.format("load service class: %s",interName)));
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

               //阻塞等待结果
               ChannelFuture channelFuture = bootstrap.bind(ip, port).sync();
               if (channelFuture.isSuccess()){
                   log.info("server start success,bind:"+serverAddress);
               }
               serverRegister.register(serverAddress);

               /**
                在这里面future.channel().closeFuture().sync();这个语句的主要目的是，方便测试，方便写一个非springboot的demo,
                比如一个简单地junit test方法，closeFuture().sync()可以阻止junit test将server关闭，
                同时停止test应用的时候也不需要手动再调用关闭服务器的方法workerGroup.shutdownGracefully()...。这样设计在测试时省心。

                但是，当将nettyserver联系到springboot应用的启动时，例如nettyserver设置为@Component,当springboot扫描到nettyserver时，
                springboot主线程执行到nettyserver的postconstruct注解的方法，然后发生了future.channel().closeFuture().sync();
                这样导致springboot主线程阻塞，无法继续加载剩下的bean,
                更糟糕的是，如果springboot还添加了springboot-web的依赖（自带tomcat容器），
                那么被阻塞后将无法启动tomcat servlet engine和webapplicationcontext.

                所以不能简单地在nettyserver中的构造方法/init方法中写future.channel().closeFuture().sync();和workerGroup.shutdownGracefully().
                只需在构造方法/init方法中bootstrap.bind(port),这是异步的，不会阻塞springboot主线程。而将stop方法单独抽取出来。

                需要注意的是，即使直接关闭springboot应用，不手动调用上面的stop方法，nettyserver也会将之前绑定的端口解除，
                为了保险起见，可以将stop方法添加@predestroy注解
                */
               //channelFuture.channel().closeFuture().sync();
           }catch (Exception e){
                e.printStackTrace();
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully();
           }


        }).start();
    }

}
