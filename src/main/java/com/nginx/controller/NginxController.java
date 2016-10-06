package com.nginx.controller;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Nginx Web Controller
 *
 * @author Ian
 * @since 2016-10-06
 */
@Controller
@RequestMapping(
        value = "/nginx",
        produces = MediaType.TEXT_PLAIN_VALUE
)
public class NginxController {

    private static final List<String> CMDS = new ArrayList<String>(){{
        add("/version");
        add("/check");

        add("/service/start");
        add("/service/stop");
        add("/service/reload");
        add("/service/status");
        add("/service/force-reload");
        add("/service/configtest");
        add("/service/upgrade");
        add("/service/restart");
        add("/service/reopen_logs");
    }};

    private static final Set<String> SERVICE_OPT = new HashSet<String>(){{
        add("start");
        add("stop");
        add("reload");
        add("status");
        add("force-reload");
        add("configtest");
        add("upgrade");
        add("restart");
        add("reopen_logs");
    }};

    private String output(boolean bool) throws Exception {
        JSONObject res = new JSONObject();

        String status = "OK!";
        if ( !bool ) {
            status = "Meh!";
        }
        res.put("status", status);

        return res.toString(4);
    }

    private String output(boolean bool, String msg) throws Exception {
        JSONObject res = new JSONObject(this.output(bool));
        res.put("message", msg);

        return res.toString(4);
    }

    @RequestMapping
    @ResponseBody
    public String listCommand() throws Exception {
        JSONArray res = new JSONArray(CMDS);
        return res.toString(4);
    }

    @RequestMapping( "/check" )
    @ResponseBody
    public String check() throws Exception {
        final String line = "rpm -qa | grep 'nginx'";
        final boolean available = this.executeCommandLine(line);
        return this.output(available);
    }

    @RequestMapping( "/version" )
    @ResponseBody
    public String version() throws Exception {
        final String line = "nginx -v";
        Map.Entry<Boolean, String> res = this.execToString(line);
        return this.output(res.getKey(), res.getValue());
    }

    @RequestMapping( "/service/{option}" )
    @ResponseBody
    public String restart(@PathVariable("option") String option) throws Exception {
        final String res;
        if ( !SERVICE_OPT.contains(option) ) {
            res = this.output(false, "Please refer to " + SERVICE_OPT.toString());
        }
        else {
            final String line = "sudo service nginx " + option;

            if ( option.equalsIgnoreCase("status") ) {
                boolean success = false;
                try {
                    CommandLine cmd = CommandLine.parse(line);
                    DefaultExecutor exec = new DefaultExecutor();
                    final int exit = exec.execute(cmd);
                    success = exit == 0;
                }
                catch (ExecuteException e) {
                    final String msg = e.getMessage();
                    if ( !msg.endsWith("(Exit value: 3)") ) {
                        throw new ExecuteException(msg, e.getExitValue());
                    }
                }

                if ( !success ) {
                    res = this.output(false, "nginx is stopped");
                }
                else {
                    res = this.output(true, "nginx is running");
                }
            }
            else {
                Map.Entry<Boolean, String> entry = this.execToString(line);
                res = this.output(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    /**
     * Execute Command with Output String.
     *
     * @param command Command Line
     * @return Output dari Command yang di-execute
     * @throws Exception
     */
    private Map.Entry<Boolean, String> execToString(String command) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CommandLine commandline = CommandLine.parse(command);
        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);

        final int exit = exec.execute(commandline);

        Map.Entry<Boolean, String> res =
                new AbstractMap.SimpleEntry<>(exit == 0, outputStream.toString().trim());
        outputStream.close();

        return res;
    }

    /**
     * Execute Command with return True for success.
     *
     * @param line Command Line String
     * @return True: Success
     * @throws Exception
     */
    private boolean executeCommandLine(String line) throws Exception {
        CommandLine cmd = CommandLine.parse(line);

        DefaultExecutor exec = new DefaultExecutor();
        final int exit = exec.execute(cmd);

        return exit == 0;
    }
}
