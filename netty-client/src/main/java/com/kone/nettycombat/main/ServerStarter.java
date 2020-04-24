package com.kone.nettycombat.main;

import io.netty.channel.EventLoopGroup;

/**
 * @author wangyg
 * @time 2020/4/23 11:54
 * @note
 **/
public class ServerStarter {

    private EventLoopGroup boss;

    private EventLoopGroup worker;

    private int port;

    private static int DEFAULT_PORT=8080;

    public ServerStarter(int port){
        if (port<0 || port>65535){
            throw new IllegalArgumentException("port:"+port+" is illegal");
        }
        this.port=port;
    }

    public void startUp(){

        if (boss.isShutdown() || worker.isShutdown()){
            throw new IllegalStateException("server was closed");
        }



    }


}
