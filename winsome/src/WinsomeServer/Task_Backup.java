package WinsomeServer;
import Risorse.Utente;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Task_Backup implements Runnable{//Thread che esegue periodicamente il backup del sistema, e anche quando il processo viene fermato in manier brusca
    private Map<String,Utente> utenti;
    private File file;
    private ObjectMapper objectMapper;
    private int minuti_backup;
    private String file_backup;
    public Task_Backup(Map<String,Utente> utenti,File file,ObjectMapper objectMapper,int minuti_backup,String file_backup){
        this.utenti=utenti;
        this.file=file;
        this.objectMapper=objectMapper;
        this.minuti_backup=minuti_backup;
        this.file_backup=file_backup;
    }
    public void run() {
        File file = new File(file_backup);
        //un while(true) che tiene il thread in vita fino a quando il server è in funzione
        while (true) {
            try {
                int secondi=minuti_backup*60;//ogni quanti minuti effettuare il backup
                int millis=secondi*1000;
                Thread.sleep(millis);//
                JsonNode node=objectMapper.valueToTree(utenti);//rappresentiamo la struttura dati utente come un albero
                objectMapper.writeValue(file,node);//lo scriviamo su un file json
                System.out.println("Backup effettuato");//avertiamo il server stampando che il backup è stato effettuato
            } catch (InterruptedException e) {//se il thread viene interroto tra un backup e l'altro, significa che il server sta per chiudersi e viene lanciata
                //una eccezzione che esegue il backup
                try {
                    JsonNode node=objectMapper.valueToTree(utenti);
                    objectMapper.writeValue(file,node);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println("Backup effettuato");//avvertiamo il server stampando che il backup è stato effettuato
                return;
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
