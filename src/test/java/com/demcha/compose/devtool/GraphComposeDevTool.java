package com.demcha.compose.devtool;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Watches the source tree, recompiles the preview provider on save, and renders
 * the first page directly from an in-memory {@link PDDocument}.
 */
public final class GraphComposeDevTool extends Application {
    private static final double WINDOW_WIDTH = 1480;
    private static final double WINDOW_HEIGHT = 940;
    private static final float PREVIEW_SCALE = 1.5f;
    private static final long FILE_DEBOUNCE_MILLIS = 250;
    private static final List<String> CHILD_FIRST_PREFIXES = List.of("com.demcha.");
    private static final List<String> PARENT_FIRST_PREFIXES = List.of("com.demcha.compose.devtool.");

    private final DevToolWorkspace workspace = DevToolWorkspace.currentProject();
    private final PreviewCompiler compiler = new PreviewCompiler();
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform()
                    .daemon()
                    .name("graphcompose-devtool-refresh-", 0)
                    .factory());
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform()
                    .daemon()
                    .name("graphcompose-devtool-debounce-", 0)
                    .factory());
    private final AtomicLong latestRevision = new AtomicLong();
    private final AtomicReference<RefreshRequest> pendingRequest = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> pendingDebounce = new AtomicReference<>();
    private final AtomicReference<LoadedPreview> currentPreview = new AtomicReference<>();
    private final ArrayDeque<Path> retainedOutputDirs = new ArrayDeque<>();

    private RecursivePathWatcher watcher;
    private Label providerValue;
    private Label lastEventValue;
    private Label compileStatusValue;
    private Label timingValue;
    private Label footerStatus;
    private TextArea watchedRootsArea;
    private TextArea diagnosticsArea;
    private ImageView imageView;

    public static void main(String[] args) {
        Application.launch(GraphComposeDevTool.class, args);
    }

    @Override
    public void start(Stage stage) {
        var root = new BorderPane();
        root.setCenter(buildSplitPane());
        root.setBottom(buildFooter());
        root.setStyle("-fx-background-color: #f8fafc;");

        var scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setTitle("GraphComposeDevTool");
        stage.setScene(scene);
        stage.show();

        initializeUiState();
        startWatcher();
        requestRefresh(RefreshRequest.startup(), false);
    }

    @Override
    public void stop() {
        ScheduledFuture<?> scheduled = pendingDebounce.getAndSet(null);
        if (scheduled != null) {
            scheduled.cancel(false);
        }

        closeWatcher();
        debounceExecutor.shutdownNow();
        refreshExecutor.shutdownNow();
        closePreview(currentPreview.getAndSet(null));
    }

    private SplitPane buildSplitPane() {
        var splitPane = new SplitPane(buildStatusPane(), buildPreviewPane());
        splitPane.setDividerPositions(0.33);
        return splitPane;
    }

    private VBox buildStatusPane() {
        providerValue = createValueLabel();
        lastEventValue = createValueLabel();
        compileStatusValue = createValueLabel();
        timingValue = createValueLabel();

        watchedRootsArea = createReadOnlyArea(4);
        diagnosticsArea = createReadOnlyArea(16);
        diagnosticsArea.setPromptText("Compilation and runtime diagnostics will appear here.");

        var reloadButton = new Button("Reload now");
        reloadButton.setOnAction(event -> {
            updateLastEvent("Manual reload");
            requestRefresh(RefreshRequest.manual(), false);
        });

        var content = new VBox(
                12,
                sectionTitle("Preview Provider"),
                providerValue,
                sectionTitle("Watched Roots"),
                watchedRootsArea,
                new Separator(),
                sectionTitle("Last Change"),
                lastEventValue,
                sectionTitle("Compile Status"),
                compileStatusValue,
                sectionTitle("Timing"),
                timingValue,
                reloadButton,
                new Separator(),
                sectionTitle("Diagnostics"),
                diagnosticsArea);

        content.setPadding(new Insets(18));
        content.setFillWidth(true);
        content.setStyle("-fx-background-color: #ffffff;");
        return content;
    }

    private ScrollPane buildPreviewPane() {
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        var emptyState = new Label("Preview will appear here after the first successful save.");
        emptyState.visibleProperty().bind(Bindings.isNull(imageView.imageProperty()));
        emptyState.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");

        var canvas = new StackPane(imageView, emptyState);
        canvas.setAlignment(Pos.TOP_CENTER);
        canvas.setPadding(new Insets(24));
        canvas.setStyle("-fx-background-color: linear-gradient(to bottom, #e2e8f0, #cbd5e1);");

        var scrollPane = new ScrollPane(canvas);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-border-color: transparent;");
        return scrollPane;
    }

    private Label buildFooter() {
        footerStatus = new Label();
        footerStatus.setPadding(new Insets(10, 14, 10, 14));
        footerStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
        return footerStatus;
    }

    private void initializeUiState() {
        providerValue.setText(workspace.previewProviderClassName());
        watchedRootsArea.setText(String.join(
                System.lineSeparator(),
                workspace.watchRoots().stream().map(workspace::displayPath).toList()));
        lastEventValue.setText("Startup scan");
        compileStatusValue.setText("Watching");
        timingValue.setText("Waiting for the first render");
        footerStatus.setText("Watching source files and waiting for the first preview build...");
    }

    private void startWatcher() {
        try {
            watcher = new RecursivePathWatcher(
                    workspace.existingWatchRoots(),
                    this::handlePathChange,
                    this::handleWatcherFailure);
            watcher.start();
        } catch (Exception ex) {
            setFailureState("Not compiled",
                    "Watcher startup failed",
                    "Watcher failed to start:%n%n%s".formatted(formatThrowable(ex)),
                    "Watching disabled");
        }
    }

    private void handlePathChange(RecursivePathWatcher.PathChange change) {
        Path changedPath = change.path();
        boolean javaChange = workspace.isJavaSource(changedPath);
        boolean resourceChange = workspace.isResourcePath(changedPath);

        if (!javaChange && !resourceChange) {
            return;
        }

        String eventText = "%s %s".formatted(change.kind().name(), workspace.displayPath(changedPath));
        updateLastEvent(eventText);
        requestRefresh(new RefreshRequest(eventText, javaChange, false), true);
    }

    private void handleWatcherFailure(Exception ex) {
        setFailureState("Not compiled",
                "Watcher failure",
                formatThrowable(ex),
                "File watching stopped");
    }

    private void requestRefresh(RefreshRequest request, boolean debounced) {
        pendingRequest.getAndUpdate(existing -> existing == null ? request : existing.merge(request));

        ScheduledFuture<?> previous = pendingDebounce.getAndSet(null);
        if (previous != null) {
            previous.cancel(false);
        }

        if (debounced) {
            ScheduledFuture<?> scheduled = debounceExecutor.schedule(
                    this::submitPendingRefresh,
                    FILE_DEBOUNCE_MILLIS,
                    TimeUnit.MILLISECONDS);
            pendingDebounce.set(scheduled);
        } else {
            submitPendingRefresh();
        }
    }

    private void submitPendingRefresh() {
        RefreshRequest request = pendingRequest.getAndSet(null);
        if (request == null) {
            return;
        }

        long revision = latestRevision.incrementAndGet();
        refreshExecutor.submit(() -> performRefresh(revision, request));
    }

    private void performRefresh(long revision, RefreshRequest request) {
        LoadedPreview previousPreview = currentPreview.get();
        boolean shouldCompile = request.forceCompilation() || request.requiresCompilation() || previousPreview == null;

        if (revision != latestRevision.get()) {
            return;
        }

        Platform.runLater(() -> {
            compileStatusValue.setText(shouldCompile ? "Compiling" : "Watching");
            footerStatus.setText(shouldCompile
                    ? "Compiling preview sources..."
                    : "Reloading preview resources...");
        });

        PreviewCompiler.CompilationResult compilationResult = null;
        Path compiledOutputDir = previousPreview == null ? null : previousPreview.outputDir();
        int sourceCount = previousPreview == null ? 0 : previousPreview.sourceCount();

        if (shouldCompile) {
            compilationResult = compiler.compile(workspace, revision);
            if (!compilationResult.success()) {
                if (revision == latestRevision.get()) {
                    setFailureState(
                            "Not compiled",
                            "Compile failed",
                            compilationResult.diagnostics(),
                            "compile %d ms | %d sources".formatted(
                                    compilationResult.compileMillis(),
                                    compilationResult.sourceCount()));
                }
                return;
            }

            compiledOutputDir = compilationResult.outputDir();
            sourceCount = compilationResult.sourceCount();
        }

        if (compiledOutputDir == null) {
            if (revision == latestRevision.get()) {
                setFailureState("Not compiled",
                        "No compiled preview available",
                        "Compile the preview provider once before doing resource-only reloads.",
                        "No output directory");
            }
            return;
        }

        SelectiveChildFirstClassLoader loader = null;
        try {
            loader = new SelectiveChildFirstClassLoader(
                    workspace.buildClassLoaderUrls(compiledOutputDir),
                    GraphComposeDevTool.class.getClassLoader(),
                    CHILD_FIRST_PREFIXES,
                    PARENT_FIRST_PREFIXES);

            PreviewFrame frame = renderFrame(loader, revision, compiledOutputDir, sourceCount, compilationResult);
            if (revision != latestRevision.get()) {
                closeLoader(loader);
                if (shouldCompile) {
                    PreviewCompiler.deleteDirectoryQuietly(compiledOutputDir);
                }
                return;
            }

            LoadedPreview loadedPreview = new LoadedPreview(loader, compiledOutputDir, sourceCount);
            LoadedPreview oldPreview = currentPreview.getAndSet(loadedPreview);
            closePreview(oldPreview);

            if (shouldCompile) {
                retainSuccessfulOutputDir(compiledOutputDir);
            }

            PreviewFrame appliedFrame = frame;
            Platform.runLater(() -> applySuccessState(appliedFrame));
        } catch (CancellationException ignored) {
            closeLoader(loader);
            if (shouldCompile) {
                PreviewCompiler.deleteDirectoryQuietly(compiledOutputDir);
            }
        } catch (Exception ex) {
            closeLoader(loader);
            if (shouldCompile) {
                PreviewCompiler.deleteDirectoryQuietly(compiledOutputDir);
            }

            if (revision == latestRevision.get()) {
                long compileMillis = compilationResult == null ? 0 : compilationResult.compileMillis();
                String timing = shouldCompile
                        ? "compile %d ms | render failed".formatted(compileMillis)
                        : "compile skipped | render failed";
                setFailureState("Not compiled", "Preview failed", formatThrowable(ex), timing);
            }
        }
    }

    private PreviewFrame renderFrame(SelectiveChildFirstClassLoader loader,
                                     long revision,
                                     Path compiledOutputDir,
                                     int sourceCount,
                                     PreviewCompiler.CompilationResult compilationResult) throws Exception {
        if (revision != latestRevision.get()) {
            throw new CancellationException("Stale refresh request");
        }

        Class<?> providerType = loader.loadClass(workspace.previewProviderClassName());
        if (!DevToolPreviewProvider.class.isAssignableFrom(providerType)) {
            throw new IllegalStateException(
                    "%s must implement %s".formatted(
                            workspace.previewProviderClassName(),
                            DevToolPreviewProvider.class.getName()));
        }

        DevToolPreviewProvider provider = (DevToolPreviewProvider) providerType.getDeclaredConstructor().newInstance();
        long renderStartedAt = System.nanoTime();

        try (PDDocument document = Objects.requireNonNull(provider.buildPreview(),
                "Preview provider returned null")) {

            if (document.getNumberOfPages() == 0) {
                throw new IllegalStateException("Preview provider returned a document without pages.");
            }

            BufferedImage bufferedImage = PdfRenderBridge.renderToImage(document, 0, PREVIEW_SCALE);
            if (revision != latestRevision.get()) {
                throw new CancellationException("Stale refresh request");
            }

            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            long renderMillis = (System.nanoTime() - renderStartedAt) / 1_000_000;
            long compileMillis = compilationResult == null ? 0 : compilationResult.compileMillis();

            return new PreviewFrame(
                    image,
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    compileMillis,
                    renderMillis,
                    sourceCount,
                    compilationResult != null,
                    compiledOutputDir);
        }
    }

    private void retainSuccessfulOutputDir(Path outputDir) {
        if (outputDir == null) {
            return;
        }

        retainedOutputDirs.remove(outputDir);
        retainedOutputDirs.addLast(outputDir);

        while (retainedOutputDirs.size() > 2) {
            Path removed = retainedOutputDirs.removeFirst();
            if (!Objects.equals(removed, outputDir)) {
                PreviewCompiler.deleteDirectoryQuietly(removed);
            }
        }
    }

    private void applySuccessState(PreviewFrame frame) {
        imageView.setImage(frame.image());
        diagnosticsArea.clear();
        compileStatusValue.setText("Rendered");
        timingValue.setText(frame.compilationTriggered()
                ? "compile %d ms | render %d ms | %d sources".formatted(
                        frame.compileMillis(),
                        frame.renderMillis(),
                        frame.sourceCount())
                : "compile skipped | render %d ms | %d sources".formatted(
                        frame.renderMillis(),
                        frame.sourceCount()));
        footerStatus.setText("Watching for changes. Last rendered %dx%d from %s".formatted(
                frame.pixelWidth(),
                frame.pixelHeight(),
                workspace.displayPath(frame.outputDir())));
    }

    private void setFailureState(String status, String footer, String diagnostics, String timing) {
        Platform.runLater(() -> {
            compileStatusValue.setText(status);
            footerStatus.setText(footer);
            diagnosticsArea.setText(diagnostics == null ? "" : diagnostics);
            timingValue.setText(timing);
        });
    }

    private void updateLastEvent(String text) {
        Platform.runLater(() -> lastEventValue.setText(text));
    }

    private void closeWatcher() {
        if (watcher == null) {
            return;
        }

        try {
            watcher.close();
        } catch (Exception ignored) {
            // Best effort shutdown only.
        } finally {
            watcher = null;
        }
    }

    private void closePreview(LoadedPreview preview) {
        if (preview == null) {
            return;
        }
        closeLoader(preview.classLoader());
    }

    private void closeLoader(SelectiveChildFirstClassLoader loader) {
        if (loader == null) {
            return;
        }

        try {
            loader.close();
        } catch (Exception ignored) {
            // Best effort shutdown only.
        }
    }

    private static Label createValueLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 13px;");
        return label;
    }

    private static TextArea createReadOnlyArea(int rows) {
        var area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(rows);
        area.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        area.setStyle("-fx-control-inner-background: #f8fafc; -fx-text-fill: #0f172a;");
        return area;
    }

    private static Label sectionTitle(String text) {
        var label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        return label;
    }

    private static String formatThrowable(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        var writer = new StringWriter();
        try (var printWriter = new PrintWriter(writer)) {
            root.printStackTrace(printWriter);
        }
        return writer.toString().strip();
    }

    private record RefreshRequest(String description, boolean requiresCompilation, boolean forceCompilation) {

        static RefreshRequest startup() {
            return new RefreshRequest("Startup scan", true, false);
        }

        static RefreshRequest manual() {
            return new RefreshRequest("Manual reload", true, true);
        }

        RefreshRequest merge(RefreshRequest other) {
            return new RefreshRequest(
                    other.description,
                    requiresCompilation || other.requiresCompilation,
                    forceCompilation || other.forceCompilation);
        }
    }

    private record LoadedPreview(SelectiveChildFirstClassLoader classLoader, Path outputDir, int sourceCount) {
    }

    private record PreviewFrame(
            Image image,
            int pixelWidth,
            int pixelHeight,
            long compileMillis,
            long renderMillis,
            int sourceCount,
            boolean compilationTriggered,
            Path outputDir) {
    }
}
