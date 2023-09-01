import java.net.InetAddress;
import java.security.PublicKey;

public class Client {
    private int Port;
    private PublicKey publicKey;

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
