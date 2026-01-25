package com.demcha.compose.Templatese.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Heder {
    private String name;
    private String address;
    private String phoneNumber;
    private EmailYaml email;
    private LinkYml gitHub;
    private LinkYml linkedIn;
}
