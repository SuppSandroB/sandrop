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
WebInspector.ConnectionsView = function(coulmnsVisibilitySetting)
{
    WebInspector.View.call(this);
    this.element.classList.add("vbox", "fill");
    this.registerRequiredCSS("networkLogView.css");
    
    this._coulmnsVisibilitySetting = coulmnsVisibilitySetting;
    this._connections = [];
    this._lastTimestamp;
    this._lastConnectionGridNodeId = 0;
    this._connectionGridNodes = {};
    this._stateFilterElements = {};
    this._filteredOutConnections = new Map();
    this._matchedConnections = [];
    this._highlightedSubstringChanges = [];
    
    this._createStatusBarItems();
    this._createStatusbarButtons();
    this._linkifier = new WebInspector.Linkifier();
    
    this._initializeView();
    
    this._setLargerRows(false);
    this._updateColumns();
    this._onAutorefreshClicked();
    
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.SandroProxyConnectionsSnapshot, this._onNewConnectionsSnapshot, this);
    
}

WebInspector.ConnectionsView._defaultColumnsVisivility = {protocol: true, lport: true, state: true, raddress: true, rport: true, rhost: true, uid: true, process: true};
WebInspector.ConnectionsView._defaultRefreshDelay = 500;
WebInspector.ConnectionsView.STATE_ALL_TYPES = "all";

WebInspector.ConnectionsState = function(state, stateShortDescription, stateDescription)
{
    this._state = state;
    this._stateShortDescription = stateShortDescription;
    this._stateDescription = stateDescription;
}

WebInspector.ConnectionsState.prototype = {
    
    getCodeShortDescription: function()
    {
        return this._stateShortDescription;
    },
    
    getCodeDescription: function()
    {
        return this._stateDescription;
    },
    
    getCode: function()
    {
        return this._state;
    }
}
WebInspector.ConnectionsStates = {
    ESTABLISHED: new WebInspector.ConnectionsState(1,   "ESTABLISHED", "(both server and client) represents an open connection, data received can be delivered to the user. The normal state for the data transfer phase of the connection."),
    SYN_SENT:    new WebInspector.ConnectionsState(2,   "SYN_SENT", "(client) represents waiting for a matching connection request after having sent a connection request."),
    SYN_RECV:    new WebInspector.ConnectionsState(3,   "SYN_RECV", "(server) represents waiting for a confirming connection request acknowledgment after having both received and sent a connection request."),
    FIN_WAIT1:   new WebInspector.ConnectionsState(4,   "FIN_WAIT1", "(both server and client) represents waiting for a connection termination request from the remote TCP, or an acknowledgment of the connection termination request previously sent."),
    FIN_WAIT2:   new WebInspector.ConnectionsState(5,   "FIN_WAIT2", "(both server and client) represents waiting for a connection termination request from the remote TCP."),
    TIME_WAIT:   new WebInspector.ConnectionsState(6,   "TIME_WAIT", "(either server or client) represents waiting for enough time to pass to be sure the remote TCP received the acknowledgment of its connection termination request. [According to RFC 793 a connection can stay in TIME-WAIT for a maximum of four minutes known as a MSL (maximum segment lifetime).]"),
    CLOSE:       new WebInspector.ConnectionsState(7,   "CLOSE", "(both server and client) represents no connection state at all."),
    CLOSE_WAIT:  new WebInspector.ConnectionsState(8,   "CLOSE_WAIT", "(both server and client) represents waiting for a connection termination request from the local user."),
    LAST_ACK:    new WebInspector.ConnectionsState(9,   "LAST_ACK", "(both server and client) represents waiting for an acknowledgment of the connection termination request previously sent to the remote TCP (which includes an acknowledgment of its connection termination request)."),
    LISTEN:      new WebInspector.ConnectionsState(10,  "LISTEN", "(server) represents waiting for a connection request from any remote TCP and port."),
    CLOSING:     new WebInspector.ConnectionsState(11,  "CLOSING", "(both server and client) represents waiting for a connection termination request acknowledgment from the remote TCP."),
    CLOSED:      new WebInspector.ConnectionsState(127, "CLOSED", "(both server and client) represents no connection state at all."),
    UNKNOWN:     new WebInspector.ConnectionsState(-1,  "UNKNOWN", "unknown state code")
};

WebInspector.ConnectionsStates.InitFromCode = function(code){
    for (var state in WebInspector.ConnectionsStates){
        var stateCode = WebInspector.ConnectionsStates[state].getCode();
        if ( stateCode == code){
            return WebInspector.ConnectionsStates[state];
        }        
    }
    return WebInspector.ConnectionsStates.UNKNOWN;
};

WebInspector.ConnectionsView.STATE_GROUP_LISTEN = { 
    LISTEN : WebInspector.ConnectionsStates.LISTEN 
};

WebInspector.ConnectionsView.STATE_GROUP_ACTIVE = { 
    ESTABLISHED: WebInspector.ConnectionsStates.ESTABLISHED 
};

WebInspector.ConnectionsView.STATE_GROUP_HANDSHAKE = { 
    SYN_SENT : WebInspector.ConnectionsStates.SYN_SENT, 
    SYN_RECV: WebInspector.ConnectionsStates.SYN_RECV 
};

WebInspector.ConnectionsView.STATE_GROUP_CLOSING = { 
    FIN_WAIT1 : WebInspector.ConnectionsStates.FIN_WAIT1, 
    FIN_WAIT2: WebInspector.ConnectionsStates.FIN_WAIT2, 
    TIME_WAIT : WebInspector.ConnectionsStates.TIME_WAIT, 
    CLOSE : WebInspector.ConnectionsStates.CLOSE,
    CLOSE_WAIT: WebInspector.ConnectionsStates.CLOSE_WAIT,
    LAST_ACK: WebInspector.ConnectionsStates.LAST_ACK,
    CLOSING: WebInspector.ConnectionsStates.CLOSING,
    CLOSED: WebInspector.ConnectionsStates.CLOSED
};

WebInspector.ConnectionsView.STATE_GROUPS = { 
    LISTEN : { title:"LISTEN", item: WebInspector.ConnectionsView.STATE_GROUP_LISTEN }, 
    ESTABLISHED: { title:"ESTABLISHED", item: WebInspector.ConnectionsView.STATE_GROUP_ACTIVE }, 
    HANDSHAKE: { title:"HANDSHAKE", item: WebInspector.ConnectionsView.STATE_GROUP_HANDSHAKE },
    CLOSING:  { title:"CLOSING", item: WebInspector.ConnectionsView.STATE_GROUP_CLOSING }
};

WebInspector.ConnectionsView.prototype = {
    
    _onStopSendingConnSnapshots: function(error, result)
    {
       //TODO 
    },
    
    _onStartSendingConnSnapshots: function(error, result)
    {
        //TODO   
    },
    
    _initializeView: function()
    {
        this.element.id = "network-container";
        
        this._createSortingFunctions();
        this._createTable();
        this._createSummaryBar();
        this._toggleStateFilter(WebInspector.ConnectionsView.STATE_ALL_TYPES, false);
    },
    
    
    _onNewConnectionsSnapshot : function(event)
    {
        var data = event.data;
        this._lastTimestamp = data.timestamp;
        this._connections = data.connections;
        this._dataGrid.rootNode().removeChildren();
        this._connections.forEach(this._mergeConnection.bind(this));
        this._sortItems();
        this._filterConnections();
        this._updateOffscreenRows();
        this._updateSummaryBar();
    },
    
    _mergeConnection: function(connection)
    {
        var node = this._createConnectionGridNode(connection, this._lastTimestamp);
        this._dataGrid.rootNode().appendChild(node);
        node.refreshConnection();
    },

    
    get statusBarItems()
    {
        return [this._autorefreshToggle.element, this._filterBarElement, this._progressBarContainer];
    },

    
    get useLargeRows()
    {
        return WebInspector.settings.resourcesLargeRows.get();
    },

    _createTable: function()
    {
        var columns = [];
        columns.push({
            id: "protocol", 
            title: WebInspector.UIString("Protocol"),
            sortable: true,
            weight: 6,
        });
        
        columns.push({
            id: "state",
            title: WebInspector.UIString("State"),
            sortable: true,
            weight: 6
        });

        columns.push({
            id: "laddress",
            title: WebInspector.UIString("Local Address"),
            sortable: true,
            weight: 6
        });

        columns.push({
            id: "lport",
            title: WebInspector.UIString("Local Port"),
            sortable: true,
            weight: 6
        });

        columns.push({
            id: "raddress",
            title: WebInspector.UIString("Remote Address"),
            sortable: true,
            weight: 6
        });

        columns.push({
            id: "rport",
            title: WebInspector.UIString("Remote Port"),
            sortable: true,
            weight: 6
        });

        columns.push({
            id: "rhost",
            title: WebInspector.UIString("Remote Host"),
            sortable: true,
            weight: 6
        });

        columns.push({
            id: "uid",
            title: WebInspector.UIString("Uid"),
            sortable: true,
            weight: 6,
            align: WebInspector.DataGrid.Align.Right
        });

        columns.push({
            id: "process",
            title: WebInspector.UIString("Process"),
            sortable: true,
            weight: 6,
            align: WebInspector.DataGrid.Align.Right
        });
        
        /*
        columns.push({
            id: "processname",
            title: WebInspector.UIString("Process Name"),
            sortable: true,
            weight: 6,
            align: WebInspector.DataGrid.Align.Right
        });
        */
        /*
        columns.push({
            id: "timestamp",
            title: WebInspector.UIString("Timestamp"),
            sortable: true,
            weight: 6,
            align: WebInspector.DataGrid.Align.Right
        });
        */

        
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
        this._sortingFunctions.protocol = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "type", false);
        this._sortingFunctions.state = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "statecode", false);
        this._sortingFunctions.laddress = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "laddress", false);
        this._sortingFunctions.lport = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "lport", false);
        this._sortingFunctions.raddress = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "raddress", false);
        this._sortingFunctions.rport = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "rport", false);
        this._sortingFunctions.rhost = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "rhost", false);
        this._sortingFunctions.uid = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "uid", false);
        this._sortingFunctions.process = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "namespace", false);
        // this._sortingFunctions.processname = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "namespace", false);
        // this._sortingFunctions.timestamp = WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator.bind(null, "timestamp", false);
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

    _addStateFilter: function(stateName, label)
        {
        var stateFilterElement = this._filterBarElement.createChild("li", stateName);
        stateFilterElement.stateName = stateName;
        stateFilterElement.createTextChild(label);
        stateFilterElement.addEventListener("click", this._onStateFilterClicked.bind(this), false);
        this._stateFilterElements[stateName] = stateFilterElement;
    },
    
    _createStatusBarItems: function()
    {
        var filterBarElement = document.createElement("div");
        filterBarElement.className = "scope-bar status-bar-item";
        filterBarElement.title = WebInspector.UIString("Use %s Click to select multiple types.", WebInspector.KeyboardShortcut.shortcutToString("", WebInspector.KeyboardShortcut.Modifiers.CtrlOrMeta));
        this._filterBarElement = filterBarElement;

        this._addStateFilter(WebInspector.ConnectionsView.STATE_ALL_TYPES, WebInspector.UIString("All"));
        filterBarElement.createChild("div", "scope-bar-divider");

        for (var stateGroupId in WebInspector.ConnectionsView.STATE_GROUPS) {
            var stateGroupType = WebInspector.ConnectionsView.STATE_GROUPS[stateGroupId];
            this._addStateFilter(stateGroupType.title, stateGroupType.title);
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
        var connectionsNumber = this._connections.length;

        if (!connectionsNumber) {
            if (this._summaryBarElement._isDisplayingWarning)
                return;
            this._summaryBarElement._isDisplayingWarning = true;

            this._summaryBarElement.createChild("div", "warning-icon-small");
            this._summaryBarElement.appendChild(document.createTextNode(
                WebInspector.UIString("No connections. Maybe SandroProxy web is not active.")));
            return;
        }
        delete this._summaryBarElement._isDisplayingWarning;
        var text = "";
        text += String.sprintf(WebInspector.UIString("%d connections"), connectionsNumber);
        text += " \u2758 " + String.sprintf(WebInspector.UIString("Last update:%s"), new Date());
        this._summaryBarElement.textContent = text;
    },

    _onStateFilterClicked: function(e)
    {
        var toggle;
        if (WebInspector.isMac())
            toggle = e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey;
        else
            toggle = e.ctrlKey && !e.metaKey && !e.altKey && !e.shiftKey;

        this._toggleStateFilter(e.target.stateName, toggle);

        this._removeAllNodeHighlights();
        this.searchCanceled();
        this._filterConnections();
    },

    _toggleStateFilter: function(stateName, allowMultiSelect)
    {
        if (allowMultiSelect && stateName !== WebInspector.ConnectionsView.STATE_ALL_TYPES)
            this._stateFilterElements[WebInspector.ConnectionsView.STATE_ALL_TYPES].classList.remove("selected");
        else {
            for (var key in this._stateFilterElements)
                this._stateFilterElements[key].classList.remove("selected");
        }

        var filterElement = this._stateFilterElements[stateName];
        filterElement.enableStyleClass("selected", !filterElement.classList.contains("selected"));
        // We can't unselect All, so we break early here
        var allowedStatesGroups = {};
        for (var key in this._stateFilterElements) {
            if (this._stateFilterElements[key].classList.contains("selected"))
                allowedStatesGroups[key] = true;
            }

        // If All wasn't selected, and now is, unselect everything else.
        if (stateName === WebInspector.ConnectionsView.STATE_ALL_TYPES)
            this._stateFilter = WebInspector.ConnectionsView._trivialStateFilter;
        else
            this._stateFilter = WebInspector.ConnectionsView._stateFilter.bind(null, allowedStatesGroups);
    },

    
    _scheduleRefresh: function()
    {
        if (this._needsRefresh)
            return;

        this._needsRefresh = true;

        if (this.isShowing() && !this._refreshTimeout)
            this._refreshTimeout = setTimeout(this.refresh.bind(this), WebInspector.ConnectionsView._defaultRefreshDelay);
    },
    
    _createConnectionGridNode: function(connection, timestamp)
    {
        var node = new WebInspector.ConnectionsDataGridNode(this, connection, timestamp);
        connection.__gridNodeId = this._lastConnectionGridNodeId++;
        this._connectionGridNodes[connection.__gridNodeId] = node;
        return node;
    },

    
    _createStatusbarButtons: function()
    {
        this._autorefreshToggle = new WebInspector.StatusBarButton(WebInspector.UIString("Autorefresh connections"), "record-profile-status-bar-item");
        this._autorefreshToggle.addEventListener("click", this._onAutorefreshClicked, this);
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

    
    _onAutorefreshClicked: function(e)
    {
        this._autorefreshToggle.toggled = !this._autorefreshToggle.toggled;
        if (this._autorefreshToggle.toggled){
            NetworkAgent.sandroProxyStartSendingConnSnapshots("1000", this._onStartSendingConnSnapshots.bind(this));
        }else{
            NetworkAgent.sandroProxyStopSendingConnSnapshots(this._onStopSendingConnSnapshots.bind(this));
        }
    },

    _mainFrameNavigated: function(event)
    {
    },
    
    _setLargerRows: function(enabled)
    {
        if (!enabled) {
            this._dataGrid.element.classList.add("small");
        } else {
            this._dataGrid.element.classList.remove("small");
        }
        this.dispatchEventToListeners(WebInspector.ConnectionsView.EventTypes.RowSizeChanged, { largeRows: enabled });
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
        var connection = gridNode && gridNode._connection;
        if (connection) {
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
                row.classList.remove("offscreen");
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
        var connection = node._connection;
        var matches = false;
        if (this._stateFilter(connection)) {
            matches = !filter || filter.test(connection.state);
            if (filter && matches)
            this._highlightMatchedRequest(request, false, filter);
        }
        if (!matches){
            node.element.classList.add("filtered-out");
            this._filteredOutConnections.put(connection, true);
        }else{
            if(node.element.classList.contains("filtered-out"))
                node.element.classList.remove("filtered-out");
        }
            
    },
    
    performFilter: function(query)
    {
        delete this._filterRegExp;
        if (query)
            this._filterRegExp = createPlainTextSearchRegex(query, "i");
        this._filterRequests();
    },
    

    _filterConnections: function()
    {
        this._removeAllHighlights();
        this._filteredOutConnections.clear();

        var nodes = this._dataGrid.rootNode().children;
        for (var i = 0; i < nodes.length; ++i)
            this._applyFilter(nodes[i]);
        this._updateSummaryBar();
        this._updateOffscreenRows();
    },
    
    searchCanceled: function()
    {
        this._clearSearchMatchedList();
        this.dispatchEventToListeners(WebInspector.ConnectionsView.EventTypes.SearchCountUpdated, 0);
    },
    

    _removeAllNodeHighlights: function()
    {
        if (this._highlightedNode) {
            this._highlightedNode.element.classList.remove("highlighted-row");
            delete this._highlightedNode;
        }
    },
    
    _highlightNode: function(node)
    {
        node.element.classList.add("highlighted-row");
        this._highlightedNode = node;
    },

    __proto__: WebInspector.View.prototype
}


WebInspector.ConnectionsView.EventTypes = {
    ViewCleared: "ViewCleared",
    RowSizeChanged: "RowSizeChanged",
    ConnectionSelected: "ConnectionSelected",
    SearchCountUpdated: "SearchCountUpdated",
    SearchIndexUpdated: "SearchIndexUpdated"
};

WebInspector.ConnectionsView._trivialStateFilter = function(connection)
{
    return true;
}

WebInspector.ConnectionsView._stateFilter = function(allowedStatesGroups, connection)
{
    var connectionState = connection.statecode;
    var statesGroupTitle;
    var stateCodeFound = false; 
    for (var stateGroupKey in WebInspector.ConnectionsView.STATE_GROUPS){
        statesGroupTitle = WebInspector.ConnectionsView.STATE_GROUPS[stateGroupKey].title;
        var statesGroupItem = WebInspector.ConnectionsView.STATE_GROUPS[stateGroupKey].item;
        for (var stateGroupItemKey in statesGroupItem){
            var stateGroupItem = statesGroupItem[stateGroupItemKey];
            var itemStateCode = stateGroupItem.getCode();
            if (itemStateCode == connectionState){
                stateCodeFound = true;
                break;
            }
        }
        if (stateCodeFound){
            break;
        }
    }
    if (stateCodeFound){
        return statesGroupTitle in allowedStatesGroups;
    }
    return false;
}

//# sourceURL=http://192.168.1.135/devtools/sandroproxy/connections/ConnectionsView.js