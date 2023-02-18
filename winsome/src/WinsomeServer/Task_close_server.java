package WinsomeServer;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
public class Task_close_server implements Runnable{
    private final Thread guadango;
    private final ExecutorService es;
    private final Thread backup;

    public Task_close_server(Thread guadango,ExecutorService es, Thread backup){
        this.guadango=guadango;
        this.backup=backup;
        this.es=es;
    }
    public void run(){//questo thread rimane in attesa del comando exit, sarà un'uscita forzata come se ci fosse un problema nel serve, esegue solamente il backup e termina tutto il processo
        Scanner scan=new Scanner(System.in);
        String string=scan.nextLine();
        if(string.equals("exit")){
            backup.interrupt();//chiama  interupt del thread backup, verrà lanciata una eccezione che eseguirà il backup
            try {
                backup.join();
                System.out.println("Chiusura Server");
                System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
