package org.eclipse.sirius.web.application.views.views.services;

import java.util.Optional;

import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IObjectSearchServiceDelegate;

public class ViewsObjectSearchServiceDelegate implements IObjectSearchServiceDelegate {
    @Override
    public boolean canHandle(IEditingContext editingContext, String objectId) {
        return false;
    }

    @Override
    public Optional<Object> getObject(IEditingContext editingContext, String objectId) {
        return Optional.empty();
    }
}
