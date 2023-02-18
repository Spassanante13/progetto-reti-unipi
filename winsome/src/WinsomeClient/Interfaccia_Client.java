package WinsomeClient;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Interfaccia_Client extends Remote {
    //Metodi esposti
    public void segnala_follow(String utente)throws RemoteException;
    public void segnala_unfollow(String utente)throws RemoteException;
}
