package sk.iway.iwcm.components.multistep_form.support;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Identifies the concrete form item represented by a reverse-condition usage link. */
@Getter
@AllArgsConstructor
public final class FormItemUsage {
    private final Long id;
    private final Long stepId;
    private final String label;
    private final String value;
}
