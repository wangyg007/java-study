package com.kone.nettycombat.module.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author wangyg
 * @time 2020/4/27 13:57
 * @note
 **/
public class JdkDynProxy implements InvocationHandler{

    private Object target;

    public Object newProxy(Object target){
        this.target=target;
        return Proxy.newProxyInstance(target.getClass().getClassLoader(),target.getClass().getInterfaces(),this);
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        before();
        Object invoke = method.invoke(this.target, args);
        after();
        return invoke;
    }

    private void before(){
        System.out.println("调用之前执行");
    }

    private void after(){
        System.out.println("调用之后执行");
    }


    public static void main(String[] args) {
        JdkDynProxy jdkDynProxy = new JdkDynProxy();
        SayHello sayHelloProx = (SayHello) jdkDynProxy.newProxy(new SayHelloImpl());
        sayHelloProx.sysHello();

    }
}
