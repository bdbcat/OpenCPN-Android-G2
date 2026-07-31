package org.opencpn;


import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessController {

    private final Activity activity;
    private String m_nativeLibraryDir;

    public ProcessController(Activity activity) {
        this.activity = activity;
        initNativeLibraryDir(activity);
    }

    private void initNativeLibraryDir(Activity activity) {
        try {
            ApplicationInfo ainfo =
                    activity.getPackageManager().getApplicationInfo(
                            "org.opencpn.opencpn",
                            PackageManager.GET_SHARED_LIBRARY_FILES
                    );

            m_nativeLibraryDir = ainfo.nativeLibraryDir;
            Log.i("OpenCPN", "native library dir " + m_nativeLibraryDir);

        } catch (Exception e) {
            Log.i("OpenCPN", "Exception on getApplicationInfo");
        }
    }

    public String createProcSync5stdout(String cmd, String arg1, String arg2, String arg3, String arg4, String arg5) {

        String cmdExec = cmd;
        if (android.os.Build.VERSION.SDK_INT > 28) {
            File cmdFile = new File(cmd);
            cmdExec = m_nativeLibraryDir + "/lib" + cmdFile.getName() + ".so";
        }

        File file = new File(cmdExec);
        if (!file.exists())
            Log.i("OpenCPN", "createProcSync5 cmd executable does not exist: " + cmdExec);
        else {
            Log.i("OpenCPN", "Setting executable on: " + cmdExec);
            boolean exect = file.setExecutable(true);
        }

        long pid = 0;
        String result = "";
        StringBuilder sbresult = new StringBuilder();

        try {
            List<String> args_list = new ArrayList<String>();
            args_list.add(cmdExec);
            args_list.add(arg1);
            args_list.add(arg2);
            args_list.add(arg3);
            args_list.add(arg4);
            args_list.add(arg5);

            ProcessBuilder pb = new ProcessBuilder()
                    .command(args_list)
                    .redirectErrorStream(true);

            Log.i("OpenCPN", "Process launched as: [" + cmdExec + "]");
            Process process = pb.start();

            // Reads stdout.
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            char[] buffer = new char[10000];
            int nbCharRead = 0;
            try {
                while ((nbCharRead = in.read(buffer, 0, 10000)) != -1) {
                    String chunk = new String(buffer, 0, nbCharRead);
                    sbresult.append(chunk);
                }
            } catch (Exception e) {
                Log.i("OpenCPN", "createProcSync5stdout exception A: " + e.getMessage());
                e.printStackTrace();
            }

            result = sbresult.toString();

/*
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            result = output.toString();
*/
            Log.i("OpenCPN", "createProcSync5stdout cmd output: " + result);
            process.waitFor();

        } catch (Exception e) {
            Log.i("OpenCPN", "createProcSync5stdout exceptionB: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public String createProc(String cmd, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String libPath) {
        Log.i("OpenCPN", "createProc");
        Log.i("OpenCPN", "cmd: " + cmd);
        Log.i("OpenCPN", "arg1: " + arg1);
        Log.i("OpenCPN", "arg2: " + arg2);
        Log.i("OpenCPN", "arg3: " + arg3);
        Log.i("OpenCPN", "arg4: " + arg4);
        Log.i("OpenCPN", "arg5: " + arg5);
        Log.i("OpenCPN", "arg6: " + arg6);
        Log.i("OpenCPN", "libPath: " + libPath);
        String libraryPath = activity.getApplicationInfo().dataDir + "/lib";
        Log.i("OpenCPN", "libraryPath: " + libraryPath);

        String cmdExec = cmd;

        if (android.os.Build.VERSION.SDK_INT > 28) {
            File cmdFile = new File(cmd);
            cmdExec = m_nativeLibraryDir + "/lib" + cmdFile.getName() + ".so";
        }

        File file = new File(cmdExec);
        if (!file.exists())
            Log.i("OpenCPN", "createProc cmd executable does not exist: " + cmdExec);
        else {
            Log.i("OpenCPN", "Setting executable on: " + cmdExec);
            file.setExecutable(true);
        }

        long pid = 0;
        try {
//            ProcessBuilder pb = new ProcessBuilder(cmdExec, arg1, arg2, arg3, arg4, arg5, "df9f5b06-4715-3f6e-85c4-90514d0935a3;xxxx; : A7600-F Lenovo A7600-F");
            ProcessBuilder pb = new ProcessBuilder(cmdExec, arg1, arg2, arg3, arg4, arg5, arg6);

            Map<String, String> env = pb.environment();
            env.put("LD_LIBRARY_PATH", libPath);

            Log.i("OpenCPN", "Process launched as: [" + cmdExec + "]");

            Process process = pb.start();
            Log.i("OpenCPN", "Process ClassName: " + process.getClass().getName());

            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                /* get the PID on unix/linux systems */
                Log.i("OpenCPN", "Fetching PID...");
                try {
                    Field f = process.getClass().getDeclaredField("pid");
                    f.setAccessible(true);
                    pid = f.getLong(process);
                } catch (Throwable e) {
                }
            } else {
                pid = 1;
                Log.i("OpenCPN", "Process:  unknown Class name");
            }


        } catch (Exception e) {
            Log.i("OpenCPN", "createProc6 exception: " + e.getMessage());
            e.printStackTrace();
        }

        String ret = String.valueOf(pid);
        Log.i("OpenCPN", "Process PID: " + ret);
        return ret;
    }

    public String createProc(String cmd, String arg1, String arg2, String libPath) {
        Log.i("OpenCPN", "createProc");
        Log.i("OpenCPN", "cmd: " + cmd);
        Log.i("OpenCPN", "arg1: " + arg1);
        Log.i("OpenCPN", "arg2: " + arg2);
        Log.i("OpenCPN", "libPath: " + libPath);
        String libraryPath = activity.getApplicationInfo().dataDir + "/lib";
        Log.i("OpenCPN", "libraryPath: " + libraryPath);

        String cmdExec = cmd;
        if (Build.VERSION.SDK_INT > 28) {
            File cmdFile = new File(cmd);
            cmdExec = m_nativeLibraryDir + "/lib" + cmdFile.getName() + ".so";
        }

        File file = new File(cmdExec);
        if (!file.exists())
            Log.i("OpenCPN", "createProc cmd executable does not exist: " + cmdExec);
        else {
            Log.i("OpenCPN", "Setting executable on: " + cmdExec);
            file.setExecutable(true);
        }

        long pid = 0;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmdExec, arg1, arg2);

            Map<String, String> env = pb.environment();
            env.put("LD_LIBRARY_PATH", libPath);

            //env.remove("OTHERVAR");
            //env.put("VAR2", env.get("VAR1") + "suffix");

            //pb.directory(new File("myDir"));
            //File log = new File("log");
            //pb.redirectErrorStream(true);
            //pb.redirectOutput(Redirect.appendTo(log));

            Log.i("OpenCPN", "Process launched as: [" + cmd + "]");

            Process process = pb.start();
            Log.i("OpenCPN", "Process ClassName: " + process.getClass().getName());

            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                /* get the PID on unix/linux systems */
                Log.i("OpenCPN", "Fetching PID...");
                try {
                    Field f = process.getClass().getDeclaredField("pid");
                    f.setAccessible(true);
                    pid = f.getLong(process);
                } catch (Throwable e) {
                }
            } else {
                pid = 1;
                Log.i("OpenCPN", "Process:  unknown Class name");
            }


        } catch (Exception e) {
            Log.i("OpenCPN", "createProcB exception: " + e.getMessage());
            e.printStackTrace();
        }

        String ret = String.valueOf(pid);
        Log.i("OpenCPN", "Process PID: " + ret);
        return ret;
    }

    public String createProcSync4(String cmd, String arg1, String arg2, String arg3, String arg4, String libPath) {
        Log.i("OpenCPN", "createProcSync4");
        Log.i("OpenCPN", "cmd: " + cmd);
        Log.i("OpenCPN", "arg1: " + arg1);
        Log.i("OpenCPN", "arg2: " + arg2);
        Log.i("OpenCPN", "arg3: " + arg3);
        Log.i("OpenCPN", "arg4: " + arg4);
        Log.i("OpenCPN", "libPath: " + libPath);


        String cmdExec = cmd;
        if (Build.VERSION.SDK_INT > 28) {
            File cmdFile = new File(cmd);
            cmdExec = m_nativeLibraryDir + "/lib" + cmdFile.getName() + ".so";
        }

        File file = new File(cmdExec);
        if (!file.exists())
            Log.i("OpenCPN", "createProcSync4 cmd executable does not exist: " + cmdExec);
        else {
            Log.i("OpenCPN", "Setting executable on: " + cmdExec);
            file.setExecutable(true);
        }

        long pid = 0;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmdExec, arg1, arg2, arg3, arg4);
            pb.redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("LD_LIBRARY_PATH", libPath);


            Log.i("OpenCPN", "Process launched as: [" + cmdExec + "]");

            Process process = pb.start();

            // Reads stdout.
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            String result = output.toString();
            Log.i("OpenCPN", "createProcSync4 cmd output: " + result);

            Log.i("OpenCPN", "Process ClassName: " + process.getClass().getName());

            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                /* get the PID on unix/linux systems */
                Log.i("OpenCPN", "Fetching PID...");
                try {
                    Field f = process.getClass().getDeclaredField("pid");
                    f.setAccessible(true);
                    pid = f.getLong(process);
                } catch (Throwable e) {
                }
            } else {
                pid = 1;
                Log.i("OpenCPN", "Process:  unknown Class name");
            }

            process.waitFor();

        } catch (Exception e) {
            Log.i("OpenCPN", "createProcSync4 exception: " + e.getMessage());
            e.printStackTrace();
        }

        String ret = String.valueOf(pid);
        Log.i("OpenCPN", "Process PID: " + ret);
        return ret;

    }







}