package WinsomeServer;
import Risorse.Utente;
import Risorse.Post;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.lang.System.out;
public class WinsomeServerMain {
    private static final File inizializzazione=new File("src/Iniz_server.json");//file di inizializzazione per prendere i dati di input per il serve(porte, indirizzi ip,tempi per calcolo ricompense e backup)
    private static String localhost;
    private static int porta_tcp;
    private static String backup;
    private static String multicast;
    private static int porta_rmi;
    private static int minuti_backup;
    private static int minuti_ricompensa;
    private static int porta_callback;
    private static int loca_registry;
    private static int porta_udp;
    private static String string_multicast;
    private static final Set<String> utenti_loggati=Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Utente> utenti = Collections.synchronizedMap(new HashMap<>());//struttura dati che mantiene gli utenti registrati su winsome
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        try {
            JsonNode nodo_inizializzazione=objectMapper.readTree(inizializzazione);//si legge il file di inizializzazione come se fosse un albero e ogni campo è un nodo
            localhost=nodo_inizializzazione.get("server_ip").asText();
            porta_tcp=nodo_inizializzazione.get("porta_tcp").asInt();
            multicast=nodo_inizializzazione.get("multicast").asText();
            porta_rmi=nodo_inizializzazione.get("porta_rmi").asInt();
            minuti_backup=nodo_inizializzazione.get("minuti_per_backup").asInt();
            backup=nodo_inizializzazione.get("file_backup").asText();
            minuti_ricompensa=nodo_inizializzazione.get("minuti_per_ricompensa").asInt();
            porta_callback=nodo_inizializzazione.get("porta_callback").asInt();
            loca_registry=nodo_inizializzazione.get("registry_callback").asInt();
            porta_udp=nodo_inizializzazione.get("porta_udp").asInt();
            string_multicast=multicast+" "+porta_udp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = new File(backup);// file json in cui verranno salvati tutti i dati per effettuare il backup
        Task_Backup task_backup = new Task_Backup(Collections.synchronizedMap(utenti), file, objectMapper,minuti_backup,backup);
        Thread backup = new Thread(task_backup);//si avvia il thread che ogni tot di tempo, deciso nel file di inizializzazione andrà a salvare nel file di backup tutto il necessario per ricostruire lo stato del sistema
        backup.start();
        //RIPRISTINO SISTEMA
        //quando il server verrà riavviato si andrà a controllare con l'objectmap il file di backup per ricostruire lo stato del sistema
        try {
            JsonNode node = objectMapper.readTree(file);//node conterrà l'intero albero del file di backup (tutti gli utenti che erano registrati nel momento dell'ultimo backup effettuato) e dobbiamo scorrere questo "albero" che sarà nella forma "string_utente":Utente
            Map<Integer,Post> post_winsome= new HashMap<>();//map utilizzata esclusivamente per salvare i post esistenti nel file, nel momento di riavvio del sistema
            for (JsonNode object_node : node) {//per ogni nodo(in questo caso per ogni string_utente) scorriamo la sua istanza Utente
                String username=object_node.get("username").asText();
                String password=object_node.get("password").asText();
                List<String> tags = new ArrayList<>();
                JsonNode lista_tags = object_node.get("tags");
                JsonNode lista_followers = object_node.get("followers");
                JsonNode lista_following = object_node.get("following");
                JsonNode lista_transazioni=object_node.get("transazioni");
                JsonNode guadagno=object_node.get("guadagno");
                JsonNode blog=object_node.get("blog");
                JsonNode feed=object_node.get("feed");
                JsonNode bitcoin=object_node.get("bitcoin");
                if (lista_tags.isArray()) {
                    for (JsonNode objtags : lista_tags) {//per ogni tag della lista di tags
                        tags.add(objtags.asText());//lo inseriamo nella struttura dati per i tags dell'utente
                    }
                }
                Utente riprendi_utente = new Utente(username, password, tags,Collections.synchronizedMap(utenti));//si "ricrea" l'utente
                utenti.put(username, riprendi_utente);//e si inserisce nella map
                riprendi_utente.setguadagno(guadagno.asDouble());
                riprendi_utente.setBitcoin(bitcoin.asDouble());
                if(lista_followers.isArray()){
                    for (JsonNode objectfollower: lista_followers){
                        riprendi_utente.setFollower(objectfollower.asText());
                    }
                }
                if(lista_following.isArray()){
                    for(JsonNode objectsfollowing: lista_following){
                        riprendi_utente.setFollowing(objectsfollowing.asText());
                    }
                }
                if(lista_transazioni.isArray()){
                    for(JsonNode objecttransazioni: lista_transazioni){
                        riprendi_utente.setTransazioni(objecttransazioni.asText());
                    }
                }
                if(blog.isArray()){
                    for (JsonNode object_blog: blog){
                        String autore=object_blog.get("autore").asText();
                        String titolo=object_blog.get("titolo").asText();
                        String contenuto=object_blog.get("contenuto").asText();
                        JsonNode like=object_blog.get("like");
                        JsonNode dislike=object_blog.get("dislike");
                        int iterazioni=object_blog.get("iterazioni").asInt();
                        int id=object_blog.get("id").asInt();
                        JsonNode commenti=object_node.findValue("commenti");
                        //la lista dei commenti è una map che per ogni string_utente avrà una lista di commente fatta da quell'utente
                        //quindi si deve andare a ricostruire questa map
                        String jsonInput = commenti.toString();
                        TypeReference<HashMap<String, List<String>>> typeRef= new TypeReference<>() {};//si usa un typereference per far capire all'objectmap che deve leggere il file json e tradurlo in un oggetto map
                        Map<String,List<String>> commenti_map=objectMapper.readValue(jsonInput,typeRef);
                        Post riprendi_post=new Post(autore,titolo,contenuto,id);//"ricreiamo" il post
                        post_winsome.put(id,riprendi_post);//si inserisce nella map dei post così che quando per qualsiasi altro utente ci sarà nel feed un post con questo id possiamo riprendere l'oggetto Post corrispondete
                        riprendi_post.setIterazioni(iterazioni);
                        riprendi_utente.add_post(post_winsome.get(id));//aggiungiamo il post nel blog dell'utente
                        if(like.isArray()){
                            for(JsonNode object_like: like){//obkect_like conterrà la string dell'utente che ha messo like
                                riprendi_post.add_rate("+1",object_like.asText());
                            }
                        }
                        if(dislike.isArray()){
                            for(JsonNode object_dislike: dislike){
                                riprendi_post.add_rate("-1",object_dislike.asText());
                            }
                        }
                        Iterator<String> commentatori=commenti_map.keySet().iterator();
                        while (commentatori.hasNext()){
                            String username_utente_commenti=commentatori.next();
                            Iterator<String> utente_commenti=commenti_map.get(username_utente_commenti).iterator();
                            while (utente_commenti.hasNext()){
                                String commento=utente_commenti.next();
                                riprendi_post.add_commento(username_utente_commenti,commento);
                            }
                        }
                    }
                }
                if(feed.isArray()){
                    //stessi procedimenti fatti per il blog
                    for(JsonNode object_feed: feed){
                        String autore=object_feed.get("autore").asText();
                        String titolo=object_feed.get("titolo").asText();
                        String contenuto=object_feed.get("contenuto").asText();
                        int id=object_feed.get("id").asInt();
                        JsonNode like=object_feed.get("like");
                        JsonNode dislike=object_feed.get("dislike");
                        JsonNode commenti=object_node.findValue("commenti");
                        String jsonInput = commenti.toString();
                        var typeRef = new TypeReference<HashMap<String, List<String>>>() {};
                        Map<String,List<String>> commenti_map=objectMapper.readValue(jsonInput,typeRef);
                        Post riprendi_post=new Post(autore,titolo,contenuto,id);
                        //se si cerca di inserire un post con un id che è gia presente nel key_set la put non verrà effettuata
                        post_winsome.put(id,riprendi_post);
                        riprendi_utente.add_feed(post_winsome.get(id));
                        if(like.isArray()){
                            for(JsonNode object_like: like){
                                riprendi_post.add_rate("+1",object_like.asText());
                            }
                        }
                        if(dislike.isArray()){
                            for (JsonNode object_dislike: dislike){
                                riprendi_post.add_rate("-1",object_dislike.asText());
                            }
                        }
                        Iterator<String> commentatori=commenti_map.keySet().iterator();
                        while (commentatori.hasNext()){
                            String username_utente_commenti=commentatori.next();
                            Iterator<String> utente_commenti=commenti_map.get(username_utente_commenti).iterator();
                            while (utente_commenti.hasNext()){
                                String commento=utente_commenti.next();
                                riprendi_post.add_commento(username_utente_commenti,commento);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            synchronized (utenti) {
                InterfacciaRemota registrazione_rmi = new ImplRem(Collections.synchronizedMap(utenti));
                InterfacciaRemota stub = (InterfacciaRemota) UnicastRemoteObject.exportObject(registrazione_rmi, porta_rmi);
                LocateRegistry.createRegistry(porta_rmi);
                Registry r = LocateRegistry.getRegistry(porta_rmi);
                r.rebind("Registrazione", stub);
                out.println("Server pronto");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Task_ricompense ricompense = new Task_ricompense(Collections.synchronizedMap(utenti),multicast,minuti_ricompensa,porta_udp);
        Thread guadagno = new Thread(ricompense);//thread che esegue il calcolo delle ricompense ogni tot di tempo e invia una notifica al client per avvertirlo
        guadagno.start();
        //Instauriamo una connessione TCP con il client
        try (ServerSocket server = new ServerSocket()) {
            //il server si mette in ascolto su l'indirizzo ip: localhost nella porta: porta_tcp
            server.bind(new InetSocketAddress(InetAddress.getByName(localhost), porta_tcp));
            //creiamo un threadpool che gesirà i client in arrivo
            ExecutorService es = Executors.newCachedThreadPool();
            //si rende disponibile tramite rmi_callback, registrarsi a un servizio notifica del server che invierà una notifica ogni volta che il client loggato riceverà un nuovo follow o sarà unfollowato
            Impl_Callback notifica_rmi = new Impl_Callback();
            Interfaccia_Callback stub = (Interfaccia_Callback) UnicastRemoteObject.exportObject(notifica_rmi, porta_callback);
            String name = "Server";
            LocateRegistry.createRegistry(loca_registry);
            Registry registry = LocateRegistry.getRegistry(loca_registry);
            registry.bind(name, stub);
            Task_close_server close_server=new Task_close_server(guadagno, es,backup);
            Thread chiudiserver=new Thread(close_server);
            chiudiserver.start();
            while (true) {
                Socket client = server.accept();
                //creata la connessione con il client, per ogni  nuovo client verrà avviato un thread del threadpool che eseguirà il paradigma client/server
                Handler handler = new Handler(client, Collections.synchronizedMap(utenti),notifica_rmi,string_multicast,Collections.synchronizedSet(utenti_loggati));
                if(!chiudiserver.isAlive()){
                    server.close();
                }
                es.execute(handler);
            }

        } catch (IOException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
}