package com.borunovv.jogging.web.controllers;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.server.nio.http.protocol.HttpResponse;
import com.borunovv.core.util.CurrentVersion;
import com.borunovv.core.util.JsonUtils;
import com.borunovv.core.util.TimeUtils;
import com.borunovv.core.web.HttpController;
import com.borunovv.core.web.IHttpController;
import com.google.gson.annotations.SerializedName;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@HttpController(path = "/uptime")
public class UptimeController implements IHttpController {

    private static final Date startTime = new Date();

    @Override
    public void handleRequest(HttpRequest request, HttpResponse response) {
        response.writeJson(JsonUtils.toJson(
                new Model(getUpTime(),
                        CurrentVersion.getGitCommitSha())));
    }

    // Format : "5 days, HH:mm:ss"
    private String getUpTime() {
        long seconds = (new Date().getTime() - startTime.getTime()) / 1000;
        return TimeUtils.secondsToDDHHMMSS(seconds);
    }


    static class Model {
        public String uptime;

        @SerializedName("git_sha")
        public String gitSha;

        public Model() {}

        public Model(String uptime, String gitSha) {
            this.uptime = uptime;
            this.gitSha = gitSha;
        }
    }
}
