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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.sirius.components.collaborative.forms.variables.FormVariableProvider;
import org.eclipse.sirius.components.collaborative.trees.api.IDeleteTreeItemHandler;
import org.eclipse.sirius.components.collaborative.trees.api.IRenameTreeItemHandler;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IEditingContextRepresentationDescriptionProvider;
import org.eclipse.sirius.components.core.api.IIdentityService;
import org.eclipse.sirius.components.core.api.ILabelService;
import org.eclipse.sirius.components.core.api.labels.StyledString;
import org.eclipse.sirius.components.core.api.labels.StyledStringFragment;
import org.eclipse.sirius.components.representations.Failure;
import org.eclipse.sirius.components.representations.IRepresentationDescription;
import org.eclipse.sirius.components.representations.IStatus;
import org.eclipse.sirius.components.representations.VariableManager;
import org.eclipse.sirius.components.trees.Tree;
import org.eclipse.sirius.components.trees.TreeItem;
import org.eclipse.sirius.components.trees.description.TreeDescription;
import org.eclipse.sirius.components.trees.renderer.TreeRenderer;
import org.eclipse.sirius.web.application.views.explorer.ExplorerEventProcessorFactory;
import org.eclipse.sirius.web.application.views.views.ViewsEventProcessorFactory;
import org.eclipse.sirius.web.application.views.views.domain.RepresentationDescriptionType;
import org.eclipse.sirius.web.application.views.views.domain.RepresentationKind;
import org.eclipse.sirius.web.domain.boundedcontexts.representationdata.RepresentationMetadata;
import org.springframework.stereotype.Service;

/**
 * Provides the form description for the "representations" view.
 *
 * @author tgiraudet
 */
@Service
public class ViewsTreeDescriptionProvider implements IEditingContextRepresentationDescriptionProvider {

    public static final String PREFIX = "views://";

    public static final String DESCRIPTION_ID = "views_form_description";

    public static final String EXISTING_REPRESENTATIONS = "existingRepresentations";

    public static final String REPRESENTATION_NAME = "Views";

    private final IIdentityService identityService;

    private final ILabelService labelService;

    private final List<IRenameTreeItemHandler> renameTreeItemHandlers;

    private final List<IDeleteTreeItemHandler> deleteTreeItemHandlers;

    public ViewsTreeDescriptionProvider(IIdentityService identityService, ILabelService labelService, List<IRenameTreeItemHandler> renameTreeItemHandlers,
            List<IDeleteTreeItemHandler> deleteTreeItemHandlers) {
        this.identityService = Objects.requireNonNull(identityService);
        this.labelService = Objects.requireNonNull(labelService);
        this.renameTreeItemHandlers = Objects.requireNonNull(renameTreeItemHandlers);
        this.deleteTreeItemHandlers = Objects.requireNonNull(deleteTreeItemHandlers);
    }

    @Override
    public List<IRepresentationDescription> getRepresentationDescriptions(IEditingContext editingContext) {
        var description = TreeDescription.newTreeDescription(DESCRIPTION_ID)
                .label(REPRESENTATION_NAME)
                .idProvider(this::getTreeId)
                .kindProvider(this::getKind)
                .labelProvider(this::getLabel)
                .targetObjectIdProvider(this::getTargetObjectId)
                .treeItemIconURLsProvider(this::getImageURL)
                .treeItemIdProvider(this::getTreeItemId)
                .treeItemObjectProvider(this::getTreeItemObject)
                .elementsProvider(this::getElements)
                .childrenProvider(this::getChildren)
                .hasChildrenProvider(this::hasChildren)
                .canCreatePredicate(variableManager -> false)
                .deleteHandler(this::getDeleteHandler)
                .renameHandler(this::getRenameHandler)
                .treeItemLabelProvider(this::getLabel)
                .iconURLsProvider(variableManager -> List.of("/views/views.svg"))
                .editableProvider(this::isEditable)
                .deletableProvider(this::isDeletable)
                .selectableProvider(this::isSelectable)
                .parentObjectProvider(this::getParentObject)
                .build();

        return List.of(description);
    }

    private String getTreeId(VariableManager variableManager) {
        List<?> expandedObjects = variableManager.get(TreeRenderer.EXPANDED, List.class).orElse(List.of());
        List<String> expandedObjectIds = expandedObjects.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(id -> URLEncoder.encode(id, StandardCharsets.UTF_8))
                .toList();

        return PREFIX + "?" + ViewsEventProcessorFactory.TREE_DESCRIPTION_ID_PARAMETER + "=" + URLEncoder.encode(DESCRIPTION_ID, StandardCharsets.UTF_8) + "&expandedIds=[" + String.join(",", expandedObjectIds) + "]";
    }

    private String getTreeItemId(VariableManager variableManager) {
        Object self = variableManager.getVariables().get(VariableManager.SELF);
        if(self instanceof RepresentationDescriptionType representationDescriptionType) {
            return representationDescriptionType.id();
        } else if(self instanceof RepresentationKind representationKind) {
            return representationKind.id();
        } else if(self instanceof RepresentationMetadata metadata) {
            return metadata.getRepresentationMetadataId().toString();
        }
        // Impossible case
        return null;
    }

    private String getKind(VariableManager variableManager) {
        Object self = variableManager.getVariables().get(VariableManager.SELF);
        if (self instanceof RepresentationKind) {
            return "siriusWeb://representationKind";
        } else if (self instanceof RepresentationDescriptionType) {
            return "siriusWeb://representationDescriptionType";
        } else if (self instanceof RepresentationMetadata representationMetadata) {
            return representationMetadata.getKind();
        }
        return "";
    }

    private StyledString getLabel(VariableManager variableManager) {
        Object self = variableManager.getVariables().get(VariableManager.SELF);
        var label = this.labelService.getStyledLabel(self);
        if(self instanceof RepresentationKind kind) {
            var count = kind.representationDescriptionTypes().stream().mapToLong(descType -> descType.representationsMetadata().size()).sum();
            label = new StyledString(Stream.concat(label.styledStringFragments().stream(), Stream.of(StyledStringFragment.of(" (%d)".formatted(count)))).toList());
        } else if(self instanceof RepresentationDescriptionType type) {
            var count = type.representationsMetadata().size();
            label = new StyledString(Stream.concat(label.styledStringFragments().stream(), Stream.of(StyledStringFragment.of(" (%d)".formatted(count)))).toList());
        }
        return label;
    }

    private String getTargetObjectId(VariableManager variableManager) {
        return variableManager.get(VariableManager.SELF, Object.class)
                .map(this.identityService::getId)
                .orElse(null);
    }

    private List<String> getImageURL(VariableManager variableManager) {
        Object self = variableManager.getVariables().get(VariableManager.SELF);
        if(self instanceof RepresentationKind || self instanceof RepresentationDescriptionType) {
            return List.of("/views/views.svg");
        }
        return this.labelService.getImagePaths(self);
    }

    private IStatus getDeleteHandler(VariableManager variableManager) {
        var optionalEditingContext = variableManager.get(IEditingContext.EDITING_CONTEXT, IEditingContext.class);
        var optionalTreeItem = variableManager.get(TreeItem.SELECTED_TREE_ITEM, TreeItem.class);
        var optionalTree = variableManager.get(TreeDescription.TREE, Tree.class);

        if (optionalEditingContext.isPresent() && optionalTreeItem.isPresent() && optionalTree.isPresent()) {
            IEditingContext editingContext = optionalEditingContext.get();
            TreeItem treeItem = optionalTreeItem.get();

            var optionalHandler = this.deleteTreeItemHandlers.stream()
                    .filter(handler -> handler.canHandle(editingContext, treeItem))
                    .findFirst();

            if (optionalHandler.isPresent()) {
                IDeleteTreeItemHandler deleteTreeItemHandler = optionalHandler.get();
                return deleteTreeItemHandler.handle(editingContext, treeItem, optionalTree.get());
            }
        }
        return new Failure("Failed to delete the element.");
    }

    private IStatus getRenameHandler(VariableManager variableManager, String newLabel) {
        var optionalEditingContext = variableManager.get(IEditingContext.EDITING_CONTEXT, IEditingContext.class);
        var optionalTreeItem = variableManager.get(TreeItem.SELECTED_TREE_ITEM, TreeItem.class);
        var optionalTree = variableManager.get(TreeDescription.TREE, Tree.class);

        if (optionalEditingContext.isPresent() && optionalTreeItem.isPresent() && optionalTree.isPresent()) {
            IEditingContext editingContext = optionalEditingContext.get();
            TreeItem treeItem = optionalTreeItem.get();

            var optionalHandler = this.renameTreeItemHandlers.stream()
                    .filter(handler -> handler.canHandle(editingContext, treeItem, newLabel))
                    .findFirst();

            if (optionalHandler.isPresent()) {
                IRenameTreeItemHandler renameTreeItemHandler = optionalHandler.get();
                return renameTreeItemHandler.handle(editingContext, treeItem, newLabel, optionalTree.get());
            }
        }
        return new Failure("");
    }

    private Object getTreeItemObject(VariableManager variableManager) {
        var optionalTreeItemId = variableManager.get(TreeDescription.ID, String.class);

        if (optionalTreeItemId.isEmpty()) {
            return null;
        }

        String treeItemId = optionalTreeItemId.get();
        List<?> existingRepresentations = variableManager.get(EXISTING_REPRESENTATIONS, List.class).orElse(List.of());

        for (Object obj : existingRepresentations) {
            if (obj instanceof RepresentationKind representationKind) {
                if (representationKind.id().equals(treeItemId)) {
                    return representationKind;
                }

                for (RepresentationDescriptionType descType : representationKind.representationDescriptionTypes()) {
                    if (descType.id().equals(treeItemId)) {
                        return descType;
                    }

                    for (RepresentationMetadata metadata : descType.representationsMetadata()) {
                        if (metadata.getRepresentationMetadataId().toString().equals(treeItemId)) {
                            return metadata;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Object getParentObject(VariableManager variableManager) {
        Object self = variableManager.getVariables().get(VariableManager.SELF);
        List<?> existingRepresentations = variableManager.get(EXISTING_REPRESENTATIONS, List.class).orElse(List.of());

        if (self instanceof RepresentationKind) {
            return null;
        }

        for (Object obj : existingRepresentations) {
            if (obj instanceof RepresentationKind representationKind) {
                for (RepresentationDescriptionType descType : representationKind.representationDescriptionTypes()) {
                    if (descType.equals(self)) {
                        return representationKind;
                    }

                    for (RepresentationMetadata metadata : descType.representationsMetadata()) {
                        if (metadata.equals(self)) {
                            return descType;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isSelectable(VariableManager variableManager) {
        return true;
    }

    private boolean isEditable(VariableManager variableManager) {
        Object self = this.getSelf(variableManager);
        return self instanceof RepresentationMetadata;
    }

    private boolean isDeletable(VariableManager variableManager) {
        Object self = this.getSelf(variableManager);
        return self instanceof RepresentationMetadata;
    }

    private boolean hasChildren(VariableManager variableManager) {
        Object self = this.getSelf(variableManager);
        return self instanceof RepresentationDescriptionType || self instanceof RepresentationKind;
    }

    private List<?> getChildren(VariableManager variableManager) {
        List<String> expandedIds = new ArrayList<>();
        Object objects = variableManager.getVariables().get(TreeRenderer.EXPANDED);
        if (objects instanceof List<?> list) {
            expandedIds = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }

        String id = this.getTreeItemId(variableManager);
        if (expandedIds.contains(id)) {
            Object self = this.getSelf(variableManager);
            if (self instanceof RepresentationKind representationKind) {
                return representationKind.representationDescriptionTypes();
            } else if (self instanceof RepresentationDescriptionType representationDescriptionType) {
                return representationDescriptionType.representationsMetadata();
            }
        }
        return List.of();
    }

    private Object getSelf(VariableManager variableManager) {
        return variableManager.get(VariableManager.SELF, Object.class).orElse(null);
    }

    private List<?> getElements(VariableManager variableManager) {
        List<?> raw = variableManager.get(ViewsTreeDescriptionProvider.EXISTING_REPRESENTATIONS, List.class).orElse(List.of());
        return raw.stream()
                .filter(RepresentationKind.class::isInstance)
                .map(RepresentationKind.class::cast)
                .toList();
    }
}
