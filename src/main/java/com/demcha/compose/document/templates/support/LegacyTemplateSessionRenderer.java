package com.demcha.compose.document.templates.support;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Temporary transition-release helper that installs exact legacy PDF artifacts
 * into a canonical {@link DocumentSession}.
 *
 * <p>This keeps {@code document.templates.*} as the public entrypoint while the
 * handler-based canonical backend catches up with the full shipped template
 * surface and legacy snapshot authority.</p>
 */
public final class LegacyTemplateSessionRenderer {
    private static final Method INSTALL_METHOD = resolveInstallMethod();

    private LegacyTemplateSessionRenderer() {
    }

    /**
     * Renders one template through the legacy PDF composer and installs the
     * resulting snapshot plus bytes into the supplied canonical session.
     *
     * @param document canonical document session that should expose the rendered artifacts
     * @param action legacy PDF composition callback
     */
    public static void renderInto(DocumentSession document, LegacyPdfComposeAction action) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(action, "action");

        LayoutCanvas canvas = document.canvas();
        Margin margin = canvas.margin();
        PDRectangle pageSize = new PDRectangle((float) canvas.width(), (float) canvas.height());

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(pageSize)
                .margin((float) margin.top(), (float) margin.right(), (float) margin.bottom(), (float) margin.left())
                .markdown(true)
                .create()) {
            action.compose(composer);
            INSTALL_METHOD.invoke(document, composer.layoutSnapshot(), composer.toBytes());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to install legacy compatibility render artifacts.", exception);
        }
    }

    private static Method resolveInstallMethod() {
        try {
            Method method = DocumentSession.class.getDeclaredMethod(
                    "installCompatibilityArtifacts",
                    com.demcha.compose.layout_core.debug.LayoutSnapshot.class,
                    byte[].class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access DocumentSession compatibility hook.", exception);
        }
    }

    @FunctionalInterface
    public interface LegacyPdfComposeAction {
        void compose(PdfComposer composer) throws Exception;
    }
}
