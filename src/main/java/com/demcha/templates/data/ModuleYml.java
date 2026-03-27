package com.demcha.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ModuleYml {
    private String name;
    private List<String> modulePoints = new ArrayList<>();
}
