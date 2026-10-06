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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
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
import org.zjzWx.util.VirtualPayUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Base64;
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
        downloadSetVo.setPayType(webSet.getPayType());
        return downloadSetVo;
    }

    @Override
    public IPage<PayOrder> getPayOrderPage(int pageNum, int pageSize,int userId,String orderNo,String orderWx,int appId,int type,int status,String startTime,String endTime) {

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
        if(type!=0){
            qw.eq("type",type);
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
        qw.orderByDesc("create_time");
        qw.orderByDesc("id");
        return baseMapper.selectPage(page,qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayVo createOrder(Integer userId, Integer photoId,String code) {

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

        if(webSet.getPayType()==2 && !StringUtils.hasText(code)){
            orderPayVo.setMsg("支付方式已更新，请重新进入页面后支付");
            return orderPayVo;
        }

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
        payOrder.setType(webSet.getPayType());
        payOrder.setCreateTime(new Date());
        baseMapper.insert(payOrder);

        try {

            orderPayVo.setType(payOrder.getType());

            //如果是虚拟支付
            if(payOrder.getType()==2){
                //获取虚拟支付下单必须要的session_key
                String url = "https://api.weixin.qq.com/sns/jscode2session?appid="+webSet.getAppId()+"&secret="+webSet.getAppSecret()+"&js_code=" + code + "&grant_type=authorization_code";

                RestTemplate restTemplate = new RestTemplate();

                //发起请求
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                JSONObject session = JSONObject.parseObject(response.getBody());

                //填写虚拟支付参数并生成签名
                JSONObject data = new JSONObject(true);
                data.put("offerId",webSet.getVirtualOfferId());
                data.put("buyQuantity",1);
                data.put("env",webSet.getVirtualPayEnvironment()-1);
                data.put("currencyType","CNY");
                data.put("productId",String.valueOf(payOrder.getAppId()));
                data.put("goodsPrice",payOrder.getMoney().movePointRight(2).longValueExact());
                data.put("outTradeNo",payOrder.getOrderNo());
                data.put("attach",payOrder.getOrderNo());
                String signData = data.toJSONString();
                orderPayVo.setMode("short_series_goods");
                orderPayVo.setSignData(signData);
                orderPayVo.setPaySig(VirtualPayUtil.hmac(webSet.getVirtualAppKey(),"requestVirtualPayment&"+signData));
                orderPayVo.setSignature(VirtualPayUtil.hmac(session.getString("session_key"),signData));
                return orderPayVo;
            }

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
            orderPayVo.setMsg("支付发起失败，请重试");
            return orderPayVo;
        }
    }

    @Override
    public String refundOrder(Integer id) {

        PayOrder payOrder = baseMapper.selectById(id);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getStatus()!=2 && payOrder.getStatus()!=4){
            return "只有支付成功或退款失败的订单可以申请退款";
        }

        try {

            WebSet webSet = webSetService.getById(1);
            //每次发起退款都生成新的退款单号
            String refundNo = UUID.randomUUID().toString().replace("-","");
            String refundWx;

            //如果是虚拟支付
            if(payOrder.getType()==2){
                User user = userService.getById(payOrder.getUserId());
                String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + webSet.getAppId() + "&secret=" + webSet.getAppSecret();

                RestTemplate restTemplate = new RestTemplate();

                //获取微信接口调用凭证
                ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
                JSONObject token = JSONObject.parseObject(tokenResponse.getBody());
                if(null==token){
                    return "与微信通讯失败，请重试";
                }
                if(token.containsKey("errcode") && token.getIntValue("errcode")!=0){
                    return "退款申请失败："+token.getString("errmsg")+"（"+token.getString("errcode")+"）";
                }

                //组装退款参数
                JSONObject data = new JSONObject(true);
                data.put("openid",user.getOpenid());
                data.put("order_id",payOrder.getOrderNo());
                data.put("refund_order_id",refundNo);
                data.put("left_fee",payOrder.getMoney().movePointRight(2).longValueExact());
                data.put("refund_fee",payOrder.getMoney().movePointRight(2).longValueExact());
                data.put("biz_meta","");
                data.put("refund_reason","3");
                data.put("req_from","1");
                data.put("env",webSet.getVirtualPayEnvironment()-1);
                String body = data.toJSONString();
                String paySig = VirtualPayUtil.hmac(webSet.getVirtualAppKey(),"/xpay/refund_order&"+body);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(body,headers);
                String url = "https://api.weixin.qq.com/xpay/refund_order?access_token=" + token.getString("access_token") + "&pay_sig=" + paySig;

                //发起退款请求
                ResponseEntity<String> response = restTemplate.postForEntity(url,entity,String.class);
                JSONObject result = JSONObject.parseObject(response.getBody());
                if(!Integer.valueOf(0).equals(result.getInteger("errcode"))){
                    return "退款申请失败："+result.getString("errmsg")+"（"+result.getString("errcode")+"）";
                }
                refundWx = result.getString("refund_wx_order_id");

            }else {

                AmountReq amount = new AmountReq();
                long amountFen = payOrder.getMoney().movePointRight(2).longValueExact();
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
                Refund refund = refundService.create(request);
                if(refund.getStatus()!=Status.SUCCESS && refund.getStatus()!=Status.PROCESSING){
                    return "退款申请失败，微信返回状态是："+refund.getStatus();
                }
                refundWx = refund.getRefundId();
            }

            //设置状态为退款中
            payOrder.setStatus(3);
            payOrder.setRefundNo(refundNo);
            payOrder.setRefundWx(refundWx);
            payOrder.setRefundMsg(null);
            payOrder.setRefundTime(null);
            baseMapper.updateById(payOrder);
            return null;
        } catch (Exception e) {
            return "退款申请失败："+e.getMessage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteOrder(Integer id) {
        PayOrder payOrder = baseMapper.selectById(id);
        if(payOrder!=null && payOrder.getStatus()==3){
            return "当前订单退款中，无法删除";
        }
        baseMapper.deleteById(id);
        return null;
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

        //退款中和退款失败都还没有退钱，用户仍然可以下载照片
        if(!unlocked){
            QueryWrapper<PayOrder> payQw = new QueryWrapper<>();
            payQw.eq("photo_id",photoId);
            payQw.in("status",2,3,4);
            paid = baseMapper.selectCount(payQw)>0;
        }

        //照片还没有解锁时，根据后台设置的下载模式决定是否放行
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

                //清空临时过期时间，保留高清编辑数据，上传超过7天后再统一清理
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
    @Transactional(rollbackFor = Exception.class)
    public JSONObject wxNotify(HttpServletRequest request, HttpServletResponse response) {

        JSONObject jsonObject = new JSONObject();
        try {

            //检查微信支付回调的签名并解密数据
            WebSet webSet = webSetService.getById(1);
            RequestParam requestParam = WeChatPayUtil.createRequestParam(request);
            Transaction transaction = new NotificationParser(WeChatPayUtil.createConfig(webSet)).parse(requestParam,Transaction.class);
            if(transaction==null){
                response.setStatus(500);
                jsonObject.put("code","FAIL");
                jsonObject.put("message","微信支付回调处理失败");
                return jsonObject;
            }

            if(transaction.getTradeState()==Transaction.TradeStateEnum.SUCCESS){
                QueryWrapper<PayOrder> qw = new QueryWrapper<>();
                qw.eq("order_no",transaction.getOutTradeNo());
                PayOrder payOrder = baseMapper.selectOne(qw);
                if(payOrder==null){
                    throw new IllegalArgumentException("订单不存在");
                }

                //检查是否重复通知
                if(payOrder.getStatus()!=1){
                    response.setStatus(200);
                    return jsonObject;
                }

                payOrder.setStatus(2);
                payOrder.setOrderWx(transaction.getTransactionId());
                payOrder.setPayTime(Date.from(OffsetDateTime.parse(transaction.getSuccessTime()).toInstant()));
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
    @Transactional(rollbackFor = Exception.class)
    public JSONObject wxRefundNotify(HttpServletRequest request, HttpServletResponse response) {

        JSONObject jsonObject = new JSONObject();
        try {

            //检查微信退款回调的签名并解密数据
            WebSet webSet = webSetService.getById(1);
            RequestParam requestParam = WeChatPayUtil.createRequestParam(request);
            RefundNotification refundNotification = new NotificationParser(WeChatPayUtil.createConfig(webSet)).parse(requestParam,RefundNotification.class);
            if(refundNotification==null){
                response.setStatus(500);
                jsonObject.put("code","FAIL");
                jsonObject.put("message","微信退款回调处理失败");
                return jsonObject;
            }

            QueryWrapper<PayOrder> qw = new QueryWrapper<>();
            qw.eq("order_no",refundNotification.getOutTradeNo());
            PayOrder payOrder = baseMapper.selectOne(qw);
            //如果订单不存在
            if(payOrder==null){
                response.setStatus(500);
                jsonObject.put("code","FAIL");
                jsonObject.put("message","微信退款回调处理失败");
                return jsonObject;
            }

            //检查是否重复通知
            if(payOrder.getStatus()==4 || payOrder.getStatus()==5 || refundNotification.getRefundStatus()==Status.PROCESSING){
                response.setStatus(200);
                return jsonObject;
            }

            //如果不是重复通知
            if(payOrder.getStatus()==3){

                //如果微信传来退款单号不是当前数据库的值，那么就是旧退款单的重复通知
                if(!refundNotification.getOutRefundNo().equals(payOrder.getRefundNo())){
                    response.setStatus(200);
                    return jsonObject;
                }



                //如果退款成功
                if(refundNotification.getRefundStatus()==Status.SUCCESS){
                    payOrder.setStatus(5);
                    payOrder.setRefundNo(refundNotification.getOutRefundNo());
                    payOrder.setRefundWx(refundNotification.getRefundId());
                    payOrder.setRefundTime(Date.from(OffsetDateTime.parse(refundNotification.getSuccessTime()).toInstant()));
                    payOrder.setRefundMsg(null);
                    baseMapper.updateById(payOrder);
                    //异步删除图片
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            asyncCenter.deleteRefundPhoto(payOrder.getPhotoId());
                        }
                    });

                }else {
                    payOrder.setStatus(4);
                    payOrder.setRefundNo(refundNotification.getOutRefundNo());
                    payOrder.setRefundWx(refundNotification.getRefundId());
                    if(refundNotification.getRefundStatus()==Status.CLOSED){
                        payOrder.setRefundMsg("退款关闭");
                    }else if(refundNotification.getRefundStatus()==Status.ABNORMAL){
                        payOrder.setRefundMsg("退款异常，退款到银行发现用户的卡作废或者冻结了，导致原路退款银行卡失败");
                    }else {
                        payOrder.setRefundMsg("退款失败，未知异常");
                    }
                    baseMapper.updateById(payOrder);


                    }

                }


            response.setStatus(200);
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.setStatus(500);
            jsonObject.put("code","FAIL");
            jsonObject.put("message","微信退款回调处理失败");
            return jsonObject;
        }
    }

    @Override
    public String verifyVirtualNotify(String signature,String timestamp,String nonce,String echostr) {
        WebSet webSet = webSetService.getById(1);
        if(signature==null || timestamp==null || nonce==null || !MessageDigest.isEqual(VirtualPayUtil.sha1(webSet.getVirtualToken(),timestamp,nonce).getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8))){
            throw new IllegalArgumentException("消息推送地址验证失败");
        }
        return echostr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String virtualNotify(HttpServletRequest request,JSONObject body) {

        WebSet webSet = webSetService.getById(1);
        String msgSignature = request.getParameter("msg_signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        String encrypted = body.getString("Encrypt");

        //检查微信回调的签名
        if(msgSignature==null || timestamp==null || nonce==null || encrypted==null){
            throw new IllegalArgumentException("虚拟支付回调的签名参数不完整");
        }
        if(!MessageDigest.isEqual(VirtualPayUtil.sha1(webSet.getVirtualToken(),timestamp,nonce,encrypted).getBytes(StandardCharsets.UTF_8),msgSignature.getBytes(StandardCharsets.UTF_8))){
            throw new IllegalArgumentException("虚拟支付回调的签名不对");
        }

        //解密微信发来的消息
        JSONObject data;
        try {
            byte[] key = Base64.getDecoder().decode(webSet.getVirtualEncodingAesKey()+"=");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new IvParameterSpec(key,0,16));
            byte[] decoded = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            if(decoded.length==0){
                throw new IllegalArgumentException("虚拟支付回调的内容不对");
            }
            int padding = decoded[decoded.length-1]&0xff;
            if(padding<1 || padding>32 || decoded.length-padding<20){
                throw new IllegalArgumentException("虚拟支付回调解密后的内容不对");
            }
            for(int i=decoded.length-padding;i<decoded.length;i++){
                if((decoded[i]&0xff)!=padding){
                    throw new IllegalArgumentException("虚拟支付回调解密后的内容不对");
                }
            }
            int messageLength = ByteBuffer.wrap(decoded,16,4).getInt();
            if(messageLength<0 || messageLength>decoded.length-padding-20){
                throw new IllegalArgumentException("虚拟支付回调的数据长度不对");
            }
            String appId = new String(decoded,20+messageLength,decoded.length-padding-20-messageLength,StandardCharsets.UTF_8);
            if(!webSet.getAppId().equals(appId)){
                throw new IllegalArgumentException("虚拟支付回调的AppID不对");
            }
            data = JSONObject.parseObject(new String(decoded,20,messageLength,StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("虚拟支付回调解密失败",e);
        }

        //苹果退款问询开发者回调，固定建议退款
        if("xpay_subscribe_ios_refund_query_notify".equals(data.getString("Event"))){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result_code",0);
            jsonObject.put("result_info","建议退款");
            jsonObject.put("evidence","按平台规定，苹果退款申请时统一给用户退款");
            return jsonObject.toJSONString();
        }

        //虚拟支付成功回调
        if("xpay_goods_deliver_notify".equals(data.getString("Event"))){
            QueryWrapper<PayOrder> qw = new QueryWrapper<>();
            qw.eq("order_no",data.getString("OutTradeNo"));
            PayOrder payOrder = baseMapper.selectOne(qw);
            //如果订单不存在
            if(payOrder==null){
                throw new IllegalArgumentException("订单不存在");
            }

            //检查是否重复通知
            if(payOrder.getStatus()!=1){
                return "success";
            }

            payOrder.setStatus(2);
            JSONObject payInfo = data.getJSONObject("WeChatPayInfo");
            //苹果支付可能没有WeChatPayInfo，没有时使用回调里的时间
            if(payInfo!=null){
                payOrder.setOrderWx(payInfo.getString("MchOrderNo"));
                payOrder.setPayTime(new Date(payInfo.getLongValue("PaidTime")*1000));
            }else {
                payOrder.setPayTime(new Date(data.getLongValue("CreateTime")*1000));
            }
            baseMapper.updateById(payOrder);



        //虚拟支付退款回调
        }else if("xpay_refund_notify".equals(data.getString("Event"))){
            QueryWrapper<PayOrder> qw = new QueryWrapper<>();
            qw.eq("order_no",data.getString("MchOrderId"));
            PayOrder payOrder = baseMapper.selectOne(qw);
            //如果订单不存在
            if(payOrder==null){
                throw new IllegalArgumentException("订单不存在");
            }

            //检查是否重复通知
            if(payOrder.getStatus()==4 || payOrder.getStatus()==5){
                return "success";
            }

            //苹果用户自己申请退款时，订单可能还是支付成功
            if(payOrder.getStatus()==2 || payOrder.getStatus()==3){

                //如果微信传来退款单号不是当前数据库的值，那么就是旧退款单的重复通知
                if(payOrder.getStatus()==3 && !data.getString("MchRefundId").equals(payOrder.getRefundNo())){
                    return "success";
                }



                //如果退款成功
                if(data.getIntValue("RetCode")==0){
                    payOrder.setStatus(5);
                    payOrder.setRefundNo(data.getString("MchRefundId"));
                    payOrder.setRefundWx(data.getString("WxRefundId"));
                    payOrder.setRefundTime(new Date(data.getLongValue("RefundSuccTimestamp")*1000));
                    payOrder.setRefundMsg(null);
                    baseMapper.updateById(payOrder);
                    //异步删除图片
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            asyncCenter.deleteRefundPhoto(payOrder.getPhotoId());
                        }
                    });

                }else {
                    payOrder.setStatus(4);
                    payOrder.setRefundNo(data.getString("MchRefundId"));
                    payOrder.setRefundWx(data.getString("WxRefundId"));
                    payOrder.setRefundMsg(data.getString("RetMsg"));
                    baseMapper.updateById(payOrder);


                }

            }


        }
        return "success";
    }
}
