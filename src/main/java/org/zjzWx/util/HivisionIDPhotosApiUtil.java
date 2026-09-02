package org.zjzWx.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

//HivisionIDPhotos图片接口请求工具类
public class HivisionIDPhotosApiUtil {

    //请求图片接口，成功时返回图片字节，失败时抛出图片接口返回的具体原因
    public static byte[] requestImage(String picApiUrl,Integer picApiType,String picApiKey,String endpoint,MultiValueMap<String,Object> body) throws IOException {
        try {

            //发起请求
            ResponseEntity<byte[]> response = new RestTemplate().exchange(picApiUrl+endpoint,HttpMethod.POST,new HttpEntity<>(body,createHeaders(picApiType,picApiKey)),byte[].class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            JsonNode jsonNode = new ObjectMapper().readTree(e.getResponseBodyAsByteArray());
            throw new IOException(jsonNode.get("msg").asText());
        }
    }

    //请求JSON接口，失败时抛出图片接口返回的具体原因
    public static String requestJson(String picApiUrl,Integer picApiType,String picApiKey,String endpoint,MultiValueMap<String,Object> body) throws IOException {
        try {

            //发起请求
            ResponseEntity<String> response = new RestTemplate().exchange(picApiUrl+endpoint,HttpMethod.POST,new HttpEntity<>(body,createHeaders(picApiType,picApiKey)),String.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            JsonNode jsonNode = new ObjectMapper().readTree(e.getResponseBodyAsByteArray());
            throw new IOException(jsonNode.get("msg").asText());
        }
    }

    //创建API请求头
    private static HttpHeaders createHeaders(Integer picApiType,String picApiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if(picApiType==2){
            headers.set("X-API-Key",picApiKey);
        }
        return headers;
    }

}
