package bonsai.email;

import bonsai.Utils.CommonUtils;
import bonsai.config.DBBasedConfigs;
import bonsai.security.MD5Util;
import dataturks.response.VerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * 邮件发送工具类
 *
 * @author 徐文祥
 */
public class SendEmail {

    private static final Logger LOG = LoggerFactory.getLogger(SendEmail.class);

    // 发件人电子邮箱
    private static final String from = "1282494272@qq.com";
    /**
     * QQ邮箱授权码
     * <p>
     * 获取方式参见<a href="https://service.mail.qq.com/cgi-bin/help?subtype=1&&no=1001256&&id=28">官方说明</a>
     */
    private static final String pwd = "ktzlcbypzgjdihea";
    // 指定发送邮件的主机
    private static final String host = "smtp.qq.com";
    // 后端服务器host
    private static final String serverHost = DBBasedConfigs.getConfig("serverHost", String.class, "http://localhost:3047/dataturks");
    // 前端服务器host
    private static final String clientHost = DBBasedConfigs.getConfig("clientHost", String.class, "http://localhost:3030/uploads");

    /**
     * 获取发送邮件的session
     *
     * @return Session
     */
    private static Session getSession() {
        // 获取系统属性
        Properties properties = System.getProperties();
        // 设置邮件服务器
        properties.setProperty("mail.smtp.host", host);
        properties.put("mail.smtp.auth", "true");// 身份认证
        // 获取默认session对象
        return Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, pwd);
            }
        });
    }

    /**
     * 发送注册确认邮件
     *
     * @param to     收件人邮箱
     * @param authId 系统生成的authId
     */
    public static void sendConfirmRegistrationEmail(String to, String authId) {
        String subject = "注册确认邮件";
        // 请求后端路由
        // （这一步可以让前端做，后端跳路由这个框架不方便）
        String msg = "请点击确认链接: "
                + "<a href='" + serverHost + "/confirmRegistration?"
                + "code=" + MD5Util.md5(authId) + "&email=" + to
                + "'>点我确认</a>";
        sendMsg(to, subject, msg);
    }

    /**
     * 发送重置密码邮件
     *
     * @param email    收件人邮箱
     * @param password 临时密码
     */
    public static void sendResetPasswordEmail(String email, String password) {
        String subject = "重置密码邮件";
        // 请求前端路由
        String msg = "您的临时密码是:<br/><br/>"
                + password + "<br/><br/>"
                + "务必请通过此<a href='" + clientHost + "/#/entryPage?type=temp" + "'>链接</a>登录!!!<br/><br/>"
                + "请登录后务必修改密码!!!";
        sendMsg(email, subject, msg);
    }

    /**
     * 给指定邮箱发送信息
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param msg     发送的信息
     */
    private static VerificationResponse sendMsg(String to, String subject, String msg) {
        LOG.info("to " + to + ", subject " + subject + ", msg " + msg);
        Session session = getSession();// 获取环境
        try {
            // 创建默认的 MimeMessage 对象
            MimeMessage message = new MimeMessage(session);
            // Set From: 头部头字段
            // InternetAddress第二个参数设置发件人名称，不直接显示邮箱名字，显得高级
            message.setFrom(new InternetAddress(from, "VIPA标注平台", "UTF-8"));
            // Set To: 头部头字段
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            // Set Subject: 邮件主题
            message.setSubject(subject);
            // 设置消息体
            message.setContent(msg, "text/html;charset=UTF-8");
            // 发送消息
            Transport.send(message);
            LOG.info("Sent message successfully....");
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOG.error("Error " + e + " " + CommonUtils.getStackTraceString(e));
            return new VerificationResponse(false, "服务器忙，邮件发送失败");
        }
        return new VerificationResponse(true, "邮件发送成功");
    }

    public static void main(String[] args) {
        sendConfirmRegistrationEmail("3140101005@zju.edu.cn", MD5Util.md5("123"));
    }
}
