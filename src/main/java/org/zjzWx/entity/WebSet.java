package org.zjzWx.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("web_set")
public class WebSet {

    /**
     * 系统设置表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 小程序AppID
     */
    private String appId;

    /**
     * 小程序AppSecret
     */
    private String appSecret;

    /**
     * 激励视频广告位ID
     */
    private String videoUnitId;

    /**
     * 登录方式：1不获取手机号，2获取手机号
     */
    private Integer loginType;

    /**
     * 图片站域名
     */
    private String picDomain;

    /**
     * 图片存储站路径
     */
    private String directory;

    /**
     * 个人中心公众号横幅：1显示，2关闭
     */
    private Integer officialSwitch;

    /**
     * 公众号二维码图片地址
     */
    private String officialQrCodeImageUrl;


    /**
     * 微信支付商户号
     */
    private String merchantId;

    /**
     * 微信支付商户证书序列号
     */
    private String merchantSerialNumber;

    /**
     * 微信支付APIv3密钥
     */
    private String apiV3Key;

    /**
     * 微信支付商户私钥PEM全文
     */
    private String merchantPrivateKey;

    /**
     * 微信支付回调地址，末尾需要斜杠
     */
    private String payNotifyUrl;

    /**
     * 美颜亮度调整强度，最大25
     */
    private Integer brightnessStrength;

    /**
     * 美颜对比度调整强度，最大50
     */
    private Integer contrastStrength;

    /**
     * 美颜锐化调整强度，最大50
     */
    private Integer sharpenStrength;

    /**
     * 美颜饱和度调整强度，最大5
     */
    private Integer saturationStrength;

    /**
     * API处理方式：1自建，2云平台
     */
    private Integer picApiType;

    /**
     * 图片API地址
     */
    private String picApiUrl;

    /**
     * 云平台API密钥
     */
    private String picApiKey;

    /**
     * 证件照人像分割模型
     */
    private String humanMattingModel;

    /**
     * 人脸检测模型
     */
    private String faceDetectModel;

    /**
     * 智能抠图模型
     */
    private String mattingModel;

    /**
     * 黑白照片上色模型
     */
    private String colourizeModel;

    /**
     * 动漫风模型
     */
    private String cartoonModel;

    /**
     * 美式证件照人像分割模型
     */
    private String americanHumanMattingModel;

    /**
     * 美式证件照人脸检测模型
     */
    private String americanFaceDetectModel;

    /**
     * 社交媒体模板照人像分割模型
     */
    private String templateHumanMattingModel;

    /**
     * 社交媒体模板照人脸检测模型
     */
    private String templateFaceDetectModel;

    /**
     * 情侣红底照人像分割模型
     */
    private String coupleHumanMattingModel;

    /**
     * 情侣红底照人脸检测模型
     */
    private String coupleFaceDetectModel;

    /**
     * 换装人脸检测模型
     */
    private String clothesFaceDetectModel;

    /**
     * 换装人体解析模型
     */
    private String clothesParsingModel;

    /**
     * 模糊图片变清晰模型
     */
    private String deblurModel;
}
