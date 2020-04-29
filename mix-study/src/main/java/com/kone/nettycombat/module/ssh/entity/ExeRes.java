package com.kone.nettycombat.module.ssh.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author wangyg
 * @time 2020/4/29 17:44
 * @note
 **/
@Data
@ApiModel("shell执行返回结果vo")
public class ExeRes {

    @ApiModelProperty(value = "日志")
    private String resStr;

    @ApiModelProperty(value = "状态")
    private int code=1;

}
