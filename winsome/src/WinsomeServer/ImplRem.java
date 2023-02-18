package WinsomeServer;
import Risorse.Utente;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.*;
public class ImplRem extends RemoteServer implements InterfacciaRemota {
    final Map<String,Utente> utenti;
    public ImplRem(Map<String,Utente> utenti)throws RemoteException{
        super();
        this.utenti=utenti;
    }
    //metodo dell'oggetto remoto
    public  String registrazione(String username, String password, List<String> tags)throws RemoteException{
        Utente newutente=new Utente(username,password,tags,Collections.synchronizedMap(utenti));//si crea un nuovo utente
        synchronized (utenti){
            if(!utenti.containsKey(username) && password!=null){//se non è già registrato su winsome
                utenti.put(username,newutente);//si inserisce nella struttura dati utenti
                return "Utente "+newutente.getUsername()+" registrato";//si invia la notifica
            }
            else {
                if(password==null || username==null){
                    return "Registrazione non effettuata";
                }
                else {
                    return "Utente non registrato perchè username già utilizzato su windsome";
                }
            }
        }

    }
}
