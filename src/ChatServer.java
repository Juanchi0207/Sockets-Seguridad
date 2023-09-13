import java.io.*;
import java.net.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class ChatServer implements  Runnable {
    public final static int PORT = 2020;
    private final static int BUFFER = 1024;

    private DatagramSocket socket;
    private HashSet<String> existing_clients; //hashset que contiene a todos los clientes conectados
    private HashMap<InetAddress, Client>clients;


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ChatServer() throws IOException {
        socket = new DatagramSocket(PORT);
        System.out.println("Server ejecutandose y escuchando en el puerto " + PORT);
        existing_clients = new HashSet();
        clients=new HashMap<>();
    }

    public void run() {
        byte[] buffer = new byte[BUFFER]; //buffer en el que se almacenan los datos recibidos por el socket
        boolean infiniteLoop=true;
        RSA rsa=new RSA();
        rsa.init();
        Hasher hasher=new Hasher();
        String hasheado=null;
        while (infiniteLoop) {
            try {
                Arrays.fill(buffer, (byte) 0);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(buffer, 0, buffer.length);
                if (!message.contains("Hasheado:")) {
                    InetAddress clientAddress = packet.getAddress(); //de la clase DatagramPacket usamos
                    //metodo para obtener la ip de ese paquete
                    int client_port = packet.getPort(); //lo mismo para el puerto asignado a esta comunicacion
                    String id = clientAddress.toString() + "|" + client_port;
                    // si el cliente no estaba ya conectado se lo agrega a:
                    if (!existing_clients.contains(id)) {
                        existing_clients.add(id); //su string convertida con la ip y el puerto en el hashset
                        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(buffer));
                        Client client = new Client(client_port, publicKey);
                        clients.put(clientAddress, client);
                        byte bufferPub[] = rsa.getPublicKey().getEncoded();
                        DatagramPacket packetPub = new DatagramPacket(bufferPub, bufferPub.length, clientAddress, client_port);
                        socket.send(packetPub);
                    }
                    for (Map.Entry<InetAddress, Client> lista : clients.entrySet()) {
                        System.out.println(RSA.encode(lista.getValue().getPublicKey().getEncoded()) + " el cliente: " + lista.getKey().toString());
                    }
                    message = message.trim();
                    String messageDecrypted = rsa.decryptWithPrivate(message, rsa.getPrivateKey());
                    String received = (id + " :" + messageDecrypted);

                    // System.out.println(received); //muestra el msj por consola
                    String senderIp = "/";
                    String finalMessage = "";
                    boolean status=false;
                    for(int i=0; i< messageDecrypted.length();i++){
                        if (messageDecrypted.charAt(i)=='#'){
                            status=true;
                        }
                        else if (status){
                            finalMessage=finalMessage+messageDecrypted.charAt(i);
                        }
                    }
                    status = false;
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
                        }
                    }

                    if (!messageDecrypted.contains("#stopServer")) {
                        String encryptedMessage = rsa.encryptWithPrivate(messageDecrypted, rsa.getPrivateKey());
                        byte[] data = encryptedMessage.getBytes(); //guardamos los datos obtenidos en el byte
                        for (Map.Entry<InetAddress, Client> client : clients.entrySet()) {
                            InetAddress cl_address = client.getKey(); //verifica que la ip del mensaje este en la lista y envia el paquete
                            //solo a esa
                            int cl_port = client.getValue().getPort();
                            if (client.getKey().toString().equals(senderIp)) {
                                if (hasheado.equals(hasher.encryptString(finalMessage))) {
                                    packet = new DatagramPacket(data, data.length, cl_address, cl_port);
                                    socket.send(packet);
                                    data = (" El mensaje fue recibido por " + cl_address).getBytes();
                                    packet = new DatagramPacket(data, data.length, clientAddress, client_port);
                                    socket.send(packet);
                                }
                            }
                        }
                    } else {
                        Thread.currentThread().join();
                        infiniteLoop = false;
                    }
                }
                else {
                    boolean status=false;
                    hasheado="";
                    for (int i=0;i<message.length();i++){
                        if(message.charAt(i)==':'){
                            status=true;
                        }
                        else if(status){
                            hasheado=hasheado+message.charAt(i);
                        }
                    }
                    hasheado=hasheado.trim();
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

    public HashSet<String> getExisting_clients() {
        return existing_clients;
    }

    public void setExisting_clients(HashSet<String> existing_clients) {
        this.existing_clients = existing_clients;
    }

    public static void main(String args[]) throws Exception {
        ChatServer server_thread = new ChatServer();
        server_thread.run();
    }
}