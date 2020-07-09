package com.kone.nettycombat.module.sortalgorithm;

/**
 * @author wangyg
 * @time 2020/7/9 15:39
 * @note
 **/
public class SortAlgorithm {

    public static int[] aa=new int[]{131,41,1,41,1,1,4124,121,4135,523,3,32,341,141,41,4,12,4,1224,124,12,4,124,1
    ,41,4,12,12,412,4,14,1,41,1,4,122,412,412,412,24,12,1515122,1515122,14,1412412,4,124,12,412,4,124,12,412,4,124,12,4
    ,14,1,125,1,533,5,235,5,2335,2,5,235,23,5,325,5,3,51,5,15,1,51,51,5,1,51,41};

    /**
     * 非稳定排序
     * 举个例子，序列58529，我们知道第一遍选择第1个元素5会和2交换，
     * 那么原序列中两个5的相对前后顺序就被破坏了，所以选择排序是一个不稳定的排序算法
     * @return
     */
    public static int[] selectSort(){
        int len=aa.length;
        for (int i=0;i<len-1;i++){
            for (int j=i+1;j<len;j++){
                if (aa[i]>aa[j]){
                    int tmp=aa[i];
                    aa[i]=aa[j];
                    aa[j]=tmp;
                }
            }
        }
        return aa;
    }

    /**
     * 序列58529,两两比较第一个5和第二个5相对顺序不变
     * @return
     */
    public static int[] maopaoSort(){
        int len=aa.length;
        for (int n=len;n>1;n--){
            //一趟循环所有元素没有出现交换，则跳出循环
            boolean swap=false;
            for (int i=0;i<n-1;i++){
                if (aa[i]>aa[i+1]){
                    swap=true;
                    int tmp=aa[i];
                    aa[i]=aa[i+1];
                    aa[i+1]=tmp;
                }
            }
            if (!swap){
                break;
            }
        }
        return aa;
    }



    public static void main( String[] args ) {
        long start = System.currentTimeMillis();
        int[] aaa=maopaoSort();
        long end = System.currentTimeMillis();
        System.out.println(String.format("cost time:%d ms", end-start));
        for(int a :aaa){
            System.out.println(a);
        }
    }

}
