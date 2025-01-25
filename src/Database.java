import java.io.File;
import java.util.Scanner;

public class Database {
    private static Catalog catalog;
    private static PageBuffer buffer;
    private static StorageManager storageManager;
    private static String dbLocation;
    private static int pageSize;
    private static int bufferSize;
    private static DDLParser ddlParser;
    private static DMLParser dmlParser;

    public static void main(String[] args) {
        // Check the correct number of args
        if (args.length != 3) {
            System.out.println("Usage: java Database <dblocation> <pageBufferSize> <pageSize>");
            return;
        }

        // Parse the args
        dbLocation = args[0];
        try {
            bufferSize = Integer.parseInt(args[1]);
            pageSize = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Error: pageBufferSize and pageSize must be integers.");
            return;
        }

        // Create our stuff
        catalog = new Catalog(dbLocation, pageSize, bufferSize);
        buffer = new PageBuffer(bufferSize);
        storageManager = new StorageManager(catalog, buffer); // idk what this needs either

        // Initialize parsers
        ddlParser = new DDLParser();
        dmlParser = new DMLParser();

        // Welcome message and initial database check
        System.out.println("Welcome to JottQL");
        System.out.println("Looking at " + dbLocation + " for existing db....");

        File dbFolder = new File(dbLocation);

        if (dbFolder.exists() && dbFolder.isDirectory()) {
            System.out.println("Existing db found at " + dbLocation);
        } else {
            System.out.println("No existing db found");
            System.out.println("Creating new db at " + dbLocation);
            if (dbFolder.mkdirs()) {
                System.out.println("New db created successfully");
            } else {
                System.out.println("Error: Failed to create db at " + dbLocation);
                return;
            }
        }

        // Display buffer and page size
        System.out.println("Page size: " + pageSize);
        System.out.println("Buffer size: " + bufferSize);

        // Start the input loop
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("\nPlease enter commands, enter <quit> to shutdown the db\n");
        
            while (true) {
                System.out.print("JottQL> ");
                String inputLine = scanner.nextLine();
        
                if (inputLine == null || inputLine.trim().equalsIgnoreCase("quit")) {
                    break;
                }
        
                inputLine = inputLine.trim();
        
                try {
                    if (inputLine.startsWith("create") || inputLine.startsWith("alter") || inputLine.startsWith("drop")) {
                        ddlParser.parseDDLstatement(inputLine, catalog, storageManager);
                    } else if (inputLine.startsWith("insert") || inputLine.startsWith("display") || inputLine.startsWith("select")) {
                        dmlParser.parseDMLstatement(inputLine, catalog, storageManager);
                    } else {
                        System.out.println("Unknown command: " + inputLine);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
        
    }
}
