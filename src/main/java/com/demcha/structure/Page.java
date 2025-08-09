package com.demcha.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.HashMap;

@AllArgsConstructor
@Getter
@Setter
public class Page {
    private final HashMap<String, Module> modules = new HashMap<>();
    private int number;
    private String title;
    private PDFont font;
    private int fontSize;

    public Page(int number, String title) {
        this.number = number;
        this.title = title;
    }

    public void setModule(Module module) {
        Module checkModule = modules.get(module.getModuleName());
        if (checkModule != null) {

//            log.info("Module {} already exist", module.getModuleName());
            return;
        }

        setModuleOrReplace(module);
    }

    private void setModuleOrReplace(Module module) {
        this.modules.put(module.getModuleName(), module);
    }

    public Module getModule(String namaModule) {
        return this.modules.get(namaModule);
    }

}
