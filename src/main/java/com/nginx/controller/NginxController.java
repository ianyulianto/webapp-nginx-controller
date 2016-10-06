package com.nginx.controller;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.codehaus.jettison.json.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Nginx Web Controller
 *
 * @author Ian
 * @since 2016-10-06
 */
@Controller
@RequestMapping(
        value = "/nginx",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class NginxController {

    private static final List<String> CMDS;
    static {
        CMDS = new ArrayList<>();
        CMDS.add("/version");
        CMDS.add("/check");
        CMDS.add("/reload");
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

        JSONArray res = new JSONArray();
        if ( available ) {
            res.put("OK");
        }
        else {
            res.put("Meh!");
        }
        return res.toString(4);
    }

    @RequestMapping( "/version" )
    @ResponseBody
    public String version() throws Exception {
        final String line = "nginx -v";
        final String output = this.execToString(line);

        JSONArray res = new JSONArray();
        if ( output != null ) {
            res.put(output);
        }
        return res.toString(4);
    }

    @RequestMapping( "/reload" )
    @ResponseBody
    public String reload() throws Exception {
        final String line = "sudo service nginx reload";
        final boolean available = this.executeCommandLine(line);

        JSONArray res = new JSONArray();
        if ( available ) {
            res.put("OK");
        }
        else {
            res.put("Meh!");
        }
        return res.toString(4);
    }

    /**
     * Execute Command with Output String.
     *
     * @param command Command Line
     * @return Output dari Command yang di-execute
     * @throws Exception
     */
    private String execToString(String command) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CommandLine commandline = CommandLine.parse(command);
        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);
        exec.execute(commandline);
        return(outputStream.toString());
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
