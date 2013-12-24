WebInspector.LogsDataGridNode = function(parentView, logEntry)
{
    WebInspector.DataGridNode.call(this, {});
    this._parentView = parentView;
    this._logEntry = logEntry;
    this._linkifier = new WebInspector.Linkifier();
}

WebInspector.LogsDataGridNode.prototype = {
    createCells: function()
    {
        this._element.addStyleClass("offscreen");
        this._levelCell = this._createDivInTD("level");
        this._timeCell = this._createDivInTD("time");
        this._pidCell = this._createDivInTD("pid");
        this._tidCell = this._createDivInTD("tid");
        this._tagCell = this._createDivInTD("tag");
        this._textCell = this._createDivInTD("text");
        this._textCell.addEventListener("click", this._onClick.bind(this), false);
        // this._processCell.addEventListener("dblclick", this._openInNewTab.bind(this), false);
    },
    
    isFilteredOut: function()
    {
        if (this._parentView._filteredOutLogs.get(this._logEntry))
            return true;
        return !this._parentView._levelFilter(this._logEntry);
    },
    
    _onClick: function()
    {
    },
    
    select: function()
    {
        this._parentView.dispatchEventToListeners(WebInspector.LogsView.EventTypes.ConnectionSelected, this._logEntry);
        WebInspector.DataGridNode.prototype.select.apply(this, arguments);
    },
    
    _createDivInTD: function(columnIdentifier)
    {
        var td = this.createTD(columnIdentifier);
        var div = td.createChild("div");
        this._element.appendChild(td);
        return div;
    },

    refreshLogEntry: function()
    {
        this._refreshLevelCell();
        this._refreshTimeCell();
        this._refreshPidCell();
        this._refreshTidCell();
        this._refreshTagCell();
        this._refreshTextCell();

        this._element.addStyleClass("network-item");
        this._updateElementStyleClasses(this._element);
    },

    _updateElementStyleClasses: function(element)
    {
        var typeClassName = "network-type-other";
        element.addStyleClass(typeClassName);
    },

    
    _refreshLevelCell: function()
    {
        this._levelCell.removeChildren();
        this._levelCell.appendChild(document.createTextNode(this._logEntry.level));
    },
    
    _refreshTimeCell: function()
    {
        this._timeCell.removeChildren();
        this._timeCell.appendChild(document.createTextNode(this._logEntry.time));
    },

    _refreshPidCell: function()
    {
        this._pidCell.removeChildren();
        this._pidCell.appendChild(document.createTextNode(this._logEntry.pid));
    },
    
    _refreshTidCell: function()
    {
        this._tidCell.removeChildren();
        this._tidCell.appendChild(document.createTextNode(this._logEntry.tid));
    },

    _refreshTagCell: function()
    {
        this._tagCell.removeChildren();
        this._tagCell.appendChild(document.createTextNode(this._logEntry.tag));
    },

    _refreshTextCell: function()
    {
        this._textCell.removeChildren();
        this._textCell.appendChild(document.createTextNode(this._logEntry.text));
    },
    __proto__: WebInspector.DataGridNode.prototype
}

WebInspector.LogsDataGridNode.LogEntryPropertyComparator = function(propertyName, revert, a, b)
{
    var aValue = a._logEntry[propertyName];
    var bValue = b._logEntry[propertyName];
    if (aValue > bValue)
        return revert ? -1 : 1;
    if (bValue > aValue)
        return revert ? 1 : -1;
    return 0;
}
//# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsDataGridNode.js
//# sourceURL=http://192.168.1.135/devtools/sandroproxy/logs/LogsDataGridNode.js