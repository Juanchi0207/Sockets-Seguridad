import javax.crypto.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.crypto.Data;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;



class MessageSender implements Runnable {
    public final static int PORT = 2020; //puerto asignado al server
    private DatagramSocket socket;
    private String hostName;
    private ClientWindow window; //ventana que usamos para el chat e ingreso de ip
    private static PublicKey publicKeyServer;

    private PublicKey publicKeyClient;
    private PrivateKey privateKeyCliente;
    private static String keyAes;

    public static void setKeyAes(String keyAes) {
        MessageSender.keyAes = keyAes;
    }

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
        Hasher hasher=new Hasher();
        String aux=""; //mensaje solo sin la ip destino
        String reciever="";
        boolean status=false;
        for (int i=0;i<s.length();i++){
            if(s.charAt(i)=='#'){
                status=true;
            }
            else if (status) {
                aux=aux+s.charAt(i);
            }
            else {
                reciever=reciever+s.charAt(i);
            }
        }
        InetAddress address = InetAddress.getByName(hostName); //obtiene ip
        String encryptedMessage= AES.encrypt(aux,keyAes); //encriptado con clave AES
        String msgFinal=reciever+"#"+encryptedMessage;
        String hashedMessage="Hasheado:"+rsa.encryptWithPrivate(hasher.encryptString(encryptedMessage),privateKeyCliente);
        //hasheamos y encriptamos con clave priv para asegurar autenticacion
        byte buffer1[] = hashedMessage.getBytes();
        DatagramPacket packetHash=new DatagramPacket(buffer1, buffer1.length,address,PORT);
        socket.send(packetHash);
        //envio del packet hasheado y encriptado
        /*Toda esta parte hay que cambiarla para que encripte con la SecretKey*/

        String encryptedMessagePub=rsa.encryptWithPublic(msgFinal,publicKeyServer); //encriptado publica servidor
        byte buffer[] = encryptedMessagePub.getBytes(); //convierte el mensaje a bytes
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORT);
        socket.send(packet); //crea y envia el paquete por el socket
    }

    public void run() {
        boolean connected = false;
        RSA rsa=new RSA();
        rsa.init();
        privateKeyCliente=rsa.getPrivateKey();
        publicKeyClient=rsa.getPublicKey();
        MessageReceiver.setPublicKeyClient(publicKeyClient);
        MessageReceiver.setPrivateKeyClient(privateKeyCliente);
        do {
            try {
                connected = true;
                byte bufferPub[] = publicKeyClient.getEncoded();
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

    private static PublicKey publicKeyServer=null;

    private static PublicKey publicKeyClient=null;
    private static PrivateKey privateKeyClient=null;

    private static String keyAes=null;


    MessageReceiver(DatagramSocket sock, ClientWindow win) {
        socket = sock;
        buffer = new byte[1024];
        window = win;
    }




    public static String getKeyAes() {
        return keyAes;
    }

    public static void setKeyAes(String keyAes) {
        MessageReceiver.keyAes = keyAes;
    }

    public void run() {
        boolean infiniteLoop=true;
        RSA rsa=new RSA();
        AES aes=new AES();
        while (infiniteLoop) {
            try { //bucle infinito que recibe paquetes
                Arrays.fill(buffer, (byte) 0); //hace que el buffer se vacie
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                if (publicKeyServer!=null && keyAes!=null) { //si no tiene almacenada la pub del server, es la primer comunicacion
                    String received= new String(buffer, 0,buffer.length).trim();
                    String decryptedMessagePub=received;
                    if (!received.contains("El mensaje fue recibido por")){
                        decryptedMessagePub=rsa.decryptWithPrivate(received.trim(),privateKeyClient);
                        //desencriptamos con la priv nuestra
                    }
                    //crea una string con los datos recibidos
                    String receivedEncrypted = "";
                    String senderIp = "";
                    boolean status = false;
                    for (int i = 0; i < decryptedMessagePub.length(); i++) {
                        if (decryptedMessagePub.charAt(i) == '#') {
                            status = true;
                            i++;
                        }
                        if (!status) {
                            senderIp = senderIp + decryptedMessagePub.charAt(i);
                        }
                        else {
                            receivedEncrypted=receivedEncrypted+decryptedMessagePub.charAt(i);
                        }
                    }
                    String decryptedAesMessage=AES.decrypt(receivedEncrypted,keyAes);

                    if (!decryptedAesMessage.contains("#stopClient")) {
                        window.displayMessage(senderIp+"#"+decryptedAesMessage); //tmb se imprime en la ventana
                    } else {
                        Thread.currentThread().join();
                        infiniteLoop = false;
                    }
                }
                else {
                    if (publicKeyServer == null){
                        publicKeyServer = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(packet.getData()));
                    MessageSender.setPublicKeyServer(publicKeyServer);//obtenemos la publica del server
                    }
                    else if(keyAes==null) {
                        /*ACA TIENE QUE RECIBIR EL SOCKET CON LA CLAVE AES*/
                        String aux=new String(buffer, 0, buffer.length).trim();
                        keyAes=aux;
                        MessageSender.setKeyAes(aux);
                    }
                    //creamos en lineas arriba la clave y luego la obtenemos



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

    public static PublicKey getPublicKeyClient() {
        return publicKeyClient;
    }

    public static void setPublicKeyClient(PublicKey publicKeyClient) {
        MessageReceiver.publicKeyClient = publicKeyClient;
    }

    public static PrivateKey getPrivateKeyClient() {
        return privateKeyClient;
    }

    public static void setPrivateKeyClient(PrivateKey privateKeyClient) {
        MessageReceiver.privateKeyClient = privateKeyClient;
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