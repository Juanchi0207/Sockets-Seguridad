import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

class MessageSender implements Runnable {
    public final static int PORT = 2020; //puerto asignado al server
    private DatagramSocket socket;
    private String hostName;
    private ClientWindow window; //ventana que usamos para el chat e ingreso de ip
    private static PublicKey publicKeyServer;


    MessageSender(DatagramSocket sock, String host, ClientWindow win) {
        socket = sock;
        hostName = host;
        window = win;
    }

    public static PublicKey getPublicKeyServer() {
        return publicKeyServer;
    }

    public static void setPublicKeyServer(PublicKey publicKeyServer) {
        MessageSender.publicKeyServer = publicKeyServer;
    }

    private void sendMessage(String s,RSA rsa) throws Exception {

            String encryptedMessage=rsa.encryptWithPublic(s,publicKeyServer);
            byte buffer[] = encryptedMessage.getBytes(); //convierte el mensaje a bytes
            InetAddress address = InetAddress.getByName(hostName); //obtiene ip
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORT);
            socket.send(packet); //crea y envia el paquete por el socket
    }

    public void run() {
        boolean connected = false;
        RSA rsa=new RSA();
        rsa.init();
        PrivateKey privateKey=rsa.getPrivateKey();
        PublicKey publicKey=rsa.getPublicKey();
        do {
            try {
                connected = true;
                byte bufferPub[] = publicKey.getEncoded();
                DatagramPacket packetPub=new DatagramPacket(bufferPub,bufferPub.length,InetAddress.getByName(hostName),PORT);
                socket.send(packetPub);

            } catch (Exception e) {
                window.displayMessage(e.getMessage());
            } //conecta al cliente y entra en un bucle infinito
        } while (!connected);
        boolean infiniteLoop=true;
        while (infiniteLoop) {
            try {
                while (!window.message_is_ready) { //este bucle se repite infinitas veces
                    //esperando que esta condicion sea true (pq esta inicializado en false)
                    Thread.sleep(100);
                }
                if (!window.getMessage().contains("#stopClient")) {
                    sendMessage(window.getMessage(),rsa); //cuando es true manda en mensaje a la ventana
                    window.setMessageReady(false); //vuelve a setearlo en false para esperar otro mensaje
                }
                else {
                    Thread.currentThread().join();
                    infiniteLoop=false;
                }
            } catch (Exception e) {
                window.displayMessage(e.getMessage()); //mensaje de error

            }
        }
        Thread.currentThread().interrupt();

    }
}

class MessageReceiver implements Runnable {
    DatagramSocket socket;
    byte buffer[];
    ClientWindow window;

    PublicKey publicKeyServer=null;

    MessageReceiver(DatagramSocket sock, ClientWindow win) {
        socket = sock;
        buffer = new byte[1024];
        window = win;
    }

    public void run() {
        boolean infiniteLoop=true;
        RSA rsa=new RSA();
        while (infiniteLoop) {
            try { //bucle infinito que recibe paquetes
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                if (publicKeyServer!=null) {
                    String received= new String(buffer, 0,buffer.length);
                    System.out.println(received);
                    String decryptedMessage=received;
                    if (!received.contains("El mensaje fue recibido por")){
                        decryptedMessage=rsa.decryptWithPublic(received.trim(),publicKeyServer);
                    }
                    //crea una string con los datos recibidos
                    String receivedFinal = "";
                    String senderIp = "";
                    boolean status = false;
                    for (int i = 0; i < decryptedMessage.length(); i++) {

                        if (decryptedMessage.charAt(i) == ':') {
                            status = true;
                            i++;
                        }
                        if (decryptedMessage.charAt(i) == '#') {
                            status = false;
                        }
                        if (status) {
                            senderIp = senderIp + decryptedMessage.charAt(i);
                        } else {
                            receivedFinal = receivedFinal + decryptedMessage.charAt(i);
                        }
                    }
                    if (senderIp.equals("Nuevo cliente conectado - Bienvenido!") == false) {
                        InetAddress address = InetAddress.getByName(senderIp);
                    } else {
                        receivedFinal = received;
                    }
                    System.out.println(receivedFinal);
                    if (!receivedFinal.contains("#stopClient")) {
                        window.displayMessage(receivedFinal); //tmb se imprime en la ventana
                    } else {
                        Thread.currentThread().join();
                        infiniteLoop = false;
                    }
                }
                else {
                    publicKeyServer = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(packet.getData()));
                    MessageSender.setPublicKeyServer(publicKeyServer);
                }
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        Thread.currentThread().interrupt();
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public ClientWindow getWindow() {
        return window;
    }

    public void setWindow(ClientWindow window) {
        this.window = window;
    }
}

public class ChatClient {

    public static void main(String args[]) throws Exception {
        ClientWindow window = new ClientWindow();
        String host = window.getHostName();
        PublicKey publicKeyServer=null;
        window.setTitle("UDP CHAT  Server: " + host);
        DatagramSocket socket = new DatagramSocket();
        MessageSender sender = new MessageSender(socket, host, window);
        Thread senderThread = new Thread(sender);
        MessageReceiver receiver = new MessageReceiver(socket, window);
        senderThread.start();
        Thread receiverThread = new Thread(receiver); //thread para recibir mensajes asignado a la clase receiver
         //thread para mandar mensajes asignado a la clase sender
        // los threads se utilizan para poder realizar multiples aciones al mismo tiempo
        // por eso minetras que estoy escribiendo puedo estar ecibiendo msjs y viceversa
        receiverThread.start(); // inicio ambos threads

    }
}