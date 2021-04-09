package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

import static gitlet.Utils.*;

/** Defines the repository and keep all teh commits that gitlet have.
 * @author Robin Yoo && Emily Ma */
public class Repo implements Serializable {

    /** branches contain head(key) and id (value). */
    private TreeMap<String, String> branches;
    /** pointer of the head of the current branch. */
    private String head;
    /** staging area where snaps are stored. */
    private TreeMap<String, String> stagingArea;
    /** length of the id. */
    private final int idLength = 40;


    /** initiate the gitlet commit. */
    public Repo() {
        File gitlet = new File(".gitlet");
        if (!gitlet.exists()) {
            gitlet.mkdir();
            File commits = join(gitlet, "commits");
            commits.mkdir();
            File b = join(gitlet, "branches");
            b.mkdir();
            File staging = join(gitlet, "staging");
            staging.mkdir();
            File removed = join(gitlet, "removed");
            removed.mkdir();
            File blobs = join(gitlet, "blobs");
            blobs.mkdir();
            Commit init = new Commit("initial commit", null, null);
            byte[] bytes = serialize(init);
            String cid = sha1(bytes);
            File cFile = join(commits, cid);
            writeContents(cFile, bytes);
            File bFile = join(b, "master");
            writeContents(bFile, cid);
            File h = join(gitlet, "head");
            writeContents(h, "master");
            this.head = "master";
            this.branches = new TreeMap<String, String>();
            this.branches.put("master", cid);
            stagingArea = new TreeMap<String, String>();
        } else {
            System.out.println("A gitlet version-control system already "
                    + "exists in the current directory.");
        }
    }

    /** re-initiates the gitlet commit between commands.
     * @param s Store msg of the constructor */
    public Repo(String s) {
        File r = new File(".gitlet/repo");
        Repo newRepo = readObject(r, Repo.class);
        this.head = newRepo.head;
        this.branches = newRepo.branches;
        this.stagingArea = newRepo.stagingArea;
    }

    /** returns the head from the branches. */
    public String getHead() {
        return branches.get(head);
    }

    /** returns the branches. */
    public TreeMap<String, String> getBranches() {
        return branches;
    }

    /** returns the staging area. */
    public TreeMap<String, String> getStagingArea() {
        return stagingArea;
    }

    /** returns the untracked files. */
    public TreeMap<String, String> getUntracked() {
        TreeMap<String, String> untracked = new TreeMap<>();
        TreeMap<String, String> tracked = idConvertor(getHead()).getFile();
        File current = new File(".");
        File[] allFiles = current.listFiles();
        if (allFiles != null) {
            for (File f : allFiles) {
                if (!f.isDirectory()) {
                    File r = new File(".gitlet/removed/" + f.getName());
                    if (tracked != null) {
                        if (!tracked.containsKey(f.getName())
                                && !stagingArea.containsKey(f.getName())
                                && !r.exists()) {
                            untracked.put(f.getName(), "");
                        }
                    } else if (!stagingArea.containsKey(f.getName())
                            && !r.exists()) {
                        untracked.put(f.getName(), "");
                    }
                }
            }
        }
        return untracked;
    }

    /** add a copy (snap) of the field as it currently
     * exists in staging area.
     * @param s the snap you are adding
     */
    public void add(String s) {
        File f = new File(s);
        if (!f.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        byte[] content = readContents(f);
        Blob b = new Blob(s, f, content);
        byte[] bytesb = serialize(b);
        String blobId = sha1(bytesb);
        File ff = new File(".gitlet/blobs/" + blobId);
        File remove = new File(".gitlet/removed");
        List<String> removedFiles = plainFilenamesIn(remove);
        if (removedFiles != null) {
            for (String r : removedFiles) {
                if (r.equals(s)) {
                    File currentFile = new File(".gitlet/removed/" + r);
                    currentFile.delete();
                }
            }
        }
        Commit recent = idConvertor(getHead());
        TreeMap<String, String> files = recent.getFile();

        if (files != null && files.containsKey(s)
                && files.get(s).equals(blobId)) {
            return;
        }

        if (!ff.exists()) {
            stagingArea.put(s, blobId);
            writeContents(ff, bytesb);
            File blob = new File(".gitlet/staging/" + s);
            writeContents(blob, blobId);
        }

    }

    /** commit with msg only.
     * @param msg contains the message of the commit */
    public void commit(String msg) {
        if (msg.trim().equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Commit recent = idConvertor(getHead());
        TreeMap<String, String> tracked = recent.getFile();
        if (tracked == null) {
            tracked = new TreeMap<>();
        }
        boolean added = false;
        boolean removed = false;
        if (!stagingArea.isEmpty()) {
            added = true;
            for (String k : stagingArea.keySet()) {
                tracked.put(k, stagingArea.get(k));
            }
            stagingArea = new TreeMap<String, String>();
            File sa = new File(".gitlet/staging");
            String[] entries = sa.list();
            for (String e : entries) {
                File currentFile = new File(".gitlet/staging/" + e);
                currentFile.delete();
            }

        }
        File remove = new File(".gitlet/removed");
        List<String> removedFiles = plainFilenamesIn(remove);
        if (removedFiles != null) {
            for (String f : removedFiles) {
                if (tracked.containsKey(f)) {
                    tracked.remove(f);
                    File currentFile = new File(".gitlet/removed/" + f);
                    currentFile.delete();
                    removed = true;
                }
            }
        }
        if (!removed) {
            if (!added) {
                System.out.println("No changes added to the commit.");
                System.exit(0);
            }
        }
        String parents = getHead();
        Commit c = new Commit(msg, tracked, parents);
        byte[] bytes = serialize(c);
        String s = sha1(bytes);
        File f = new File(".gitlet/commits/" + s);
        writeContents(f, bytes);
        branches.put(head, s);
        File b = new File(".gitlet/branches/" + head);
        writeContents(b, s);

    }

    /** removes the given file name from the staging area and also deletes
     * it if is in tracked files.
     * @param name of the file to be removed.
     */
    public void rm(String name) {
        File f = new File(name);
        Commit recent = idConvertor(getHead());
        TreeMap<String, String> tracked = recent.getFile();
        if (!f.exists() && !tracked.containsKey(name)) {
            System.out.println("File does not exist.");
        }

        if (tracked != null && tracked.containsKey(name)) {
            restrictedDelete(f);
            File r = join(new File(".gitlet/removed"), name);
            writeContents(r);
            if (stagingArea.containsKey(name)) {
                stagingArea.remove(name);
                File currentFile = new File(".gitlet/staging/" + name);
                currentFile.delete();
            }
        } else if (stagingArea.containsKey(name)) {
            stagingArea.remove(name);
            File nf = new File(".gitlet/staging/" + name);
            nf.delete();
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** Converts the id into commit.
     * @param id id of the commit
     * @return Commit object of the id*/
    public Commit idConvertor(String id) {
        File f = new File(".gitlet/commits/" + id);
        if (f.exists()) {
            return readObject(f, Commit.class);
        } else {
            System.out.println("No commit with that id exists.");
            System.exit(0);
            return null;
        }
    }

    /** prints the commits.
     * @param id id of the commit that you will print */
    public void printCommit(String id) {
        Commit c = idConvertor(id);
        if (c != null) {
            System.out.println("===");
            System.out.println("Commit " + id);
            System.out.println(c.getTime());
            System.out.println(c.getMessage());
        }
        System.out.println();
    }

    /** returns all the history of the head. */
    public void log() {
        String h = getHead();
        while (h != null) {
            Commit c = idConvertor(h);
            printCommit(h);
            h = c.getParents();
        }
    }

    /** returns all the history in out of order. */
    public void globalLog() {
        File c = new File(".gitlet/commits");
        File[] commit = c.listFiles();
        for (File file : commit) {
            printCommit(file.getName());
        }
    }

    /** Searches through all of commits ever and prints the commits
     * that contain the same message in a separate line.
     * @param msg the message of the matching commits to be printed.
     */
    public void find(String msg) {
        File c = new File(".gitlet/commits");
        File[] commit = c.listFiles();

        if (commit == null) {
            System.exit(0);
        }
        boolean exists = false;
        for (File file : commit) {
            Commit comm = idConvertor(file.getName());

            if (comm.getMessage().equals(msg)) {
                System.out.println(file.getName());
                exists = true;
            }
        }
        if (!exists) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Displays all of the current branches, staged files, and
     * files to be removed in alphabetical order.
     */
    public void status() {
        System.out.println("=== Branches ===");
        File branchDir = new File(".gitlet/branches");
        List<String> listBranches = Utils.plainFilenamesIn(branchDir);
        if (listBranches != null) {
            for (String b : listBranches) {
                if (b.equals(head)) {
                    System.out.print("*");
                }
                System.out.println(b);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        File stageDir = new File(".gitlet/staging");
        List<String> listStaging = Utils.plainFilenamesIn(stageDir);
        if (listStaging != null) {
            for (String s : listStaging) {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        File removedDir = new File(".gitlet/removed");
        List<String> listRemoved = Utils.plainFilenamesIn(removedDir);
        if (listRemoved != null) {
            for (String r : listRemoved) {
                System.out.println(r);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
    }

    /** Reverts the given file to the version in the previous head commit.
     * @param name of the given file
     */
    public void checkout(String name) {
        Commit recent = idConvertor(getHead());
        TreeMap<String, String> tracked = recent.getFile();
        if (!tracked.containsKey(name)) {
            System.out.println("File does not exist in that commit.");
        } else {
            String blobId = tracked.get(name);
            File f = new File(name);
            File blob = new File(".gitlet/blobs/" + blobId);
            Blob blobObject = readObject(blob, Blob.class);
            byte[] content = blobObject.getContents();
            writeContents(f, content);
        }
    }

    /** Convert Id in short version.
     * @param id storest the id.
     * @return stirng of the id. */
    private String convertId(String id) {
        String rev = "";
        if (id.length() == idLength) {
            rev = id;
        }

        File commits = new File(".gitlet/commits");
        File[] comm = commits.listFiles();
        for (File s : comm) {
            if (s.getName().contains(id)) {
                rev = s.getName();
                break;
            }
        }
        return rev;
    }

    /** Reverts the given file to the version in the given commit.
     * @param name of the given file.
     * @param id of the commit to be reverted to.
     */
    public void checkout(String id, String name) {
        String newid = convertId(id);
        if (newid.equals("")) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit recent = idConvertor(newid);
        TreeMap<String, String> tracked = recent.getFile();
        if (!tracked.containsKey(name)) {
            System.out.println("File does not exist in that commit.");
        } else {
            String blobId = tracked.get(name);
            File f = new File(name);
            File blob = new File(".gitlet/blobs/" + blobId);
            Blob blobObject = readObject(blob, Blob.class);
            byte[] content = blobObject.getContents();
            writeContents(f, content);
        }
    }
    /** Checks if there is an untracked file that
     * would be overwritten by the checkout.
     * @param id of the commit to be reverted to.
     * @return boolena of the untracked file.
     */
    private boolean checkUntracked(String id) {
        String s = "There is an untracked file in the way; "
                + "delete it or add it first.";
        TreeMap<String, String> untracked = getUntracked();
        boolean error = false;
        if (untracked == null) {
            System.exit(0);
        }
        for (String u : untracked.keySet()) {
            File file = new File(u);
            TreeMap<String, String> branchCommit = idConvertor(id).getFile();
            if (branchCommit != null && branchCommit.containsKey(u)) {
                String bid = branchCommit.get(u);
                byte[] content = readContents(file);
                Blob b = new Blob(u, file, content);
                byte[] bytesb = serialize(b);
                String blobId = sha1(bytesb);
                if (!blobId.equals(bid)) {
                    System.out.println(s);
                    error = true;
                }
            }

        }
        return error;
    }

    /** Reverts the files to the commit at the head of given branch.
     * Head is changed to branch.
     * @param branch to change to
     */
    public void checkoutBranch(String branch) {
        if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        } else if (head.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        String s = branches.get(branch);
        Commit c = idConvertor(s);
        TreeMap<String, String> cFiles = c.getFile();
        if (checkUntracked(branches.get(branch))) {
            System.exit(0);
        }
        TreeMap<String, String> tracked = idConvertor(getHead()).getFile();
        for (String s1 : tracked.keySet()) {
            File f2 = new File(s1);
            if (cFiles == null) {
                restrictedDelete(f2);
            } else {
                if (!cFiles.containsKey(s1)) {
                    restrictedDelete(f2);
                }
            }
        }
        if (cFiles != null) {
            for (String file : cFiles.keySet()) {
                File blob = new File(".gitlet/blobs/" + cFiles.get(file));
                byte[] content = readObject(blob, Blob.class).getContents();
                File f2 = new File(file);
                writeContents(f2, content);
            }
        }
        stagingArea = new TreeMap<String, String>();
        File sa = new File(".gitlet/staging");
        String[] entries = sa.list();

        if (entries != null) {
            for (String e : entries) {
                File currentFile = new File(".gitlet/staging/" + e);
                currentFile.delete();
            }
        }
        File remove = new File(".gitlet/removed");
        String[] removal = remove.list();
        if (removal != null) {
            for (String e : removal) {
                File currentFile = new File(".gitlet/removed/" + e);
                currentFile.delete();
            }
        }

        head = branch;
        File headF = new File("head");
        writeContents(headF, branch);
    }

    /** Creates a new branch pointing to head commit.
     * @param name name of new branch.
     */
    public void branch(String name) {
        if (!branches.containsKey(name)) {
            File b = new File(".gitlet/branches/" + name);
            writeContents(b, getHead());
            branches.put(name, getHead());
        } else {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
    }

    /** Removes given branch.
     * @param name name of the branch to remove.
     */
    public void rmBranch(String name) {
        if (head.equals(name)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else if (branches.containsKey(name)) {
            branches.remove(name);
            File f = new File(".gitlet/branches/" + name);
            f.delete();
        } else {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /** Resets to the commit given by id.
     * @param id Commit id to revert back to.
     */
    public void reset(String id) {
        String newid = convertId(id);
        if (newid.equals("")) {
            System.exit(0);
        } else {
            File commits = new File(".gitlet/commits/" + newid);
            if (!commits.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
        }
        Commit c = idConvertor(id);
        TreeMap<String, String> branchFiles = c.getFile();
        if (checkUntracked(newid)) {
            System.exit(0);
        }

        TreeMap<String, String> tracked = idConvertor(getHead()).getFile();

        for (String b : branchFiles.keySet()) {
            checkout(newid, b);
        }
        for (String t : tracked.keySet()) {
            if (!branchFiles.containsKey(t)) {
                rm(t);
            }

        }
        stagingArea = new TreeMap<>();
        File sa = new File(".gitlet/staging");
        String[] entries = sa.list();
        if (entries != null) {
            for (String e : entries) {
                File currentFile = new File(".gitlet/staging/" + e);
                currentFile.delete();
            }
        }
        File remove = new File(".gitlet/removed");
        String[] removal = remove.list();
        if (removal != null) {
            for (String e : removal) {
                File currentFile = new File(".gitlet/removed/" + e);
                currentFile.delete();
            }
        }
        branches.put(head, newid);
        File branch = new File(".gitlet/branches/" + head);
        writeContents(branch, id);
    }

    /** Merges given branch and current branch.
     * @param branch name of branch to merge current branch with
     */
    public void merge(String branch) {
        checkMergeError(branch);
        String splitPoint = findSplit(branch);
        checkSplitError(branch, splitPoint);
        boolean conflict = false;
        TreeMap<String, String> headCommit =
                idConvertor(getHead()).getFile();
        TreeMap<String, String> branchCommit =
                idConvertor(branches.get(branch)).getFile();
        TreeMap<String, String> splitCommit =
                idConvertor(splitPoint).getFile();
        if (splitCommit != null) {
            for (String s : splitCommit.keySet()) {
                if (branchCommit.containsKey(s) && headCommit.containsKey(s)) {
                    String bid = branchCommit.get(s);
                    String hid = headCommit.get(s);
                    String sid = splitCommit.get(s);
                    if (!bid.equals(sid) && hid.equals(sid)) {
                        checkout(branches.get(branch), s);
                        stagingArea.put(s, bid);
                        File sa = new File(".gitlet/staging/" + s);
                        writeContents(sa, bid);
                    } else if (!bid.equals(hid)
                            && !bid.equals(sid) && !hid.equals(sid)) {
                        mergeConflict(hid, bid);
                        conflict = true;
                    }
                } else if (!branchCommit.containsKey(s)
                        && headCommit.containsKey(s)) {
                    String hid = headCommit.get(s);
                    String sid = splitCommit.get(s);
                    if (hid.equals(sid)) {
                        rm(s);
                    }
                }
            }
        }

        middleMerge(branchCommit, headCommit, branch, splitCommit, conflict);

        if (!headCommit.keySet().isEmpty()) {
            for (String m : headCommit.keySet()) {
                if (splitCommit.containsKey(m)
                        && !branchCommit.containsKey(m)) {
                    String hid = headCommit.get(m);
                    String sid = splitCommit.get(m);
                    if (!hid.equals(sid)) {
                        mergeConflict(hid, null);
                        conflict = true;
                    }
                }
            }
        }
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        } else {
            commit("Merged " + head + " with " + branch + ".");
        }
    }

    /** line is too long so seperating merge.
     * @param branchCommit treemap for branch commit
     * @param headCommit treemap for head commit
     * @param branch branch name
     * @param splitCommit treemap for split commit
     * @param conflict conflicted or not*/
    public void middleMerge(TreeMap<String, String> branchCommit,
                            TreeMap<String, String> headCommit, String branch,
                            TreeMap<String, String> splitCommit,
                            boolean conflict) {
        if (!branchCommit.keySet().isEmpty()) {
            for (String n : branchCommit.keySet()) {
                if (!splitCommit.containsKey(n) && !headCommit.containsKey(n)) {
                    checkout(branches.get(branch), n);
                    String bid = branchCommit.get(n);
                    stagingArea.put(n, bid);
                    File sa = new File(".gitlet/staging/" + n);
                    writeContents(sa, bid);
                } else if (splitCommit.containsKey(n)
                        && !headCommit.containsKey(n)) {
                    String bid = branchCommit.get(n);
                    String sid = splitCommit.get(n);
                    if (!bid.equals(sid)) {
                        mergeConflict(null, bid);
                        conflict = true;
                    }
                } else if (!splitCommit.containsKey(n)) {
                    String bid = branchCommit.get(n);
                    String hid = headCommit.get(n);
                    if (!bid.equals(hid)) {
                        mergeConflict(hid, bid);
                        conflict = true;
                    }
                }
            }
        }
    }

    /** Checks if there is an error with split point.
     * @param branch name of branch in merge()
     * @param splitPoint commit that is most recently shared
     */
    public void checkSplitError(String branch, String splitPoint) {
        if (splitPoint != null
                && splitPoint.equals(branches.get(branch))) {
            System.out.println(
                    "Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint != null && splitPoint.equals(getHead())) {
            System.out.println("Current branch fast-forwarded.");
            branches.put(head, branches.get(branch));
            File b = new File(".gitlet/branches/" + head);
            writeContents(b, branches.get(branch));
            System.exit(0);
        }
    }

    /** Checks if there is an error with merging.
     * @param branch name of branch in merge()
     */
    public void checkMergeError(String branch) {
        File remove = new File(".gitlet/removed");
        if (!stagingArea.keySet().isEmpty()
                || !plainFilenamesIn(remove).isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        } else if (!branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (head.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (checkUntracked(branches.get(branch))) {
            System.exit(0);
        }
    }

    /** Finds the split point between given branch and current branch.
     * @param branch name of branch to find split with
     * @return return the split points.
     */
    public String findSplit(String branch) {
        String splitPoint = null;
        String branchHead = branches.get(branch);
        String bParent = branchHead;
        String hParent = getHead();
        TreeMap<String, String> bList = new TreeMap<>();
        while (bParent != null) {
            bList.put(bParent, "");
            bParent = idConvertor(bParent).getParents();
        }
        while (hParent != null) {
            if (bList.containsKey(hParent)) {
                splitPoint = hParent;
                break;
            }
            hParent = idConvertor(hParent).getParents();
        }
        return splitPoint;
    }

    /** Resolves merge conflicts found in merge().
     * @param hid sha1 id of head file blob
     * @param bid sha1 if of branch file blob
     */
    public void mergeConflict(String hid, String bid) {
        String header = "<<<<<<< HEAD\n";
        String middle = "=======\n";
        String tail = ">>>>>>>\n";
        String name;
        String newContent;
        if (hid == null) {
            File blobFile = new File(".gitlet/blobs/" + bid);
            String bName = readObject(blobFile, Blob.class).getName();
            byte[] bBytes = readObject(blobFile, Blob.class).getContents();
            File h = new File(bName);
            writeContents(h, bBytes);
            name = bName;
            String bContent = stringIn(name);
            newContent = header + middle + bContent + tail;
            writeContents(h, newContent);
        } else if (bid == null) {
            File blobFile = new File(".gitlet/blobs/" + hid);
            byte[] hBytes = readObject(blobFile, Blob.class).getContents();
            String hName = readObject(blobFile, Blob.class).getName();
            File h = new File(hName);
            writeContents(h, hBytes);
            name = hName;
            String hContent = stringIn(name);
            newContent = header + hContent + middle + tail;
            writeContents(h, newContent);
        } else {
            File blobFile = new File(".gitlet/blobs/" + bid);
            String bName = readObject(blobFile, Blob.class).getName();
            byte[] bBytes = readObject(blobFile, Blob.class).getContents();
            File help = new File("help");
            writeContents(help, bBytes);
            File blobFile2 = new File(".gitlet/blobs/" + hid);
            byte[] hBytes = readObject(blobFile2, Blob.class).getContents();
            File f = new File(bName);
            writeContents(f, hBytes);
            String bContent = stringIn("help");
            String hContent = stringIn(bName);
            newContent = header + hContent + middle + bContent + tail;
            writeContents(f, newContent);
            restrictedDelete("help");
        }
    }
}
