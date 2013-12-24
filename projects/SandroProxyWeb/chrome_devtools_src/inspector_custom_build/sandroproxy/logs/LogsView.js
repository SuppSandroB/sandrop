/*
 * Copyright (C) 2013, SandroProxy
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @constructor
 * @extends {WebInspector.View}
 * @param {WebInspector.Setting} coulmnsVisibilitySetting
 */
WebInspector.LogsView = function(coulmnsVisibilitySetting)
{
    WebInspector.View.call(this);
    this.element.classList.add("vbox", "fill");
    this.registerRequiredCSS("networkLogView.css");
    
    this._coulmnsVisibilitySetting = coulmnsVisibilitySetting;
    this._logs = [];
    this._lastTimestamp;
    this._lastLogGridNodeId = 0;
    this._logGridNodes = [];
    this._levelFilterElements = {};
    this._filteredOutLogs = new Map();
    this._matchedConnections = [];
    this._highlightedSubstringChanges = [];
    
    this._createStatusBarItems();
    this._createStatusbarButtons();
    this._linkifier = new WebInspector.Linkifier();
    
    this._initializeView();
    
    this._setLargerRows(false);
    this._updateColumns();
    this._onAutorefreshClicked();
    
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.SandroProxyLogsFetch, this._onNewLogsFetch, this);
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.SandroProxyLogMessage, this._onNewLogMessage, this);
}

WebInspector.LogsView._defaultColumnsVisivility = {level: true, time: true, pid: true, tid: true, tag: true, text: true};
WebInspector.LogsView._defaultRefreshDelay = 500;
WebInspector.LogsView.LEVEL_VERBOSE = "Verbose";

WebInspector.LogLevel = function(level, levelShortDescription, levelDescription)
{
    this._level = level;
    this._levelShortDescription = levelShortDescription;
    this._levelDescription = levelDescription;
}

WebInspector.LogLevel.prototype = {
    
    getLevelShortDescription: function()
    {
        return this._levelShortDescription;
    },
    
    getLevelDescription: function()
    {
        return this._levelDescription;
    },
    
    getLevel: function()
    {
        return this._level;
    }
}
WebInspector.LogsView.LOG_LEVELS = {
    DEBUG: new WebInspector.LogLevel("D",   "DEBUG", "DEBUG"),
    INFO:    new WebInspector.LogLevel("I",   "INFO", "INFO"),
    WARN:    new WebInspector.LogLevel("W",   "WARN", "WARN"),
    ERROR:   new WebInspector.LogLevel("E",   "ERROR", "ERROR"),
};

WebInspector.LogsView.prototype = {
    
    _onStopSendingLogsFetches: function(error, result)
    {
       //TODO 
    },
    
    _onStartSendingLogsFetches: function(error, result)
    {
        //TODO   
    },
    
    _initializeView: function()
    {
        this.element.id = "network-container";
        
        this._createSortingFunctions();
        this._createTable();
        this._createSummaryBar();
        this._toggleLevelFilter(WebInspector.LogsView.LEVEL_VERBOSE, false);
    },
    
    
    _onNewLogsFetch : function(event)
    {
        var data = event.data;
        // this._dataGrid.rootNode().removeChildren();
        data.forEach(this._mergeLog.bind(this));
        this._sortItems();
        this._filterLogs();
        this._updateOffscreenRows();
        this._updateSummaryBar();
    },
    
    _onNewLogMessage : function(event)
    {
        var data = event.data;
        // this._dataGrid.rootNode().removeChildren();
        this._logs.forEach(this._mergeLog.bind(this));
        this._sortItems();
        this._filterLogs();
        this._updateOffscreenRows();
        this._updateSummaryBar();
    },
    
    _mergeLog: function(log)
    {
        var nrToRemove = 500;
        if (this._logs.length > 1000){
            for (var i=0; i < nrToRemove; i++){
               var node = this._logGridNodes.slice(0,1)[0];
               this._dataGrid.rootNode().removeChild(node);
               this._logGridNodes.shift();
               this._logs.shift();
           }
        }
        this._logs.push(log);
        var node = this._createLogEntryGridNode(log);
        this._dataGrid.rootNode().appendChild(node);
        node.refreshLogEntry();
    },

    
    get statusBarItems()
    {
        return [ this._clearButton.element, this._autorefreshToggle.element, this._filterBarElement, this._progressBarContainer];
    },

    
    get useLargeRows()
    {
        return WebInspector.settings.resourcesLargeRows.get();
    },

    _createTable: function()
    {
        var columns = [];
        columns.push({
            id: "level", 
            title: WebInspector.UIString("Level"),
            sortable: true,
            weight: 1,
        });
        
        columns.push({
            id: "time",
            title: WebInspector.UIString("Time"),
            sortable: true,
            weight: 3
        });

        columns.push({
            id: "pid",
            title: WebInspector.UIString("Pid"),
            sortable: true,
            weight: 3
        });

        columns.push({
            id: "tid",
            title: WebInspector.UIString("Tid"),
            sortable: true,
            weight: 3
        });

        columns.push({
            id: "tag",
            title: WebInspector.UIString("Tag"),
            sortable: true,
            weight: 3
        });

        columns.push({
            id: "text",
            title: WebInspector.UIString("Text"),
            sortable: true,
            weight: 6
        });

        
        this._dataGrid = new WebInspector.DataGrid(columns);
        this._dataGrid.setName("connections");
        this._dataGrid.resizeMethod = WebInspector.DataGrid.ResizeMethod.Last;
        this._dataGrid.element.classList.add("network-log-grid");
        this._dataGrid.element.addEventListener("contextmenu", this._contextMenu.bind(this), true);
        this._dataGrid.show(this.element);

        this._dataGrid.addEventListener(WebInspector.DataGrid.Events.SortingChanged, this._sortItems, this);
        this._dataGrid.scrollContainer.addEventListener("scroll", this._updateOffscreenRows.bind(this));
        
    },

    _createSortingFunctions: function()
    {
        this._sortingFunctions = {};
        this._sortingFunctions.level = WebInspector.LogsDataGridNode.LogEntryPropertyComparator.bind(null, "level", false);
        this._sortingFunctions.time = WebInspector.LogsDataGridNode.LogEntryPropertyComparator.bind(null, "time", false);
        this._sortingFunctions.pid = WebInspector.LogsDataGridNode.LogEntryPropertyComparator.bind(null, "pid", false);
        this._sortingFunctions.tid = WebInspector.LogsDataGridNode.LogEntryPropertyComparator.bind(null, "tid", false);
        this._sortingFunctions.tag = WebInspector.LogsDataGridNode.LogEntryPropertyComparator.bind(null, "tag", false);
        this._sortingFunctions.text = WebInspector.LogsDataGridNode.LogEntryPropertyComparator.bind(null, "text", false);
    },
    
    _sortItems: function()
    {
        this._removeAllNodeHighlights();
        var columnIdentifier = this._dataGrid.sortColumnIdentifier();
        var sortingFunction = this._sortingFunctions[columnIdentifier];
        if (!sortingFunction)
            return;

        this._dataGrid.sortNodes(sortingFunction, !this._dataGrid.isSortOrderAscending());
        this._updateOffscreenRows();

        // this.searchCanceled();

        WebInspector.notifications.dispatchEventToListeners(WebInspector.UserMetrics.UserAction, {
            action: WebInspector.UserMetrics.UserActionNames.NetworkSort,
            column: columnIdentifier,
            sortOrder: this._dataGrid.sortOrder()
        });
    },

    _addLevelFilter: function(levelName, label)
        {
        var levelFilterElement = this._filterBarElement.createChild("li", levelName);
        levelFilterElement.levelName = levelName;
        levelFilterElement.createTextChild(label);
        levelFilterElement.addEventListener("click", this._onLevelFilterClicked.bind(this), false);
        this._levelFilterElements[levelName] = levelFilterElement;
    },
    
    _createStatusBarItems: function()
    {
        var filterBarElement = document.createElement("div");
        filterBarElement.className = "scope-bar status-bar-item";
        filterBarElement.title = WebInspector.UIString("Use %s Click to select multiple levels.", WebInspector.KeyboardShortcut.shortcutToString("", WebInspector.KeyboardShortcut.Modifiers.CtrlOrMeta));
        this._filterBarElement = filterBarElement;

        this._addLevelFilter(WebInspector.LogsView.LEVEL_VERBOSE, WebInspector.UIString("Verbose"));
        filterBarElement.createChild("div", "scope-bar-divider");

        for (var logLevelId in WebInspector.LogsView.LOG_LEVELS) { 
            var levelType = WebInspector.LogsView.LOG_LEVELS[logLevelId];
            var val1 = levelType._levelShortDescription;
            this._addLevelFilter(val1, val1);
        }
        this._progressBarContainer = document.createElement("div");
        this._progressBarContainer.className = "status-bar-item";
    },
    
    
    _createSummaryBar: function()
    {
        var tbody = this._dataGrid.dataTableBody;
        var tfoot = document.createElement("tfoot");
        var tr = tfoot.createChild("tr", "revealed network-summary-bar");
        var td = tr.createChild("td");
        td.setAttribute("colspan", 7);
        tbody.parentNode.insertBefore(tfoot, tbody);
        this._summaryBarElement = td;
    },
    
    _updateSummaryBar: function()
    {
        var logsNumber = this._logs.length;

        if (!logsNumber) {
            if (this._summaryBarElement._isDisplayingWarning)
                return;
            this._summaryBarElement._isDisplayingWarning = true;

            this._summaryBarElement.createChild("div", "warning-icon-small");
            this._summaryBarElement.appendChild(document.createTextNode(
                WebInspector.UIString("No logcat entries. Maybe SandroProxy web is not active.")));
            return;
        }
        delete this._summaryBarElement._isDisplayingWarning;
        var text = "";
        text += String.sprintf(WebInspector.UIString("%d log entries"), logsNumber);
        text += " \u2758 " + String.sprintf(WebInspector.UIString("Last update:%s"), new Date());
        this._summaryBarElement.textContent = text;
    },

    _onLevelFilterClicked: function(e)
    {
        var toggle;
        if (WebInspector.isMac())
            toggle = e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey;
        else
            toggle = e.ctrlKey && !e.metaKey && !e.altKey && !e.shiftKey;

        this._toggleLevelFilter(e.target.levelName, toggle);

        this._removeAllNodeHighlights();
        this.searchCanceled();
        this._filterLogs();
    },

    _toggleLevelFilter: function(levelName, allowMultiSelect)
    {
        if (allowMultiSelect && levelName !== WebInspector.LogsView.LEVEL_VERBOSE)
            this._levelFilterElements[WebInspector.LogsView.LEVEL_VERBOSE].removeStyleClass("selected");
        else {
            for (var key in this._levelFilterElements)
                this._levelFilterElements[key].removeStyleClass("selected");
        }

        var filterElement = this._levelFilterElements[levelName];
        filterElement.classList.add("selected", !filterElement.classList.contains("selected"));
        // We can't unselect All, so we break early here
        var allowedLevelsGroups = {};
        for (var key in this._levelFilterElements) {
            if (this._levelFilterElements[key].classList.contains("selected"))
                allowedLevelsGroups[key] = true;
            }

        // If All wasn't selected, and now is, unselect everything else.
        if (levelName === WebInspector.LogsView.LEVEL_VERBOSE)
            this._levelFilter = WebInspector.LogsView._trivialStateFilter;
        else
            this._levelFilter = WebInspector.LogsView._levelFilter.bind(null, allowedLevelsGroups);
    },

    
    _scheduleRefresh: function()
    {
        if (this._needsRefresh)
            return;

        this._needsRefresh = true;

        if (this.isShowing() && !this._refreshTimeout)
            this._refreshTimeout = setTimeout(this.refresh.bind(this), WebInspector.LogsView._defaultRefreshDelay);
    },
    
    _createLogEntryGridNode: function(log)
    {
        var node = new WebInspector.LogsDataGridNode(this, log);
        log.__gridNodeId = this._lastLogGridNodeId++;
        this._logGridNodes.push(node);
        return node;
    },

    
    _createStatusbarButtons: function()
    {
        this._autorefreshToggle = new WebInspector.StatusBarButton(WebInspector.UIString("Capture logcat"), "record-profile-status-bar-item");
        this._autorefreshToggle.addEventListener("click", this._onAutorefreshClicked, this);
        
        this._clearButton = new WebInspector.StatusBarButton(WebInspector.UIString("Clear"), "clear-status-bar-item");
        this._clearButton.addEventListener("click", this._clear, this);
    },
    

    
    _onLoadEventFired: function(event)
    {
        this._mainRequestLoadTime = event.data || -1;
        // Schedule refresh to update boundaries and draw the new line.
        this._scheduleRefresh();
    },
    
    _domContentLoadedEventFired: function(event)
    {
    },
    

    refresh: function()
    {
        this._needsRefresh = false;
        if (this._refreshTimeout) {
            clearTimeout(this._refreshTimeout);
            delete this._refreshTimeout;
        }
    },
    
    _clear: function()
    {

        this._logs = [];
        this._lastTimestamp;
        this._lastLogGridNodeId = 0;
        this._logGridNodes = [];
        this._levelFilterElements = {};
        this._filteredOutLogs = new Map();
        this._matchedConnections = [];
        this._highlightedSubstringChanges = [];

        if (this._dataGrid) {
            this._dataGrid.rootNode().removeChildren();
            this._updateSummaryBar();
        }
    },

    
    _onAutorefreshClicked: function(e)
    {
        this._autorefreshToggle.toggled = !this._autorefreshToggle.toggled;
        if (this._autorefreshToggle.toggled){
            NetworkAgent.sandroProxyStartSendingLogs("1000", this._onStartSendingLogsFetches.bind(this));
        }else{
            NetworkAgent.sandroProxyStopSendingLogs(this._onStopSendingLogsFetches.bind(this));
        }
    },

    _mainFrameNavigated: function(event)
    {
    },
    
    _setLargerRows: function(enabled)
    {
        if (!enabled) {
            this._dataGrid.element.addStyleClass("small");
        } else {
            this._dataGrid.element.removeStyleClass("small");
        }
        this.dispatchEventToListeners(WebInspector.LogsView.EventTypes.RowSizeChanged, { largeRows: enabled });
        this._updateOffscreenRows();
    },
    
    _updateColumns: function()
    {
        var columnsVisibility = this._coulmnsVisibilitySetting;
        for (var columnIdentifier in columnsVisibility) {
            var visible = columnsVisibility[columnIdentifier];
            this._dataGrid.setColumnVisible(columnIdentifier, visible);
        }
        this._dataGrid.applyColumnWeights();
    },
    
    _contextMenu: function(event)
    {
        var contextMenu = new WebInspector.ContextMenu(event);

        var gridNode = this._dataGrid.dataGridNodeFromNode(event.target);
        var log = gridNode && gridNode._logEntry;
        if (log) {
            // TODO we can have some exports...
        }
    },

    _updateOffscreenRows: function()
    {
        var dataTableBody = this._dataGrid.dataTableBody;
        var rows = dataTableBody.children;
        var recordsCount = rows.length;
        if (recordsCount < 2)
            return;  // Filler row only.

        var visibleTop = this._dataGrid.scrollContainer.scrollTop;
        var visibleBottom = visibleTop + this._dataGrid.scrollContainer.offsetHeight;

        var rowHeight = 0;

        // Filler is at recordsCount - 1.
        var unfilteredRowIndex = 0;
        for (var i = 0; i < recordsCount - 1; ++i) {
            var row = rows[i];

            var dataGridNode = this._dataGrid.dataGridNodeFromNode(row);
            if (dataGridNode.isFilteredOut()) {
                row.removeStyleClass("offscreen");
                continue;
            }

            if (!rowHeight)
                rowHeight = row.offsetHeight;

            var rowIsVisible = unfilteredRowIndex * rowHeight < visibleBottom && (unfilteredRowIndex + 1) * rowHeight > visibleTop;
            if (rowIsVisible !== row.rowIsVisible) {
                row.enableStyleClass("offscreen", !rowIsVisible);
                row.rowIsVisible = rowIsVisible;
            }
            unfilteredRowIndex++;
        }
    },

    _clearSearchMatchedList: function()
    {
        delete this._searchRegExp;
        this._matchedConnections = [];
        this._matchedConnectionsMap = {};
        this._removeAllHighlights();
    },

    _removeAllHighlights: function()
    {
        for (var i = 0; i < this._highlightedSubstringChanges.length; ++i)
            WebInspector.revertDomChanges(this._highlightedSubstringChanges[i]);
        this._highlightedSubstringChanges = [];
    },

    _applyFilter: function(node)
    {
        var filter = this._filterRegExp;
        var log = node._logEntry;
        var matches = false;
        if (this._levelFilter(log)) {
            matches = !filter || filter.test(log.state);
            if (filter && matches)
            this._highlightMatchedRequest(request, false, filter);
        }
        node.element.enableStyleClass("filtered-out", !matches);
        if (!matches)
            this._filteredOutLogs.put(log, true);
    },
    
    performFilter: function(query)
    {
        delete this._filterRegExp;
        if (query)
            this._filterRegExp = createPlainTextSearchRegex(query, "i");
        this._filterRequests();
    },
    

    _filterLogs: function()
    {
        this._removeAllHighlights();
        this._filteredOutLogs.clear();

        var nodes = this._dataGrid.rootNode().children;
        for (var i = 0; i < nodes.length; ++i)
            this._applyFilter(nodes[i]);
        this._updateSummaryBar();
        this._updateOffscreenRows();
    },
    
    searchCanceled: function()
    {
        this._clearSearchMatchedList();
        this.dispatchEventToListeners(WebInspector.LogsView.EventTypes.SearchCountUpdated, 0);
    },
    

    _removeAllNodeHighlights: function()
    {
        if (this._highlightedNode) {
            this._highlightedNode.element.removeStyleClass("highlighted-row");
            delete this._highlightedNode;
        }
    },
    
    _highlightNode: function(node)
    {
        node.element.addStyleClass("highlighted-row");
        this._highlightedNode = node;
    },

    __proto__: WebInspector.View.prototype
}


WebInspector.LogsView.EventTypes = {
    ViewCleared: "ViewCleared",
    RowSizeChanged: "RowSizeChanged",
    LogSelected: "LogSelected",
    SearchCountUpdated: "SearchCountUpdated",
    SearchIndexUpdated: "SearchIndexUpdated"
};

WebInspector.LogsView._trivialStateFilter = function(logEntry)
{
    return true;
}

WebInspector.LogsView._levelFilter = function(allowedLevelsGroups, logEntry)
{
    var logLevel = logEntry.level;
    for (var logLevelKey in WebInspector.LogsView.LOG_LEVELS){
        var levelCode = WebInspector.LogsView.LOG_LEVELS[logLevelKey]._level;
        var levelShortDesc = WebInspector.LogsView.LOG_LEVELS[logLevelKey]._levelShortDescription;
        if (levelCode == logLevel){
            var match = levelShortDesc in allowedLevelsGroups;
            return match;
        }
    }
    return false;
}
 //# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsView.js
//# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsView.js
//# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsView.js
//# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsView.js
//# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsView.js
//# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsView.js