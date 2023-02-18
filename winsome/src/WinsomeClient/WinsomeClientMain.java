package WinsomeClient;
import WinsomeServer.Interfaccia_Callback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import WinsomeServer.InterfacciaRemota;
public class WinsomeClientMain {
    private  final static   Set<String> followers=new HashSet<>();
    private final static File inizializzazione=new File("src/Iniz_client.json");
    private static boolean client_attivo=true;
    private static String localhost;
    private static int porta_ascolto_tcp;
    private static int porta_rmi;
    private static String name_object_rmi;
    private static String name_multicast;
    private static int porta_callback;
    private static String name_callback;
    public static void main(String[] args) {
        //la maggior parte del codice sono operazine di invio comando al server e stampa della risposta
        ObjectMapper objectMapper=new ObjectMapper();
        try {
            JsonNode nodo_inizializzazione=objectMapper.readTree(inizializzazione);
            localhost=nodo_inizializzazione.get("server_ip").asText();
            porta_ascolto_tcp=nodo_inizializzazione.get("porta_ascolto").asInt();
            porta_rmi=nodo_inizializzazione.get("porta_rmi").asInt();
            name_object_rmi=nodo_inizializzazione.get("rmi_name_object").asText();
            name_multicast=nodo_inizializzazione.get("multicast_name").asText();
            porta_callback=nodo_inizializzazione.get("porta_callback").asInt();
            name_callback=nodo_inizializzazione.get("name_callback_object").asText();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Interfaccia_Client callback=null;
        Notifica_Ricompensa notifica_ricompensa=null;
        Registry registry;
        Registry r;
        Socket socket=new Socket();
        BufferedReader reader=null;
        BufferedWriter writer=null;
        String risposta;
        //connessione TCP con il server
        try{
            socket.connect(new InetSocketAddress(InetAddress.getByName(localhost),porta_ascolto_tcp));
            writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connesso al server");
        }catch (ConnectException e){
            System.out.println("Problemi con il server");
            client_attivo=false;
        }
        catch (IOException er){
            er.printStackTrace();
        }
        while (client_attivo) {
            Scanner scan = new Scanner(System.in);
            System.out.println("Se utente già registrato fare login in caso contrario registrarsi e poi fare il login");
            String stringa = scan.nextLine();
            String[] stringhe = stringa.split("\\s+");
            String comando = stringhe[0];
            String username = null;
            String password=null;
            //inizialmente si possono solamente effettuare i comandi di login o register
            switch (comando) {
                case "register":
                    if (stringhe.length > 1) {
                        username = stringhe[1];
                    } else {
                        System.out.println("Nessun username inserito");
                    }
                    if (stringhe.length > 2) {
                        password = stringhe[2];
                    }
                    List<String> tags = new ArrayList<>(5);
                    for (int i = 3; i < stringhe.length; i++) {
                        tags.add(stringhe[i]);
                    }
                    try {
                        r = LocateRegistry.getRegistry(porta_rmi);
                        Remote remote = r.lookup(name_object_rmi);
                        InterfacciaRemota serverObject = (InterfacciaRemota) remote;
                        System.out.println(serverObject.registrazione(username, password, tags));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "login":
                    try {
                        if (stringhe.length > 2) {//controllo errore del comando login
                            assert writer != null;
                            writer.write(stringa + "\r\n");
                            writer.flush();
                            assert reader != null;
                            risposta = reader.readLine();
                            if (risposta.equals("login effettuato correttamente")) {
                                System.out.println("login effettuato correttamente");
                                System.out.println("Sei all'interno del social Winsome.");
                                //ricevo la lista di utente che seguono l'utente che è loggato, da qui in poi fino al logout il Set followers è aggiornato localmento tramite RMI callback
                                while (!(risposta = reader.readLine()).equals("fine")) {
                                    followers.add(risposta);
                                }
                                //esportazione oggetto remoto per ricevere la notifica
                                registry = LocateRegistry.getRegistry(porta_callback);
                                Interfaccia_Callback server = (Interfaccia_Callback) registry.lookup(name_callback);
                                callback = new Impl_Client(followers);
                                Interfaccia_Client stub = (Interfaccia_Client) UnicastRemoteObject.exportObject(callback, 0);
                                server.registra_al_callback(stub, stringhe[1]);
                                String ascolto_multicast = reader.readLine();
                                notifica_ricompensa = new Notifica_Ricompensa(ascolto_multicast, name_multicast);
                                notifica_ricompensa.start();
                                while (!(stringhe[0].equals("logout"))) {
                                    System.out.println("Digitare \"help\"  per conoscere i comandi di Winsome");
                                    System.out.println("Inserire comandi: ");
                                    stringa = scan.nextLine();
                                    stringhe = stringa.split("\\s+");
                                    //ovviamente il comando login non è accettato se non si fa prima il logout
                                    if (stringhe[0].equals("login")) {
                                        System.out.println("C'è un utente già collegato, deve essere prima scollegato");
                                    } else {
                                        comando = stringhe[0];
                                        String comando_segue;
                                        String message;
                                        if (comando.equals("list")) {//controllo errore sul comando list
                                            if (stringhe.length > 1) {
                                                comando_segue = stringhe[1];
                                                switch (comando_segue) {
                                                    case "users" -> {
                                                        writer.write(stringa + "\r\n");
                                                        writer.flush();
                                                        System.out.println("Utente  | Tag");
                                                        System.out.println("-------------------------");
                                                        while (!(message = reader.readLine()).equals("fine")) {//fina a quando il server ha risposte da inviare
                                                            System.out.println(message);
                                                        }
                                                    }
                                                    case "following" -> {
                                                        writer.write(stringa + "\r\n");
                                                        writer.flush();
                                                        System.out.println("Utente | Tag");
                                                        System.out.println("-------------------------");
                                                        while (!(message = reader.readLine()).equals("fine")) {
                                                            System.out.println(message);
                                                        }
                                                    }
                                                    case "followers" -> {
                                                        System.out.println("I tuoi followers sono: ");
                                                        Iterator<String> seguono = followers.iterator();
                                                        while (seguono.hasNext()) {
                                                            System.out.println(seguono.next());
                                                        }
                                                    }
                                                    default -> {
                                                        System.out.println("Comando scritto male,ripova");
                                                        info_comandi();
                                                    }
                                                }
                                            } else {
                                                System.out.println("Comando scritto male,ripova");
                                                info_comandi();
                                            }

                                        } else if (comando.equals("follow")) {
                                            //comandi di invio richieste e stampa della risposta
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            message = reader.readLine();
                                            System.out.println(message);
                                        } else if (comando.equals("unfollow")) {
                                            //invio richiesta e stampa della risposta
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            message = reader.readLine();
                                            System.out.println(message);
                                        } else if (comando.equals("post")) {
                                            String[] dividi_titolo_commento = stringa.split("\"");
                                            if (dividi_titolo_commento[1] == null && dividi_titolo_commento[3] == null) {
                                                System.out.println("sia titolo che contenuto devono essere scitti ra virgolette");
                                            }
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            message = reader.readLine();
                                            System.out.println(message);
                                        } else if (comando.equals("blog")) {
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            System.out.println("Id   |   Autore   |   Titolo");
                                            System.out.println("------------------------------");
                                            while (!(message = reader.readLine()).equals("fine")) {
                                                System.out.println(message);
                                            }
                                        } else if (stringa.equals("show feed")) {
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            System.out.println("Id   |   Autore   |   Titolo");
                                            System.out.println("------------------------------");
                                            while (!(message = reader.readLine()).equals("fine")) {
                                                System.out.println(message);
                                            }
                                        } else if (comando.equals("rate")) {
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            message = reader.readLine();
                                            System.out.println(message);
                                        } else if (comando.equals("comment")) {
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            message = reader.readLine();
                                            System.out.println(message);
                                        } else if (comando.equals("show") && stringhe[1].equals("post")) {
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            while (!(message = reader.readLine()).equals("fine")) {
                                                System.out.println(message);
                                            }
                                        } else if (comando.equals("rewin")) {
                                            if(stringhe.length>1) {
                                                writer.write(stringa + "\r\n");
                                                writer.flush();
                                                message = reader.readLine();
                                                System.out.println(message);
                                            }
                                            else {
                                                System.out.println("Nessun campo id post inserito, riprova");
                                            }
                                        } else if (comando.equals("delete")) {
                                            if(stringhe.length>1){
                                                writer.write(stringa + "\r\n");
                                                writer.flush();
                                                message = reader.readLine();
                                                System.out.println(message);
                                            }
                                            else {
                                                System.out.println("Nessun campo id post inserito, riprova");
                                            }
                                        } else if (stringa.equals("wallet btc")) {
                                            writer.write("bitcoin" + "\r\n");
                                            writer.flush();
                                            message = reader.readLine();
                                            System.out.println(message);
                                        } else if (stringa.equals("wallet")) {
                                            writer.write(stringa + "\r\n");
                                            writer.flush();
                                            while (!(message = reader.readLine()).equals("fine")) {
                                                System.out.println(message);
                                            }
                                        } else if (comando.equals("exit")) {
                                            System.out.println("Per uscire da winsome, prima effettuare il logout");
                                        }
                                        else if(comando.equals("help")){
                                            info_comandi();
                                        }
                                        else {
                                            if(!comando.equals("logout")){
                                                System.out.println("Comando non esistente si Winsome");
                                                info_comandi();
                                            }
                                        }
                                    }
                                }
                                // quando si effettua il logout pulisco il set followers per non avere complicazioni per quando un nuovo utente esegue il login
                                followers.clear();
                                //tolgo la registrazione per non ricevere più notifice di follow e unfollo
                                server.unregister_al_callback(stub);
                                notifica_ricompensa.stop_notifiche();
                                writer.write("logout" + "\r\n");
                                writer.flush();
                                System.out.println("logout effettuato.");
                            } else {
                                System.out.println(risposta);
                            }
                        } else {
                            System.out.println("Manca campo username e password");
                        }

                    } catch (SocketException e) {
                        //In caso di chiusura improvvisa del server
                        risposta = "Nessuna connessione con il server";
                        System.out.println(risposta);
                        try {
                            notifica_ricompensa.interrupt();
                            UnicastRemoteObject.unexportObject(callback, true);
                        } catch (NullPointerException e1) {
                            return;
                        } catch (NoSuchObjectException ex) {
                            System.out.println("Errore");
                        }
                        System.out.println("Arrivederci");
                        client_attivo = false;
                    } catch (IOException | NotBoundException e) {
                        e.printStackTrace();
                    }
                    break;
                case "exit":
                    //termino i thread e chiudo la connessione per terminare il processo
                    client_attivo = false;
                    try {
                        if (notifica_ricompensa != null) {
                            notifica_ricompensa.interrupt();
                        }
                        writer.write("Ciao dal client" + "\r\n");
                        writer.flush();
                        risposta = reader.readLine();
                        System.out.println(risposta);
                        socket.close();
                        System.out.println("Chiusura dei thread ancora attivi...");
                        if (callback != null) {
                            UnicastRemoteObject.unexportObject(callback, true);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Arrivederci");
                    break;
                default:
                    System.out.println("Comando scritto male, comandi disponibili:\n- register <username> <password> <tags>\n- login <username> <password>");
                    break;
            }
        }
    }
    public static void info_comandi(){
        System.out.println("Lista comandi disponibili su Winsome: ");
        System.out.println("- list users: per vedere la lista di utente con almeno un tag in comune\n- list followers: per vedere la lista di utente che ti seguono\n- follow <username>: per iniziare a seguire un utente\n- unfollow <username>: per smettere di seguire un utente");
        System.out.println("- blog: per vedere tutti i post di cui sei autore\n- post \"titolo\" \"contenuto\": per creare un post(scrivere sia titolo che contenuto fra virgolette)\n- show post <idpost>: per vedere contenuto,like e commenti post\n- delete post <idpost>: per eliminare un post di cui sei autore");
        System.out.println("- rate <idpost>: per dare un voto ad un post(+1 positivo, -1 negativo)\n- rewin <idpost>: per inoltrare un post che è nel tuo feed, nel tuo blog\n- comment <idpost> <comment>: per aggiungere un commento ad un post\n- wallet: per visionare il portafoglio contenente wincoin");
        System.out.println("- wallet btc: per convertire wincoin in bitcoin");
    }
}
