import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    private static Catalog catalog;
    private static PageBuffer buffer;
    private static StorageManager storageManager;
    private static String dbLocation;
    private static int pageSize;
    private static int bufferSize;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Main <db location> <page size> <buffer size>");
            return;
        }
    
        initializeDatabase(args);
        runCommandLoop();
        shutdownDatabase();
    }
    
    private static void initializeDatabase(String[] args) {
        dbLocation = args[0].endsWith("/") ? args[0] : args[0] + "/";
        pageSize = Integer.parseInt(args[1]);
        bufferSize = Integer.parseInt(args[2]);
    
        new File(dbLocation + "tables").mkdirs();
    
        String catalogPath = dbLocation + "catalog.bin";
        System.out.println("Welcome to JottQL\nLooking for catalog at " + catalogPath + "...");
    
        buffer = new PageBuffer(bufferSize);
        catalog = new Catalog(dbLocation, pageSize, bufferSize);
        storageManager = new StorageManager(catalog, buffer);
    
        try {
            if (new File(catalogPath).exists()) {
                catalog.readCatalog(catalogPath);
                System.out.println("Catalog loaded successfully.");
            } else {
                System.out.println("No existing catalog found. Creating a new one.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load catalog: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void runCommandLoop() {
        System.out.println("\nPlease enter commands. Type <quit> to exit.\n");
    
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            StringBuilder commandBuffer = new StringBuilder();
            String inputLine;
    
            while (true) {
                System.out.print("JottQL> ");
                inputLine = reader.readLine();
    
                if (inputLine == null || inputLine.trim().equalsIgnoreCase("quit")) {
                    executeBufferedCommand(commandBuffer);
                    break;
                }
    
                commandBuffer.append(" ").append(inputLine.trim());
    
                if (inputLine.trim().endsWith(";")) {
                    executeBufferedCommand(commandBuffer);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }
    
    private static void executeBufferedCommand(StringBuilder commandBuffer) {
        if (commandBuffer.length() > 0) {
            parser.parse(commandBuffer.toString().trim(), catalog, buffer, dbLocation, pageSize, storageManager);
            commandBuffer.setLength(0);
        }
    }
    
    private static void shutdownDatabase() {
        buffer.writeBuffer();
        try {
            catalog.writeCatalog(dbLocation + "catalog.bin");
        } catch (IOException e) {
            System.err.println("Error saving catalog: " + e.getMessage());
        }
    
        System.out.println("Exiting...");
    }    

    public static String getDBLocation() {
        return dbLocation;
    }

    public static PageBuffer getBuffer() {
        return buffer;
    }

    public static Catalog getCatalog() {
        return catalog;
    }

    public static StorageManager getStorageManager() {
        return storageManager;
    }

    public static int getPageSize() {
        return pageSize;
    }

    public static void writeBuffer() {
        buffer.writeBuffer();
    }

    public static void writeCatalogToFile(String catalogPath) throws IOException {
        catalog.writeCatalog(catalogPath);
    }

}
