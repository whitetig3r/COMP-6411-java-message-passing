import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class Person extends Thread {
    private final ArrayList<String> callees;
    private final LinkedBlockingQueue callerQueue;
    private final String caller;
    private final Random random = new Random();

    public Person(String caller, ArrayList<String> callees) {
        this.callees = callees;
        this.callerQueue = Exchange.queueDirectory.get(caller);
        this.caller = caller;
    }

    @Override
    public void run()
    {
        try {
            callees.forEach(callee -> {
                try {
                    Thread.sleep(random.nextInt(100));
                    Exchange.queueDirectory.get(callee).put(new String[]{caller, "intro"});
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            long lastReceived = System.currentTimeMillis();

            while(((System.currentTimeMillis() - lastReceived) <= 5000)) {
                if(callerQueue.size() > 0) {
                    String[] messageReceived = (String []) callerQueue.take();
                    if(messageReceived[1].equals("intro")){
                        Thread.sleep(random.nextInt(100));
                        Exchange.queueDirectory.get(messageReceived[0])
                                .put(new String[]{caller, "reply"});
                    }
                    lastReceived = System.currentTimeMillis();
                    Exchange.mainQueue.put(
                            String.format("%s received %s message from %s (%s)",caller, messageReceived[1], messageReceived[0], lastReceived)
                    );
                }
            }
            System.out.printf("Process %s has received no calls for 5 seconds, ending...\n", caller);
            Thread.currentThread().interrupt();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
