package com.atguigu.gulimall.thirdparty.component;

import com.atguigu.common.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SmsComponentTest {

    public void sendCode(String phone, String code){
        String host = "https://aliapi.market.alicloudapi.com";
        String path = "/smsApi/verifyCode/send";
        String method = "POST";
        String appcode = "386a9e13b100451288d228a3cae43f25";
        Map<String, String> headers = new HashMap<>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<>();
        querys.put("phone", phone);
        querys.put("templateId", "540");
        querys.put("variables", code);

        Map<String, String> bodys = new HashMap<>();
        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
