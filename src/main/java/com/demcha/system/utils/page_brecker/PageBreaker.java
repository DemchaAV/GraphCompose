package com.demcha.system.utils.page_brecker;

import com.demcha.components.LineTextData;
import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.text.BlockTextData;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.renderable.BlockText;
import com.demcha.components.style.Margin;
import com.demcha.core.EntityManager;
import com.demcha.exeptions.BigSizeElementException;
import com.demcha.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.system.intarfaces.RenderingSystemECS;
import com.demcha.system.intarfaces.SystemECS;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility class responsible for the logic of breaking content across pages.
 * <p>
 * The main responsibilities of this class are:
 * <ol>
 *     <li>Sorting entities ({@link Entity}) by their vertical position.</li>
 *     <li>Calculating and assigning a page number and in-page coordinates for each entity.</li>
 *     <li>Handling "breakable" entities, such as {@link BlockText}, whose content can
 *     flow onto the next page.</li>
 * </ol>
 * The class operates in a coordinate system where the Y-axis points downwards. It does not create new pages
 * but rather calculates and assigns {@link Placement} components to entities.
 */
@Slf4j
public class PageBreaker {
    private PageBreaker() {
    }


    // === Sorting =============================================================

    /**
     * Sorts the given map of entities by their Y-position in **descending** order (top to bottom).
     * <p>
     * Sorting is based on the {@code RenderingPosition} component of each {@link Entity}.
     * The result is a {@link Set} of {@link Map.Entry} objects, with order preserved
     * by collecting into a {@link LinkedHashSet}.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects to be sorted. Must not be null.
     * @return A {@code Set} of map entries, sorted by Y-position in descending order.
     * @throws NullPointerException  if the input map is null.
     * @throws IllegalStateException if any entity is missing a {@code RenderingPosition}.
     */
    public static Set<Map.Entry<UUID, Entity>> sortByYPosition(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .peek(entry -> {
                    Optional<RenderingPosition> pos = RenderingPosition.from(entry.getValue());
                    log.debug("{} -> {}", entry.getValue(), pos.orElse(null));
                })

                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Sorts the given map of entities by their Y-position in **descending** order
     * and returns the result as a new {@link LinkedHashMap}.
     * <p>
     * This method provides a convenient map-based view where the iteration order matches the sorted order.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects to be sorted. Must not be null.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @throws NullPointerException  if the input map is null.
     * @throws IllegalStateException if any entity is missing a {@code RenderingPosition}.
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Loads entities by their UUID from an {@link EntityManager} and sorts them by Y-position.
     *
     * @param entityManager The entity manager to retrieve {@link Entity} objects from.
     * @param entityUuids   A set of UUIDs for the entities to be sorted.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @see #sortByYPositionToMap(Map)
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(EntityManager entityManager, Set<UUID> entityUuids) {
        Objects.requireNonNull(entityUuids, "entities must not be null");


        return sortByYPositionToMap(entityManager.getSetEntitiesFromUuids(entityUuids));
    }

    /**
     * Loads entities by their UUID from an {@link EntityManager} and sorts them by Y-position.
     *
     * @param entityManager The entity manager to retrieve {@link Entity} objects from.
     * @param entityUuids   A list of UUIDs for the entities to be sorted.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @see #sortByYPositionToMap(Map)
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(EntityManager entityManager, List<UUID> entityUuids) {
        Objects.requireNonNull(entityUuids, "entities must not be null");


        return sortByYPositionToMap(entityManager.getSetEntitiesFromUuids(new HashSet<>(entityUuids)));
    }

    /**
     * Sorts the given set of entities by their Y-position and returns the result as a {@link LinkedHashMap}.
     *
     * @param entities A set of entities to be sorted.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @see #sortByYPositionToMap(Map)
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(Set<Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.stream()
                .sorted(Comparator.comparingDouble(PageBreaker::yOf).reversed())
                .collect(Collectors.toMap(
                        Entity::getUuid,           // key mapper → UUID
                        e -> e,                  // value mapper → Entity
                        (a, b) -> a,             // merge function (shouldn't happen)
                        LinkedHashMap::new       // preserve order
                ));
    }

    /**
     * A helper method to extract the Y-coordinate from an entity's {@link RenderingPosition} component.
     *
     * @param e The entity from which to extract the coordinate.
     * @return The Y-coordinate value.
     * @throws IllegalStateException if the entity is missing a {@code RenderingPosition}.
     */
    private static double yOf(Entity e) {
        return RenderingPosition.from(e)
                .map(RenderingPosition::y)
                .orElseThrow(() -> new IllegalStateException(
                        "Entity " + e + " has no RenderingPosition"));
    }

    // === Paging math =========================================================

    /**
     * The main method for performing the page-breaking process.
     * It sorts entities, iterates through them, and calculates their final position ({@link Placement}),
     * including the page number and in-page coordinates. Special attention is given to "breakable" elements,
     * such as {@link BlockText}, which require additional processing.
     *
     * @param entities The map of entities to process.
     * @param canvas   The {@link Canvas} object, providing information about page dimensions and margins.
     * @throws IllegalStateException if an entity is missing a required component (e.g., {@code RenderingPosition}).
     */
    public static void process(@NonNull Map<UUID, Entity> entities, Canvas canvas) {
        Offset yOffset = new Offset();

        entities.entrySet().stream()
                // Sorting our Entitles in to  by Rendering position to  make Shifting lees accessible
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) ->
                                RenderingPosition.from(e.getValue())
                                        .orElseThrow(
                                                () -> new IllegalStateException("Entity " + e + " has no RenderingPosition"))
                                        .y())
                        .reversed())
                //Starting process of breaking Pages
                .forEach(e -> {
                    Entity entity = e.getValue();
                    log.info("Work with Element {} ", entity);

                    if (!Breakable.class.isAssignableFrom(entity.getRender().getClass())) {
                        log.info("{} -> {}", entity, Breakable.class);
                        definePlacement(canvas, entity, yOffset, false);

                    } else {
                        definePlacement(canvas, entity, yOffset, true);
                        if (entity.hasAssignable(BlockText.class)) {
                            try {
                                blockTextDataPositions(entity, canvas, yOffset);

                                System.out.println("Map Shifts");
                                System.out.println(yOffset);
                                if (log.isDebugEnabled()) {
                                    BlockTextData blockTextData = entity.getComponent(BlockTextData.class).orElseThrow();
                                    blockTextData.lines().forEach((t) -> log.debug(t.toString()));
                                }
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }

                    }
                });
    }

    /**
     * Determines and adds a {@link Placement} component to an entity.
     * This method updates the entity's vertical position based on the total {@code yOffset},
     * calculates its position on the page, and stores the result in a new {@code Placement} component.
     *
     * @param canvas      The canvas configuration.
     * @param entity      The entity to process.
     * @param yOffset     The total Y-axis offset accumulated from previous elements.
     * @param isBreakable A flag indicating whether the element can be broken across pages.
     */
    private static void definePlacement(Canvas canvas, Entity entity, Offset yOffset, boolean isBreakable) {
        entity.updateVerticalComputedPosition(yOffset);
        var computedPosition = entity.getComponent(ComputedPosition.class).orElseThrow();
        ContentSize contentSize = entity.getComponent(ContentSize.class)
                .orElseThrow(() -> new IllegalStateException("Entity " + entity + " has no ContentSize"));
        log.debug("Defining position for {}", entity);


//        YPositionOnPage position = definePositionOnPage(computedPosition.y(), canvas.boundingTopLine(), contentSize.height(), 0, canvas, yOffset, isBreakable);
        YPositionOnPage position = definePositionOnPage(computedPosition.y(), entity, 0, canvas, yOffset, isBreakable);

        Placement placement = setYInPlacement(entity, position);
        entity.addComponent(placement);
    }

    /**
     * Initiates the page-breaking process for all entities managed by the {@link EntityManager}.
     * This method automatically finds the active rendering system ({@link RenderingSystemECS}) to retrieve
     * canvas information ({@link Canvas}).
     *
     * @param entityManager The entity manager containing all entities and systems.
     */
    public static void process(@NonNull EntityManager entityManager) {
        log.info("Breaking pages");
        RenderingSystemECS renderingSystemECS = null;
        log.info("Definition a RenderingSystemECS");
        for (SystemECS system : entityManager.getSystems()) {
            if (RenderingSystemECS.class.isAssignableFrom(system.getClass())) {
                renderingSystemECS = (RenderingSystemECS) system;
                break;
            }

        }
        if (renderingSystemECS == null) {
            log.error("No RenderingSystemECS found");
            throw new IllegalStateException("No RenderingSystemECS found");
        }
        process(entityManager.getEntities(), renderingSystemECS.canvas());
    }

    private static Placement setYInPlacement(Entity entity, double yPosition, int startPage, int endPage) {
        ComputedPosition computedPosition = entity.getComponent(ComputedPosition.class).orElseThrow();
        var size = entity.getComponent(ContentSize.class).orElseThrow();
        return new Placement(computedPosition.x(), yPosition, size.width(), size.height(), startPage, endPage);
    }

    private static Placement setYInPlacement(Entity entity, YPositionOnPage position) {
        return setYInPlacement(entity, position.yPosition(), position.startPage(), position.startPage());
    }

    /**
     * Determines an object's position on a page based on its current Y-coordinate and dimensions.
     * This method calculates the page number and the new Y-coordinate within that page.
     * It also handles situations where an object crosses the bottom boundary of a page,
     * applying an offset to move it.
     *
     * @param currentPositionY   The object's current absolute Y-coordinate.
     * @param objectHeight       The height of the object.
     * @param objectMarginTop    The object's top margin.
     * @param objectMarginBottom The object's bottom margin.
     * @param currentPageNumber  The current page number (used as a base for calculating the new page).
     * @param canvas             The canvas object, providing page dimensions.
     * @param yOffset            An object for tracking and applying vertical offsets.
     * @param isBreakable        A flag indicating whether the object can be broken.
     * @return A {@link YPositionOnPage} containing the new Y-position and page number.
     */
    public static YPositionOnPage definePositionOnPage(double currentPositionY,
                                                       double objectHeight, double objectMarginTop, double objectMarginBottom,
                                                       int currentPageNumber, Canvas canvas,
                                                       Offset yOffset, boolean isBreakable) {
        return definePositionOnPage(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvas.boundingTopLine(), canvas.margin().top(), canvas.margin().bottom(), yOffset, isBreakable);
    }

    /**
     * Internal delegate method for {@link #definePositionOnPage}.
     */
    private static YPositionOnPage definePositionOnPage(double currentPositionY,
                                                        Entity entity,
                                                        int currentPageNumber, double canvasHigh, double canvasMarginTop, double canvasMarginBottom,  //Canvas Settings
                                                        Offset yOffset,
                                                        boolean isBreakable) {
        var size = entity.getComponent(ContentSize.class).orElseThrow();
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());

        double objectHeight = size.height();
        double objectMarginTop = margin.top();
        double objectMarginBottom = margin.bottom();
        return definePositionOnPage(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvasHigh, canvasMarginTop, canvasMarginBottom, yOffset, isBreakable);

    }

    /**
     * Internal delegate method for {@link #definePositionOnPage}.
     */
    private static YPositionOnPage definePositionOnPage(double currentPositionY,
                                                        Entity entity,
                                                        int currentPageNumber, Canvas canvas,  //Canvas Settings
                                                        Offset yOffset,
                                                        boolean isBreakable) {
        var size = entity.getComponent(ContentSize.class).orElseThrow();
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());

        double objectHeight = size.height();
        double objectMarginTop = margin.top();
        double objectMarginBottom = margin.bottom();
        return definePositionOnPage(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvas, yOffset, isBreakable);

    }

    /**
     * Core logic for calculating the position on a page.
     */
    private static YPositionOnPage definePositionOnPage(double currentPositionY,
                                                        double objectHeight, double objectMarginTop, double objectMarginBottom, //object Settings
                                                        int currentPageNumber, double canvasTopBondingLine, double canvasMarginTop, double canvasMarginBottom,  //Canvas Settings
                                                        Offset yOffset, boolean isBreakable) {

        if (!(canvasTopBondingLine > 0.0) || Double.isNaN(canvasTopBondingLine) || Double.isInfinite(canvasTopBondingLine)) {
            throw new IllegalArgumentException("canvasTopBondingLine must be a finite positive number; was " + canvasTopBondingLine);
        }

        log.debug("""
                        --- Defining Position On Page ---
                        Object Settings:
                          - currentPositionY:   {}
                          - objectHeight:       {}
                          - objectMargin (T/B): {}/{}
                        Canvas Settings:
                          - canvasTopBondingLine: {}
                          - canvasMargin (T/B):   {}/{}
                        State:
                          - currentPageNumber:  {}
                          - isBreakable:        {}
                          - yOffset (before):     {}
                        """,
                currentPositionY, objectHeight, objectMarginTop, objectMarginBottom,
                canvasTopBondingLine, canvasMarginTop, canvasMarginBottom,
                currentPageNumber, isBreakable, yOffset);

        int pageOffset = definePage(currentPositionY, canvasTopBondingLine); // may be negative, zero, or positive
        int finalPage = Math.addExact(currentPageNumber, pageOffset); // detect overflow
        if (finalPage < 0) {
            log.error("Invalid page number {}", finalPage);
            throw new IllegalArgumentException("Page number is less than zero after pageOffset: " + finalPage);
        }


        // Normalize y into [0, pageHeight)
        double yInPage = positiveModulo(currentPositionY, canvasTopBondingLine);
        // we
        if (yInPage < canvasMarginBottom) {
            log.debug("currentPositionY: {}" +
                      "                                                         objectHeight {} double objectMarginTop, double objectMarginBottom, //object Settings\n" +
                      "                                                        int currentPageNumber, double canvasTopBondingLine, double canvasMarginTop, double canvasMarginBottom,  //Canvas Settings\n" +
                      "                                                        Offset yOffset, boolean isBreakable ");
            shift(yInPage, objectHeight, objectMarginTop, objectMarginBottom, canvasMarginBottom, canvasMarginBottom + canvasTopBondingLine);
            double currentOffset = (yInPage + objectHeight) * -1;
            yInPage = canvasTopBondingLine - objectHeight;
            finalPage++;
            yOffset.incrementY(currentOffset);
        }


        YPositionOnPage result = new YPositionOnPage(yInPage, finalPage);


        log.debug("Defined position on page: {}", result);
        return result;

    }


    /**
     * Calculates the page offset relative to a starting page.
     * <p>
     * Given an absolute Y-coordinate and a fixed {@code pageHeight} ($H$), this
     * determines how many pages away the coordinate is from the page that starts at Y=0.
     * The offset is defined as follows (with the Y-axis pointing down):
     * <ul>
     * <li>$Y \in [0, H)$ $\rightarrow$ 0 (same page)</li>
     * <li>$Y \in [H, 2H)$ $\rightarrow$ -1 (one page down)</li>
     * <li>$Y \in [-H, 0)$ $\rightarrow$ 1 (one page up)</li>
     * </ul>
     * The calculation formula is: $- \lfloor \frac{Y}{H} \rfloor$.
     *
     * @param currentPositionY The entity's absolute Y-coordinate.
     * @param pageHeight       The height of a single page (must be a finite positive number).
     * @return The integer page offset.
     * @throws IllegalArgumentException if {@code pageHeight} is not a finite positive number.
     */
    public static int definePage(double currentPositionY, double pageHeight) {
        if (!(pageHeight > 0.0) || Double.isNaN(pageHeight) || Double.isInfinite(pageHeight)) {
            throw new IllegalArgumentException("pageHeight must be a finite positive number; was " + pageHeight);
        }
        log.debug("definePage: pageHeight={}, y={}", pageHeight, currentPositionY);

        // floor(y / H) is ... -2, -1, 0, 1, 2 ...
        // We want offset = -floor(y/H)
        int definedPage = (int) Math.floor(currentPositionY / pageHeight) * -1;

        log.debug("definePage -> offset={}", definedPage);
        return definedPage;
    }

    /**
     * Calculates a true positive modulo for floating-point numbers.
     * The result is always in the range [0, m).
     *
     * @param a The dividend (current position).
     * @param m The divisor (page height).
     * @return The remainder in the range [0, m).
     */
    private static double positiveModulo(double a, double m) {
        double r = a % m;
        return (r < 0) ? r + m : r;
    }

    private static BlockText.ValidatedTextData getValidatedTextData(Entity e) {
        var textValueOpt = e.getComponent(BlockTextData.class);
        var styleOpt = e.getComponent(TextStyle.class);

        BlockTextData textValue;
        TextStyle style;
        if (textValueOpt.isEmpty()) {
            log.info("TextComponent has no BlockTextData; skipping: {}", e);
            textValue = textValueOpt.orElse(BlockTextData.empty());
        } else {
            textValue = textValueOpt.get();
        }
        if (styleOpt.isEmpty()) {
            log.info("TextComponent has no TextStyle; skipping: {}", e);
            style = styleOpt.orElse(TextStyle.defaultStyle());
        } else {
            style = styleOpt.get();
        }

        return new BlockText.ValidatedTextData(style, textValue);
    }

    /**
     * Calculates and assigns the position for each line of text within a {@link BlockText}.
     * This method iterates through the lines, determines their position on the page, handles
     * page breaks, and updates the entity's {@link BlockTextData} component with the new,
     * positioned data.
     *
     * @param e       The entity containing the {@link BlockText}.
     * @param canvas  The canvas object for retrieving page dimension information.
     * @param yOffset The total Y-axis offset, which will be updated based on
     *                offsets caused by page breaks within the text block.
     * @throws IOException           if an error occurs while working with fonts.
     * @throws IllegalStateException if the entity is missing required components.
     */
    private static void blockTextDataPositions(Entity e, Canvas canvas, @NonNull Offset yOffset) throws IOException {

        Offset entityYOffset = new Offset();

        if (!e.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", e);
            return;
        }

        var positionOpt = RenderingPosition.from(e);
        if (positionOpt.isEmpty()) {
            log.warn("TextComponent has no RenderingPosition; skipping: {}", e);
            return;
        }


        var placement = e.getComponent(Placement.class).orElseThrow();
        InnerBoxSize innerBoxSize = InnerBoxSize.from(e).orElseThrow();

        BlockText.ValidatedTextData validateText = getValidatedTextData(e);


        var style = validateText.style();
        float fontSize = (float) style.size();
        PDFont font = style.font();
        var textHeight = (float) style.getLineHeight();

        float scale = fontSize / 1000f;
        PDFontDescriptor fd = font.getFontDescriptor();

        float descentPx = Math.abs((fd != null ? fd.getDescent() : font.getBoundingBox().getLowerLeftY()) * scale);

        var blockTextData = validateText.textValue().lines();

        double spacing = e.getComponent(Align.class).orElseGet(() -> {
            log.warn("TextComponent has no Align; using default: {}", e);
            return Align.defaultAlign(2);
        }).spacing() * -1;

        log.debug("Rendering textBlock '{}' at placement ({}, {}) fontSize={}  textStyle= ", blockTextData, placement.x(), placement.y());
        log.debug("fontSize={}  textStyle= {}", fontSize, style);

        int currentPage = placement.startPage();


        // стартовая позиция (левый верх «абзаца»)
        float startX = (float) placement.x() - descentPx;
        float startY = (float) (placement.y() + innerBoxSize.height()) - textHeight + descentPx; // if spacing will be negative
        boolean isStarted = false;
        BlockTextData newBlockTextData;
        List<LineTextData> assignPositionTextData = new ArrayList<>();

        for (LineTextData ltd : blockTextData) {
            if (!isStarted) {
                log.debug("Started print a block text, Position Y is {}", startY);
                isStarted = true;
                YPositionOnPage yPositionOnPage = PageBreaker
                        .definePositionOnPage(startY, textHeight, 0.0, 0.0, currentPage, canvas, yOffset, false);
                startY = (float) yPositionOnPage.yPosition();
                currentPage = yPositionOnPage.startPage();
            }

            float currenPosition = (float) ltd.x() + startX;


            LineTextData nltd = new LineTextData(ltd, currenPosition, startY, currentPage);
            assignPositionTextData.add(nltd);
            startY -= (float) (textHeight - spacing);
            YPositionOnPage yPositionOnPage = PageBreaker
                    .definePositionOnPage(startY, textHeight, 0.0, 0.0, currentPage, canvas, yOffset, false);

            if (log.isDebugEnabled()) {
                log.debug(ltd.toString());
                log.debug("Line placement {}, Current page: {}", ltd.x(), currentPage);
                log.debug(yPositionOnPage.toString());
            }


            startY = (float) yPositionOnPage.yPosition();

            double shift = shift(e, startY, canvas);
            if (shift != 0) {
                double currentShift;
                currentShift = shift;
                startY += (float) currentShift;
                entityYOffset.incrementY(currentShift);
                log.debug("Current Entity Offset {} {}", entityYOffset, e);
            }
            if (currentPage != yPositionOnPage.startPage()) {
                currentPage = yPositionOnPage.startPage();

            }
        }


        newBlockTextData = new BlockTextData(assignPositionTextData, (float) spacing);
        e.verticalOffsetAndCorrectionSize(entityYOffset);
        yOffset.incrementY(entityYOffset);
        log.debug("Returned Offset:  {} , {}", yOffset, e);
        e.addComponent(newBlockTextData);

    }

    /**
     * Calculates the necessary vertical offset for an element to ensure it does not go outside
     * the canvas rendering bounds ({@code canvas.boundingTopLine()} and {@code canvas.boundingBottomLine()}).
     *
     * @param e      The entity to check.
     * @param startY The element's current Y-position.
     * @param canvas The canvas with boundary information.
     * @return The offset value. Returns 0.0 if no offset is required.
     */
    private static double shift(Entity e, float startY, Canvas canvas) {
        var margin = e.getComponent(Margin.class).orElse(Margin.zero());
        if (e.hasAssignable(BlockText.class)) {
            var style = e.getComponent(TextStyle.class).orElseThrow(() -> new IllegalStateException("TextComponent has no TextStyle"));

            return shift(startY, style.getLineHeight(), 0.0, 0.0, canvas);
        } else {
            if (!Breakable.class.isAssignableFrom(e.getRender().getClass())) {
                var high = e.getComponent(ContentSize.class).orElseThrow();
                return shift(startY, high.height(), margin.top(), margin.bottom(), canvas);

            }
        }
        return 0;
    }

    /**
     * Core logic for calculating the shift. Checks if the element fits on the page
     * and does not exceed the top or bottom boundaries.
     *
     * @param yPosition     The top Y-coordinate of the element.
     * @param elementHeight The height of the element.
     * @param marginTop     The top margin.
     * @param marginBottom  The bottom margin.
     * @param canvas        The canvas.
     * @return The calculated offset. A positive value means shift up, negative means shift down.
     * @throws BigSizeElementException if a non-breakable element is too large to fit
     *                                 in the available page space.
     *
     */
    private static double shift(
            double yPosition,
            double elementHeight,
            double marginTop,
            double marginBottom,
            Canvas canvas
    ) {
        return shift(yPosition, elementHeight, marginTop, marginBottom, canvas.boundingBottomLine(), canvas.boundingTopLine());

    }

    private static double shift(
            double yPosition,
            double elementHeight,
            double marginTop,
            double marginBottom,
            double canvasBottomBondingLine,
            double canvasTopBondingLine
    ) {

        // 1) Can it ever fit (including margins)?
        final double requiredHeight = elementHeight + marginTop + marginBottom;
        final double availableHeight = canvasTopBondingLine - canvasBottomBondingLine;
        if (requiredHeight > availableHeight) {
            throw new BigSizeElementException(
                    "Element is too large and non-breakable — it cannot fit between the bounding lines with margins."
            );
        }

        // 2) Check top overflow
        if (yPosition + elementHeight + marginTop > canvasTopBondingLine) {
            double delta = canvasTopBondingLine - (yPosition + elementHeight + marginTop); // negative => move down
            log.info("Element shifted down by {} from top bound {}", delta, canvasTopBondingLine);
            return delta;
        }

        // 3) Check bottom overflow
        if (yPosition - marginBottom < canvasBottomBondingLine) {
            double delta = (canvasBottomBondingLine + marginBottom) - yPosition; // positive => move up
            // Re-check (defensive): after moving up, we still must not exceed top
            if (yPosition + delta + elementHeight + marginTop > canvasTopBondingLine) {
                throw new BigSizeElementException(
                        "Element is too large and non-breakable — shifting up would exceed the top bound."
                );
            }
            log.info("Element shifted up by {} from bottom bound {}", delta, canvasBottomBondingLine);
            return delta;
        }

        // 4) Already within bounds
        return 0.0;
    }

    //TODO has to be removed after testing
    public static void main(String[] args) {

        double y = 50;
        double elementHigh = 50;
        double marginTop = 5;
        double marginBottom = 5;
        var canvas = new PdfCanvas(100, 90, 0, 0, Margin.of(5));

        double shift = shift(y, elementHigh, marginTop, marginBottom, canvas);
        System.out.println(shift);
        System.out.println(y + shift);
        System.out.println(shift(y + shift, elementHigh, marginTop, marginBottom, canvas));
    }

}
