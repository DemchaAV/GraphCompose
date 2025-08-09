package com.demcha;

import com.demcha.structure.*;
import com.demcha.structure.Module;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class CV {

// Full Name


    // ─── Basic Info ─────────────────────────────────────────────────────────────
// blocks with icons anchor right

    // ─── Professional Summary  ────────────────────────────────────────────────────────────────

// string
    // ─── Technical Skills ───────────────────────────────────────────────────────
    // list  скили с листом пречисление скилов

    // ─── Education ──────────────────────────────────────────────────────────────
    // лист  в котором есть тайтл  и описание

    // ─── Projects ───────────────────────────────────────────────────────────────
// лист с тайтлом

    // ───Professional Experience ────────────────────────────────────────────────────────
    //лист работ с тайтлом и описанием

    // ─── Languages and Eligibility ──────────────────────────────────────────────
    private final List<String> spokenLanguages;
    private final boolean hasRightToWorkInUK = true;

    public static void main(String[] args) {
        Page page = new Page(1, "CV");

        Module name = new Module("name");
        name.addRow(
                new Row(
                        List.of(Element.of("name", new Text("Artem Demchyshyn")))
                )

        );

        page.setModule(name);


    }

}
