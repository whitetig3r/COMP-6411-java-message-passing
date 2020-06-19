import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Person extends Thread {
    private final ArrayList<String> callees;
    private final LinkedBlockingQueue<String[]> callerQueue;
    private final String caller;
    private final boolean isLastThread;
    private final Object lock;

    public Person(String caller, ArrayList<String> callees, boolean isLastThread, Object lock) {
        this.callees = callees;
        this.callerQueue = Exchange.queueDirectory.get(caller);
        this.caller = caller;
        this.isLastThread = isLastThread;
        this.lock = lock;
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

        waitIUntilAllThreadsSpawned();

        /*
            sends intro messages to all contacts in list
        */

        sendIntroMessages();

        /*
            infinite loop where it check for new messages in
            message queue
        */

        receiveMessages();
    }

    private void receiveMessages() {
        long lastReceived = System.nanoTime();

        while(((System.nanoTime() - lastReceived) <= 5e9)) {
            if (callerQueue.size() > 0) {
                lastReceived = System.nanoTime();
                try {
                    String[] messageReceived;
                    messageReceived = callerQueue.take();
                    if (messageReceived[1].equals("intro")) {
                        randomizedSleep();
                        Exchange.queueDirectory.get(messageReceived[0])
                                .put(new String[]{caller, "reply", String.valueOf(lastReceived)});
                    }
                    Exchange.mainQueue.put(
                            String.format(
                                    "%s received %s message from %s (%s)",
                                    caller, messageReceived[1],
                                    messageReceived[0],
                                    (messageReceived.length == 3 ?
                                    messageReceived[2] : lastReceived)
                            )
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.printf("Process %s has received no calls for 5 seconds, ending...\n", caller);
        Thread.currentThread().interrupt();
    }

    private void randomizedSleep() throws InterruptedException {
        Thread.sleep(Exchange.randomSleepTimes.pop());
    }

    private void sendIntroMessages() {
        callees.forEach(callee -> {
            try {
                randomizedSleep();
                Exchange.queueDirectory.get(callee).put(new String[]{caller, "intro"});
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void waitIUntilAllThreadsSpawned() {
        synchronized (lock) {
            if (!isLastThread) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                lock.notifyAll();
            }
        }
    }

}
