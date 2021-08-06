package com.eg.ccnu.net;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 路由器工具类
 */
public class RouterUtil {
    public static final String ROUTER_DOMAIN = "192.168.10.1";

    /**
     * 生成nonce
     *
     * @return
     */
    public static String generateNonce() {
        String deviceId = "70:66:55:dd:18:ab";
        String time = System.currentTimeMillis() / 1000 + "";
        String random = RandomUtil.randomNumbers(4);
        return "0_" + deviceId + "_" + time + "_" + random;
    }

    /**
     * 计算pwd参数
     *
     * @return
     */
    public static String getPwd(String nonce) {
        String key = "a2ffa5c9be07488bbb04a3a47d3c5f6a";
        String pwd = "j3esk554";
        return DigestUtil.sha1Hex(nonce + DigestUtil.sha1Hex(pwd + key));
    }

    /**
     * 登陆路由器，获取token
     */
    public static String loginAndGetToken(String password, String nonce) {
        String url = "http://" + ROUTER_DOMAIN + "/cgi-bin/luci/api/xqsystem/login";
        String body = "username=admin" +
                "&password=" + password +
                "&logtype=2" +
                "&nonce=" + URLUtil.encode(nonce);
        String json = HttpUtil.post(url, body);
        JSONObject jsonObject = JSON.parseObject(json);
        Integer code = jsonObject.getInteger("code");
        System.out.println("登陆路由器, code = " + code + " 完整返回: " + json);
        if (code == 0) {
            String token = jsonObject.getString("token");
            System.out.println("登陆返回code为0，登陆成功，返回token = " + token);
            return token;
        } else {
            System.err.println("登陆路由器返回code不为0，发生错误，需要排查");
            return null;
        }
    }

    /**
     * 设置新mac
     *
     * @param token
     * @param mac
     */
    public static JSONObject setMac(String token, String mac) {
        String json = HttpUtil.get("http://" + ROUTER_DOMAIN + "/cgi-bin/luci/;" +
                "stok=" + token + "/api/xqnetwork/mac_clone?" +
                "mac=" + URLUtil.encode(mac));
        JSONObject jsonObject = JSON.parseObject(json);
        Integer code = jsonObject.getInteger("code");
        System.out.println("给路由器设置新mac = " + mac +
                " 返回code = " + code + " 完整返回json为: " + json);
        return jsonObject;
    }

}
