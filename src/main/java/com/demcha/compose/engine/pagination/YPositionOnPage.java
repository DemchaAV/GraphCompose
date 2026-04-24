package com.demcha.compose.engine.pagination;

public record YPositionOnPage(double yPosition, int startPage, int endPage){
    public YPositionOnPage(double yPosition, int startPage) {
        this(yPosition, startPage, startPage);
    }

}
