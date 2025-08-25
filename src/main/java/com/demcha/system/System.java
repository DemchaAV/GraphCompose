package com.demcha.system;

import com.demcha.core.PdfDocument;

import java.sql.DriverManager;

public interface System {
      void process(PdfDocument pdfDocument);
}
