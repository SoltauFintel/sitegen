package github.soltaufintel.sitegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.github.template72.compiler.TemplateCompiler;
import com.github.template72.compiler.TemplateCompilerBuilder;
import com.github.template72.data.DataList;
import com.github.template72.data.DataMap;
import com.github.template72.loader.TemplateLoader;

public class SiteGenApp {
    private String dir;
    private TemplateCompiler compiler;

    public static void main(String[] args) {
        new SiteGenApp().start(args[0]);
        System.out.println("\nfinished");
    }
    public void start(String dir) {
        this.dir = dir;
        System.out.println("work dir: " + new File(dir).getAbsolutePath() + "\n");
        File outDir = new File(dir, "../out");
		outDir.mkdirs();
        for (File file0 : outDir.listFiles()) {
        	if (file0.getName().endsWith(".html")) {
        		file0.delete();
        	}
        }
        compiler = new TemplateCompilerBuilder().withLoader(new TemplateLoader() {
            @Override
            public String loadTemplate(String filename) {
                try {
                    return new String(Files.readAllBytes(Paths.get(dir + "/templates/" + filename + ".html")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setParentLoader(TemplateLoader parentLoader) {
            }
        }).build();
		compiler.setFirstLoader(null);

        DataMap model = new DataMap();
        readTOC(model.list("toc"));
        readVars(model);
        for (File file : new File(dir, "templates").listFiles()) {
            String name = shortFilename(file.getName());
            if (!name.startsWith("master") && !name.startsWith("menu")) {
                render(file.getName(), model);
            }
        }
    }

    private void render(String filename, DataMap model) {
    	String html = readFile(filename);
    	if (filename.endsWith(".md")) {
    		html = "{{master: master_doc}}\n\n" + markdown2html(html);
    	}
        model.put("title", extractTitle(shortFilename(filename), html));
		try {
			String out = compiler.compile(html).render(model);
			out = out.replace("LBRACELBRACE", "{{");
			write(new File(dir, "../out/" + shortFilename(filename) + ".html"), out);
			System.out.println("- " + filename);
		} catch (Exception e) {
			throw new RuntimeException("Error rendering file " + filename + "\n" + e.getMessage(), e);
		}
    }

    private String markdown2html(String markdown) {
    	Parser parser = Parser.builder().build();
    	Node document = parser.parse(markdown);
    	HtmlRenderer renderer = HtmlRenderer.builder().build();
    	return renderer.render(document);
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
        String c = readFile("../toc");
        boolean first = true;
        for (String line : c.replace("\r\n", "\n").split("\n")) {
            int o = line.indexOf("\"");
            int oo = line.indexOf("\"", o + 1);
            if (o >= 0 && oo > o) {
                String title = line.substring(o + 1, oo);
                String file = line.substring(oo + 1).trim();
                DataMap map = list.add();
                map.put("title", title);
                map.put("link", file + ".html");
                map.put("isSection", file.isEmpty());
                map.put("isFirst", first);
                first = false;
            }
        }
    }

    private void readVars(DataMap model) {
        String c = readFile("../vars");
        for (String line : c.replace("\r\n", "\n").split("\n")) {
			int o = line.indexOf("=");
			if (o >= 0) {
				model.put(line.substring(0, o).trim(), line.substring(o + 1).trim());
			}
        }
    }
    
    private String readFile(String filename) {
    	File file = new File(dir + "/templates/" + filename);
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + file.getAbsolutePath(), e);
        }
    }

	private void write(File file, String text) {
        try (FileWriter w = new FileWriter(file)) {
            w.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
