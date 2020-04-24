package com.kone.nettycombat.core.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author wangyg
 * @time 2020/4/23 15:37
 * @note
 **/
@Slf4j
@Component
public class ServerRegister {

    @Value("${zookeeper.register.address}")
    private String registAddress;

    @Value("${zookeeper.register.root_dir}")
    private String rootDir;

    public void register(String data){

       try {
           if (!StringUtils.isEmpty(data)){
               ZkClient zkClient = connectServer();
               if (null!=zkClient){
                   addRootNode(zkClient);
                   createNode(zkClient,data);
               }
           }
       }catch (Exception e){
           e.printStackTrace();
       }

    }

    private ZkClient connectServer(){
       return new ZkClient(registAddress, 20000, 20000);
    }

    private void addRootNode(ZkClient client){
        boolean exists = client.exists(rootDir);
        if (!exists){
            client.createPersistent(rootDir);
            log.info("zk create root node:"+rootDir);
        }
    }

    private void createNode(ZkClient client,String data){
        String path = client.create(rootDir + "/server", data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        log.info("create zk path:"+path);

    }

}
