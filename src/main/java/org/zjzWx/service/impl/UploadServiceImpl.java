package org.zjzWx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.zjzWx.service.UploadService;
import org.zjzWx.util.HivisionIDPhotosApiUtil;
import org.zjzWx.util.PicUtil;


@Service
public class UploadServiceImpl implements UploadService {

    @Override
    public String checkNsfw(byte[] imageBytes,String originalFilename,Double threshold,String picApiUrl,Integer picApiType,String picApiKey) {

        try {

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("input_image",new PicUtil.MultipartByteArrayResource(imageBytes,originalFilename));


            //发起请求
            String response = HivisionIDPhotosApiUtil.requestJson(picApiUrl,picApiType,picApiKey,"checkImg",body);

            JsonNode responseJson = new ObjectMapper().readTree(response);

            //鉴黄接口没有返回成功状态时
            if(responseJson.get("code").asInt()!=200){
                return responseJson.get("msg").asText();
            }

            //计算Porn和Hentai总分
            JsonNode data = responseJson.get("data");
            double riskScore = data.get("porn").asDouble()+data.get("hentai").asDouble();

            //当总分达到鉴黄阈值时
            if(riskScore>=threshold){
                return "图片色情，制作失败";
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
