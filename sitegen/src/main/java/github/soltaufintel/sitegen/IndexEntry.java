package github.soltaufintel.sitegen;

public class IndexEntry {
    private final String title;
    private final String file;

    public IndexEntry(String title, String file) {
        super();
        this.title = title;
        this.file = file;
    }

    public String getTitle() {
        return title;
    }

    public String getFile() {
        return file;
    }
}
