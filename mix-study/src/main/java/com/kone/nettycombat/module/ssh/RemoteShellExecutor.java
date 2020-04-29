package com.kone.nettycombat.module.ssh;

/**
 * @author wangyg
 * @time 2020/4/29 10:35
 * @note
 * @ref https://www.jianshu.com/p/513c72dfee1b
 **/

import ch.ethz.ssh2.*;
import com.kone.nettycombat.common.utils.IdUtil;
import com.kone.nettycombat.module.ssh.entity.ExeRes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;

@Slf4j
@Component
public class RemoteShellExecutor implements InitializingBean {

    private volatile Connection conn;
    /** 远程机器IP */
    @Value("${remote.ssh.ip}")
    private String ip;
    /** 用户名 */
    @Value("${remote.ssh.username}")
    private String osUsername;
    /** 密码 */
    @Value("${remote.ssh.password}")
    private String password;

    private String charset = Charset.defaultCharset().toString();

    private static final int TIME_OUT = 1000 * 5 * 60;

    private RemoteShellExecutor(){
        log.info("private RemoteShellExecutor construct...");
    }

    /**
     * 配置加载完毕后登陆
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        login();
    }

    /**
     * 登录
     * @return
     * @throws IOException
     */
    private synchronized boolean login() throws IOException {

       if (null==conn){
           conn = new Connection(ip);
           conn.connect();
           return conn.authenticateWithPassword(osUsername, password);
       }else {
           return true;
       }

    }


    /**
     * 升级版 执行脚本
     *
     * @param cmds
     * @return
     * @throws Exception
     */
    public ExeRes exec2(String cmds) throws Exception {
        ExeRes exeRes = new ExeRes();
        StringBuilder stringBuilder = new StringBuilder();
        InputStream stdOut = null;
        InputStream stdErr = null;
        Session session=null;
        String outStr = "";
        String outErr = "";
        int ret = -1;
        try {
            if (login()) {
                session = conn.openSession();
                // 建立虚拟终端
                session.requestPTY("bash");
                // 打开一个Shell
                session.startShell();
                stdOut = new StreamGobbler(session.getStdout());
                stdErr = new StreamGobbler(session.getStderr());
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdOut));
                BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stdErr));

                // 准备输入命令
                PrintWriter out = new PrintWriter(session.getStdin());
                // 输入待执行命令
                out.println(cmds);
                out.println("exit");
                // 6. 关闭输入流
                out.close();
                
                log.info("Here is the output from stdout:");
                stringBuilder.append("Here is the output from stdout:");
                while (true)
                {
                    String line = stdoutReader.readLine();
                    if (line == null) { break; }
                    log.info(line);
                    stringBuilder.append(line);
                }
                log.info("Here is the output from stderr:");
                stringBuilder.append("Here is the output from stderr:");
                while (true) {
                    String line = stderrReader.readLine();
                    if (line == null) { break; }
                    log.info(line);
                    stringBuilder.append(line);
                }

                // 7. 等待，除非1.连接关闭；2.输出数据传送完毕；3.进程状态为退出；4.超时
                session.waitForCondition(ChannelCondition.CLOSED | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS , 30000);

                log.info("ExitCode: " + session.getExitStatus());
                ret = session.getExitStatus();
                exeRes.setCode(ret);

            } else {
                throw new Exception("登录远程机器失败" + ip); // 自定义异常类 实现略
            }
        }finally {
            if (null!=stdOut){stdOut.close();}
            if (null!=stdErr){stdErr.close();}
            if (null!=session){session.close();}
        }
        exeRes.setResStr(stringBuilder.toString());
        return exeRes;
    }

    /**
     * 根据文件名 远程传输单个文件
     * @param fileName
     * @param remoteTargetDirectory
     * @return
     * @throws Exception
     */
    public void transferFile1(String fileName, String remoteTargetDirectory) throws Exception {

        File file=null;
        SCPOutputStream scpOutputStream=null;
        SCPClient sCPClient=null;
        try {
            if (this.login()){
                file = new File(fileName);
                sCPClient = conn.createSCPClient();
                scpOutputStream = sCPClient.put(file.getName(), file.length(), remoteTargetDirectory, "0600");

                String content = IOUtils.toString(new FileInputStream(file), charset);

                scpOutputStream.write(content.getBytes());
                scpOutputStream.flush();

            }else {
                throw new RuntimeException("登陆远程主机失败");
            }
        }catch (Exception e){
           throw new Exception(e);
        }finally {
            if (null!=scpOutputStream){
                scpOutputStream.close();
            }
            if (null!=sCPClient){sCPClient=null;}
            if (null!=file){file=null;}
        }

    }

    /**
     * 升级版 根据string内容远程传输单个文件
     * @param content
     * @param remoteTargetDirectory
     * @return
     * @throws Exception
     */
    public String transferFile2(String content, String remoteTargetDirectory) throws Exception {

        String filename=IdUtil.getId()+".json";
        SCPOutputStream scpOutputStream=null;
        SCPClient sCPClient=null;
        try {
           if (this.login()){
               sCPClient = conn.createSCPClient();
               scpOutputStream = sCPClient.put(filename, content.getBytes().length, remoteTargetDirectory, "0600");

               scpOutputStream.write(content.getBytes());
               scpOutputStream.flush();
               return filename;
           }else {
               throw new RuntimeException("登陆远程主机失败");
           }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            if (null!=scpOutputStream){
                scpOutputStream.close();
            }
            if (null!=sCPClient){sCPClient=null;}
        }

    }

    /**
     * 传输整个目录
     *
     * @param localDirectory
     * @param remoteTargetDirectory
     * @throws IOException
     */
    public void transferDirectory(String localDirectory, String remoteTargetDirectory) throws Exception {
        File dir = new File(localDirectory);
        if (!dir.isDirectory()) {
            throw new RuntimeException(localDirectory + " is not directory");
        }

        String[] files = dir.list();
        for (String file : files) {
            if (file.startsWith(".")) {
                continue;
            }
            String fullName = localDirectory + "/" + file;
            if (new File(fullName).isDirectory()) {
                String rdir = remoteTargetDirectory + "/" + file;
                exec2("mkdir -p " + remoteTargetDirectory + "/" + file);
                transferDirectory(fullName, rdir);
            } else {
                transferFile1(fullName, remoteTargetDirectory);
            }
        }

    }

    public static void main(String args[]) throws Exception {

        // 执行myTest.sh 参数为java Know dummy
        ///usr/bin/python2.7 /usr/local/datax/bin/datax.py /usr/local/datax/script/file_2_file.json
        //        String command="/usr/bin/python2.7 /usr/local/datax/bin/datax.py /usr/local/datax/script/file_2_file.json";
        //        System.out.println(executor.exec2(command));

    }


}