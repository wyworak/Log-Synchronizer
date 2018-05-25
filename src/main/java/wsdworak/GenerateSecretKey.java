package wsdworak;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Encoder;

import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

public class GenerateSecretKey {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        System.out.println("Starting...");
        String name = "prime256v1";
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
            kpg.initialize(new ECGenParameterSpec(name));
            KeyPair keyPair = kpg.generateKeyPair();
            try (FileOutputStream writer = new FileOutputStream("private.key")) {
                final PrivateKey privateKey = keyPair.getPrivate();
                BASE64Encoder b64 = new BASE64Encoder();
                String base64pem = "-----BEGIN PRIVATE KEY-----\n"+b64.encode(privateKey.getEncoded())+"\n-----END PRIVATE KEY-----";
                writer.write(base64pem.getBytes());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
