package org.zjzWx.service;

import org.zjzWx.model.dto.CreatePhotoDto;
import org.zjzWx.model.dto.ChangeClothesDto;
import org.zjzWx.model.dto.UpdatePhotoDto;
import org.zjzWx.util.R;

//证件照相关功能
public interface ApiService {


 //使用上传接口返回的photoId生成证件照，并返回编辑页面需要的数据
 R createIdPhoto(CreatePhotoDto createPhotoDto);

 //生成高清证件照
 R createIdHdPhoto(Integer photoId,Integer userId);

 //换背景色
 R updateIdPhoto(UpdatePhotoDto updatePhotoDto);

 //换装
 R changeClothes(ChangeClothesDto changeClothesDto);

 //保存用户当前编辑结果
 R updateUserPhonto(Integer userId,Integer photoId);

}
