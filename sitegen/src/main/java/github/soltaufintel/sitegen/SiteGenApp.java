package github.soltaufintel.sitegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.github.template72.compiler.TemplateCompiler;
import com.github.template72.compiler.TemplateCompilerBuilder;
import com.github.template72.data.DataList;
import com.github.template72.data.DataMap;
import com.github.template72.loader.TemplateLoader;

public class SiteGenApp {
    private final String dir;
    private final String outDir;
    private TemplateCompiler compiler;

    public static void main(String[] args) {
        new SiteGenApp(args[0], args[1]).start();
        System.out.println("\nfinished");
    }

    public SiteGenApp(String dir, String outDir) {
        this.dir = dir;
        this.outDir = outDir;
    }

    public void start() {
        System.out.println("input  dir: " + dir);
        System.out.println("output dir: " + outDir);
        getTemplateCompiler();
        DataMap model = new DataMap();
        readTOC(model.list("toc"));
        readVars(model);
        for (File file : new File(dir).listFiles()) {
            String name = shortFilename(file.getName());
            if (file.isFile() && file.getName().endsWith(".md") && !name.startsWith("master") && !name.startsWith("menu")) {
                render(file.getName(), model);
            }
        }
        new File(outDir, "img").mkdirs();
        for (File file : new File(dir, "img").listFiles()) {
            try {
                Files.copy(file.toPath(), new File(outDir, "img/" + file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("ERROR " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }
        
        String index = readFile("index.html");
        if (index != null) {
	        String out = compiler.compile(index).render(model);
	        write(new File(outDir, "index.html"), out);
        }
        
        File file = new File(dir, "site.css");
        if (file.isFile()) {
	        try {
	            Files.copy(file.toPath(), new File(outDir, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
	        } catch (IOException e) {
	            System.err.println("ERROR " + file.getAbsolutePath() + ": " + e.getMessage());
	        }
        }
    }

    private void getTemplateCompiler() {
        compiler = new TemplateCompilerBuilder().withLoader(new TemplateLoader() {
            @Override
            public String loadTemplate(String filename) {
                try {
                    return new String(Files.readAllBytes(Paths.get(dir + "/" + filename + ".html")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setParentLoader(TemplateLoader parentLoader) {
            }
        }).build();
		compiler.setFirstLoader(null);
    }

    private void render(String filename, DataMap model) {
    	String html = readFile(filename);
    	String rawHtml = html;
    	if (filename.endsWith(".md")) {
    		html = "{{master: master}}\n\n" + markdown2html(html);
    	}
        model.put("title", extractTitle(shortFilename(filename), html));
        model.put("menu", "menu--");
		try {
			String out = compiler.compile(html).render(model);
			out = out.replace("LBRACELBRACE", "{{");
			write(new File(outDir, shortFilename(filename) + ".html"), out);
			System.out.println("- " + filename);
	        checkLinks(rawHtml, filename);
		} catch (Exception e) {
			throw new RuntimeException("Error rendering file " + filename + "\n" + e.getMessage(), e);
		}
    }

    private void checkLinks(String html, String filename) {
        int start = 0;
        int o = html.indexOf("[", start);
        while (o > 0) {
            if (o > 0 && html.charAt(o - 1) != '!') {
                int oo = html.indexOf("](", o + 1);
                if (oo > o) {
                    int ooo = html.indexOf(")", oo + 1);
                    if (ooo > 0) {
                        String text = html.substring(o + 1, oo);
                        String link = html.substring(oo + 2, ooo);
                        if (!text.contains("\n") && !link.contains("\n")) {
                            if (!link.startsWith("http") && (link.contains("..") || link.contains("/") || !link.contains(".html"))) {
                                System.out.println("\t** " + text + " => " + link);
                            }
                            start = ooo;
                        }
                    }
                }
            }
            start = o + 1;
            o = html.indexOf("[", start);
        }
    }

    private String markdown2html(String markdown) {
    	markdown = removeComments(markdown);
    	List<Extension> extensions = Arrays.asList(TablesExtension.create());
    	Parser parser = Parser.builder().extensions(extensions).build();
    	Node document = parser.parse(markdown);
    	HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
    	return renderer.render(document);
	}
    
	private String removeComments(String markdown) {
		String ret = "";
        for (String line : markdown.replace("\r\n", "\n").split("\n")) {
            ret += line + "\n";
		}
		return ret;
	}
	private String shortFilename(String filename) {
		int o = filename.lastIndexOf(".");
		if (o >= 0) {
			filename = filename.substring(0, o);
		}
		return filename;
	}

    private String extractTitle(String filename, String html) {
        String title = filename;
        int o = html.indexOf("<h1>");
        if (o >= 0) {
            o += "<h1>".length();
            int oo = html.indexOf("</h1>", o);
            title = html.substring(o, oo).trim();
        }
        return title;
    }

    private void readTOC(DataList list) {
        String c = readFile("toc");
        boolean first = true;
        for (String line : c.replace("\r\n", "\n").split("\n")) {
        	if (line.trim().startsWith("//")) {
        		continue;
        	}
            int o = line.indexOf("\"");
            int oo = line.indexOf("\"", o + 1);
            if (o >= 0 && oo > o) {
                String title = line.substring(o + 1, oo);
                String file = line.substring(oo + 1).trim();
                DataMap map = list.add();
                map.put("title", title);
                map.put("link", file.startsWith("http") ? file : file + ".html");
                map.put("isSection", file.isEmpty());
                map.put("isFirst", first);
                map.put("isEmpty", title.isBlank());
                first = false;
            }
        }
    }

    private void readVars(DataMap model) {
        String c = readFile("vars");
        for (String line : c.replace("\r\n", "\n").split("\n")) {
			int o = line.indexOf("=");
			if (o >= 0) {
				model.put(line.substring(0, o).trim(), line.substring(o + 1).trim());
			}
        }
    }
    
    private String readFile(String filename) {
    	File file = new File(dir + "/" + filename);
    	if (!file.isFile()) {
    	    throw new RuntimeException("File not found: " + file.getAbsolutePath());
    	}
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + file.getAbsolutePath(), e);
        }
    }

	private void write(File file, String text) {
	    file.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(file)) {
            w.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
