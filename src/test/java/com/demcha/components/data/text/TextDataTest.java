package com.demcha.components.data.text;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.demcha.core.Element;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;
import java.util.Optional;

public class TextDataTest {

    @Mock
    private Element element;  // Mock the Element class

    @Mock
    private TextStyle textStyle;  // Mock the TextStyle class

    @Mock
    private PDFont pdfont;  // Mock the PDFont used in TextStyle

    private TextData textData;  // The actual object under test

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);  // Initialize mocks
        when(textStyle.font()).thenReturn(pdfont);  // Return mocked PDFont when font() is called
        when(pdfont.getStringWidth(anyString())).thenReturn(100f);  // Simulate text width calculation
        when(pdfont.getFontDescriptor().getCapHeight()).thenReturn(10f);  // Simulate the cap height value

        // Initialize TextData with mock objects
        textData = new TextData("Hello World", textStyle);
    }

    @Test
    void testAutoMeasureText() throws IOException {
        // Simulate the `Element` getting the `TextData` object
        when(element.get(TextData.class)).thenReturn(Optional.of(textData));

        // Call the autoMeasureText method
        TextData.autoMeasureText(element);

    // Checking that the width is 100 and height is 10
    }
}

