package WinsomeClient;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.HashSet;
import java.util.Set;
//implementazione dei metodi dell'interfaccia: Interfaccia_Client
public class Impl_Client extends RemoteObject implements Interfaccia_Client {
    private Set<String> followers;
    public Impl_Client(Set<String> followers) throws RemoteException{
        super();
        this.followers=followers;
    }
    public void segnala_follow(String  utente) throws RemoteException {
        String messaggio=utente+" ha iniziato a seguirti";
        System.out.println(messaggio);
        this.followers.add(utente);
    }

    public void segnala_unfollow(String utente) throws RemoteException {
        String messaggio=utente+" ha smesso di seguirti";
        System.out.println(messaggio);
        this.followers.remove(utente);
    }

}
