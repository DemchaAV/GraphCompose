package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;

final class BuiltInCvTemplateSupport {
    private BuiltInCvTemplateSupport() {
    }

    static void compose(String templateId, DocumentSession document, CvDocumentSpec documentSpec, Renderer renderer) {
        long startNanos = TemplateLifecycleLog.start(templateId, documentSpec);
        try {
            renderer.compose(new SessionTemplateComposeTarget(document), documentSpec);
            TemplateLifecycleLog.success(templateId, documentSpec, startNanos);
        } catch (RuntimeException | Error ex) {
            TemplateLifecycleLog.failure(templateId, documentSpec, startNanos, ex);
            throw ex;
        }
    }

    @FunctionalInterface
    interface Renderer {
        void compose(TemplateComposeTarget target, CvDocumentSpec documentSpec);
    }
}
