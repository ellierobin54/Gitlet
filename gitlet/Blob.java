package gitlet;
import java.io.File;
import java.io.Serializable;


/** Defines the Blob class with the blob that
 *  contains the File.
 *  @author Robin Yoo Emily Ma */
public class Blob implements Serializable {

    /** name of the file. */
    private String name;
    /** file of name. */
    private File file;
    /** bytes of the file. */
    private byte[] contents;

    /**Constructor that store name, file, and byte[].
     * @param n contains name of file.
     * @param f file of the name.
     * @param c byte[] of file.
     */
    public Blob(String n, File f, byte[] c) {
        this.name = n;
        this.file = f;
        this.contents = c;
    }

    /** returns the name of file. */
    public String getName() {
        return name;
    }

    /** returns the file. */
    public File getFile() {
        return file;
    }

    /** returns the byte[] of the file. */
    public byte[] getContents() {
        return contents;
    }
}
