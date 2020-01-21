import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import static cn.hutool.crypto.Mode.CBC;

public class DecryptCdn {

    public static String decryptCdn(String vid, String body) {
        String vidMd5 = SecureUtil.md5(vid);
        byte[] bytes = Convert.hexToBytes(body);
        AES aes = new AES(CBC, Padding.PKCS5Padding, vidMd5.substring(0, 16).getBytes(), vidMd5.substring(16, 32).getBytes());
        byte[] decryptByte = aes.decrypt(bytes);
        String base64 = new String(decryptByte);
        return Base64.decodeStr(base64);
    }

}
