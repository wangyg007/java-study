package com.kone.nettycombat.module.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author wangyg
 * @time 2020/4/28 10:22
 * @note
 **/
public class MapredSort extends Configured implements Tool {


    static class SortMapper extends Mapper<LongWritable, Text, IntWritable, IntWritable>{

        private int topN;
        //从大到小排序
        private TreeMap<Integer,Integer> treeMap= new TreeMap<>((o1, o2) -> {
            if (o1 > o2) {
                return -1;
            }
            return 0;
        });

        /**
         * 从Configuration获取topN参数
         * @param context
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            topN=context.getConfiguration().getInt("topN",10);
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            //大于topn时，移除最小的
            if (treeMap.size()>=topN){
                treeMap.remove(treeMap.lastKey());
            }
            treeMap.put(Integer.parseInt(value.toString()),null);

        }

        /**
         * map任务结束时，写数据
         * @param context
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            for (Map.Entry entry:treeMap.entrySet()){
                context.write(new IntWritable(Integer.parseInt(String.valueOf(entry.getKey()))),
                        new IntWritable(Integer.parseInt(String.valueOf(entry.getKey()))));
            }
        }
    }




    static class SortReducer extends Reducer<IntWritable,IntWritable,IntWritable,NullWritable>{

        private int topN;
        //从大到小排序
        private TreeMap<Integer,Integer> treeMap= new TreeMap<>((o1, o2) -> {
            if (o1 > o2) {
                return -1;
            }
            return 0;
        });

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            topN=context.getConfiguration().getInt("topN",10);
        }


        @Override
        protected void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            for (IntWritable intWritable:values){
                if (treeMap.size()>=topN){
                    treeMap.remove(treeMap.lastKey());
                }
                treeMap.put(intWritable.get(),null);
            }

        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            for (Map.Entry entry:treeMap.entrySet()){
                context.write(new IntWritable(Integer.parseInt(String.valueOf(entry.getKey()))),
                        NullWritable.get());
            }
        }
    }


    @Override
    public int run(String[] strings) throws Exception {
        if (strings.length!=3){
            System.out.println("<topN><input><output>");
            System.exit(127);
        }

        Configuration conf = getConf();
        conf.setInt("topN",Integer.parseInt(strings[0]));

        Job job = Job.getInstance(conf);
        job.setJarByClass(MapredSort.class);
        FileInputFormat.addInputPath(new JobConf(conf),new Path(strings[1]));
        FileOutputFormat.setOutputPath(new JobConf(conf),new Path(strings[2]));

        job.setMapperClass(SortMapper.class);
        job.setReducerClass(SortReducer.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(NullWritable.class);

        job.setNumReduceTasks(1);
        job.setJobName("sort job");
        return job.waitForCompletion(true)?0:1;
    }

    public static void main(String[] args) throws Exception {
        int run = ToolRunner.run(new MapredSort(), args);
        System.exit(run);

    }

}
