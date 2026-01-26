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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IRepresentationDescriptionSearchService;
import org.eclipse.sirius.components.core.api.IURLParser;
import org.eclipse.sirius.components.representations.IRepresentationDescription;
import org.eclipse.sirius.components.representations.IRepresentationRenderVariableCustomizer;
import org.eclipse.sirius.components.representations.VariableManager;
import org.eclipse.sirius.web.application.UUIDParser;
import org.eclipse.sirius.web.application.views.views.domain.RepresentationDescriptionType;
import org.eclipse.sirius.web.application.views.views.domain.RepresentationKind;
import org.eclipse.sirius.web.domain.boundedcontexts.representationdata.RepresentationMetadata;
import org.eclipse.sirius.web.domain.boundedcontexts.representationdata.services.api.IRepresentationMetadataSearchService;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;

/**
 * Provides additional render variables to be used by {@link ViewsTreeDescriptionProvider} to reduce the amount of duplicate work (notably database access):
 *
 * <dl>
 * <dt>{@code existingRepresentations}<dt>
 * <dd>A tree containing all existing representations in the editing context.
 * <dd>Representations are organized hierarchically: each representation is grouped under its representation description type (e.g., Topography), which in turn is grouped under its representation kind (e.g., Diagram, Tree).
 * </dl>
 *
 * @author tgiraudet
 */
@Service
public class ViewsRenderVariablesCustomizer implements IRepresentationRenderVariableCustomizer {

    private final IRepresentationMetadataSearchService representationMetadataSearchService;
    private final IRepresentationDescriptionSearchService representationDescriptionSearchService;
    private final IURLParser urlParser;

    public ViewsRenderVariablesCustomizer(IRepresentationMetadataSearchService representationMetadataSearchService,
            IRepresentationDescriptionSearchService representationDescriptionSearchService, IURLParser urlParser) {
        this.representationMetadataSearchService = Objects.requireNonNull(representationMetadataSearchService);
        this.representationDescriptionSearchService = Objects.requireNonNull(representationDescriptionSearchService);
        this.urlParser = Objects.requireNonNull(urlParser);
    }

    @Override
    public VariableManager customize(IRepresentationDescription representationDescription, VariableManager variableManager) {
        if (ViewsTreeDescriptionProvider.DESCRIPTION_ID.equals(representationDescription.getId())) {
            var optionalEditingContext = variableManager.get(IEditingContext.EDITING_CONTEXT, IEditingContext.class);
            if (optionalEditingContext.isPresent()) {
                VariableManager customizedVariableManager = variableManager.createChild();
                String editingContextId = optionalEditingContext.get().getId();
                var optionalSemanticDataId = new UUIDParser().parse(editingContextId);
                if(optionalSemanticDataId.isPresent()) {
                    List<RepresentationMetadata> allRepresentationMetadata = this.representationMetadataSearchService.findAllRepresentationMetadataBySemanticData(
                            AggregateReference.to(optionalSemanticDataId.get()));
                    Map<String, IRepresentationDescription> allRepresentationDescription = this.representationDescriptionSearchService.findAll(optionalEditingContext.get());

                    List<RepresentationDescriptionType> descType = allRepresentationMetadata.stream().collect(Collectors.groupingBy(RepresentationMetadata::getDescriptionId)).entrySet().stream()
                            .map(entry -> Optional.ofNullable(allRepresentationDescription.get(entry.getKey())).map(desc -> new RepresentationDescriptionType(entry.getKey(), desc, entry.getValue())))
                            .flatMap(Optional::stream).toList();

                    List<RepresentationKind> representationKind = descType.stream().collect(Collectors.groupingBy(reprDesc -> reprDesc.representationsMetadata().get(0).getKind())).entrySet().stream()
                            .map(entry -> new RepresentationKind(UUID.nameUUIDFromBytes(entry.getKey().getBytes()).toString(), this.urlParser.getParameterValues(entry.getKey()).get("type").get(0),
                                    entry.getValue())).toList();

                    customizedVariableManager.put(ViewsTreeDescriptionProvider.EXISTING_REPRESENTATIONS, representationKind);
                    return customizedVariableManager;
                }
            }
        }
        return variableManager;
    }
}
