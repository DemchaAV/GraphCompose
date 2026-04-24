package com.demcha.compose.engine.components.layout;

import com.demcha.compose.engine.components.core.Component;

/**
 * Internal stack direction component used by container layout.
 *
 * <p>This belongs to the V2 engine layout model. Public authoring code should
 * describe rows/modules through canonical document APIs instead of attaching
 * this component directly.</p>
 *
 * @author Artem Demchyshyn
 */
public enum StackAxis implements Component {
    VERTICAL,
    HORIZONTAL,
    REVERSE_VERTICAL,
    REVERSE_HORIZONTAL,
    DEFAULT
}
