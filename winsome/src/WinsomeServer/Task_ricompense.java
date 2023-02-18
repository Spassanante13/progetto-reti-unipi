package WinsomeServer;
import Risorse.Utente;
import Risorse.Post;
import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
public class Task_ricompense implements Runnable {
    private final Map<String,Utente> utenti;
    private int like_totali;
    private int dislike_totali;
    private double sommatoria_commenti;
    private int count_commenti;
    private int like_dislike;
    private DatagramPacket invio;
    private SocketAddress address;
    private DatagramSocket serverSocket;
    private int minuti_ricompensa;
    public Task_ricompense(Map<String,Utente> utenti,String multicast,int minuti_ricompensa,int porta_udp){
        this.utenti=utenti;
        this.like_totali=0;
        this.sommatoria_commenti=0;
        this.count_commenti=0;
        this.dislike_totali=0;
        this.like_dislike=0;
        this.minuti_ricompensa=minuti_ricompensa;
        //MULTICAST con UDP
        byte[] buffer=new byte[500];//creiamo il buffer dove andranno i byte di ricezione
        DatagramPacket ricevi=new DatagramPacket(buffer, buffer.length);//pacchetto inviato dal client che ci servirà per avere il suo indirizzo e porta utilizzata
        byte[] data;
        String messaggio="Nuovo calcolo delle ricompense eseguito";//messaggio da inviare al client
        data=messaggio.getBytes();//traduciamo in byte
        int port=porta_udp;//porta udp del file di inizializzazione
        int porta_anonima=6800;
        try {
            InetAddress ia=InetAddress.getByName(multicast);
            this.invio=new DatagramPacket(data, data.length,ia,port);//creaimo un pacchetto di invio
            this.address=ricevi.getSocketAddress();//indirizzo del client
            this.serverSocket=new DatagramSocket(porta_anonima);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
    public void run(){
        double guadagno;
        double guadagno_autore;
        double guadagno_curatori;
        while (!Thread.interrupted()) {
                try {
                    int secondi=minuti_ricompensa*60;
                    int millis=secondi*1000;
                    //Il calcolo delle ricompense deve essere fatto ogni tot minuti(i minuti sono indicati nel file di inizializzazione)
                    Thread.sleep(millis);
                    synchronized (utenti) {//accesso sincronizzato sulla struttura utenti
                        Iterator<String> lista_utenti = utenti.keySet().iterator();//si scorre tutta la lista di utenti
                        while (lista_utenti.hasNext()) {
                            String stringa_utente_corrente = lista_utenti.next();
                            Utente utente_corrente=utenti.get(stringa_utente_corrente);
                            synchronized (utente_corrente.getBlog()){
                            Iterator<Post> blog = utente_corrente.getBlog().iterator();
                            while (blog.hasNext()) {//per ogni utente scorriamo la sua lista di post nel suo blog
                                Post post_corrente = blog.next();
                                Set<String> like_correnti=post_corrente.getLike();
                                Set<String> dislike_correnti=post_corrente.getDislike();
                                if(like_correnti.size()>like_totali){//controlliamo se tra un calcolo delle ricompense e l'altro ha ricevuto nuovi like
                                    like_dislike=(like_correnti.size()-dislike_correnti.size())-(like_totali-dislike_totali);//like_dislike contterà il la differenza tra nuovi like e nuovi dislike ricevuti tra un calcolo e l'altro
                                    like_totali=like_correnti.size();
                                    dislike_totali=dislike_correnti.size();
                                }
                                else{
                                    like_dislike=0;
                                }
                               if (like_dislike < 0) {//per evitare che si ci siano post che possano creare un malus, se si ricevono più dislike che like,like_dislike sarà uguale a 0
                                    like_dislike = 0;
                                }
                                //per ogni post controlliamo anche i commenti
                                Iterator<String> commentatori = post_corrente.keyset_commentatori().iterator();
                                while (commentatori.hasNext()) {//per tutti i commentatori di un post
                                    count_commenti = count_commenti+post_corrente.numero_commenti_utente(commentatori.next());//post_corrente.numero_commenti_utente(commentatori.next()) ci ritorna quanti commenti ha fatto un utente
                                }
                                sommatoria_commenti = 2 / (1 + Math.pow(Math.E, -(count_commenti - 1)));
                                guadagno=(Math.log(like_dislike + 1) + Math.log(sommatoria_commenti+1))/post_corrente.getIterazioni();//formula per il calcolo delle ricompense
                                guadagno_autore=(guadagno*7)/10;//ricompensa autore, che va al creatore del post
                                guadagno_curatori=guadagno-guadagno_autore;//ricompensa curatori, dobbiamo distribuirla ad ogni curatore
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                String Data=LocalDateTime.now().format(formatter);//per avere le transizioni dei vari guadagni
                                String string_guadagno=String.valueOf(guadagno_autore);
                                String data_guadagno="Il "+Data+" hai guadagnato: "+string_guadagno;
                                //dobbiamo prendere l'il creatore del post è dargli il guadagno autore riferente al post
                                //non possiamo utilizzare utente_corrente perchè data l'esistenza del comando rewin, un utente potrebbe avere nel suo blog un post di cui non è autore
                                //in quel caso il guadagno_autore deve andare al solo autore del post
                                String autore_post=post_corrente.getAutore();
                                Utente utente_creatore=utenti.get(autore_post);
                                utente_creatore.setTransazioni(data_guadagno);//metodo della classe Utente che aggiunge la transazione alla sua struttura dati corrispondente
                                utente_creatore.setguadagno(guadagno_autore);//aggiorna il guadagno
                                //Si deve dividere il guadagno curatore in maniera equa, per ogni curatore(chi ha commentato o messo like)
                                Iterator<String> lista_like=like_correnti.iterator();
                                commentatori=post_corrente.keyset_commentatori().iterator();
                                double guadagno_singolo_curatore=guadagno_curatori/(like_correnti.size()+post_corrente.keyset_commentatori().size());//quanto ogni curatore ha guadagnato da questo post
                                while (lista_like.hasNext()){
                                    String string_utente_like=lista_like.next();
                                    Utente utente_like=utenti.get(string_utente_like);
                                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                    Data=LocalDateTime.now().format(formatter);
                                    string_guadagno=String.valueOf(guadagno_singolo_curatore);
                                    data_guadagno="Il "+Data+" hai guadagnato: "+string_guadagno;
                                    utente_like.setTransazioni(data_guadagno);//aggiungiamo una transazione nella lista transazioni
                                    utente_like.setguadagno(guadagno_singolo_curatore);//aggiorniamo il guadagno
                                }
                                while (commentatori.hasNext()){//stessa cosa per ogni utente che ha commentato
                                    String string_utente_commento=commentatori.next();
                                    Utente utente_commento=utenti.get(string_utente_commento);
                                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                    Data=LocalDateTime.now().format(formatter);
                                    string_guadagno=String.valueOf(guadagno_singolo_curatore);
                                    data_guadagno="Il "+Data+" hai guadagnato: "+string_guadagno;
                                    utente_commento.setTransazioni(data_guadagno);
                                    utente_commento.setguadagno(guadagno_singolo_curatore);
                                }
                                post_corrente.addIterazione();//aggiorniamo le iterazione
                                count_commenti=0;
                            }
                            }
                        }
                    }
                    serverSocket.send(invio);//Il server invia a tutti i client registrati la notifica di un nuovo calcolo delle ricompense effettuato
                } catch (InterruptedException e) {
                    serverSocket.close();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
