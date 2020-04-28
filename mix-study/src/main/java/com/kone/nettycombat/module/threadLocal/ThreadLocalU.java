package com.kone.nettycombat.module.threadLocal;

/**
 * @author wangyg
 * @time 2020/4/28 13:50
 * @note
 **/
public class ThreadLocalU {

    private final ThreadLocal<Integer> threadLocal=new ThreadLocal<>();

    public Integer getValue(){
        Integer integer = threadLocal.get();
        if (null==integer){
            threadLocal.set(0);
            return 0;
        }
        return threadLocal.get();
    }

    public void setValue(Integer value){
        threadLocal.set(value);
    }


    public static void main(String[] args) {
        ThreadLocalU threadLocalU = new ThreadLocalU();

        new Thread(()->{
            int i=0;
            while (true){
                System.out.println(Thread.currentThread().getName()+" value:"+threadLocalU.getValue());
                i++;
                threadLocalU.setValue(i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(()->{
            int i=0;
            while (true){
                System.out.println(Thread.currentThread().getName()+" value:"+threadLocalU.getValue());
                i++;
                threadLocalU.setValue(i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
