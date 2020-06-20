import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Person extends Thread {
    private final ArrayList<String> callees;
    private final LinkedBlockingQueue<String[]> callerQueue;
    private final String caller;
    private static final Random random = new Random();
    private final CountDownLatch latch;

    public Person(String caller, ArrayList<String> callees, CountDownLatch latch) {
        this.callees = callees;
        this.callerQueue = exchange.queueDirectory.get(caller);
        this.caller = caller;
        this.latch = latch;
    }

    @Override
    public void run() {
        sendAndReceiveMessages();
    }

    private void sendAndReceiveMessages() {
        /*
            waits until all threads have spawned and then begins
            execution ie. sending and receiving
        */

        waitUntilAllThreadsSpawned();

        /*
            sends intro messages to all contacts in list
        */

        sendIntroMessages();

        /*
            infinite loop where it check for new messages in
            message queue
        */

        try {
            receiveMessages();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waitUntilAllThreadsSpawned() {
        try {
            latch.countDown();
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() throws InterruptedException {
        String[] messageReceived;

        while((messageReceived = callerQueue.poll(5, TimeUnit.SECONDS)) != null) {
            long lastReceived = System.nanoTime();
            if (messageReceived[1].equals("intro")) {
                randomizedSleep();
                exchange.queueDirectory.get(messageReceived[0])
                        .put(new String[]{caller, "reply", String.valueOf(lastReceived)});
            }
            exchange.mainQueue.put(
                    String.format(
                            "%s received %s message from %s (%s)",
                            caller, messageReceived[1],
                            messageReceived[0],
                            (messageReceived.length == 3 ?
                            messageReceived[2] : lastReceived)
                    )
            );
        }

        System.out.printf("\nProcess %s has received no calls for 5 seconds, ending...\n", caller);
    }

    private void randomizedSleep() throws InterruptedException {
        Thread.sleep(random.nextInt(100));
    }

    private void sendIntroMessages() {
        callees.forEach(callee -> {
            try {
                randomizedSleep();
                exchange.queueDirectory.get(callee).put(new String[]{caller, "intro"});
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

}
