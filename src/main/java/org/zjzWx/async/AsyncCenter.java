package org.zjzWx.async;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zjzWx.dao.*;
import org.zjzWx.entity.PayOrder;
import org.zjzWx.entity.Photo;
import org.zjzWx.entity.User;
import org.zjzWx.entity.UserRecord;
import org.zjzWx.entity.WebSet;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;

import java.util.List;

//异步任务统一入口,只允许导入Dao层，否则会出现循环依赖问题
@Service
public class AsyncCenter {

    @Autowired
    private Ip2Region ip2Region;

    @Autowired
    private UserDao userDao;
    @Autowired
    private PhotoDao photoDao;
    @Autowired
    private UserRecordDao userRecordDao;
    @Autowired
    private PayOrderDao payOrderDao;
    @Autowired
    private WebSetDao webSetDao;







    //异步删除已经不再使用的图片文件
    @Async("bizExecutor")
    public void deleteImage(String imageUrl) {
        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("directory");
        PicUtil.deleteImage(imageUrl,webSetDao.selectOne(storageQw).getDirectory());
    }



    //退款成功后删除照片记录、正式图片和临时编辑目录
    @Async("bizExecutor")
    public void deleteRefundPhoto(Integer photoId) {
        Photo photo = photoDao.selectById(photoId);
        if(photo==null){
            return;
        }
        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("directory");
        String directory = webSetDao.selectOne(storageQw).getDirectory();
        photoDao.deleteById(photoId);
        PicUtil.deleteImage(photo.getNImg(),directory);
        PicUtil.deleteTempDirectory(photoId,directory);
    }



    //管理员后台异步清理用户图片、行为记录或支付订单
    @Async("bizExecutor")
    public void clearUserData(Integer userId,Integer type) {
        if(type==3){
            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("directory");
            String directory = webSetDao.selectOne(storageQw).getDirectory();

            QueryWrapper<Photo> photoQw = new QueryWrapper<>();
            photoQw.eq("user_id",userId);
            List<Photo> photos = photoDao.selectList(photoQw);
            for (Photo photo : photos) {
                PicUtil.deleteImage(photo.getNImg(),directory);
                PicUtil.deleteTempDirectory(photo.getId(),directory);
                photoDao.deleteById(photo.getId());
            }
        }else if(type==4){
            QueryWrapper<UserRecord> recordQw = new QueryWrapper<>();
            recordQw.eq("user_id",userId);
            userRecordDao.delete(recordQw);
        }else if(type==7){
            QueryWrapper<PayOrder> orderQw = new QueryWrapper<>();
            orderQw.eq("user_id",userId);
            payOrderDao.delete(orderQw);
        }
    }



    //用户登录后解析并保存用户本次登录的IP和城市
    @Async("bizExecutor")
    public void updateUserLoginRegion(Integer userId,String ip) {
        String city = null;
        try {
            String region = ip2Region.search(ip);

            //当IP能够查询到省市信息时，只保留省份和城市
            if(StringUtils.hasText(region)){
                String[] regionArray = region.split("\\|",-1);

                //当查询结果同时包含省份和城市字段时，开始整理城市名称
                if(regionArray.length>=3){
                    String province = "0".equals(regionArray[1]) ? "" : regionArray[1]
                            .replaceAll("壮族自治区$|回族自治区$|维吾尔自治区$|特别行政区$|自治区$|省$|市$","");
                    String cityName = "0".equals(regionArray[2]) ? "" : regionArray[2]
                            .replaceAll("自治州$|地区$|市$|盟$","");

                    //当省份存在时，保存为江苏徐州这种格式，直辖市不重复拼接
                    if(StringUtils.hasText(province)){
                        city = province.equals(cityName) || !StringUtils.hasText(cityName) ? province : province+cityName;
                    }

                    //当省份缺失但城市存在时，只保存城市名称
                    if(!StringUtils.hasText(province) && StringUtils.hasText(cityName)){
                        city = cityName;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //每次登录都覆盖IP和城市，定位失败时把旧城市清空，方便后台及时发现异常
        UpdateWrapper<User> qw = new UpdateWrapper<>();
        qw.eq("id",userId);
        qw.set("ip",ip);
        qw.set("city",city);
        userDao.update(null,qw);
    }












}
