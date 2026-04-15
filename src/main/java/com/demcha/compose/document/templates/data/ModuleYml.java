package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable named bullet/list module used by CV templates.
 */
@Data
@NoArgsConstructor
public class ModuleYml {
    private String name;
    private List<String> modulePoints = new ArrayList<>();
}
