package com.demcha;

import com.demcha.structure.Block;
import com.demcha.structure.Element;
import com.demcha.structure.Module;
import com.demcha.structure.Row;
import com.demcha.structure.components.Text;

import java.util.Arrays;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Module basicInfo = new Module("Basic Info");
        Row row = new Row();
        Element phone = new Text("phoneNumber","+44659862988");
        Block<String> email = new Block<>("email","demchaav@gmail.com");
        Block<String> linkedIn = new Block<>("linkedIn","www.linkedin.com/in/artemdemchyshyn");
        row.setBlocks(Arrays.asList(phone,email,linkedIn));

    }

}