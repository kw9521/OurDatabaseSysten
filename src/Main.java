import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Main {
    private static Catalog catalog;
    private static PageBuffer buffer;
    private static StorageManager storageManager;
    private static String dbLocation;
    private static int pageSize;
    private static int bufferSize;

    private static boolean indexing;
    private static ArrayList<BPlusTree> bPlusTrees;

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java Main <db location> <page size> <buffer size> <indexing>");
            return;
        }
    
        initializeDatabase(args);
        runCommandLoop();
        shutdownDatabase();
    }
    
    // if catalog exists, then keep page size, update buffer size
    private static void initializeDatabase(String[] args) {
        dbLocation = args[0].endsWith("/") ? args[0] : args[0] + "/";
        pageSize = Integer.parseInt(args[1]);
        bufferSize = Integer.parseInt(args[2]);
        indexing = Boolean.parseBoolean(args[3]);
    
        new File(dbLocation + "tables").mkdirs();
    
        String catalogPath = dbLocation + "catalog.bin";
        System.out.println("Welcome to JottQL\nLooking at " + catalogPath + " for existing db...");
    
        buffer = new PageBuffer(bufferSize);
        catalog = new Catalog(dbLocation, pageSize, bufferSize);
        storageManager = new StorageManager(catalog, buffer);
        bPlusTrees = new ArrayList<>();
    
        try {
            if (new File(catalogPath).exists()) {
                catalog.readCatalog(catalogPath);
                System.out.println("Database found...");
                System.out.println("Restarting the database...");
                System.out.println("    Ignoring provided pages size, using stored page size\n");
                System.out.println("Page Size: "+pageSize);
                System.out.println("Buffer Size: "+bufferSize);
                System.out.println("Indexing: "+indexing);
            } else {
                System.out.println("No existing db found.\nCreating new db at " +catalogPath);
                System.out.println("New db created successfully");
                System.out.println("Page Size: "+pageSize);
                System.out.println("Buffer Size: "+bufferSize);
                System.out.println("Indexing: "+indexing);
            }

        } catch (IOException e) {
            System.err.println("Failed to load catalog: " + e.getMessage());
            System.exit(1);
        }

        // Load BPlus tree roots from file
        if (indexing) {
            System.out.println("READING TREE");
            try {
                File indexDir = new File(dbLocation + "/BPIndex");
                if (indexDir.exists()) {
                    System.out.println("EXISTS?!");
                    for (Table table : catalog.getTables()) {
                        System.out.println(table);
                        Attribute primaryKey = null;
                        for (Attribute attribute : table.getAttributes()) {
                            System.out.println(attribute);
                            if (attribute.isPrimaryKey()) {
                                primaryKey = attribute;
                                break;
                            }
                        }
        
                        System.out.println("TEST");
                        System.out.println(primaryKey);

                        if (primaryKey == null) {
                            System.err.println("No primary key found for table: " + table.getName());
                            continue; // Skip this table
                        }
        
                        BPlusTree tree = BPlusTree.fromFile(table.getTableID(), primaryKey);
                        if (tree != null) {
                            // Use ensureCapacity if bPlusTrees is indexed by tableID
                            while (bPlusTrees.size() <= table.getTableID()) {
                                bPlusTrees.add(null);
                            }
                            bPlusTrees.set(table.getTableID(), tree);
                        } else {
                            System.err.println("Failed to load index for table: " + table.getName());
                        }
                    }
                    System.out.println("\nBPlus trees loaded successfully.");
                }
            } catch (Exception e) {
                System.err.println("\nFailed to load BPlus Tree: " + e.getMessage());
                e.printStackTrace();
            }
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
        System.out.println("\nSafely shutting down the database...");
        System.out.println("Purging page buffer...");
        buffer.writeBuffer();
        try {
            System.out.println("Saving catalog...\n");
            catalog.writeCatalog(dbLocation + "catalog.bin");
        } catch (IOException e) {
            System.err.println("Error saving catalog: " + e.getMessage());
        }
        for (BPlusTree tree : bPlusTrees) {
            tree.writeToFile();
            System.out.println("Indexes saved");
        }
        System.out.println("Exiting the database...\n");
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

    public static Boolean getIndexing() {
        return indexing;
    }

    public static ArrayList<BPlusTree> getBPlusTrees() {
        return bPlusTrees;
    }

    public static void writeBuffer() {
        buffer.writeBuffer();
    }

    public static void writeCatalogToFile(String catalogPath) throws IOException {
        catalog.writeCatalog(catalogPath);
    }

}
