
package gitlet;
import static gitlet.Utils.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;


/** Defines the Commit class with the commit that is initialized
 *  with the message, hashmap of files, ArrayList of the parents.
 *  @author Robin Yoo Emily Ma */
public class Commit implements Serializable {

    /** Committed nessage. */

    private String message;
    /** Committed time. */
    private String time;
    /** Hashmap of blobs that are tracked. */
    private TreeMap<String, String> files;
    /** Array of head hash. */
    private String parents;
    /** Format of the date. */
    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-d HH:mm:ss ");

    /**Contractor for commit.
     * @param msg stroes the message
     * @param f stores Treemap of file
     * @param p stores String[] of parent */
    public Commit(String msg, TreeMap f, String p) {
        message = msg;
        files = f;
        parents = p;
        Date date;
        date = new Date();
        time = DATE_FORMAT.format(date);
    }

    /** return message. */
    public String getMessage() {
        return message;
    }

    /** return files. */
    public TreeMap<String, String> getFile() {
        return files;
    }

    /** return time. */
    public String getTime() {
        return time;
    }

    /** return parents the id of the parent commit. */
    public String getParents() {
        return parents;
    }

    /** check if the files is empty and return current
     * commit with the commit message, file, time, and master into
     * Utils.sha1, the id of the commit.
     */
    public String hashCommit() {
        byte[] bytes = serialize(this);
        return sha1(bytes);
    }
}
