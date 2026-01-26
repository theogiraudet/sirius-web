/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.application.views.views.services;

import java.util.List;

import org.eclipse.sirius.components.core.api.ILabelServiceDelegate;
import org.eclipse.sirius.components.core.api.labels.StyledString;
import org.eclipse.sirius.components.core.api.labels.StyledStringFragment;
import org.eclipse.sirius.components.core.api.labels.StyledStringFragmentStyle;
import org.eclipse.sirius.web.application.views.views.domain.RepresentationDescriptionType;
import org.eclipse.sirius.web.application.views.views.domain.RepresentationKind;
import org.springframework.stereotype.Service;

/**
 * Label service delegate for Views objects.
 *
 * @author tgiraudet
 */
@Service
public class ViewsLabelServiceDelegate implements ILabelServiceDelegate {

    @Override
    public boolean canHandle(Object object) {
        return object instanceof RepresentationKind || object instanceof RepresentationDescriptionType;
    }

    @Override
    public StyledString getStyledLabel(Object object) {
        if (object instanceof RepresentationKind kind) {
            return getColoredLabel(kind.name());
        } else if (object instanceof RepresentationDescriptionType descriptionType) {
            return getColoredLabel(descriptionType.descriptions().getLabel());
        }
        return StyledString.of("");
    }

    @Override
    public List<String> getImagePaths(Object object) {
        return List.of();
    }

    private StyledString getColoredLabel(String label) {
        return new StyledString(List.of(
                new StyledStringFragment(label.toUpperCase(), StyledStringFragmentStyle.newDefaultStyledStringFragmentStyle()
                        .foregroundColor("#261E588A")
                        .build())
        ));
    }
}
