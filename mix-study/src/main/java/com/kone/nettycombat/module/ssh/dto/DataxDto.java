package com.kone.nettycombat.module.ssh.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author wangyg
 * @time 2020/4/29 14:25
 * @note
 **/
@Data
@ApiModel("datax json 请求体")
public class DataxDto {

    @ApiModelProperty(value = "json配置",required = true)
    private String json;

}
