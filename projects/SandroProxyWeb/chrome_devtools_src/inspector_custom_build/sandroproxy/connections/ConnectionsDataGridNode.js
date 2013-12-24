WebInspector.ConnectionsDataGridNode = function(parentView, connection, timestamp)
{
    WebInspector.DataGridNode.call(this, {});
    this._parentView = parentView;
    this._connection = connection;
    this._timestamp = timestamp;
    this._linkifier = new WebInspector.Linkifier();
}

WebInspector.ConnectionsDataGridNode.prototype = {
    createCells: function()
    {
        // this._element.classList.add("offscreen");
        this._protocolCell = this._createDivInTD("protocol");
        this._stateCell = this._createDivInTD("state");
        this._laddressCell = this._createDivInTD("laddress");
        this._lportCell = this._createDivInTD("lport");
        this._raddressCell = this._createDivInTD("raddress");
        this._rportCell = this._createDivInTD("rport");
        this._rhostCell = this._createDivInTD("rhost");
        this._uidCell = this._createDivInTD("uid");
        this._processCell = this._createDivInTD("process");
        // this._processnameCell = this._createDivInTD("processname");
        // this._timestampCell = this._createDivInTD("timestamp");
        this._processCell.addEventListener("click", this._onClick.bind(this), false);
        // this._processCell.addEventListener("dblclick", this._openInNewTab.bind(this), false);
    },
    
    isFilteredOut: function()
    {
        if (this._parentView._filteredOutConnections.get(this._connection))
            return true;
        return !this._parentView._stateFilter(this._connection);
    },
    
    _onClick: function()
    {
    },
    
    select: function()
    {
        this._parentView.dispatchEventToListeners(WebInspector.ConnectionsView.EventTypes.ConnectionSelected, this._connection);
        WebInspector.DataGridNode.prototype.select.apply(this, arguments);
    },
    
    _createDivInTD: function(columnIdentifier)
    {
        var td = this.createTD(columnIdentifier);
        var div = td.createChild("div");
        this._element.appendChild(td);
        return div;
    },

    refreshConnection: function()
    {
        this._refreshProtocolCell();
        this._refreshLocalAddressCell();
        this._refreshLocalPortCell();
        this._refreshStateCell();
        this._refreshRemoteAddressCell();
        this._refreshRemotePortCell();
        this._refreshRemoteHostCell();
        this._refreshUidCell();
        this._refreshProcessCell();
        // this._refreshProcessNameCell();
        // this._refreshTimestampCell();

        this._element.classList.add("network-item");
        this._updateElementStyleClasses(this._element);
    },

    _updateElementStyleClasses: function(element)
    {
        var typeClassName = "network-type-other";
        element.classList.add(typeClassName);
    },

    
    _refreshProtocolCell: function()
    {
        this._protocolCell.removeChildren();
        this._protocolCell.appendChild(document.createTextNode(this._connection.type));
    },
    
    _refreshLocalAddressCell: function()
    {
        this._laddressCell.removeChildren();
        this._laddressCell.appendChild(document.createTextNode(this._connection.laddress));
    },

    _refreshLocalPortCell: function()
    {
        this._lportCell.removeChildren();
        this._lportCell.appendChild(document.createTextNode(this._connection.lport));
    },
    
    _refreshStateCell: function()
    {
        this._stateCell.removeChildren();
        var state = WebInspector.ConnectionsStates.InitFromCode(this._connection.statecode);
        this._stateCell.appendChild(document.createTextNode(state.getCodeShortDescription()));
    },

    _refreshRemoteAddressCell: function()
    {
        this._raddressCell.removeChildren();
        this._raddressCell.appendChild(document.createTextNode(this._connection.raddress));
    },

    _refreshRemotePortCell: function()
    {
        this._rportCell.removeChildren();
        this._rportCell.appendChild(document.createTextNode(this._connection.rport));
    },

    _refreshRemoteHostCell: function()
    {
        this._rhostCell.removeChildren();
        this._rhostCell.appendChild(document.createTextNode(this._connection.rhost));
    },

    _refreshUidCell: function()
    {
        this._uidCell.removeChildren();
        this._uidCell.appendChild(document.createTextNode(this._connection.uid));
    },

    _refreshProcessCell: function()
    {
        this._processCell.removeChildren();
        var haveMoreNamespaces = this._connection.namespaces && this._connection.namespaces.length > 0;
        if (haveMoreNamespaces){
            this._processCell.title = "";
            for (var i = 0; i <  this._connection.namespaces.length; i++){
                var namespace = this._connection.namespaces[i]; 
                this._processCell.title += namespace + " \u2758 "; 
            }
            this._processCell.appendChild(document.createTextNode(this._connection.namespace + " ..."));
        }else{
            this._processCell.appendChild(document.createTextNode(this._connection.namespace));
        }
    },
    
    _refreshProcessNameCell: function()
    {
        this._processnameCell.removeChildren();
        this._processnameCell.appendChild(document.createTextNode(this._connection.namespace));
        
    },
    
    _refreshTimestampCell: function()
    {
        this._timestampCell.removeChildren();
        this._timestampCell.appendChild(document.createTextNode(this._timestamp));
    },
    __proto__: WebInspector.DataGridNode.prototype
}

WebInspector.ConnectionsDataGridNode.ConnectionPropertyComparator = function(propertyName, revert, a, b)
{
    var aValue = a._connection[propertyName];
    var bValue = b._connection[propertyName];
    if (aValue > bValue)
        return revert ? -1 : 1;
    if (bValue > aValue)
        return revert ? 1 : -1;
    return 0;
}

//@ sourceURL=http://192.168.1.135/devtools/sandroproxy/connections/ConnectionsDataGridNode.js
//@ sourceURL=http://192.168.1.135/devtools/sandroproxy/connections/ConnectionsDataGridNode.js