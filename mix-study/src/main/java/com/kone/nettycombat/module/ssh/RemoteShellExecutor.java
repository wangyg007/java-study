package com.kone.nettycombat.module.ssh;

/**
 * @author wangyg
 * @time 2020/4/29 10:35
 * @note
 **/

import ch.ethz.ssh2.*;
import com.kone.nettycombat.common.utils.IdUtil;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RemoteShellExecutor {

    private Connection conn;
    /** 远程机器IP */
    private String ip;
    /** 用户名 */
    private String osUsername;
    /** 密码 */
    private String password;
    private String charset = Charset.defaultCharset().toString();

    private static final int TIME_OUT = 1000 * 5 * 60;

    public RemoteShellExecutor(String ip, String usr, String pasword) {
        this.ip = ip;
        this.osUsername = usr;
        this.password = pasword;
    }



    /**
     * 登录
     * @return
     * @throws IOException
     */
    private boolean login() throws IOException {
        conn = new Connection(ip);
        conn.connect();
        return conn.authenticateWithPassword(osUsername, password);
    }

    /**
     * 执行脚本
     *
     * @param cmds
     * @return
     * @throws Exception
     */
    public int exec(String cmds) throws Exception {
        InputStream stdOut = null;
        InputStream stdErr = null;
        String outStr = "";
        String outErr = "";
        int ret = -1;
        try {
            if (login()) {
                // Open a new {@link Session} on this connection
                Session session = conn.openSession();
                // Execute a command on the remote machine.
                session.execCommand(cmds);
                stdOut = new StreamGobbler(session.getStdout());
                outStr = processStream(stdOut, charset);

                stdErr = new StreamGobbler(session.getStderr());
                outErr = processStream(stdErr, charset);

                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);

                System.out.println("outStr=" + outStr);
                System.out.println("outErr=" + outErr);

                ret = session.getExitStatus();
            } else {
                throw new Exception("登录远程机器失败" + ip); // 自定义异常类 实现略
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
            IOUtils.closeQuietly(stdOut);
            IOUtils.closeQuietly(stdErr);
        }
        return ret;
    }

    private String processStream(InputStream in, String charset) throws Exception {
        byte[] buf = new byte[1024];
        StringBuilder sb = new StringBuilder();
        while (in.read(buf) != -1) {
            sb.append(new String(buf, charset));
        }
        return sb.toString();
    }

    private String processStdErr(InputStream in, String charset)
            throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, charset));
        StringBuffer sb = new StringBuffer();
        if (in.available() != 0) {
            while (true) {
                String line = br.readLine();
                if (line == null)
                {
                    break;
                }
                sb.append(line).append(System.getProperty("line.separator"));
            }
        }
        return sb.toString();
    }

    /**
     * 执行脚本
     *
     * @param cmds
     * @return
     * @throws Exception
     */
    public int exec2(String cmds) throws Exception {
        InputStream stdOut = null;
        InputStream stdErr = null;
        String outStr = "";
        String outErr = "";
        int ret = -1;
        try {
            if (login()) {
                Session session = conn.openSession();
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
                // 7. 等待，除非1.连接关闭；2.输出数据传送完毕；3.进程状态为退出；4.超时


                System.out.println("Here is the output from stdout:");
                while (true)
                {
                    String line = stdoutReader.readLine();
                    if (line == null) { break; }
                    System.out.println(line);
                }
                System.out.println("Here is the output from stderr:");
                while (true) {
                    String line = stderrReader.readLine();
                    if (line == null) { break; }
                    System.out.println(line);
                }
                /* Show exit status, if available (otherwise "null") */

                session.waitForCondition(ChannelCondition.CLOSED | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS , 30000);

                System.out.println("ExitCode: " + session.getExitStatus());
                ret = session.getExitStatus();
                session.close();/* Close this session */
                conn.close();/* Close the connection */

            } else {
                throw new Exception("登录远程机器失败" + ip); // 自定义异常类 实现略
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
            if (null!=stdOut){stdOut.close();}
            if (null!=stdErr){stdErr.close();}
        }
        return ret;
    }

    /**
     * 远程传输单个文件
     *
     * @param localFile
     * @param remoteTargetDirectory
     * @throws IOException
     */

    public void transferFile(String localFile, String remoteTargetDirectory) throws Exception {
        File file = new File(localFile);
        if (file.isDirectory()) {
            throw new RuntimeException(localFile + "  is not a file");
        }
        String fileName = file.getName();
        exec2("mkdir -p " + remoteTargetDirectory);

        SCPClient sCPClient = conn.createSCPClient();
        SCPOutputStream scpOutputStream = sCPClient.put(fileName, file.length(), remoteTargetDirectory, "0600");

        String content = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        scpOutputStream.write(content.getBytes());
        scpOutputStream.flush();
        scpOutputStream.close();
    }

    public String transferFile2(String content, String remoteTargetDirectory) throws Exception {

        String filename=IdUtil.getId()+".json";
        SCPOutputStream scpOutputStream=null;

        try {
//            exec2("mkdir -p " + remoteTargetDirectory);
//
           if (this.login()){
               SCPClient sCPClient = conn.createSCPClient();
               scpOutputStream = sCPClient.put(filename, content.getBytes().length, remoteTargetDirectory, "0600");

               scpOutputStream.write(content.getBytes());
               scpOutputStream.flush();
               return filename;
           }else {
               throw new RuntimeException("登陆远程主机失败");
           }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (null!=scpOutputStream){
                scpOutputStream.close();
            }
            if (null!=conn){
                conn.close();
            }
        }

        return null;

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
                transferFile(fullName, remoteTargetDirectory);
            }
        }

    }

    public static void main(String args[]) throws Exception {
        RemoteShellExecutor executor = new RemoteShellExecutor("192.168.140.128", "root", "0000");
        // 执行myTest.sh 参数为java Know dummy
        ///usr/bin/python2.7 /usr/local/datax/bin/datax.py /usr/local/datax/script/file_2_file.json
//        String command="/usr/bin/python2.7 /usr/local/datax/bin/datax.py /usr/local/datax/script/file_2_file.json";
//        System.out.println(executor.exec2(command));
        executor.transferFile2("123","/usr/local/datax/script");
    }
}