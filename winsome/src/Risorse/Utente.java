package Risorse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
//definisce l'istanza del singolo utente
public class Utente {
    private final String username;
    private final String password;
    private final List<String> tags;
    private final Set<String> following;
    private final Set<String> followers;
    private final Set<Post> blog;
    private final Set<Post> feed;
    private double guadagno;
    private final Set<String> transazioni;
    private double bitcoin;
    public Utente(String username, String password, List<String> tags,Map<String,Utente> utenti){
        this.username=username;
        this.password=password;
        this.tags=tags;
        this.following=Collections.synchronizedSet(new HashSet<>());
        this.blog=Collections.synchronizedSet(new HashSet<>());
        this.feed=Collections.synchronizedSet(new HashSet<>());
        this.followers=Collections.synchronizedSet(new HashSet<>());
        this.guadagno=0;
        this.transazioni=Collections.synchronizedSet(new HashSet<>());
        this.bitcoin=0;
    }
    public String getUsername(){
        return this.username;
    }
    public String getPassword(){
        return this.password;
    }
    public List<String> getTags(){
        return this.tags;
    }
    public double getBitcoin(){
        return this.bitcoin;
    }
    public synchronized int setFollowing(String username){
        if(this.following.add(username)) {
            return 1;
        }
        else{
            return -1;
        }
    }
    public synchronized int delFollowing(String username){
        if (this.following.remove(username)) {
            return 1;
        } else {
            return 0;
        }
    }
    public synchronized int setFollower(String username) {
        if (this.followers.add(username)) {
            return 1;
        } else {
            return -1;
        }
    }
    public synchronized int delFollowers(String username){
        if (this.followers.remove(username)) {
            return 1;
        } else {
            return 0;
        }
    }
    public  Set<String> getFollowers(){
        return Collections.synchronizedSet(this.followers);
    }
    public  Set<String> getFollowing(){
        return Collections.synchronizedSet(this.following);
    }
    public synchronized void add_feed(Post post){
        this.feed.add(post);
    }
    public synchronized void set_feed(Set<Post> blog){
        this.feed.addAll(blog);
    }
    public synchronized void set_Blog(Set<Post> blog){
        this.blog.addAll(blog);
    }
    public synchronized void reset_feed(Set<Post> blog){
        Iterator<Post> lista_post=blog.iterator();
        while (lista_post.hasNext()){
            Post post_corrente= lista_post.next();
            synchronized (feed) {
                this.feed.remove(post_corrente);
            }
        }
    }
    public synchronized void elimina_post(Post post) {
        blog.remove(post);
        feed.remove(post);
    }
    public synchronized Post trova_post_blog(int id_post, String utente){
      Iterator<Post> lista_post=this.getBlog().iterator();
      Post post_da_eliminare = null;
      while (lista_post.hasNext()){
          Post post_corrente=lista_post.next();
          if(post_corrente.getId()==id_post && post_corrente.getAutore().equals(utente)){//se trova il post richiesto e l'utente ne è l'autore restituisce il post
              post_da_eliminare=post_corrente;
          }
          else if(post_corrente.getId()==id_post && !post_corrente.getAutore().equals(utente)){
              post_da_eliminare=new Post("//","//","//",-1);//restituisce post con id=-1 caso contrario
          }
      }
      return post_da_eliminare;
    }
    public synchronized Set<Post> feed_e_blog(){//restiuisce un Set che coniente sia i post del feed e del blog di un utente
        Set<Post> set_return=new HashSet<>();
        set_return.addAll(this.feed);
        set_return.addAll(this.blog);
        return set_return;
    }
    public  Set<Post> getFeed(){
        return Collections.synchronizedSet(this.feed);
    }
    public synchronized int add_post(Post post){
        if(post.getTitolo().length()<=20 && post.getContenuto().length()<=500){
            blog.add(post);
            return 1;
        }
        else if(post.getContenuto().length()>500 && post.getTitolo().length()<=20){
            return -1;
        }
        else if(post.getTitolo().length()>20 && post.getContenuto().length()<=500){
            return -2;
        }
        else{
            return -3;
        }

    }
    public  synchronized boolean nelfeed(Post post){
        return feed.contains(post);
    }
    public synchronized void setTransazioni(String data_guadagno){
        this.transazioni.add(data_guadagno);
    }
    public synchronized void setBitcoin(Double bitcoin){
        this.bitcoin=bitcoin;
    }
    public synchronized void setguadagno(double newguadagno){
        this.guadagno += newguadagno;
    }
    public synchronized  Set<Post> getBlog(){
        return Collections.synchronizedSet(this.blog);
    }
    public synchronized Set<String> getTransazioni(){
        return Collections.synchronizedSet(this.transazioni);
    }
    public double getGuadagno(){
        return this.guadagno;
    }
    public void Bitcoin(){
        HttpURLConnection http;
        try {
            URL url=new URL("https://www.random.org/integers/?num=1&min=1&max=99&col=1&base=10&format=plain&rnd=new");//path che indica la richieesta di un numero rando da 1 a 99 in base 10
            http=(HttpURLConnection) url.openConnection();
            if(http.getResponseCode()==200){//se la connessione è stata effettuata correttamente
                try(BufferedReader risposta=new BufferedReader(new InputStreamReader(http.getInputStream()))) {
                    String cambio="0.0"+risposta.readLine();//riceve il numero casuale dato da Random.org
                    Double cambioDouble=Double.parseDouble(cambio);
                    bitcoin=cambioDouble*guadagno;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
