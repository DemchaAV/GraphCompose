package com.demcha.system.utils.page_brecker;

public record YPositionOnPage(double yPosition, int startPage, int endPage){
    public YPositionOnPage(double yPosition, int startPage) {
        this(yPosition, startPage, startPage);
    }

}
