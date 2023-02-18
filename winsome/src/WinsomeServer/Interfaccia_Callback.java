package WinsomeServer;
import WinsomeClient.Interfaccia_Client;
import java.rmi.Remote;
import java.rmi.RemoteException;
public interface Interfaccia_Callback  extends Remote {
    //metodi per registrare/disiscrivere il client alla ricezioni delle notifiche per un nuovo follow/unfollow
    public void registra_al_callback(Interfaccia_Client interfaccia_client,String username)throws RemoteException;
    public void unregister_al_callback(Interfaccia_Client interfaccia_client)throws RemoteException;
}
