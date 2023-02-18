package WinsomeServer;
import WinsomeClient.Interfaccia_Client;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.*;
public class Impl_Callback extends RemoteServer implements Interfaccia_Callback {
    private Map<Interfaccia_Client,String> clients;
    public Impl_Callback()throws RemoteException{
        super();
        clients=new HashMap<Interfaccia_Client,String>();
    }
    public synchronized void registra_al_callback(Interfaccia_Client interfaccia_client,String username)throws RemoteException{
        if(!clients.keySet().contains(interfaccia_client)){
            clients.put(interfaccia_client,username);
            System.out.println("Nuovo utente registrato al servizio di notifica nuovo follow o unfollow");
        }
    }
    public synchronized void unregister_al_callback(Interfaccia_Client interfaccia_client)throws RemoteException{
        if(clients.keySet().remove(interfaccia_client)){
            return;
        }
    }
    public void segnala(String influencer,String seguace,int flag)throws RemoteException{
        doCallaback(influencer,seguace,flag);
    }
    private synchronized void doCallaback(String  influencer,String seguace,int flag) throws RemoteException {
        Iterator<Interfaccia_Client> i=clients.keySet().iterator();//scorriamo i client registrati al servizio
        while (i.hasNext()){
            Interfaccia_Client client=i.next();
            if(clients.get(client).equals(influencer)){//se il client corrisponde all'utente che ha ricevuto un nuovo follow
                if(flag==1) {
                    client.segnala_follow(seguace);//invia notifica
                }
                else {
                    client.segnala_unfollow(seguace);
                }
            }

        }
    }
}
