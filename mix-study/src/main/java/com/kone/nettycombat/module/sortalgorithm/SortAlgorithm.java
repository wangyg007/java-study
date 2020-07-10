package com.kone.nettycombat.module.sortalgorithm;

/**
 * @author wangyg
 * @time 2020/7/9 15:39
 * @note ref: https://www.cnblogs.com/itsharehome/p/11058010.html
 **/
public class SortAlgorithm {

    public static int[] aa=new int[]{131,41,1,41,1,1,4124,121,4135,523,3,32,341,141,41,4,12,4,1224,124,12,4,124,1
    ,41,4,12,12,412,4,14,1,41,1,4,122,412,412,412,24,12,1515122,1515122,14,1412412,4,124,12,412,4,124,12,412,4,124,12,4
    ,14,1,125,1,533,5,235,5,2335,2,5,235,23,5,325,5,3,51,5,15,1,51,51,5,1,51,41};

    /**
     * 选择排序
     * 1、时间复杂度：O(n2)  2、空间复杂度：O(1)  3、非稳定排序  4、原地排序
     *
     * 非稳定排序:
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
     * 冒泡排序
     * 1、时间复杂度：O(n2)  2、空间复杂度：O(1)  3、稳定排序  4、原地排序
     *
     * 例：序列58529,两两比较第一个5和第二个5相对顺序不变
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

    /**
     * 计数排序
     * 1、时间复杂度：O(n+k)  2、空间复杂度：O(k)  3、稳定排序  4、非原地排序
     * @return
     */
    public static int[] counterSort(){
        if (aa.length<2){return aa;}
        int n=aa.length;
        int max=0;
        for (int i=0;i<n;i++){
            if (max<aa[i]){max=aa[i];}
        }
        int[] temp=new int[max+1];
        for (int i=0;i<n;i++){
            temp[aa[i]]++;
        }
        int k=0;
        for (int i=0;i<=max;i++){
            //temp[i]存的是i出现的次数
            for(int j=temp[i];j>0;j--){
                aa[k++]=i;
            }
        }
        return aa;
    }

    /**
     * 快速排序，中轴排序，二分排序
     *
     * 1．先从数列中取出一个数作为基准数。
     * 2．分区过程，将比这个数大的数全放到它的右边，小于或等于它的数全放到它的左边。
     * 3．再对左右区间重复第二步，直到各区间只有一个数。
     *
     * 1、时间复杂度：O(nlogn)  2、空间复杂度：O(logn)  3、非稳定排序  4、原地排序
     * @return
     */
    public static int[] quickSort(int[] aaa,int low,int high){
        if (low<high){
            int mid=getIndex(aaa,low,high);
            aaa=quickSort(aaa,low,mid-1);
            aaa=quickSort(aaa,mid+1,high);
        }
        return aaa;
    }

    private static int getIndex( int[] aaa, int low, int high ) {
        //1．先从数列中取出一个数作为基准数。
        int tmp=aaa[low];
        while (low<high){

            //小于或等于它的数全放到它的左边
            while (low<high && aaa[high]>=tmp){
                high--;
            }
            aaa[low]=aaa[high];

            //将比这个数大的数全放到它的右边
            while (low<high && aaa[low]<tmp){
                low++;
            }
            aaa[high]=aaa[low];

        }
        //跳出循环，此时low=high
        aaa[low]=tmp;
        return low;
    }

    /**
     * 二分查找，局限性：只能对有序集查找，遇到相同值只能返回其中一个的index
     * @param x
     * @return
     */
    public static int halfSearch(int x){
        int[] sort=quickSort(aa,0,aa.length-1);
        int low=0;
        int high=sort.length-1;
        while (low<=high){
            int mid=(low+high)/2;
            if (x==sort[mid]){
                return mid;
            }
            if (x<sort[mid]){
                high=mid-1;
            }else if (x>sort[mid]){
                low=mid+1;
            }
        }
        return -1;
    }

    /**
     * 二分查找,递归版本
     * @param x
     * @return
     */
    public static int halfSearchPro(int[] sort,int x,int low,int high){
        if (low>high){return -1;}

        int mid=(low+high)/2;
        if (x==sort[mid]){
            return mid;
        }else {
            if (x<sort[mid]){
                return halfSearchPro(sort,x,low,mid-1);
            }else{
                return halfSearchPro(sort,x,mid+1,high);
            }
        }
    }



    public static void main( String[] args ) {
        long start = System.currentTimeMillis();
        int[] aaa=quickSort(aa,0,aa.length-1);
        for(int i : aaa){
            System.out.print(i+" ");
        }
        System.out.println();
        System.out.println(halfSearchPro(aaa,2,0,aaa.length));
        long end = System.currentTimeMillis();
        System.out.println(String.format("cost time:%d ms", end-start));

    }

}
