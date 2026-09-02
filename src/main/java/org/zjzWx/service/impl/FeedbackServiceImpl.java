package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zjzWx.dao.FeedbackDao;
import org.zjzWx.entity.Feedback;
import org.zjzWx.entity.WebSet;
import org.zjzWx.service.FeedbackService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;
import org.zjzWx.util.R;

import java.time.LocalDate;
import java.util.Date;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackDao, Feedback> implements FeedbackService {

    @Autowired
    private WebSetService webSetService;

    @Override
    public R submitFeedback(Feedback feedback, Integer userId, MultipartFile file) {
        if(feedback.getType()<1 || feedback.getType()>4){
            return R.no("请选择反馈类型");
        }
        if(feedback.getContent()==null || feedback.getContent().trim().length()<10 || feedback.getContent().trim().length()>300){
            return R.no("反馈内容请输入10到300个字");
        }
        QueryWrapper<Feedback> qw = new QueryWrapper<>();
        qw.eq("user_id",userId);
        qw.ge("create_time",LocalDate.now().atStartOfDay());
        qw.lt("create_time",LocalDate.now().plusDays(1).atStartOfDay());
        if(baseMapper.selectCount(qw)>0){
            return R.no("今天已经反馈过啦，请明天再来吧");
        }
        feedback.setUserId(userId);
        feedback.setContent(feedback.getContent().trim());
        if(feedback.getContact()!=null){
            feedback.setContact(feedback.getContact().trim());
        }
        if(file!=null && file.getSize()>15*1024*1024){
            return R.no("图片大小不能超过15M");
        }
        String imageUrl = null;
        String directory = null;
        try {
            if(file!=null){
                byte[] imageBytes = file.getBytes();
                String extension = PicUtil.getImageExtension(imageBytes);
                if(extension==null){
                    return R.no("图片不合法，仅支持jpg/png/jpeg");
                }
                WebSet webSet = webSetService.getById(1);
                directory = webSet.getDirectory();
                imageUrl = PicUtil.savePermanentImage("feedback",imageBytes,directory,webSet.getPicDomain(),extension);
                feedback.setImageUrls(imageUrl);
            }
            feedback.setCreateTime(new Date());
            baseMapper.insert(feedback);
            return R.ok();
        } catch (Exception e) {
            PicUtil.deleteImage(imageUrl,directory);
            return R.no("反馈提交失败，请重试");
        }
    }
}
