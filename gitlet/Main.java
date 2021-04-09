package gitlet;
import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

/** Driver class for Gitlet, the tiny stupid version-control system.
   @author Emily Ma and Robin Yoo
*/
public class Main implements Serializable {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
       <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args[0].equals("init")) {
            Repo repo = new Repo();
            byte[] repoBytes = serialize(repo);
            File rFile = new File(".gitlet/repo");
            writeContents(rFile, repoBytes);
            return;
        }
        Repo repo = new Repo("new repo");
        if (args[0].equals("")) {
            System.out.println("Please enter a command.");
        } else if (args[0].equals("add")) {
            repo.add(args[1]);
        } else if (args[0].equals("commit")) {
            repo.commit(args[1]);
        } else if (args[0].equals("rm")) {
            repo.rm(args[1]);
        } else if (args[0].equals("log")) {
            repo.log();
        } else if (args[0].equals("global-log")) {
            repo.globalLog();
        } else if (args[0].equals("find")) {
            repo.find(args[1]);
        } else if (args[0].equals("status")) {
            repo.status();
        } else if (args[0].equals("checkout")) {
            if (args.length == 3 && args[1].equals("--")) {
                repo.checkout(args[2]);
            } else if (args.length == 4 && args[2].equals("--")) {
                repo.checkout(args[1], args[3]);
            } else if (args.length == 2) {
                repo.checkoutBranch(args[1]);
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("branch")) {
            repo.branch(args[1]);
        } else if (args[0].equals("reset")) {
            repo.reset(args[1]);
        } else if (args[0].equals("rm-branch")) {
            repo.rmBranch(args[1]);
        } else if (args[0].equals("merge")) {
            repo.merge(args[1]);
        } else {
            if (!args[0].equals("init")) {
                System.out.println("Command not found.");
            }
        }
        byte[] repoBytes = serialize(repo);
        File rFile = new File(".gitlet/repo");
        writeContents(rFile, repoBytes);

    }

}
