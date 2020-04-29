package com.kone.nettycombat.module.ssh.controller;

import com.kone.nettycombat.module.ssh.RemoteShellExecutor;
import com.kone.nettycombat.module.ssh.entity.ExeRes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangyg
 * @time 2020/4/29 11:35
 * @note
 **/
@RestController
@Api(description = "datax远程调用")
@RequestMapping("/datax")
@Slf4j
public class RemoteShellController {


    @Autowired
    RemoteShellExecutor remoteShellExecutor;

    private static final String BASE_SHELL_DIR="/usr/local/datax/script/";

    private static final String BIN="/usr/bin/python2.7 /usr/local/datax/bin/datax.py ";

    @ApiOperation("datax远程调用")
    @PostMapping("remoteExecute")
    //@ApiImplicitParam(name="json",value = "json配置",paramType = "query",required = true,dataType = "String")
    public ExeRes remoteExecuteDatax(@RequestBody String dto){
        ExeRes res=null;
        try {
            String file = remoteShellExecutor.transferFile2(dto, BASE_SHELL_DIR);
            if (!StringUtils.isEmpty(file)){
                log.info("file create sucess:"+file);
                res = remoteShellExecutor.exec2(BIN + BASE_SHELL_DIR+file);
            }
        } catch (Exception e) {
            log.error("remoteExecute e:",e);
        }
        return res;
    }

}
