package com.eg.ccnu.net;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NetTask {

    private boolean isNetworkAvailable() {
        HttpResponse response;
        try {
            response = HttpRequest.get("https://www.baidu.com/")
                    .setConnectionTimeout(5000)
                    .execute();
        } catch (Exception e) {
            return false;
        }
        int status = response.getStatus();
        return status == HttpStatus.HTTP_OK;
    }

    /**
     * 连接
     *
     * @return
     */
    private boolean connect() {
        log.info("开始尝试重连");
        HttpResponse response;
        String body = Base64.decodeStr(
                "REREREQlM0QyMDIwMTgwMDExJTI1NDBjaGluYW5l" +
                        "dCUyNnVwYXNzJTNEY2NudTU2MTIxMjMlMjZzdWZ" +
                        "maXglM0QxJTI2ME1LS2V5JTNEMTIz");
        try {
            response = HttpRequest.post("http://l.ccnu.edu.cn/0.htm")
                    .header(HttpHeaders.CONTENT_LENGTH, body.length() + "")
                    .body(body)
                    .execute();
        } catch (Exception e) {
            log.warn("尝试重连抛异常: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
        if (response.getStatus() == 200) {
            log.info("连接成功");
            return true;
        } else {
            log.warn("连接失败，可能是请求有问题，http status = {}, response body = {}",
                    response.getStatus(), response.body());
            return false;
        }
    }

    /**
     * 断开
     *
     * @return
     */
    private boolean disConnect() {
        //http://l.ccnu.edu.cn/F.htm
        return true;
    }

    @Scheduled(fixedRate = 1000 * 3)
    private void autoCheck() {
        boolean networkAvailable = isNetworkAvailable();
        log.info("检查网络结果: {}", networkAvailable);
        if (!networkAvailable)
            connect();
    }
}
