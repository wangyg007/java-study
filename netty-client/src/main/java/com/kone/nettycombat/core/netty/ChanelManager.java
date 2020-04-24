package com.kone.nettycombat.core.netty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.channel.Channel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wangyg
 * @time 2020/4/24 14:22
 * @note
 **/
@Component
@Slf4j
public class ChanelManager {

    @Autowired
    NettyClient nettyClient;
    //轮训id
    private AtomicInteger roundRobin = new AtomicInteger(0);
    //所有channel
    private List<Channel> channels = new CopyOnWriteArrayList<>();
    //远程服务和chanel的关系
    private Map<SocketAddress, Channel> channelNodes = new ConcurrentHashMap<>();
    private Lock updateLock = new ReentrantLock(true);

    /**
     * 轮训获取channel
     *
     * @return
     */
    public Channel chooseChannel() {

        if (channels.size() > 0) {
            int size = channels.size();
            int index = (roundRobin.getAndAdd(1) + size) % size;
            return channels.get(index);
        }
        return null;
    }

    /**
     * 根据server列表更新channels和channelNodes
     * @param list
     */
    public void updateConnectServer(List<String> list) {
        updateLock.lock();
        try {

            if (null == list || list.isEmpty()) {
                log.error("have no useable server list!!!");
                for (final Channel channel : channels) {
                    SocketAddress remoteAddress = channel.remoteAddress();
                    Channel channelNode = channelNodes.get(remoteAddress);
                    channelNode.close();
                }
                channels.clear();
                channelNodes.clear();
                return;
            }

            HashSet<SocketAddress> allServerNodeSet = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {

                String[] split = list.get(i).split(":");
                if (2 == split.length) {
                    String host=split[0];
                    int port = Integer.parseInt(split[1]);
                    final SocketAddress remotePeer=new InetSocketAddress(host,port);
                    allServerNodeSet.add(remotePeer);
                }
            }

            for (final SocketAddress address: allServerNodeSet){
                Channel channel = channelNodes.get(address);
                if (null!=channel && channel.isOpen()){
                    log.info("current server-node-channel exist,not nead update!!!");
                }else {
                    //新连接
                    connectServerNode(address);
                }
            }

            for (int i=0;i<channels.size();i++){
                Channel channel = channels.get(i);
                SocketAddress address = channel.remoteAddress();
                if (!allServerNodeSet.contains(address)){
                    log.info("delete unabel channel:"+address);
                    Channel channelNode = channelNodes.get(address);
                    if (null!=channelNode){
                        channelNode.close();
                    }
                    channels.remove(channel);
                    channelNodes.remove(address);
                }
            }

        }catch (Exception e){
            log.error("updateConnectServer e:",e);
        }finally {
            updateLock.unlock();
        }

    }

    private void connectServerNode(SocketAddress address){
        try {
            Channel channel = nettyClient.doConnect(address);
            addChannel(channel,address);

        }catch (Exception e){
            log.error("e:",e);
        }
    }

    private void addChannel(Channel channel, SocketAddress address) {
        channels.add(channel);
        channelNodes.put(address,channel);
        log.info("add channel:{}",address);
    }


    public void removeChannel(Channel channel){
        SocketAddress address = channel.remoteAddress();
        channelNodes.remove(address);
        channels.remove(channel);
        log.info("remove channel:{}",address);
    }

}
