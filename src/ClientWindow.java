import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

@SuppressWarnings("serial")
public class ClientWindow extends JFrame {

    String host_name;
    JTextPane message_field;
    JTextPane room_field;

    String message = "";
    boolean message_is_ready = false;

    public ClientWindow() {
        //en esta seccion creamos un cuadro de dialogo que te permita ingresar la ip del servidor.
        JDialog hostNameDialog = new JDialog(this, "Ingresar IP del servidor: ", true);
        JTextField hostField = new JTextField("                            ");
        JButton ok = new JButton("OK"); // boton para confirmar
        hostNameDialog.setLayout(new FlowLayout());
        hostNameDialog.add(hostField); // campo de texto
        hostNameDialog.add(ok); //el boton creado
        hostNameDialog.setLocationRelativeTo(null); //posicion
        hostNameDialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        hostNameDialog.setSize(250, 65); //tamaño de la ventana
        hostNameDialog.setResizable(false);
        ok.addActionListener(new ActionListener() { //event listener que cuando se da click en okay
            // cierra la ventana y guarda en host_name la ip ingresada.
            @Override
            public void actionPerformed(ActionEvent e) {
                host_name = hostField.getText().trim();
                hostNameDialog.dispose();
            }
        });
        hostNameDialog.setVisible(true);
        // Establecemos nuevas propiedades para nuestra ventana
        setSize(800, 600);
        setTitle("UDP Chat room");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE); //cuando se cierra corta la ejecucion
        room_field = new JTextPane();
        message_field = new JTextPane(); // espacio para escribir
        room_field.setEditable(false);
        ScrollPane x = new ScrollPane();
        x.add(room_field);
        ScrollPane z = new ScrollPane();
        z.add(message_field);
        z.setPreferredSize(new Dimension(100, 100)); //dimesiones
        add(x, BorderLayout.CENTER);
        add(z, BorderLayout.SOUTH);

        setVisible(true);
        message_field.addKeyListener(new KeyListener() {
        // key listener
            @Override
            public void keyTyped(KeyEvent e) { // escribe
            }

            @Override
            public void keyReleased(KeyEvent e) {

                if (e.getKeyCode() == 10) {
                    message_field.setCaretPosition(0);
                } //reinicia al cursor a la primer posicion del mensaje
            }

            @Override
            public void keyPressed(KeyEvent e) { // envia el mensaje y resetea el campo a vacio

                if (e.getKeyCode() == 10 && !message_is_ready) {
                    message = message_field.getText().trim();
                    message_field.setText(null);
                    if (!message.equals(null) && !message.equals("")) {
                        message_is_ready = true;
                    }
                }
            }
        });
    }

    public void displayMessage(String receivedMessage) {
        StyledDocument doc = room_field.getStyledDocument(); //obtenemos el todos los mensajes anteriores
        try {
            // insertamos nuestro mensaje recibido a la ventana junto con los anteriores
            doc.insertString(doc.getLength(), receivedMessage + "\n", null);
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
    }

    public boolean isMessageReady() {
        return message_is_ready;
    }

    public void setMessageReady(boolean messageReady) {
        this.message_is_ready = messageReady;
    }

    public String getMessage() {
        return message;
    }

    public String getHostName() {
        return host_name;
    }
}