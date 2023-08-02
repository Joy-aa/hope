package bonsai.security;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * MD5工具类
 *
 * @author 徐文祥
 */
public class MD5Util {

    // 撒把盐
    private static final String salt = "vipa-tcmyxc";

    /**
     * 对明文字符串进行 MD5 加密
     *
     * @param src 明文字符串
     * @return 加密过的字符串
     */
    public static String md5(String src) {
        return DigestUtils.md5Hex(src + salt);
    }

    /**
     * 验证明文和密文是否一致
     *
     * @param src 明文
     * @param dst 密文
     */
    // 根据传入的密钥进行验证
    public static boolean verify(String src, String dst) {
        return md5(src).equalsIgnoreCase(dst);
    }
}
