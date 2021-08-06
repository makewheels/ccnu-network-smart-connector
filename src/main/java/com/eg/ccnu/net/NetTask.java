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
     * 给路由器设置新mac
     */
    private void setRouterNewMac() {
        String nonce = RouterUtil.generateNonce();
        String pwd = RouterUtil.getPwd(nonce);
        String token = RouterUtil.loginAndGetToken(pwd, nonce);

        JSONObject jsonObject = RouterUtil.setMac(token, MacUtil.generateMac());
        Integer code = jsonObject.getInteger("code");
        if (code == 0) {
            System.out.println("路由器设置新mac成功");
        } else if (code == 1637) {
            System.out.println("路由器设置mac失败，因为路由器说了，这是组播地址");
        } else {
            System.out.println("路由器设置mac失败，原因未知 code = " + code);
        }
        int count = 0;
        while (code == 1637) {
            count++;
            if (count >= 10) {
                System.err.println("路由器设置mac，都试了" + count + "回了，来看看咋回事吧");
            }
            System.out.println("开始尝试创新生成mac");
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
        log.info("开始尝试重连");
        HttpResponse response;
        String body = Base64.decodeStr(
                "REREREQ9MjAyMDE4MDAxMSU0MGNoaW5hbmV0Jn" +
                        "VwYXNzPWNjbnU1NjEyMTIzJnN1ZmZpeD0xJjBNS0tleT0xMjM=");
        try {
            response = HttpRequest.post("http://10.220.250.50/0.htm")
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

    @Scheduled(fixedRate = 1000 * 60)
    private void autoCheck() {
        boolean networkAvailable = isNetworkAvailable();
        log.info("检查网络结果: {}", networkAvailable);
        if (!networkAvailable) {
            setRouterNewMac();
            connectCcnu();
        }
    }

}
