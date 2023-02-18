package WinsomeClient;
import java.io.IOException;
import java.net.*;
public class Notifica_Ricompensa extends Thread{
    private String ascolto_multicast;
    private MulticastSocket ms;
    private InetSocketAddress group;
    private NetworkInterface networkInterface;
    private String multicast_name;
    private boolean thread_attivo;
    public Notifica_Ricompensa(String ascolto_multicast,String multicast_name){
        //fase di registrazione al gruppo di UDP multicast per ricevere notifiche
        this.ascolto_multicast=ascolto_multicast;
        this.thread_attivo=true;
        String[] multicast = ascolto_multicast.split("\\s+");
        String address = multicast[0];
        String porta = multicast[1];
        this.multicast_name=multicast_name;
        int port = Integer.parseInt(porta);
        try {
            this.ms = new MulticastSocket(port);
            this.group= new InetSocketAddress(InetAddress.getByName(address), port);
            this.networkInterface = NetworkInterface.getByName(multicast_name);
            ms.joinGroup(group, networkInterface);
        } catch (BindException e){
            System.out.println("Connessione internet assente");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void run() {
        int flag=0;
        while (thread_attivo) {
            try {
                byte[] buffer = new byte[8192];
                DatagramPacket ricevi = new DatagramPacket(buffer, buffer.length);
                ms.receive(ricevi);
                //quando ricevo un paccheto stampo la stringa del pacchetto ricevuto
                String s = new String(ricevi.getData(), 0, ricevi.getLength());
                if(flag==0) {
                    System.out.println(s);
                }
            } catch (SocketException e) {
                System.out.println("Interfaccia utente in chiusura...");
                thread_attivo=false;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //Riscrivo il metodo interrupt per chiudere la socket quando viene chiamato
    @Override
    public void interrupt() {
        super.interrupt();
        ms.close();
        thread_attivo=false;
    }
    //metodo per disiscriversi al gruppo multicast e lasciare il thrad attivo
    public void stop_notifiche(){
        try {
            this.ms.leaveGroup(group,networkInterface);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

