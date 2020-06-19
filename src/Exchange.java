import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Exchange {
    private static final HashMap<String, ArrayList<String>> directory = new HashMap<>();
    public static HashMap<String, LinkedBlockingQueue<String[]>> queueDirectory = new HashMap<>();
    public static LinkedBlockingQueue<String> mainQueue = new LinkedBlockingQueue<>();
    public static Stack<Integer> randomSleepTimes = new Stack<>();

    public static void main(String[] args) {
        readFileIntoStructure();
        threadedFunctionalityExecutor();
    }

    private static void threadedFunctionalityExecutor() {

        createDistinctRandomSleepTimeStack();
        /*
            spawn friend threads and execute their run
            methods
        */

        ExecutorService threadPool = spawnThreadsAndExecute();

        /*
            receive messages from friend threads
            shutdown main thread on timeout
        */

        receiveMessages();
        threadPool.shutdown();
    }

    private static void createDistinctRandomSleepTimeStack() {
        int randValuesToSeed = directory.values().stream().map(ArrayList::size)
                .reduce(Integer::sum).orElse(0);

        ThreadLocalRandom.current().ints(0, 100)
                .distinct().limit(randValuesToSeed*2).forEach(randVal -> {
                randomSleepTimes.push(randVal);
        });
    }

    private static ExecutorService spawnThreadsAndExecute() {
        ExecutorService threadPool = Executors.newFixedThreadPool(directory.size());
        int threadIndex = 0;
        boolean isLastThread = false;
        // a placeholder shared resource for all friend threads
        Object lock = new Object();

        for(Map.Entry<String, ArrayList<String>> tuple : directory.entrySet()){
            if(threadIndex == directory.size() - 1) isLastThread = true;
            threadPool.execute(new Person(tuple.getKey(), tuple.getValue(), isLastThread, lock));
            threadIndex++;
        }

        return threadPool;
    }

    private static void receiveMessages() {
        long lastReceived = System.nanoTime();
        while(System.nanoTime() - lastReceived <= 1e10) {
            if(mainQueue.size() > 0){
                try {
                    System.out.println(mainQueue.take());
                    lastReceived = System.nanoTime();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Master has received no replies for 10 seconds, ending...");
    }

    private static void readFileIntoStructure() {
        System.out.println("** Calls to be made **");
        File file=new File("calls.txt");
        try(FileReader reader = new FileReader(file)) {
            BufferedReader bReader = new BufferedReader(reader);
            String line;
            while((line = bReader.readLine())!=null)
            {
                processLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private static void processLine(String line) {
        String tuple = line.substring(1,line.length() - 2);
        String[] keyValList = tuple.split("\\[");
        String stringifiedVals = keyValList[1].trim();

        String keyForDirectory = keyValList[0].trim();
        keyForDirectory = keyForDirectory.substring(0, keyForDirectory.length() - 1);

        queueDirectory.put(keyForDirectory, new LinkedBlockingQueue<>());

        System.out.printf("%s: [%s\n", keyForDirectory,stringifiedVals);

        stringifiedVals = stringifiedVals.substring(0,stringifiedVals.length() - 1);
        String[] vals = stringifiedVals.split(",");
        directory.put(keyForDirectory, new ArrayList<>(Arrays.asList(vals)));
    }
}
