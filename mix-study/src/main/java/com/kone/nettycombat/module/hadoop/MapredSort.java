package com.kone.nettycombat.module.hadoop;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
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

/**
 * @author wangyg
 * @time 2020/4/28 10:22
 * @note
 **/
public class MapredSort extends Configured implements Tool {


    static class SortMapper extends Mapper<LongWritable, Text, IntWritable,IntWritable>{

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            IntWritable valueW = new IntWritable(Integer.parseInt(value.toString()));
            context.write(valueW,valueW);

        }
    }

    static class SortReducer extends Reducer<IntWritable,IntWritable,IntWritable,IntWritable>{

        @Override
        protected void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            while (null!=values && values.iterator().hasNext()){
                context.write(values.iterator().next(),values.iterator().next());
            }
        }
    }


    @Override
    public int run(String[] strings) throws Exception {
        if (strings.length!=2){
            System.out.println("<input><output>");
            System.exit(127);
        }

        Job job = Job.getInstance(getConf());
        job.setJarByClass(MapredSort.class);
        FileInputFormat.addInputPath(new JobConf(getConf()),new Path(strings[0]));
        FileOutputFormat.setOutputPath(new JobConf(getConf()),new Path(strings[1]));

        job.setMapperClass(SortMapper.class);
        job.setReducerClass(SortReducer.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);

        job.setNumReduceTasks(1);
        job.setJobName("sort job");
        return job.waitForCompletion(true)?0:1;
    }

    public static void main(String[] args) throws Exception {
        int run = ToolRunner.run(new MapredSort(), args);
        System.exit(run);
    }

}
