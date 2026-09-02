package org.zjzWx.service;

import org.zjzWx.model.dto.ExploreDto;
import org.zjzWx.model.vo.ExploreApplicationVo;
import org.zjzWx.util.R;

import java.util.List;

//探索功能里面的功能
public interface OtherApiService {

    //返回探索中心应用配置和累计使用次数
    List<ExploreApplicationVo> exploreIndex();

    //黑白图片上色
    R colourize(ExploreDto exploreDto);

    //智能抠图
    R matting(ExploreDto exploreDto);

    //证件照排版
    R generateLayoutPhotos(ExploreDto exploreDto);

    //动漫风照片
    R cartoon(ExploreDto exploreDto);

    //图片格式转换
    R convertImageFormat(ExploreDto exploreDto);

    //美式证件照
    R americanIdPhoto(ExploreDto exploreDto);

    //模板照
    R generateTemplatePhotos(ExploreDto exploreDto);

    //图片加水印
    R watermark(ExploreDto exploreDto);

    //图片压缩
    R compressImage(ExploreDto exploreDto);

    //模糊图片变清晰
    R deblurImage(ExploreDto exploreDto);

    //情侣红底照
    R coupleRedPhoto(ExploreDto exploreDto);


}
