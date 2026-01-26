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
import {
  RepresentationLoadingIndicator,
  SelectionEntry,
  useSelection,
  WorkbenchViewComponentProps,
  WorkbenchViewHandle,
} from '@eclipse-sirius/sirius-components-core';
import { GQLTree, GQLTreeItem, TreeView, useTreeSelection } from '@eclipse-sirius/sirius-components-trees';
import { forwardRef, useEffect, useState } from 'react';
import { makeStyles } from 'tss-react/mui';
import { ViewsViewState } from './ViewsView.types';
import { useViewsViewSubscription } from './useViewsViewSubscription';
import { GQLViewsEventPayload, GQLViewsRefreshedEventPayload } from './useViewsViewSubscription.types';

const useStyles = makeStyles()(() => ({
  treeView: {
    display: 'grid',
    gridTemplateColumns: 'auto',
    gridTemplateRows: '1fr',
    justifyItems: 'stretch',
    overflow: 'auto',
  },
  treeContent: {
    overflow: 'auto',
  },
}));

const isViewsRefreshedEventPayload = (payload: GQLViewsEventPayload): payload is GQLViewsRefreshedEventPayload =>
  payload && payload.__typename === 'TreeRefreshedEventPayload';

export const ViewsView = forwardRef<WorkbenchViewHandle, WorkbenchViewComponentProps>(
  ({ editingContextId, readOnly }: WorkbenchViewComponentProps) => {
    const { classes: styles } = useStyles();

    const [state, setState] = useState<ViewsViewState>({
      expanded: [],
      tree: null,
      selectedTreeItemIds: [],
    });

    const { payload } = useViewsViewSubscription(editingContextId, state.expanded);

    useEffect(() => {
      if (isViewsRefreshedEventPayload(payload)) {
        setState((prevState) => ({ ...prevState, tree: payload.tree }));
      }
    }, [payload]);

    const { selection, setSelection } = useSelection();
    const { treeItemClick } = useTreeSelection();

    const onExpandedElementChange = (newExpandedIds: string[]) => {
      setState((prevState) => ({
        ...prevState,
        expanded: newExpandedIds,
      }));
    };

    const onTreeItemClick = (event: React.MouseEvent<HTMLDivElement, MouseEvent>, tree: GQLTree, item: GQLTreeItem) => {
      const localSelection = treeItemClick(event, tree, item, state.selectedTreeItemIds, true);
      setState((prevState) => ({
        ...prevState,
        selectedTreeItemIds: localSelection.selectedTreeItemIds,
      }));
      const globalSelection = treeItemClick(
        event,
        state.tree,
        item,
        selection.entries.map((entry) => entry.id),
        true
      );
      setSelection({ entries: globalSelection.selectedTreeItemIds.map<SelectionEntry>((id) => ({ id })) });
    };

    if (!state.tree) {
      return (
        <div className={styles.treeView}>
          <RepresentationLoadingIndicator />
        </div>
      );
    }

    return (
      <div className={styles.treeView}>
        <div className={styles.treeContent}>
          <TreeView
            editingContextId={editingContextId}
            readOnly={readOnly}
            tree={state.tree}
            textToHighlight=""
            textToFilter={null}
            onExpandedElementChange={onExpandedElementChange}
            expanded={state.expanded}
            maxDepth={1}
            onTreeItemClick={onTreeItemClick}
            treeItemActionRender={}
            selectTreeItems={(selectedTreeItemIds: string[]) =>
              setState((prevState) => {
                return { ...prevState, selectedTreeItemIds };
              })
            }
            selectedTreeItemIds={state.selectedTreeItemIds}
            data-testid="views://"
          />
        </div>
      </div>
    );
  }
);

ViewsView.displayName = 'ViewsView';
