package Risorse;
import java.util.*;
//questa classe definisce l'istanza del singolo post
public class Post {
    private String autore;
    private int id;
    private String titolo;
    private String contenuto;
    private Set<String> like;
    private Set<String> dislike;
    private Map<String,List<String>> commenti;
    private int iterazioni;
    public Post(String autore,String titolo,String contenuto,int id){
        this.autore=autore;
        this.titolo=titolo;
        this.contenuto=contenuto;
        this.id=id;
        this.like=Collections.synchronizedSet(new HashSet<>());
        this.dislike=Collections.synchronizedSet(new HashSet<>());
        this.commenti=Collections.synchronizedMap(new HashMap<>());
        this.iterazioni=1;
    }
    public String show_post(){
        return id+"   |   "+autore+"   |   "+titolo;
    }
    public String getAutore(){
        return this.autore;
    }
    public String getTitolo(){
        return this.titolo;
    }
    public String getContenuto(){
        return this.contenuto;
    }
    public int getId(){
        return this.id;
    }
    public void setIterazioni(int iterazioni){
        this.iterazioni=iterazioni;
    }
    public void addIterazione(){
        this.iterazioni++;
    }
    public int getIterazioni(){
        return this.iterazioni;
    }
    public synchronized int add_rate(String voto,String username){
        if(voto.equals("+1")) {
            //controlla se l'utene ha precedentemente votato questo post
            if (dislike.contains(username) == false && like.add(username)) {
                return 1;
            } else {
                return -2;
            }
        }
        else {
            if (like.contains(username) == false && dislike.add(username)) {
                return 1;
            } else {
                return -2;
            }
        }
    }
    public synchronized void add_commento(String username, String commento){
        if (!this.commenti.containsKey(username)) {
            this.commenti.put(username, new ArrayList<String>());
            this.commenti.get(username).add(commento);
        } else {
            this.commenti.get(username).add(commento);
        }
    }
    public synchronized Set<String> keyset_commentatori(){
        return Collections.synchronizedMap(this.commenti).keySet();
    }
    public synchronized Map<String,List<String>> getCommenti(){
        return Collections.synchronizedMap(this.commenti);
    }
    public synchronized List<String> commenti(String utente){
        return Collections.synchronizedMap(this.commenti).get(utente);
    }
    public synchronized boolean contiene_commenti(){
        if(!this.commenti.isEmpty()){
            return true;
        }
        else {
            return false;
        }
    }
    public synchronized int numero_commenti_utente(String utente){
        return Collections.synchronizedMap(this.commenti).get(utente).size();
    }
    public synchronized Set<String> getLike(){
        return Collections.synchronizedSet(this.like);
    }
    public synchronized Set<String> getDislike(){
        return Collections.synchronizedSet(this.dislike);
    }
    public synchronized int numero_like(){
        return Collections.synchronizedSet(this.like).size();
    }
    public synchronized int numero_dislike(){
        return Collections.synchronizedSet(this.dislike).size();
    }
}
