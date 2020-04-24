package com.kone.nettycombat.core.zookeeper;

import com.alibaba.fastjson.JSON;
import com.kone.nettycombat.core.netty.ChanelManager;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangyg
 * @time 2020/4/24 15:44
 * @note
 **/
@Slf4j
@Component
public class ServerDiscover {

    @Value("${zookeeper.register.address}")
    private String zkAddres;

    @Value("${zookeeper.register.root_dir}")
    private String zkRootDir;

    @Autowired
    ChanelManager chanelManager;

    private volatile List<String> serverList=new ArrayList<>();
    private ZkClient zkClient;

    /**
     * 服务器加载servlet完成后执行
     * Constructor(@Component) > @Autowired > @PostConstruct
     */
    @PostConstruct
    public void  init(){
        zkClient=createZkClient();
        if (null!=zkClient){
            log.info("success connect to zk:{}",zkAddres);
            watchNode(zkClient);
        }
    }

    private ZkClient createZkClient(){
        ZkClient zkClient = new ZkClient(zkAddres, 20000, 20000);
        return zkClient;
    }

    private void watchNode(final ZkClient zkClient){
        List<String> nodeList = zkClient.subscribeChildChanges(zkRootDir, (s, nodes) -> {
            log.info("listen zk:{},data change,node:{}", zkAddres, JSON.toJSONString(nodes));
            serverList.clear();
            getNodeData(nodes);
            chanelManager.updateConnectServer(serverList);

        });

        getNodeData(nodeList);
        log.info("found server list from zk:{}",serverList);
        chanelManager.updateConnectServer(serverList);

    }

    private void getNodeData(List<String> nodes){
        for (String node:nodes){
            String serverAddress = zkClient.readData(zkRootDir + "/" + node);
            log.info("zk:{},data change to:{}",zkAddres, serverAddress);
            serverList.add(serverAddress);
        }
    }


}
