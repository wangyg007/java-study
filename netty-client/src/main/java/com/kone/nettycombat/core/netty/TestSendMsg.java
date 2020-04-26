package com.kone.nettycombat.core.netty;

import com.kone.nettycombat.common.utils.IdUtil;
import com.kone.nettycombat.entity.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author wangyg
 * @time 2020/4/24 17:00
 * @note
 **/
@Component
@Slf4j
public class TestSendMsg {

    @Autowired
    NettyClient nettyClient;

    /**
     * Constructor(@Component) > @Autowired > @PostConstruct
     */
    @PostConstruct
    public void test(){
        log.info("test send...");
        final Request request = new Request();

        /**
         * 测试发送消息
         */
        new Thread(()->{
            while (true){
                try {

                    request.setId(IdUtil.getId());
                    request.setMethodName("test");
                    request.setClassName("class");

                    nettyClient.send(request);

                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.error("e:",e);
                }
            }
        }).start();
    }

}
