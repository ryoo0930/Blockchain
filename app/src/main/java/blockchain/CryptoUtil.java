package blockchain;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class CryptoUtil {
    /**
     * SHA-256의 방식으로 Hash하는 함수
     * @param input
     */
    public static String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sign(PrivateKey privateKey, String data) {
        try {
            Signature ecdsa = Signature.getInstance("SHA256withECDSA");
            ecdsa.initSign(privateKey);
            ecdsa.update(data.getBytes(StandardCharsets.UTF_8));
            return ecdsa.sign();
        } catch (Exception e) {
            throw new RuntimeException("Signature Failed.", e);
        }
    }

    public static Boolean verify(PublicKey publickey, String data, byte[] signature) {
        try {
            Signature ecdsa = Signature.getInstance("SHA256withECDSA");
            ecdsa.initVerify(publickey);
            ecdsa.update(data.getBytes(StandardCharsets.UTF_8));
            return ecdsa.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static String keyToString(Key key) { return Base64.getEncoder().encodeToString(key.getEncoded()); }
}
