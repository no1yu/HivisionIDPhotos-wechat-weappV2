package org.zjzWx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zjzWx.async.AsyncCenter;
import org.zjzWx.dao.PayOrderDao;
import org.zjzWx.entity.AppSet;
import org.zjzWx.entity.PayOrder;
import org.zjzWx.entity.Photo;
import org.zjzWx.entity.User;
import org.zjzWx.entity.WebSet;
import org.zjzWx.model.vo.DownloadSetVo;
import org.zjzWx.model.vo.OrderPayVo;
import org.zjzWx.model.vo.PicVo;
import org.zjzWx.service.AppSetService;
import org.zjzWx.service.PayOrderService;
import org.zjzWx.service.PhotoService;
import org.zjzWx.service.UserService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;
import org.zjzWx.util.WeChatPayUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;


@Service
public class PayOrderServiceImpl extends ServiceImpl<PayOrderDao, PayOrder> implements PayOrderService {

    @Autowired
    private AppSetService appSetService;
    @Autowired
    private WebSetService webSetService;
    @Autowired
    private PhotoService photoService;
    @Autowired
    private UserService userService;
    @Autowired
    private AsyncCenter asyncCenter;

    @Override
    public DownloadSetVo getDownloadSet(Integer appId) {

        AppSet appSet = appSetService.getById(appId);
        WebSet webSet = webSetService.getById(1);
        DownloadSetVo downloadSetVo = new DownloadSetVo();
        downloadSetVo.setStatus(appSet.getStatus());
        downloadSetVo.setDownloadPrice(appSet.getDownloadPrice());
        downloadSetVo.setVideoUnitId(webSet.getVideoUnitId());
        return downloadSetVo;
    }

    @Override
    public IPage<PayOrder> getPayOrderPage(int pageNum, int pageSize,int userId,String orderNo,String orderWx,int appId,int status,String startTime,String endTime) {

        Page<PayOrder> page = new Page<>(pageNum,pageSize);
        QueryWrapper<PayOrder> qw = new QueryWrapper<>();
        if(userId!=0){
            qw.eq("user_id",userId);
        }
        if(!orderNo.isEmpty()){
            qw.like("order_no",orderNo);
        }
        if(!orderWx.isEmpty()){
            qw.like("order_wx",orderWx);
        }
        if(appId!=0){
            qw.eq("app_id",appId);
        }
        if(status!=0){
            qw.eq("status",status);
        }
        if(!startTime.isEmpty()){
            qw.ge("create_time",LocalDate.parse(startTime).atStartOfDay());
        }
        if(!endTime.isEmpty()){
            qw.le("create_time",LocalDateTime.of(LocalDate.parse(endTime),LocalTime.MAX));
        }
        qw.orderByDesc("create_time").orderByDesc("id");
        return baseMapper.selectPage(page,qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayVo createOrder(Integer userId, Integer photoId) {

        OrderPayVo orderPayVo = new OrderPayVo();

        //当照片不存在/不属于当前用户/没有进入具体功能时
        Photo photo = photoService.getById(photoId);
        if(photo==null || !userId.equals(photo.getUserId()) || photo.getAppId()==null){
            orderPayVo.setMsg("非法请求");
            return orderPayVo;
        }

        //只有付费下载和广告、付费任选两种模式可以创建支付订单
        AppSet appSet = appSetService.getById(photo.getAppId());
        if(appSet.getStatus()!=3 && appSet.getStatus()!=4){
            orderPayVo.setMsg("当前功能不需要支付");
            return orderPayVo;
        }
        if((photo.getAppId()==3 && photo.getDownloadStatus()==3)
                || (photo.getAppId()!=3 && photo.getDownloadStatus()==2)){
            orderPayVo.setMsg("照片已解锁，无需重复支付");
            return orderPayVo;
        }
        if((photo.getAppId()==3 && (photo.getHdPath()==null || photo.getResultPath()==null))
                || (photo.getAppId()!=3 && photo.getNImg()==null)){
            orderPayVo.setMsg("请先生成照片");
            return orderPayVo;
        }

        WebSet webSet = webSetService.getById(1);
        User user = userService.getById(userId);

        String orderNo = UUID.randomUUID().toString().replace("-","");
        String orderName = appSet.getName() + "下载";

        //插入待支付订单
        PayOrder payOrder = new PayOrder();
        payOrder.setOrderNo(orderNo);
        payOrder.setUserId(userId);
        payOrder.setAppId(photo.getAppId());
        payOrder.setPhotoId(photoId);
        payOrder.setName(orderName);
        payOrder.setMoney(appSet.getDownloadPrice());
        payOrder.setStatus(1);
        payOrder.setCreateTime(new Date());
        baseMapper.insert(payOrder);

        try {

            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(WeChatPayUtil.createConfig(webSet)).build();

            //组装微信支付参数
            PrepayRequest request = new PrepayRequest();
            request.setAppid(webSet.getAppId());
            request.setMchid(webSet.getMerchantId());
            request.setDescription(orderName);
            request.setOutTradeNo(orderNo);
            request.setNotifyUrl(webSet.getPayNotifyUrl()+"pay/wxNotify");
            request.setAttach(payOrder.getId().toString());

            Amount amount = new Amount();
            amount.setTotal(payOrder.getMoney().movePointRight(2).intValue());
            request.setAmount(amount);

            Payer payer = new Payer();
            payer.setOpenid(user.getOpenid());
            request.setPayer(payer);

            //发起请求
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            orderPayVo.setTimeStamp(response.getTimeStamp());
            orderPayVo.setNonceStr(response.getNonceStr());
            orderPayVo.setPackageVal(response.getPackageVal());
            orderPayVo.setSignType(response.getSignType());
            orderPayVo.setPaySign(response.getPaySign());
            return orderPayVo;
        } catch (Exception e) {
            e.printStackTrace();
            orderPayVo.setMsg("支付发起失败，请重试");
            return orderPayVo;
        }
    }

    @Override
    public String refundOrder(Integer id) {

        PayOrder payOrder = baseMapper.selectById(id);
        if(payOrder.getStatus()==1){
            return "待支付状态不能退款";
        }
        if(payOrder.getStatus()==3){
            return "已退款，无需重复退款";
        }

        try {

            WebSet webSet = webSetService.getById(1);
            String refundNo = "R"+payOrder.getOrderNo();

            AmountReq amount = new AmountReq();
            long amountFen = payOrder.getMoney().movePointRight(2).longValue();
            amount.setRefund(amountFen);
            amount.setTotal(amountFen);
            amount.setCurrency("CNY");

            CreateRequest request = new CreateRequest();
            request.setTransactionId(payOrder.getOrderWx());
            request.setOutRefundNo(refundNo);
            request.setReason("管理员退款");
            request.setNotifyUrl(webSet.getPayNotifyUrl()+"pay/wxRefundNotify");
            request.setAmount(amount);

            RefundService refundService = new RefundService.Builder()
                    .config(WeChatPayUtil.createConfig(webSet))
                    .build();

            //发起请求
            Refund refund = refundService.create(request);

            //只有微信明确返回SUCCESS时才更新订单并删除关联照片
            if(refund.getStatus()==Status.SUCCESS){
                payOrder.setStatus(3);
                payOrder.setRefundNo(refundNo);
                payOrder.setRefundWx(refund.getRefundId());
                payOrder.setRefundTime(Date.from(OffsetDateTime.parse(refund.getSuccessTime()).toInstant()));
                baseMapper.updateById(payOrder);
                asyncCenter.deleteRefundPhoto(payOrder.getPhotoId());
                return null;
            }
            return "退款未成功，微信返回状态是："+refund.getStatus();
        } catch (Exception e) {
            e.printStackTrace();
            return "退款申请失败："+e.getMessage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PicVo downloadPhoto(Integer userId, Integer photoId, int rewarded) {

        PicVo picVo = new PicVo();

        //当照片不存在/不属于当前用户/没有进入具体功能时
        Photo photo = photoService.getById(photoId);
        if(photo==null || !userId.equals(photo.getUserId()) || photo.getAppId()==null){
            picVo.setMsg("非法请求");
            return picVo;
        }

        boolean unlocked = photo.getAppId()==3
                ? photo.getDownloadStatus()==3
                : photo.getDownloadStatus()==2;

        boolean paid = false;

        //照片尚未解锁时，查询是否存在这张照片的支付成功订单
        if(!unlocked){
            QueryWrapper<PayOrder> payQw = new QueryWrapper<>();
            payQw.eq("photo_id",photoId);
            payQw.eq("status",2);
            paid = baseMapper.selectCount(payQw)>0;
        }

        //照片没有永久解锁时，根据后台设置的下载模式决定是否放行
        if(!unlocked && !paid){
            AppSet appSet = appSetService.getById(photo.getAppId());
            if(appSet.getStatus()==0){
                picVo.setMsg("当前功能维护中，请稍后再试");
                return picVo;
            }
            if(appSet.getStatus()==2 && rewarded!=1){
                picVo.setMsg("请先观看广告");
                return picVo;
            }
            if(appSet.getStatus()==3){
                picVo.setMsg("请先支付");
                return picVo;
            }
            if(appSet.getStatus()==4 && rewarded!=1){
                picVo.setMsg("请选择看广告或付费下载");
                return picVo;
            }
        }

        try {

            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("pic_domain","directory");
            WebSet storageSet = webSetService.getOne(storageQw);

            String oldImagePath = photo.getNImg();

            //智能证件照需要保存当前高清换底结果，并标记高清下载权
            if(photo.getAppId()==3){
                if(photo.getHdPath()==null || photo.getResultPath()==null){
                    picVo.setMsg("请先生成高清照片");
                    return picVo;
                }
                String imageExtension = photo.getResultPath().substring(photo.getResultPath().lastIndexOf(".")+1);
                photo.setNImg(PicUtil.savePermanentImage(new SimpleDateFormat("yyyyMMdd").format(new Date()),Files.readAllBytes(PicUtil.getFile(photo.getResultPath(),storageSet.getDirectory())),storageSet.getDirectory(),storageSet.getPicDomain(),imageExtension));
                photo.setDownloadStatus(3);
                photo.setExpireTime(null);

                //更新照片并把临时图片过期时间清空，防止定时器误删图片
                photoService.updateById(photo);
            }else {
                //探索功能生成时已经保存了正式成片，这里只需要标记已解锁
                photo.setDownloadStatus(2);
                photoService.updateById(photo);
            }

            //先让数据库指向新成片，事务提交成功后再异步删除原来n_img对应的旧文件
            if(oldImagePath!=null && !oldImagePath.equals(photo.getNImg())){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        asyncCenter.deleteImage(oldImagePath);
                    }
                });
            }

            picVo.setPhotoId(photoId);
            picVo.setPicUrl(photo.getNImg());
            return picVo;
        } catch (Exception e) {
            throw new RuntimeException("图片存入失败",e);
        }
    }

    @Override
    public JSONObject wxNotify(HttpServletRequest request, HttpServletResponse response) {

        JSONObject jsonObject = new JSONObject();
        try {

            //验签并解密支付通知
            WebSet webSet = webSetService.getById(1);
            RequestParam requestParam = WeChatPayUtil.createRequestParam(request);
            Transaction transaction = new NotificationParser(WeChatPayUtil.createConfig(webSet)).parse(requestParam,Transaction.class);
            if(transaction==null){
                jsonObject.put("code","FAIL");
                jsonObject.put("message","微信支付回调处理失败");
                return jsonObject;
            }

            if(transaction.getTradeState()==Transaction.TradeStateEnum.SUCCESS){
                //检查是否重复通知
                PayOrder payOrder = baseMapper.selectById(transaction.getAttach());
                if(payOrder.getStatus()!=1){
                    response.setStatus(200);
                    return jsonObject;
                }
                payOrder.setStatus(2);
                payOrder.setOrderWx(transaction.getTransactionId());
                payOrder.setPayTime(new Date());
                baseMapper.updateById(payOrder);
            }

            //微信支付回调处理成功
            response.setStatus(200);
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            jsonObject.put("code","FAIL");
            jsonObject.put("message","微信支付回调处理失败");
            return jsonObject;
        }
    }

    @Override
    public JSONObject wxRefundNotify(HttpServletRequest request, HttpServletResponse response) {

        JSONObject jsonObject = new JSONObject();
        try {

            //验签并解密退款通知
            WebSet webSet = webSetService.getById(1);
            RequestParam requestParam = WeChatPayUtil.createRequestParam(request);
            RefundNotification refundNotification = new NotificationParser(WeChatPayUtil.createConfig(webSet)).parse(requestParam,RefundNotification.class);
            if(refundNotification==null){
                response.setStatus(500);
                jsonObject.put("code","FAIL");
                jsonObject.put("message","微信退款回调处理失败");
                return jsonObject;
            }

            response.setStatus(200);
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            jsonObject.put("code","FAIL");
            jsonObject.put("message","微信退款回调处理失败");
            return jsonObject;
        }
    }
}
