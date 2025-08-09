# Project Reorganization Plan

## New Package Structure

1. `com.demcha` - Root package
   - Main application classes (Main, CV, PDFRender, PdfCreateExample, BasicInfo)

2. `com.demcha.core` - Core functionality
   - Component (interface)
   - Element (class that manages components)
   - Units

3. `com.demcha.layout` - Layout related classes
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
| com.demcha.structure.Component | com.demcha.core.Component |
| com.demcha.structure.Element | com.demcha.core.Element |
| com.demcha.structure.Units | com.demcha.core.Units |
| com.demcha.structure.Container | com.demcha.layout.Container |
| com.demcha.structure.MeasureCtx | com.demcha.layout.MeasureCtx |
| com.demcha.structure.interfaces.ArrangeCtx | com.demcha.layout.ArrangeCtx |
| com.demcha.structure.Row | com.demcha.layout.Row |
| com.demcha.structure.Module | com.demcha.layout.Module |
| com.demcha.structure.Page | com.demcha.layout.Page |
| com.demcha.structure.interfaces.UiElement | com.demcha.ui.UiElement |
| com.demcha.structure.Anchor | com.demcha.ui.Anchor |
| com.demcha.structure.TextDecoration | com.demcha.ui.TextDecoration |
| com.demcha.structure.components.Text | com.demcha.ui.components.Text |
| com.demcha.structure.interfaces.Element | com.demcha.ui.interfaces.Element |
| com.demcha.structure.interfaces.ui.* | com.demcha.ui.interfaces.* |

This reorganization provides a clearer separation of concerns:
- Core functionality is separated from UI
- Layout-related classes are grouped together
- UI components and interfaces have their own packages
- The main application classes remain in the root package