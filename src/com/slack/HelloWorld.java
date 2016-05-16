package com.slack;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import com.jcraft.jsch.*;

public class HelloWorld {

    public static void main(String[] args) throws IOException{

        if (args.length != 3) {
            throw new IllegalArgumentException("!!!Missing user and/or password and/or hostname!!!");
        }
        String host=args[2];
        String user=args[0];
        String password=args[1];

        String command = concatinateCommands();
        System.out.println("Command to run on remote system over SSH: " + command);
        try{

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            Session session=jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            System.out.println("Connected");

            Channel channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec)channel).setErrStream(System.err);

            InputStream in=channel.getInputStream();
            channel.connect();
            byte[] tmp=new byte[1024];
            while(true){
                while(in.available()>0){
                    int i=in.read(tmp, 0, 1024);
                    if(i<0)break;
                    System.out.print(new String(tmp, 0, i));
                }
                if(channel.isClosed()){
                    System.out.println("exit-status: "+channel.getExitStatus());
                    break;
                }
                try{Thread.sleep(1000);}catch(Exception ee){}
            }
            BufferedReader br =new  BufferedReader(new InputStreamReader(in));
            Thread.sleep(10000);
            boolean ready = false;
            int c = 0;
            StringBuilder line = new StringBuilder();
            while((ready = br.ready()) == true){
                ready = br.ready();
                c = br.read();
                System.out.print(String.valueOf((char) c));
            }
            channel.disconnect();
            session.disconnect();
            System.out.println("DONE");
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private static String concatinateCommands() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("rm -f ~/helloWorld.sh");
        sb.append(";");

        // logic for installing all packages
        InputStream packageList = HelloWorld.class.getResourceAsStream("resources/packages.txt");
        BufferedReader readerPackageList = new BufferedReader(new InputStreamReader(packageList));

        String packageName;
        while ((packageName = readerPackageList.readLine()) != null) {
            sb.append("apt-get -y install " + packageName);
            sb.append(";");
        }
        readerPackageList.close();

        // logic for getting all files to be created
        // filesList contains path to file and folder
        InputStream filesList = HelloWorld.class.getResourceAsStream("resources/filesList.txt");
        BufferedReader readerFilesList = new BufferedReader(new InputStreamReader(filesList));

        String fileNameDestPath;
        while ((fileNameDestPath = readerFilesList.readLine()) != null) {
            List<String> fileNamePathSplit = Arrays.asList(fileNameDestPath.split("/"));

            String fileName = fileNamePathSplit.get(fileNamePathSplit.size() - 1);

            InputStream fileLocalPath = HelloWorld.class.getResourceAsStream("resources/" + fileName);
            BufferedReader readerFileContent = new BufferedReader(new InputStreamReader(fileLocalPath));

            String fileNameContent;
            StringBuilder sbFileContent = new StringBuilder();
            while ((fileNameContent = readerFileContent.readLine()) != null) {
                sbFileContent.append(fileNameContent + "\n");
            }
            readerFileContent.close();
            fileLocalPath.close();
            sb.append("echo \'" + sbFileContent.toString() + "\' > " + fileNameDestPath);
            sb.append(";");
        }
        readerFilesList.close();

        // logic for starting all services
        InputStream serviceList = HelloWorld.class.getResourceAsStream("resources/services.txt");
        BufferedReader readerServiceList = new BufferedReader(new InputStreamReader(serviceList));

        String serviceName;
        while ((serviceName = readerServiceList.readLine()) != null) {
            sb.append("/etc/init.d/" + serviceName + " restart" + " >> ~/helloWorld.sh");
            sb.append(";");
        }
        readerServiceList.close();

        sb.append(" > /helloWorld.log 2>&1 &");

        return sb.toString();
    }

}