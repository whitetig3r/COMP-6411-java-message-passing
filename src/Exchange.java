import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Exchange {
    private static HashMap<String, ArrayList<String>> directory = new HashMap<>();
    public static HashMap<String, LinkedBlockingQueue> queueDirectory = new HashMap<>();
    public static LinkedBlockingQueue mainQueue = new LinkedBlockingQueue();

    public static void main(String[] args) {
        System.out.println("** Calls to be made **");
        readFileIntoStructure();
        System.out.println();
        ExecutorService threadPool = Executors.newFixedThreadPool(directory.size());
        for(Map.Entry<String, ArrayList<String>> tuple : directory.entrySet()){
            threadPool.execute(new Person(tuple.getKey(), tuple.getValue()));
        }
        long lastReceived = System.currentTimeMillis();
        while(System.currentTimeMillis() - lastReceived <= 10000) {
            if(mainQueue.size() > 0){
                try {
                    System.out.println(mainQueue.take());
                    lastReceived = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Master has received no replies for 10 seconds, ending...");
        threadPool.shutdown();
    }

    private static void readFileIntoStructure() {
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
    }

    private static void processLine(String line) {
        String tuple = line.substring(1,line.length() - 2);
        String[] keyValList = tuple.split("\\[");
        String stringifiedVals = keyValList[1].trim();

        String keyForDirectory = keyValList[0].trim();
        keyForDirectory = keyForDirectory.substring(0, keyForDirectory.length() - 1);

        queueDirectory.put(keyForDirectory, new LinkedBlockingQueue());

        System.out.printf("%s: [%s\n", keyForDirectory,stringifiedVals);

        stringifiedVals = stringifiedVals.substring(0,stringifiedVals.length() - 1);
        String[] vals = stringifiedVals.split(",");
        directory.put(keyForDirectory, new ArrayList<>(Arrays.asList(vals)));
    }
}
