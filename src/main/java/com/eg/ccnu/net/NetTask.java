package com.eg.ccnu.net;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NetTask {
    public static final String LOGIN_CCNU_DOMAIN = "10.220.250.50";

    /**
     * 检查外网连通性
     *
     * @return
     */
    private boolean isInternetAvailable() {
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
     * 检查登陆ccnu网页连通性
     *
     * @return
     */
    private boolean isLoginCcnuPageAvailable() {
        HttpResponse response;
        try {
            response = HttpRequest.get("http://" + LOGIN_CCNU_DOMAIN + "/0.htm")
                    .setConnectionTimeout(5000)
                    .execute();
        } catch (Exception e) {
            return false;
        }
        int status = response.getStatus();
        return status == HttpStatus.HTTP_OK;
    }

    /**
     * 给路由器设置新mac
     */
    private void setRouterNewMac() {
        String nonce = RouterUtil.generateNonce();
        String pwd = RouterUtil.getPwd(nonce);
        String token = RouterUtil.loginAndGetToken(pwd, nonce);

        JSONObject jsonObject = RouterUtil.setMac(token, MacUtil.generateMac());
        Integer code = jsonObject.getInteger("code");
        if (code == 0) {
            log.info("路由器设置新mac成功");
        } else if (code == 1637) {
            log.info("路由器设置mac失败，因为路由器说了，这是组播地址");
        } else {
            log.error("路由器设置mac失败，原因未知 code = " + code);
        }
        int count = 0;
        while (code == 1637) {
            count++;
            if (count >= 10) {
                log.error("路由器设置mac，都试了" + count + "回了，来看看咋回事吧");
            }
            log.info("开始尝试创新生成mac");
            jsonObject = RouterUtil.setMac(token, MacUtil.generateMac());
            code = jsonObject.getInteger("code");
        }
    }

    /**
     * 连接
     *
     * @return
     */
    private boolean connectCcnu() {
        log.info("开始尝试重连ccnu");
        HttpResponse response;
        String body = Base64.decodeStr(
                "REREREQ9MjAyMDE4MDAxMSU0MGNoaW5hbmV0Jn" +
                        "VwYXNzPWNjbnU1NjEyMTIzJnN1ZmZpeD0xJjBNS0tleT0xMjM=");
        try {
            response = HttpRequest.post("http://" + LOGIN_CCNU_DOMAIN + "/0.htm")
                    .header(HttpHeaders.CONTENT_LENGTH, body.length() + "")
                    .body(body)
                    .execute();
        } catch (Exception e) {
            log.warn("尝试重连ccnu抛异常: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
        if (response.getStatus() == 200) {
            log.info("ccnu连接成功, body = " + response.body());
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

    private static boolean isRunning = false;

    @Scheduled(fixedRate = 1000 * 25)
    private synchronized void autoCheck() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        boolean isInternetAvailable = isInternetAvailable();
//        log.info("检查外网连通性: {}", isInternetAvailable);
        if (!isInternetAvailable) {
            setRouterNewMac();
            for (int i = 0; i < 20; i++) {
                boolean loginCcnuPageAvailable = isLoginCcnuPageAvailable();
                log.info("检查ccnu连通性: " + loginCcnuPageAvailable + " , 重试次数 = " + i);
                if (loginCcnuPageAvailable) {
                    break;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            connectCcnu();
        }
        isRunning = false;
    }

}
