package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import org.zjzWx.entity.Feedback;
import org.zjzWx.util.R;

public interface FeedbackService extends IService<Feedback> {

    //提交意见反馈
    R submitFeedback(Feedback feedback,Integer userId,MultipartFile file);
}
