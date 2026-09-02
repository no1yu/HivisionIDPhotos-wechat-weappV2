package org.zjzWx.service;

public interface UploadService {

    //图片鉴黄
    String checkNsfw(byte[] imageBytes,String originalFilename,Double threshold,String picApiUrl,Integer picApiType,String picApiKey);

}
