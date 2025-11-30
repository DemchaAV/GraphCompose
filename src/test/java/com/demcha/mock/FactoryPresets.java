package com.demcha.mock;

import com.demcha.mock.data.CanvasData;
import com.demcha.mock.data.MarginData;
import com.demcha.mock.data.OffsetData;
import com.demcha.mock.data.SizeData;

public class FactoryPresets {

    // --- Margins (General) ---
    public static final MarginData MARGIN_ZERO = MarginData.all(0);
    public static final MarginData MARGIN_STANDARD_20 = MarginData.all(20);
    public static final MarginData MARGIN_ASYMMETRIC_10_20_30_40 = new MarginData(10, 20, 30, 40);

    // --- Margins (Test / Vertical Specific) ---
    // Top=10, Bot=10 (Симметричные вертикальные)
    public static final MarginData MARGIN_VERT_10 = new MarginData(10, 10, 0, 0);
    public static final MarginData MARGIN_VERT_1 = new MarginData(1, 1, 0, 0);
    // Top=5, Bot=5
    public static final MarginData MARGIN_VERT_5 = new MarginData(5, 5, 0, 0);
    // Canvas Margins: Top=10, Bot=20 (Несимметричные)
    public static final MarginData MARGIN_CANVAS_TOP10_BOT20 = new MarginData(10, 20, 0, 0);
    // Canvas Margins: Top=10, Bot=10
    public static final MarginData MARGIN_CANVAS_TOP10_BOT10 = new MarginData(10, 10, 0, 0);


    // --- Objects (Standard) ---
    public static final SizeData OBJ_SMALL_BOX_100_50 = new SizeData(100, 50);
    public static final SizeData OBJ_FULL_WIDTH_500_100 = new SizeData(500, 100);

    // --- Objects (Test / Low Height) ---
    public static final SizeData OBJ_W100_H20 = new SizeData(100, 20);
    public static final SizeData OBJ_W100_H60 = new SizeData(100, 60);
    public static final SizeData OBJ_W100_H40 = new SizeData(100, 40);


    // --- Canvases (Standard A4) ---
    public static final CanvasData CANVAS_A4_STANDARD = new CanvasData(595, 842, MARGIN_STANDARD_20);
    public static final CanvasData CANVAS_A4_NO_MARGINS = new CanvasData(595, 842, MARGIN_ZERO);

    // --- Canvases (Test / Small Area) ---
    // 100x100, Margins: Top 10, Bot 20
    public static final CanvasData CANVAS_TEST_100_100_M_T10_B20 = new CanvasData(100, 100, MARGIN_CANVAS_TOP10_BOT20);

    // 100x200, Margins: Top 10, Bot 20
    public static final CanvasData CANVAS_TEST_100_200_M_T10_B20 = new CanvasData(100, 200, MARGIN_CANVAS_TOP10_BOT20);

    // 100x100, Margins: Top 10, Bot 10
    public static final CanvasData CANVAS_TEST_100_100_M_10 = new CanvasData(100, 100, MARGIN_CANVAS_TOP10_BOT10);


    // --- Offsets ---
    public static final OffsetData OFFSET_DATA_ZERO = OffsetData.zero();
    public static final OffsetData OFFSET_DATA_ALL_2 = OffsetData.all(2);
    public static final OffsetData OFFSET_DATA_ALL_NEGATIVE_2 = OffsetData.all(-2);
    public static final OffsetData OFFSET_DATA_ALL_15 = OffsetData.all(15);
    public static final OffsetData OFFSET_DATA_NEGATIVE_ALL_40 = OffsetData.all(-40);

    // --- Objects (For Splitting Tests) ---
    // Height 150: Will require split on a 100px canvas
    public static final SizeData OBJ_TALL_100_150 = new SizeData(100, 150);
    public static final SizeData OBJ_TALL_5_12 = new SizeData(5, 12);
    public static final SizeData OBJ_TALL_5_6 = new SizeData(5, 6);
    // Height 250: Might require 3 pages on a 100px canvas
    public static final SizeData OBJ_VERY_TALL_100_250 = new SizeData(100, 250);


    // --- Canvases (For Splitting Tests) ---
    // 100x100, Margins: Top 10, Bot 10. Safe Area = 80px.
    // Useful for easy math: Total 100, Safe 80.
    public static final CanvasData CANVAS_SPLIT_100_M10 = new CanvasData(100, 100, MARGIN_VERT_10);
    public static final CanvasData CANVAS_SPLIT_5_M1 = new CanvasData(5, 5, MARGIN_VERT_1);
}