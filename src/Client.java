import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.security.PublicKey;

public class Client {
    private int Port;
    private PublicKey publicKey;
    private SecretKey secretKey;

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public Client(int port, PublicKey publicKey, SecretKey secretKey) {
        Port = port;
        this.publicKey = publicKey;
        this.secretKey = secretKey;
    }

    public Client(int port, PublicKey publicKey) {
        Port = port;
        this.publicKey = publicKey;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
