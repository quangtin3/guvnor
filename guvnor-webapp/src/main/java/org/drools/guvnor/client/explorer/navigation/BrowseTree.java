/*
 * Copyright 2011 JBoss Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.drools.guvnor.client.explorer.navigation;

import com.google.gwt.user.client.ui.IsTreeItem;
import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.configurations.Capability;
import org.drools.guvnor.client.configurations.UserCapabilities;
import org.drools.guvnor.client.explorer.TabContainer;
import org.drools.guvnor.client.explorer.TabManager;
import org.drools.guvnor.client.explorer.navigation.BrowseTreeView.Presenter;
import org.drools.guvnor.client.rpc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowseTree implements Presenter {

    private final BrowseTreeView view;
    private final RepositoryServiceAsync repositoryService;
    private final CategoryServiceAsync categoryService;
    private final Map<IsTreeItem, String> categories = new HashMap<IsTreeItem, String>();
    private final List<IsTreeItem> states = new ArrayList<IsTreeItem>();
    private IsTreeItem incomingInboxTreeItem;
    private IsTreeItem statesRootTreeItem;
    private IsTreeItem findRootTreeItem;
    private IsTreeItem inboxRecentlyEditedTreeItem;
    private IsTreeItem inboxRecentlyViewedTreeItem;

    public BrowseTree(BrowseTreeView view,
                      RepositoryServiceAsync repositoryService,
                      CategoryServiceAsync categoryService) {
        this.view = view;
        this.repositoryService = repositoryService;
        this.categoryService = categoryService;
        this.view.setPresenter(this);

        if (canShowMenu()) {
            this.view.showMenu();
        }
        this.view.addRootTreeItem();
        addInbox();
        findRootTreeItem = this.view.addFind();
        if (canShowStates()) {
            statesRootTreeItem = this.view.addRootStateTreeItem();
        }
        addRootCategory();
    }

    private void addInbox() {
        incomingInboxTreeItem = this.view.addInboxIncomingTreeItem();
        inboxRecentlyEditedTreeItem = this.view.addInboxRecentEditedTreeItem();
        inboxRecentlyViewedTreeItem = this.view.addInboxRecentViewedTreeItem();
    }

    private boolean canShowStates() {
        return UserCapabilities.INSTANCE.hasCapability(Capability.SHOW_KNOWLEDGE_BASES_VIEW);
    }

    private boolean canShowMenu() {
        return UserCapabilities.INSTANCE.hasCapability(Capability.SHOW_CREATE_NEW_ASSET);
    }

    private void addRootCategory() {
        IsTreeItem item = this.view.addRootCategoryTreeItem();
        categories.put(item, "/");
    }

    private void addCategoryItem(String categoryName, IsTreeItem treeItem) {
        IsTreeItem subItem = view.addTreeItem(treeItem, categoryName);
        String path = getItemPath(categoryName, categories.get(treeItem));
        categories.put(subItem, path);
    }

    private String getItemPath(String categoryName, String parentItemPath) {
        String path;
        if (isParentRoot(parentItemPath)) {
            path = parentItemPath + categoryName;
        } else {
            path = parentItemPath + "/" + categoryName;
        }
        return path;
    }

    private boolean isParentRoot(String parentItemPath) {
        return parentItemPath.equals("/");
    }

    private void addSubStatesToTreeItem() {
        view.removeStates();
        repositoryService.listStates(new GenericCallback<String[]>() {
            public void onSuccess(String[] result) {
                for (String name : result) {
                    IsTreeItem item = view.addStateItem(name);
                    states.add(item);
                }
            }
        });
    }

    public BrowseTreeView getView() {
        return view;
    }

    public void onTreeItemSelection(IsTreeItem selectedItem, String title) {
        TabManager tabManager = TabContainer.getInstance();
        if (!tabManager.showIfOpen(title)) {
            if (selectedItem.equals(statesRootTreeItem)) {
                addSubStatesToTreeItem();
            } else if (states.contains(selectedItem)) {
                tabManager.openStatePagedTable(title);
            } else if (categories.containsKey(selectedItem)) {
                String categoryPath = categories.get(selectedItem);
                tabManager.openCategory(title, categoryPath);
            } else if (selectedItem.equals(incomingInboxTreeItem)) {
                tabManager.openInboxIncomingPagedTable(title);
            } else if (selectedItem.equals(inboxRecentlyEditedTreeItem) || selectedItem.equals(inboxRecentlyViewedTreeItem)) {
                tabManager.openInboxPagedTable(title);
            }
        }
    }

    public void onTreeItemOpen(IsTreeItem target) {
        if (categories.containsKey(target)) {
            view.removeCategories(target);
            loadCategories(target);
        } else if (target.equals(findRootTreeItem)) {
            TabContainer.getInstance().openFind();
        }
    }

    private void loadCategories(final IsTreeItem treeItem) {
        String path = categories.get(treeItem);
        categoryService.loadChildCategories(path,
                new GenericCallback<String[]>() {
                    public void onSuccess(String[] result) {
                        for (String categoryName : result) {
                            addCategoryItem(categoryName, treeItem);
                        }
                    }
                });
    }

}