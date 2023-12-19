package github.soltaufintel.sitegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.template72.compiler.CompiledTemplates;
import com.github.template72.compiler.TemplateCompiler;
import com.github.template72.compiler.TemplateCompilerBuilder;
import com.github.template72.data.DataList;
import com.github.template72.data.DataMap;
import com.github.template72.loader.TemplateLoader;

public class SiteGenApp {
    private String dir;
    private CompiledTemplates templates;

    public static void main(String[] args) {
        new SiteGenApp().start(args[0]);
    }

    public void start(String dir) {
        this.dir = dir;
        System.out.println("work dir: " + new File(dir).getAbsolutePath());
        TemplateCompiler compiler = new TemplateCompilerBuilder().withLoader(new TemplateLoader() {
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
        templates = new CompiledTemplates(compiler, null, true);

        DataMap model = new DataMap();
        readTOC(model.list("toc"));
        readVars(model);
        for (File file : new File(dir, "templates").listFiles()) {
            String name = file.getName().replace(".html", "");
            if (name.startsWith("master") || name.startsWith("menu")) {
            } else {
                render(name, model);
            }
        }
    }

    private void render(String filename, DataMap model) {
        model.put("title", readTitle(filename));
        String out = templates.render(filename, model);
        write(new File(dir, "../out/" + filename + ".html"), out);
        System.out.println(filename);
    }

    private void write(File file, String text) {
        file.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(file)) {
            w.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readTitle(String filename) {
        String title = filename;
        String c;
        try {
            c = new String(Files.readAllBytes(Paths.get(dir + "/templates/" + filename + ".html")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int o = c.indexOf("<h1>");
        if (o >= 0) {
            o += "<h1>".length();
            int oo = c.indexOf("</h1>", o);
            title = c.substring(o, oo).trim();
        }
        return title;
    }

    private void readTOC(DataList list) {
        String c;
        try {
            c = new String(Files.readAllBytes(Paths.get(dir + "/toc")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            }
        }
    }

    private void readVars(DataMap model) {
        Path p = Paths.get(dir + "/vars");
        if (!p.toFile().isFile()) {
            return;
        }
        String c;
        try {
            c = new String(Files.readAllBytes(p));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String line : c.replace("\r\n", "\n").split("\n")) {
            model.put(line.substring(0, line.indexOf("=")), line.substring(line.indexOf("=") + 1));
        }
    }
}
