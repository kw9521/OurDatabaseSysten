import java.io.File;
import java.util.Scanner;

import ddl.DDLParserInterface;
import dml.DMLParserInterface;

public class Database {
    private static String dbLocation;
    private static int pageSize;
    private static int bufferSize;
    private static DDLParserInterface ddlParser;
    private static DMLParserInterface dmlParser;

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
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nPlease enter commands, enter <quit> to shutdown the db");

        while (true) {
            System.out.print("\nJottQL> ");
            String userInput = scanner.nextLine().trim();

            // Check if the user wants to quit
            if (userInput.equalsIgnoreCase("quit")) {
                System.out.println("Exiting the program. Goodbye!");
                break;
            }

            // Handle commands
            // Eventually create statement handler that will send queries to DDL and DML Parsers
            // Need to create instances of each parser above
            // Please check parseDDLstatement and parseDMLstatement in their respective classes
            switch (userInput.toLowerCase()) {
                case "display schema;":
                    displaySchema();
                    break;
                case "create table":
                    System.out.println("Placeholder: Create table logic goes here.");
                    break;
                case "alter table":
                    System.out.println("Placeholder: Alter table logic goes here.");
                    break;
                case "drop table":
                    System.out.println("Placeholder: Drop table logic goes here.");
                    break;
                case "insert into":
                    System.out.println("Placeholder: Insert into logic goes here.");
                    break;
                default:
                    System.out.println("Unrecognized command: " + userInput);
            }
        }

        scanner.close();
    }

    // PLACEHOLDER FOR TABLES
    private static void displaySchema() {
        System.out.println("\nDB location: " + dbLocation);
        System.out.println("Page Size: " + pageSize);
        System.out.println("Buffer Size: " + bufferSize);
        System.out.println("\nNo tables to display");
        System.out.println("SUCCESS");
    }
}
