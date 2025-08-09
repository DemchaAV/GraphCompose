package com.demcha.structure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class Module {
    private final String moduleName;
    private final List<Row> rows = new ArrayList<>();

    public void addRow(Row row) {
        rows.add(row);
    }

}
