package commands;

import org.crsh.cli.Command;
import org.crsh.cli.Usage;
import org.crsh.command.BaseCommand;

@Usage("shutdown the program")
public class stop extends BaseCommand {

    @Command
    @Usage("shutdown the program")
    public void main() {
        System.out.println("Exiting.");
        System.exit(0);
    }
}