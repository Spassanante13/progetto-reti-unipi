package WinsomeServer;
import Risorse.Utente;
import Risorse.Post;
import java.io.*;
import java.net.Socket;
import java.util.*;
public class Handler  implements  Runnable{
    Socket client;
    private static String string_multicast;
    private final Map<String,Utente> utenti;
    Impl_Callback notifica_rmi;
    private  final Set<String> utenti_loggati;
    public Handler(Socket client,Map<String,Utente> utenti,Impl_Callback notifica_rmi,String string_multicast,Set<String> utenti_loggati){
        this.client=client;
        this.utenti=utenti;
        this.notifica_rmi=notifica_rmi;
        this.string_multicast=string_multicast;//stringa di inizializzazione per il servizio di multicast
        this.utenti_loggati=utenti_loggati;//struttura dati che mantiene tutti gli utenti loggati
    }
    public  void run(){
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(client.getInputStream()));//mi inizializzo dei bufferedReader e bufferedWriter
                BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
                String message;
                String username = null;
                String password=null;
                String id_stringa;
                int id_post;
                System.out.println("Connessione con il client");
                Set<String> following;
                Iterator<Post> lista_post;
                Iterator<String> lista_utenti;
                while (!(message = reader.readLine()).equals("Ciao dal client")) {//fino a quando il client non decide di chiudere la connessione
                    String[] intera = message.split("\\s+");
                    String comando = intera[0];//la string comando prende la prima parte del messaggio riferente appunto al comando che il cliente vuole eseguire
                    String risposta;
                    if (comando.equals("login")) {
                        if(intera.length>1){//controllo se il comando login da parte del client è stato scritto bene, cioè, se contiene il campo username e password
                            username = intera[1];
                        }
                        else {
                            writer.write("Username non inserito"+"\r\n");
                            writer.flush();
                        }
                        if(intera.length>2){
                            password = intera[2];
                        }
                        int flag = 0;
                            if(utenti.containsKey(username)){
                                Utente utente = utenti.get(username);
                                if (utente.getPassword().equals(password) && !utenti_loggati.contains(username)) {//si controlla se la password inserita è uguale a quella immessa nel momento della registrazione e se l'utente non è già loggato con qualche altro client
                                    synchronized (utenti_loggati) {
                                        utenti_loggati.add(username);
                                    }
                                    risposta = "login effettuato correttamente";
                                    writer.write(risposta + "\r\n");//il servere comunica al client che il login è avvenuto correttamente
                                    writer.flush();
                                    //dato che il client deve tenere aggiornata una struttura dati che contiene i followers dell'utente loggato, il server invia al client tutti gli utenti che lo seguono per aggiornare la sua struttura dati
                                    Iterator<String> lista_follower = utente.getFollowers().iterator();
                                    while (lista_follower.hasNext()) {
                                        writer.write(lista_follower.next() + "\r\n");
                                        writer.flush();
                                    }
                                    writer.write("fine" + "\r\n");
                                    writer.flush();
                                    String ascolto_multicast = this.string_multicast;
                                    //dato che il login è avvenuto correttamente il server invia al client i riferimenti per mettersi in ascolto sulla MulticastSocket
                                    writer.write(ascolto_multicast + "\r\n");
                                    writer.flush();
                                    message = reader.readLine();//il server aspetta altri comandi da parte del client
                                    intera = message.split("\\s+");
                                    comando = intera[0];
                                    while (!comando.equals("logout")) {//fino a quando il client non richiede di fare il logout all'utente
                                        switch (comando) {
                                            case "list":
                                                String comando2 = intera[1];
                                                switch (comando2) {
                                                    case "users":
                                                        //il server deve inviare la lista di utenti registrati su winsome che hanno almeno un tag in comune con l'utente che ne ha richiesto la lista
                                                        lista_utenti = utenti.keySet().iterator();
                                                        //sfogliamo la lista di utenti
                                                        while (lista_utenti.hasNext()) {
                                                            String string_utente_rete = lista_utenti.next();
                                                            Utente utente_rete = utenti.get(string_utente_rete);
                                                            if (!utente.equals(utente_rete)) {//questo if evita di fare comparire nella lista l'utente stesso che ha richiesto la lista
                                                                risposta = creastringa(utente, utente_rete, risposta);//un metodo che crea la stringa di risposta da inviare al client
                                                                //creastringa risponderà con "no" se utente e utente_rete (che sarebbe un utente registrato su winsome != da l'utente che ha richiest list users) non hanno tag in comune
                                                                //creastring risponderà con la stringa da inviare al client se utente_rete ha almeno un tag in comune con utente
                                                                if (!risposta.equals("no")) {
                                                                    writer.write(risposta + "\r\n");
                                                                    writer.flush();
                                                                }
                                                            }
                                                        }
                                                        writer.write("fine" + "\r\n");//"fine" per comunicare ha inviato tutto quello che c'era da inviare e aspetta nuovi comandi
                                                        writer.flush();
                                                        break;
                                                    case "following"://il client ha richiesto la lista di utenti che utente segue
                                                        following = utente.getFollowing();
                                                        lista_utenti = utenti.keySet().iterator();
                                                        //scorriamo la lista degli utenti registrati
                                                        while (lista_utenti.hasNext()) {
                                                            //se un utente registrato compare nella lista following dell'utente loggat, si invia al client
                                                            String string_utente_rete = lista_utenti.next();
                                                            Utente utente_rete = utenti.get(string_utente_rete);
                                                            if (following.contains(string_utente_rete)) {
                                                                risposta = string_utente_rete + " | " + utente_rete.getTags().toString();
                                                                writer.write(risposta + "\r\n");
                                                                writer.flush();
                                                            }
                                                        }
                                                        //non ci sono più utenti
                                                        writer.write("fine" + "\r\n");
                                                        writer.flush();
                                                        break;
                                                }
                                                break;
                                            case "follow"://utente vuole iniziare a seguire un nuovo utente
                                                String utente_da_seguire = intera[1];
                                                int flag1;
                                                int flag2;
                                                //si controlla se l'utente che si vuole seguire è registrato a winsome e se l'utente sta provando a seguire se stesso
                                                if(utenti.containsKey(utente_da_seguire) && !username.equals(utente_da_seguire)){
                                                    Utente utente1=utenti.get(utente_da_seguire);
                                                    flag1=utente.setFollowing(utente_da_seguire);
                                                    flag2=utente1.setFollower(username);
                                                    if(flag1==1 && flag2==1){//se le operazioni di set follow e foller sono andate a buon fine
                                                        notifica_rmi.segnala(utente_da_seguire,username, 1);//utilizziamo rmi_callback per segnalare all'utente che è stato seguito che ha un nuovo follower
                                                        utente.set_feed(utente1.getBlog());//aggiungo i post "dell'influencer" nel feed dell'utente che lo ha iniziato a seguire
                                                        risposta = "Ora segui " + intera[1];
                                                    }
                                                    else if(flag1==-1){
                                                        risposta="Impossibile seguire due volte lo stesso utente";

                                                    }
                                                }
                                                else if(username.equals(utente_da_seguire)){
                                                    risposta="Impossibile seguire se stessi";
                                                }
                                                else {
                                                    risposta="Impossibile seguire un utente non registrato a winsome";
                                                }
                                                writer.write(risposta+"\r\n");
                                                writer.flush();
                                                break;
                                            case "unfollow":
                                                String utente_unfollow=intera[1];
                                                if(utenti.containsKey(utente_unfollow) && !username.equals(utente_unfollow)){
                                                    Utente utente1=utenti.get(utente_unfollow);
                                                    if(utente.delFollowing(utente_unfollow)==1 && utente1.delFollowers(username)==1){
                                                        notifica_rmi.segnala(utente_unfollow,username, 0);//il flag 0 servirà ad indicare che si deve inviare un messaggio di unfollow
                                                        utente.reset_feed(utente1.getBlog());//metodo della classe Utente per togliere dal feed i post creati dall'utente che ha smesso di seguire
                                                        risposta = "Hai smesso di seguire " + utente_unfollow;
                                                    }
                                                    else {
                                                        risposta = "Impossibile smettere di seguire un utente che non segui.";
                                                    }
                                                }
                                                else if(username.equals(utente_unfollow)){
                                                    risposta="Impossibile smettere di seguire se stessi";
                                                }
                                                else {
                                                    risposta="Impossibile smettere di seguire un utente non registrato a winsome";
                                                }
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                                break;
                                            case "post":
                                                //la stringa che ci arriva dal client contiene le virgolette (" ") per indicare qual è il titolo e qual è il contenuto
                                                String[] dividi_titolo_commento = message.split("\"");//dividiamo titolo e contenuto
                                                String titolo = dividi_titolo_commento[1];
                                                String contenuto = dividi_titolo_commento[3];
                                                int id = (int) (Math.random() * 9999);//il server assegna un id casuale tra 0 e 9999 al post
                                                Post post = new Post(username, titolo, contenuto, id);//si crea un nuovo post
                                                int valore_post = utente.add_post(post);//metodo della classe Utente per inserire un nuovo post nel blog dell'utente che lo ha creato
                                                //valore_post conterrà 1 se il post rispettava i criteri di winsome(titolo minore di 20 caratteri e contenuto minore di 500 caratteri)
                                                //-1 se solo il contenuto non rispetta i criteri
                                                //-2 se solo il titolo non rispetta i criteri
                                                //-3 se sia titolo che contenuto non rispettano i criteri
                                                if (valore_post == 1) {
                                                    risposta = "Nuovo post creato (id=" + id + ")";
                                                    //aggiungio il post al feed dei followers del creatore del post
                                                    //così ogni volta che utente crea un post il feed dei suo followers sarà aggiornato
                                                    Iterator<String> lista_followers = utente.getFollowers().iterator();
                                                    while (lista_followers.hasNext()) {
                                                        String follower = lista_followers.next();
                                                        Utente utente_follower = utenti.get(follower);
                                                        utente_follower.add_feed(post);
                                                    }
                                                } else if (valore_post == -1) {
                                                    risposta = "Contenuto del post troppo grande, deve contenere meno di 500 caratteri";
                                                } else if (valore_post == -2) {
                                                    risposta = "Titolo del post troppo grande, deve contenere meno di 20 caratteri";
                                                } else {
                                                    risposta = "Titolo e contenuto del post troppo grandi, devon contenere rispettivamente al massimo 20 e 500 caratteri";
                                                }
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                                break;
                                            case "rewin":
                                                //un utente può fare il rewin solo dei post presenti nel suo feed
                                                lista_post = utente.getFeed().iterator();
                                                id_stringa = intera[1];
                                                id_post = Integer.parseInt(id_stringa);
                                                flag = 0;
                                                Post post_rewin = null;
                                                while (lista_post.hasNext()) {
                                                    Post post_corrente = lista_post.next();
                                                    if (post_corrente.getId() == id_post) {//se si trova un post nel feed con l'id scelto
                                                        post_rewin = post_corrente;
                                                        utente.add_post(post_rewin);//il post viene aggiunto nel  blog dell'utente
                                                        flag = 1;
                                                    }
                                                }
                                                lista_follower = utente.getFollowers().iterator();
                                                //e per tutti i suoi followers
                                                while (lista_follower.hasNext()) {
                                                    String string_utente_follower = lista_follower.next();
                                                    Utente utente_follower = utenti.get(string_utente_follower);
                                                    if (flag == 1) {
                                                        utente_follower.add_feed(post_rewin);//viene aggiunto nel loro feed
                                                    }
                                                }
                                                if (flag == 1) {
                                                    risposta = "Post inoltrato nel tuo blog";
                                                } else {
                                                    risposta = "Post con id " + id_post + " non presente nel tuo feed";
                                                }
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                                break;
                                            case "blog":
                                                //richiesta di lista dei post scritti dall'utente
                                                lista_post = utente.getBlog().iterator();
                                                //per ogni post presente nel blog dell'utente, si invia al client
                                                while (lista_post.hasNext()) {
                                                    Post post_corrente = lista_post.next();
                                                    risposta = post_corrente.show_post();
                                                    writer.write(risposta + "\r\n");
                                                    writer.flush();
                                                }
                                                writer.write("fine" + "\r\n");
                                                writer.flush();
                                                break;
                                            case "show":
                                                if (intera[1].equals("feed")) {
                                                    lista_post = utente.getFeed().iterator();
                                                    while (lista_post.hasNext()) {
                                                        Post post_corrente = lista_post.next();
                                                        risposta = post_corrente.show_post();
                                                        writer.write(risposta + "\r\n");
                                                        writer.flush();
                                                    }
                                                } else if (intera[1].equals("post")) {
                                                    id_stringa = intera[2];
                                                    id_post = Integer.parseInt(id_stringa);
                                                    lista_post = utente.feed_e_blog().iterator();
                                                    flag = 0;
                                                    while (lista_post.hasNext()) {
                                                        Post post_corrente = lista_post.next();
                                                        //se si trova il post
                                                        if (post_corrente.getId() == id_post) {
                                                            flag = 1;
                                                            titolo = "Titolo: " + post_corrente.getTitolo();
                                                            contenuto = "Contenuto: " + post_corrente.getContenuto();
                                                            int like = post_corrente.numero_like();
                                                            int dislike = post_corrente.numero_dislike();
                                                            String voti = "Voti: " + like + " positivi, " + dislike + " negativi";
                                                            writer.write(titolo + "\r\n");//si invia il titolo
                                                            writer.flush();
                                                            writer.write(contenuto + "\r\n");//il contenuto
                                                            writer.flush();
                                                            writer.write(voti + "\r\n");
                                                            System.out.println(post_corrente.contiene_commenti());
                                                            //per ogni utente che ha commentato il post, si invia la lista di commenti dell'utente
                                                            if (post_corrente.contiene_commenti()) {
                                                                writer.flush();
                                                                writer.write("Commenti: " + "\r\n");
                                                                Iterator<String> hanno_commentato = post_corrente.keyset_commentatori().iterator();
                                                                while (hanno_commentato.hasNext()) {
                                                                    String string_ha_commentato = hanno_commentato.next();
                                                                    List<String> commenti = post_corrente.commenti(string_ha_commentato);
                                                                    for (int i = 0; i < commenti.size(); i++) {
                                                                        String commento = "   " + string_ha_commentato + ": " + commenti.get(i);
                                                                        writer.write(commento + "\r\n");
                                                                        writer.flush();
                                                                    }
                                                                }
                                                            } else {
                                                                risposta = "Commenti: 0";
                                                                writer.write(risposta + "\r\n");
                                                                writer.flush();
                                                            }
                                                        }
                                                    }
                                                    if (flag != 1) {
                                                        writer.write("Post non trovato" + "\r\n");
                                                        writer.flush();
                                                    }
                                                }
                                                writer.write("fine" + "\r\n");
                                                writer.flush();
                                                break;
                                            case "rate":
                                                id_stringa = intera[1];
                                                String voto = intera[2];
                                                flag1 = 0;
                                                id_post = Integer.parseInt(id_stringa);
                                                lista_post = utente.getFeed().iterator();
                                                //si cerca il post
                                                while (lista_post.hasNext()) {
                                                    Post post_corrente = lista_post.next();
                                                    //si verifica se si sta votando un post già votato o se si sta votando un post di cui si è l'autore o se il post non è nel feed
                                                    if (id_post == post_corrente.getId() && !post_corrente.getAutore().equals(username)) {
                                                        flag1 = post_corrente.add_rate(voto,username);
                                                    } else if (id_post == post_corrente.getId() && post_corrente.getAutore().equals(username)) {
                                                        flag1 = -1;
                                                    }
                                                }
                                                if (flag1 == 1) {
                                                    risposta = "Post votato correttamente.";
                                                } else if (flag1 == -1) {
                                                    risposta = "Impossibile votare un post di cui si è l'autore";
                                                } else if (flag1 == -2) {
                                                    risposta = "Impossibile votare più di una volta lo stesso post";
                                                } else {
                                                    risposta = "Impossibile votare un post che non è nel tuo feed o che non esiste";
                                                }
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                                break;
                                            case "comment":
                                                id_stringa = intera[1];
                                                String commento = intera[2];
                                                if (3 < intera.length) {
                                                    for (int i = 3; i < intera.length; i++) {
                                                        commento = commento + " " + intera[i];
                                                    }
                                                }
                                                flag1 = 0;
                                                id_post = Integer.parseInt(id_stringa);
                                                lista_post = utente.getFeed().iterator();
                                                while (lista_post.hasNext()) {
                                                    Post post_corrente = lista_post.next();
                                                    //stesso controllo effettuato per i like, ma in questo caso con i commenti
                                                    if (id_post == post_corrente.getId() && !post_corrente.getAutore().equals(username)) {
                                                        if (utente.nelfeed(post_corrente)) {
                                                            post_corrente.add_commento(username, commento);
                                                            flag1 = 1;
                                                        } else {
                                                            flag1 = -2;
                                                        }
                                                    } else if (id_post == post_corrente.getId() && post_corrente.getAutore().equals(utente.getUsername())) {
                                                        flag1 = -1;
                                                    }
                                                }
                                                if (flag1 == 1) {
                                                    risposta = "Hai commentato il post con id " + id_post;
                                                } else if (flag1 == -2) {
                                                    risposta = "Impossibile commentare un post che non è nel tuo feed";
                                                } else if (flag1 == -1) {
                                                    risposta = "Impossibile commentare il proprio post";
                                                } else {
                                                    risposta = "Impossibile commentare un post che non esiste";
                                                }
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                                break;
                                            case "delete":
                                                id_stringa = intera[1];
                                                id_post = Integer.parseInt(id_stringa);
                                                //il metodo trova_post_blog restituisce un post con id=-1 se l'utente prova ad eliminare un post di cui non è autore, l'id del post in caso contrario
                                                Post post_da_eliminare = utente.trova_post_blog(id_post,username);
                                                if (post_da_eliminare != null && post_da_eliminare.getId() != -1) {
                                                    utente.elimina_post(post_da_eliminare);//si elimina il post dal blog e se è presente nel feed(rewin del post di un utente seguit)
                                                    lista_utenti = utenti.keySet().iterator();//per ogni utente registrato su winsome, se ha il post nel blog o nel feed, si toglie
                                                    while (lista_utenti.hasNext()) {
                                                        String string_utente_corrente = lista_utenti.next();
                                                        Utente utente_corrente = utenti.get(string_utente_corrente);
                                                        utente_corrente.elimina_post(post_da_eliminare);
                                                    }
                                                    risposta = "Post eliminato correttamente";
                                                } else if (post_da_eliminare != null && post_da_eliminare.getId() == -1) {
                                                    risposta = "Non puoi eliminare un post del quale non sei autore";
                                                } else {
                                                    risposta = "Post non trovato";
                                                }
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                                break;
                                            case "wallet":
                                                double guadagno = utente.getGuadagno();
                                                String stringa_guadagno = Double.toString(guadagno);
                                                writer.write("Il tuo wallete contiene: " + stringa_guadagno + " wincoin" + "\r\n");
                                                writer.flush();
                                                Set<String> transazioni = utente.getTransazioni();
                                                Iterator<String> date = transazioni.iterator();
                                                //si stampano tutte le transazioni
                                                while (date.hasNext()) {
                                                    String data_e_guadagno = date.next();
                                                    writer.write(data_e_guadagno + "\r\n");
                                                    writer.flush();
                                                }
                                                writer.write("fine" + "\r\n");
                                                writer.flush();
                                                break;
                                            case "bitcoin":
                                                //chiama un metodo che effettua richiede un numero da Random.org
                                                utente.Bitcoin();
                                                double wallet_bitcoin = utente.getBitcoin();
                                                risposta = "Hai un totale di " + wallet_bitcoin + " bitcoin";
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                            default:
                                                risposta = "Comando non esistente su Winsome";
                                                writer.write(risposta + "\r\n");
                                                writer.flush();
                                        }
                                        message = reader.readLine();
                                        intera = message.split("\\s+");
                                        comando = intera[0];
                                    }
                                    //se l'utente ha effettuato il logut si toglie dalla struttura dati
                                    synchronized (utenti_loggati) {
                                        utenti_loggati.remove(username);
                                    }
                                } else if (!utente.getPassword().equals(password)) {
                                    risposta = "login non possibile: password errata.";
                                    writer.write(risposta + "\r\n");
                                    writer.flush();
                                } else {
                                    risposta = "Impossibile eseguire login, utente già loggato";
                                    writer.write(risposta + "\r\n");
                                    writer.flush();
                                }
                            }
                            else {
                                risposta = "login non possibile: utente non esiste nella rete sociale";
                                writer.write(risposta + "\r\n");
                                writer.flush();
                            }
                    }
                    else {
                        risposta = "Comando non esistente, riprova.";
                        writer.write(risposta + "\r\n");
                        writer.flush();
                    }
                }

            }
            catch (IOException  e){
                e.printStackTrace();
            }
            catch (ArrayIndexOutOfBoundsException er){
                String risposta="comando inviato dal client scritto male";
                System.out.println(risposta);
            }
    }
    public String creastringa(Utente utente, Utente utente_rete,String risposta){
        List<String> tag_rete=utente_rete.getTags();
        List<String> tags_loggato=utente.getTags();
        String tag_uguali="";
        int flag=0;
        for (int k = 0; k < tag_rete.size(); k++) {
            String tag = tag_rete.get(k);
            for (int j = 0; j < utente.getTags().size(); j++) {
                if (tag.equals(tags_loggato.get(j))) {
                    flag=1;
                    tag_uguali=tag_uguali+tag+" ";
                    risposta=utente_rete.getUsername()+" | "+tag_uguali;
                }
            }
        }
        if(flag==1){
            return risposta;
        }
        else {
            return "no";
        }
    }
}