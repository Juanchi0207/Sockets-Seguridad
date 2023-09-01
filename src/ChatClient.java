import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

class MessageSender implements Runnable {
    public final static int PORT = 2020; //puerto asignado al server
    private DatagramSocket socket;
    private String hostName;
    private ClientWindow window; //ventana que usamos para el chat e ingreso de ip

    MessageSender(DatagramSocket sock, String host, ClientWindow win) {
        socket = sock;
        hostName = host;
        window = win;
    }

    private void sendMessage(String s) throws Exception {
        byte buffer[] = s.getBytes(); //convierte el mensaje a bytes
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
        PublicKey publicKeyServer=null;
        do {
            try {
                connected = true;
                System.out.println(RSA.encode(publicKey.getEncoded()));
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
                    sendMessage(window.getMessage()); //cuando es true manda en mensaje a la ventana
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

    MessageReceiver(DatagramSocket sock, ClientWindow win) {
        socket = sock;
        buffer = new byte[1024];
        window = win;
    }

    public void run() {
        boolean infiniteLoop=true;
        PublicKey publicKeyServer=null;
        while (infiniteLoop) {
            try { //bucle infinito que recibe paquetes
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                if (publicKeyServer!=null) {
                    String received = new String(packet.getData(), 1, packet.getLength() - 1).trim();
                    //crea una string con los datos recibidos
                    String receivedFinal = "";
                    String senderIp = "";
                    boolean status = false;
                    for (int i = 0; i < received.length(); i++) {

                        if (received.charAt(i) == ':') {
                            status = true;
                            i++;
                        }
                        if (received.charAt(i) == '#') {
                            status = false;
                        }
                        if (status) {
                            senderIp = senderIp + received.charAt(i);
                        } else {
                            receivedFinal = receivedFinal + received.charAt(i);
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
                    //seguir aca
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
        window.setTitle("UDP CHAT  Server: " + host);
        DatagramSocket socket = new DatagramSocket();
        MessageReceiver receiver = new MessageReceiver(socket, window);
        MessageSender sender = new MessageSender(socket, host, window);
        Thread receiverThread = new Thread(receiver); //thread para recibir mensajes asignado a la clase receiver
        Thread senderThread = new Thread(sender); //thread para mandar mensajes asignado a la clase sender
        // los threads se utilizan para poder realizar multiples aciones al mismo tiempo
        // por eso minetras que estoy escribiendo puedo estar ecibiendo msjs y viceversa
        receiverThread.start(); // inicio ambos threads
        senderThread.start();
    }
}