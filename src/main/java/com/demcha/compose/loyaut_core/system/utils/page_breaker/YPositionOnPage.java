package com.demcha.compose.loyaut_core.system.utils.page_breaker;

public record YPositionOnPage(double yPosition, int startPage, int endPage){
    public YPositionOnPage(double yPosition, int startPage) {
        this(yPosition, startPage, startPage);
    }

}
