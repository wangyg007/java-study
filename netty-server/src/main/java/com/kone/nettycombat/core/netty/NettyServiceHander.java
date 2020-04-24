package com.kone.nettycombat.core.netty;

import com.alibaba.fastjson.JSON;
import com.kone.nettycombat.entity.Request;
import com.kone.nettycombat.entity.Response;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author wangyg
 * @time 2020/4/23 16:02
 * @note
 **/
@Slf4j
@ChannelHandler.Sharable
public class NettyServiceHander extends ChannelInboundHandlerAdapter {

    private final Map<String,Object> serviceMap;

    public NettyServiceHander(Map<String,Object> serviceMap){
        this.serviceMap=serviceMap;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("client connect success:"+ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("client disconnect:"+ctx.channel().remoteAddress());;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Request request = JSON.parseObject(msg.toString(), Request.class);
        if ("heartBeat".equals(request.getMethodName())){
            log.info("client:"+ctx.channel().remoteAddress()+" heartbeat msg");
        }else {
            log.info("client request:"+JSON.toJSONString(msg));
//            Response response = new Response();
//            response.setRequestId(request.getId());
//            try {
//                Object res = this.handler(request);
//                response.setData(res);
//            } catch (Throwable throwable) {
//                throwable.printStackTrace();
//                response.setCode(1);
//                response.setError_msg(throwable.toString());
//            }
//            ctx.writeAndFlush(response);
        }
    }


    /**
     * 通过反射，执行本地方法
     * @param request
     * @return
     * @throws Throwable
     */
    private Object handler(Request request) throws Throwable{
        String className = request.getClassName();
        Object serviceBean = serviceMap.get(className);

        if (serviceBean!=null){
            Class<?> serviceClass = serviceBean.getClass();
            String methodName = request.getMethodName();
            Class<?>[] parameterTypes = request.getParameterTypes();
            Object[] parameters = request.getParameters();

            Method method = serviceClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(serviceBean, getParameters(parameterTypes,parameters));
        }else{
            throw new Exception("未找到服务接口,请检查配置!:"+className+"#"+request.getMethodName());
        }
    }

    /**
     * 获取参数列表
     * @param parameterTypes
     * @param parameters
     * @return
     */
    private Object[] getParameters(Class<?>[] parameterTypes,Object[] parameters){
        if (parameters==null || parameters.length==0){
            return parameters;
        }else{
            Object[] new_parameters = new Object[parameters.length];
            for(int i=0;i<parameters.length;i++){
                new_parameters[i] = JSON.parseObject(parameters[i].toString(),parameterTypes[i]);
            }
            return new_parameters;
        }
    }

}
