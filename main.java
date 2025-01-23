import java.util.Scanner;

public class main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter your input (type 'exit' to quit):");

        while (true) {
            // Check DB Location, verify existss

            System.out.print("Input: ");
            String userInput = scanner.nextLine().trim();

            // Check if the user wants to exit
            if (userInput.equalsIgnoreCase("exit")) {
                System.out.println("Exiting the program. Goodbye!");
                break;
            }

            // Handle commands
            if (userInput.equalsIgnoreCase("create table")) {
                System.out.println("Placeholder: Create table logic goes here.");
            } else if (userInput.equalsIgnoreCase("alter table")) {
                System.out.println("Placeholder: Alter table logic goes here.");
            } else if (userInput.equalsIgnoreCase("drop table")) {
                System.out.println("Placeholder: Drop table logic goes here.");
            } else if (userInput.equalsIgnoreCase("insert into")) {
                System.out.println("Placeholder: Insert into logic goes here.");
            } else {
                System.out.println("Unrecognized command: " + userInput);
            }
        }

        scanner.close();
    }
}
