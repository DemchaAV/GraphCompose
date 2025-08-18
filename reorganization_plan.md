# Project Reorganization Plan

## New Package Structure

1. `com.demcha` - Root package
   - Main application classes (Main, CV, PDFRender, PdfCreateExample, BasicInfo)

2. `com.demcha.legacy.core` - Core functionality
   - Component (interface)
   - Element (class that manages components)
   - Units

3. `com.demcha.legacy.layout` - Layout related classes
   - Container
   - MeasureCtx
   - ArrangeCtx (moved from interfaces)
   - Row
   - Module
   - Page

4. `com.demcha.ui` - UI elements
   - UiElement (moved from interfaces)
   - Anchor
   - TextDecoration

5. `com.demcha.ui.components` - Concrete UI components
   - Text (moved from structure.components)
   - Block (if source exists)

6. `com.demcha.ui.interfaces` - UI interfaces
   - Element (interface, moved from structure.interfaces)
   - All interfaces from structure.interfaces.ui

## Class Movements

| Current Location | New Location |
|------------------|--------------|
| com.demcha.components.core.Component | com.demcha.components.core.Component |
| com.demcha.legacy.scene.Element | com.demcha.legacy.core.Element |
| com.demcha.components.core.Units | com.demcha.legacy.core.Units |
| com.demcha.legacy.scene.Container | com.demcha.legacy.layout.Container |
| com.demcha.legacy.layout.MeasureCtx | com.demcha.legacy.layout.MeasureCtx |
| com.demcha.legacy.layout.ArrangeCtx | com.demcha.legacy.layout.ArrangeCtx |
| com.demcha.legacy.scene.Row | com.demcha.legacy.layout.Row |
| com.demcha.legacy.scene.Module | com.demcha.legacy.layout.Module |
| com.demcha.legacy.scene.Page | com.demcha.legacy.layout.Page |
| com.demcha.legacy.core.UiElement | com.demcha.ui.UiElement |
| com.demcha.components.layout.AnchorType | com.demcha.ui.Anchor |
| com.demcha.legacy.components.text.TextDecoration | com.demcha.ui.TextDecoration |
| com.demcha.legacy.scene.components.Text | com.demcha.ui.components.Text |
| com.demcha.legacy.core.Element | com.demcha.ui.interfaces.Element |
| com.demcha.legacy.scene.interfaces.ui.* | com.demcha.ui.interfaces.* |

This reorganization provides a clearer separation of concerns:
- Core functionality is separated from UI
- Layout-related classes are grouped together
- UI components and interfaces have their own packages
- The main application classes remain in the root package