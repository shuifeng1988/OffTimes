const Dysmsapi20170525 = require('@alicloud/dysmsapi20170525');
const OpenApi = require('@alicloud/openapi-client');
const Util = require('@alicloud/tea-util');

/**
 * 阿里云短信服务类
 */
class AliCloudSmsService {
    constructor() {
        // 从环境变量读取配置
        this.accessKeyId = process.env.ALIBABA_CLOUD_ACCESS_KEY_ID;
        this.accessKeySecret = process.env.ALIBABA_CLOUD_ACCESS_KEY_SECRET;
        this.signName = process.env.ALIBABA_CLOUD_SMS_SIGN_NAME || 'OffTimes';
        this.templateCode = process.env.ALIBABA_CLOUD_SMS_TEMPLATE_CODE || 'SMS_123456789';
        
        // 检查配置
        if (!this.accessKeyId || !this.accessKeySecret) {
            console.warn('⚠️ 阿里云短信服务配置缺失，将使用模拟模式');
            this.isConfigured = false;
        } else {
            this.isConfigured = true;
            this.initClient();
        }
    }

    /**
     * 初始化阿里云客户端
     */
    initClient() {
        try {
            const config = new OpenApi.Config({
                accessKeyId: this.accessKeyId,
                accessKeySecret: this.accessKeySecret,
            });
            config.endpoint = 'dysmsapi.aliyuncs.com';
            this.client = new Dysmsapi20170525.default(config);
            console.log('✅ 阿里云短信服务客户端初始化成功');
        } catch (error) {
            console.error('❌ 阿里云短信服务客户端初始化失败:', error);
            this.isConfigured = false;
        }
    }

    /**
     * 发送短信验证码
     * @param {string} phoneNumber 手机号
     * @param {string} code 验证码
     * @returns {Promise<boolean>} 发送结果
     */
    async sendSmsCode(phoneNumber, code) {
        // 如果未配置，使用模拟模式
        if (!this.isConfigured) {
            return this.simulateSendSms(phoneNumber, code);
        }

        try {
            console.log(`📱 正在发送短信验证码: ${phoneNumber} -> ${code}`);
            
            const sendSmsRequest = new Dysmsapi20170525.SendSmsRequest({
                phoneNumbers: phoneNumber,
                signName: this.signName,
                templateCode: this.templateCode,
                templateParam: JSON.stringify({ code: code })
            });
            
            const runtime = new Util.RuntimeOptions({});
            const response = await this.client.sendSmsWithOptions(sendSmsRequest, runtime);
            
            console.log('📤 阿里云短信API响应:', JSON.stringify(response.body, null, 2));
            
            if (response.body.code === 'OK') {
                console.log(`✅ 短信发送成功: ${phoneNumber} -> ${code}`);
                return true;
            } else {
                console.error(`❌ 短信发送失败: ${response.body.code} - ${response.body.message}`);
                return false;
            }
        } catch (error) {
            console.error('❌ 发送短信时发生错误:', error);
            // 发生错误时回退到模拟模式
            console.log('🔄 回退到模拟发送模式');
            return this.simulateSendSms(phoneNumber, code);
        }
    }

    /**
     * 模拟发送短信（用于开发和测试）
     * @param {string} phoneNumber 手机号
     * @param {string} code 验证码
     * @returns {Promise<boolean>} 始终返回true
     */
    async simulateSendSms(phoneNumber, code) {
        console.log(`🧪 模拟发送短信验证码: ${phoneNumber} -> ${code}`);
        console.log(`📝 模拟短信内容: 【${this.signName}】您的验证码是${code}，5分钟内有效。请勿泄露给他人。`);
        
        // 模拟网络延迟
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        console.log('✅ 模拟短信发送成功');
        return true;
    }

    /**
     * 检查服务配置状态
     * @returns {object} 配置状态信息
     */
    getServiceStatus() {
        return {
            isConfigured: this.isConfigured,
            mode: this.isConfigured ? 'production' : 'simulation',
            signName: this.signName,
            templateCode: this.templateCode,
            hasAccessKey: !!this.accessKeyId,
            hasSecretKey: !!this.accessKeySecret
        };
    }
}

// 创建单例实例
const smsService = new AliCloudSmsService();

module.exports = {
    smsService,
    AliCloudSmsService
};

