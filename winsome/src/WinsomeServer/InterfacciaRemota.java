package WinsomeServer;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

public interface InterfacciaRemota extends Remote {
    public String registrazione(String username, String password, List<String> tags)throws RemoteException;//metodo per registrare un utente a winsome
}
