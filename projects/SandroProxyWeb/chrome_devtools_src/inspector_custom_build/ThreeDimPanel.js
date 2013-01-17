/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) 2008, 2009 Anthony Ricaud <rik@webkit.org>
 * Copyright (C) 2011 Google Inc. All rights reserved.
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
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
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
 
importScript("three/three.js");
importScript("three/libs/tween.js");
importScript("three/controls/TrackballControls.js");
importScript("three/renderers/CSS3DRenderer.js");

// three data interaction
var _threeCamera, _threeeScene, _threeRenderer;
var _threeControls;
var _threeSelectedType = "Table";

var _threeObjects = [];
var _threeTargets = { table: [], sphere: [], helix: [], grid: [] };


function _threeOnWindowResize(){
    _threeCamera.aspect = window.innerWidth / window.innerHeight;
    _threeCamera.updateProjectionMatrix();
    _threeRenderer.setSize( window.innerWidth, window.innerHeight );
}

function _threeAnimate(){
    requestAnimationFrame( _threeAnimate );
    TWEEN.update();
    _threeControls.update();
}
    
function _threeRender(){
    _threeRenderer.render(_threeScene, _threeCamera );
}

WebInspector.ThreeDimView = function(){

    WebInspector.View.call(this);
    this.registerRequiredCSS("three/threeDimView.css");
    
    // chrome data interaction
    this._allowRequestSelection = false;
    this._viewInitialised = false;
    this._requests = [];
    this._requestsById = {};
    this._requestsByURL = {};
    this._staleRequests = {};
    this._requestGridNodes = {};
    this._lastRequestGridNodeId = 0;
    this._mainRequestLoadTime = -1;
    this._mainRequestDOMContentTime = -1;
    this._hiddenCategories = {};
    this._matchedRequests = [];
    this._highlightedSubstringChanges = [];
    this._filteredOutRequests = new Map();
    
    this._matchedRequestsMap = {};
    this._currentMatchedRequestIndex = -1;

    this._createThreeTypeBarItems();
    this._createStatusBarItems();
    this._linkifier = new WebInspector.Linkifier();

    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.RequestStarted, this._onRequestStarted, this);
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.RequestUpdated, this._onRequestUpdated, this);
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.RequestFinished, this._onRequestUpdated, this);

    WebInspector.resourceTreeModel.addEventListener(WebInspector.ResourceTreeModel.EventTypes.MainFrameNavigated, this._mainFrameNavigated, this);
    WebInspector.resourceTreeModel.addEventListener(WebInspector.ResourceTreeModel.EventTypes.OnLoad, this._onLoadEventFired, this);
    WebInspector.resourceTreeModel.addEventListener(WebInspector.ResourceTreeModel.EventTypes.DOMContentLoaded, this._domContentLoadedEventFired, this);

    WebInspector.networkLog.requests.forEach(this._appendRequest.bind(this));
    this._initializeView();
    this._viewInitialised = true;

}

WebInspector.ThreeDimView.prototype = {
    
    _initializeView: function()
    {
        this.element.id = "threeDim-container";
        this._threeInit();
        _threeAnimate();
    },
    
    _threeCreateHtmlElement: function(request, pos){
        
        var host = request._parsedURL.host;
        var mimeType = request._mimeType;
        var requestId = request.requestId;
    
        var element = document.createElement( 'div' );
        element.className = 'request';
        element.style.backgroundColor = 'rgba(0,127,127,' + ( Math.random() * 0.5 + 0.25 ) + ')';

        var number = document.createElement( 'div' );
        number.className = 'data';
        number.textContent = pos + 1;
        element.appendChild( number );

        var symbol = document.createElement( 'div' );
        symbol.className = 'content';
        
        if (request._type._title == "Image"){
            symbol.innerHTML = "<img src=\"data:" + mimeType +  "; base64,"+ request._content +"\"  />"
        }else{
            symbol.textContent = host;    
        }
        element.appendChild( symbol );

        var details = document.createElement( 'div' );
        details.className = 'details';
        details.innerHTML = mimeType + '<br>' + requestId;
        element.appendChild( details );
        return element;
    },
    
    _threeAddNewObject: function(request, pos){
            
            var element = this._threeCreateHtmlElement(request, pos);
            var object = new THREE.CSS3DObject( element );
            object.position.x = Math.random() * 4000 - 2000;
            object.position.y = Math.random() * 4000 - 2000;
            object.position.z = Math.random() * 4000 - 2000;
            _threeScene.add( object );
            _threeObjects.push( object );
            
            // table
            var posX = Math.floor(pos/20);
            var posY = pos%20;

            var objectTable = new THREE.Object3D();
            objectTable.position.x = ( posX * 160 ) - 1540;
            objectTable.position.y = - ( posY * 200 ) + 1100;
            _threeTargets.table.push( objectTable );   
            
            // sphere
            var vectorSphere = new THREE.Vector3();
            var l = pos + 1;
            var phi = Math.acos( -1 + ( 2 * pos ) / l );
            var theta = Math.sqrt( l * Math.PI ) * phi;
            var objectSphere = new THREE.Object3D();
            objectSphere.position.x = 1000 * Math.cos( theta ) * Math.sin( phi );
            objectSphere.position.y = 1000 * Math.sin( theta ) * Math.sin( phi );
            objectSphere.position.z = 1000 * Math.cos( phi );
            vectorSphere.copy( objectSphere.position ).multiplyScalar( 2 );
            object.lookAt( vectorSphere );
            _threeTargets.sphere.push( objectSphere );
            
            // helix
            var vectorHelix = new THREE.Vector3();
            var phi = pos * 0.175 + Math.PI;
            var objectHelix = new THREE.Object3D();
            objectHelix.position.x = 1100 * Math.sin( phi );
            objectHelix.position.y = - ( pos * 8 ) + 450;
            objectHelix.position.z = 1100 * Math.cos( phi );
            vectorHelix.copy( object.position );
            vectorHelix.x *= 2;
            vectorHelix.z *= 2;
            object.lookAt( vectorHelix );
            _threeTargets.helix.push( objectHelix );
            
            // grid 
            var objectGrid = new THREE.Object3D();
            objectGrid.position.x = ( ( pos % 5 ) * 400 ) - 800;
            objectGrid.position.y = ( - ( Math.floor( pos / 5 ) % 5 ) * 400 ) + 800;
            objectGrid.position.z = ( Math.floor( pos / 25 ) ) * 1000 - 2000;
            _threeTargets.grid.push( objectGrid );
            
    },
    
    _threeCreateObjects: function (){
        // create three objects

        var j = 1;
        var k = 1;
        _threeTargets = { table: [], sphere: [], helix: [], grid: [] };

        for ( var i = 0; i < this._requests.length; i ++ ) {
            var request = this._requests[i];
            var element = this._threeCreateHtmlElement(request, i);
            
            var object = new THREE.CSS3DObject( element );
            object.position.x = Math.random() * 4000 - 2000;
            object.position.y = Math.random() * 4000 - 2000;
            object.position.z = Math.random() * 4000 - 2000;
            _threeScene.add( object );
            _threeObjects.push( object );
            
            // table setting
            k++;
            if (k >= 20){
                j++;
                k = 1;
            }
            var object = new THREE.Object3D();
            object.position.x = ( k * 160 ) - 1540;
            object.position.y = - ( j * 200 ) + 1100;
            _threeTargets.table.push( object );
        
        }        

        // sphere
        var vector = new THREE.Vector3();
        for ( var i = 0, l = _threeObjects.length; i < l; i ++ ) {
            var object = _threeObjects[ i ];
            var phi = Math.acos( -1 + ( 2 * i ) / l );
            var theta = Math.sqrt( l * Math.PI ) * phi;
            var object = new THREE.Object3D();
            object.position.x = 1000 * Math.cos( theta ) * Math.sin( phi );
            object.position.y = 1000 * Math.sin( theta ) * Math.sin( phi );
            object.position.z = 1000 * Math.cos( phi );
            vector.copy( object.position ).multiplyScalar( 2 );
            object.lookAt( vector );
            _threeTargets.sphere.push( object );

        }

        // helix
        var vector = new THREE.Vector3();
        for ( var i = 0, l = _threeObjects.length; i < l; i ++ ) {
            var object = _threeObjects[ i ];
            var phi = i * 0.175 + Math.PI;
            var object = new THREE.Object3D();
            object.position.x = 1100 * Math.sin( phi );
            object.position.y = - ( i * 8 ) + 450;
            object.position.z = 1100 * Math.cos( phi );
            vector.copy( object.position );
            vector.x *= 2;
            vector.z *= 2;
            object.lookAt( vector );
            _threeTargets.helix.push( object );
        }

        // grid
        for ( var i = 0; i < _threeObjects.length; i ++ ) {
            var object = _threeObjects[ i ];
            var object = new THREE.Object3D();
            object.position.x = ( ( i % 5 ) * 400 ) - 800;
            object.position.y = ( - ( Math.floor( i / 5 ) % 5 ) * 400 ) + 800;
            object.position.z = ( Math.floor( i / 25 ) ) * 1000 - 2000;
            _threeTargets.grid.push( object );
        }
    },
    
    _threeInit: function(){
        _threeCamera = new THREE.PerspectiveCamera( 75, window.innerWidth / window.innerHeight, 1, 5000 );
        _threeCamera.position.z = 1800;

        _threeScene = new THREE.Scene();

        this._threeCreateObjects();
        
        _threeRenderer = new THREE.CSS3DRenderer();
        _threeRenderer.setSize( window.innerWidth, window.innerHeight );
        _threeRenderer.domElement.style.position = 'absolute';
        this.element.appendChild( _threeRenderer.domElement );
        _threeControls = new THREE.TrackballControls( _threeCamera, _threeRenderer.domElement );
        _threeControls.rotateSpeed = 0.5;
        _threeControls.addEventListener( 'change', _threeRender );
        this._activateThreeTransform();
        window.addEventListener( 'resize', _threeOnWindowResize, false );
    },

    _threeTransform: function(targets, duration){
        TWEEN.removeAll();
        for ( var i = 0; i < _threeObjects.length; i ++ ) {
            var object = _threeObjects[ i ];
            var target = targets[ i ];
            new TWEEN.Tween( object.position )
                .to( { x: target.position.x, y: target.position.y, z: target.position.z }, Math.random() * duration + duration )
                .easing( TWEEN.Easing.Exponential.InOut )
                .start();
            new TWEEN.Tween( object.rotation )
                .to( { x: target.rotation.x, y: target.rotation.y, z: target.rotation.z }, Math.random() * duration + duration )
                .easing( TWEEN.Easing.Exponential.InOut )
                .start();
        }
        new TWEEN.Tween( this )
            .to( {}, duration * 2 )
            .onUpdate( _threeRender )
            .start();
    },
    
    _createThreeTypeBarItems: function(){
        var threeTypeBarElement = document.createElement("div");
        threeTypeBarElement.className = "scope-bar status-bar-item";

        /**
         * @param {string} typeName
         * @param {string} label
         */
        function createThreeTypeElement(typeName, label)
        {
            var threeTypeElement = document.createElement("li");
            threeTypeElement.typeName = typeName;
            threeTypeElement.className = typeName;
            threeTypeElement.appendChild(document.createTextNode(label));
            threeTypeElement.addEventListener("click", this._updateThreeVievTypeByClick.bind(this), false);
            threeTypeBarElement.appendChild(threeTypeElement);

            return threeTypeElement;
        }
        
        var threeTypes = ["Table", "Sphere", "Helix", "Grid"];
        for (var typeId in threeTypes) {
            var typeName = threeTypes[typeId];
            createThreeTypeElement.call(this, typeName, typeName);
        }
        
        var dividerElement = document.createElement("div");
        dividerElement.addStyleClass("scope-bar-divider");
        threeTypeBarElement.appendChild(dividerElement);
        
        this._threeTypeBarElement = threeTypeBarElement;
    },
    
    _updateThreeVievTypeByClick: function(e)
    {
        _threeSelectedType = e.target.typeName;
        this._activateThreeTransform();
    },
    
    _activateThreeTransform : function(){
        var selectedType = _threeSelectedType;
        var animationTime = 1000;
        if (selectedType == "Sphere"){
            this._threeTransform( _threeTargets.sphere, animationTime );    
        }else if (selectedType == "Table"){
            this._threeTransform( _threeTargets.table, animationTime );    
        }else if (selectedType == "Helix"){
            this._threeTransform( _threeTargets.helix, animationTime );    
        }else if (selectedType == "Grid"){    
            this._threeTransform( _threeTargets.grid, animationTime );    
        }
    },
    
    _createStatusBarItems: function(){
        var filterBarElement = document.createElement("div");
        filterBarElement.className = "scope-bar status-bar-item";

        /**
         * @param {string} typeName
         * @param {string} label
         */
        function createFilterElement(typeName, label)
        {
            var categoryElement = document.createElement("li");
            categoryElement.typeName = typeName;
            categoryElement.className = typeName;
            categoryElement.appendChild(document.createTextNode(label));
            categoryElement.addEventListener("click", this._updateFilter.bind(this), false);
            filterBarElement.appendChild(categoryElement);

            return categoryElement;
        }

        this._filterAllElement = createFilterElement.call(this, "all", WebInspector.UIString("All"));

        // Add a divider
        var dividerElement = document.createElement("div");
        dividerElement.addStyleClass("scope-bar-divider");
        filterBarElement.appendChild(dividerElement);

        for (var typeId in WebInspector.resourceTypes) {
            var type = WebInspector.resourceTypes[typeId];
            createFilterElement.call(this, type.name(), type.categoryTitle());
        }
        this._filterBarElement = filterBarElement;
        this._progressBarContainer = document.createElement("div");
        this._progressBarContainer.className = "status-bar-item";
    },
    
    get statusBarItems()
    {
        return [ this._threeTypeBarElement, this._filterBarElement, this._progressBarContainer ];
    },

    _onRequestStarted: function(event)
    {
        this._appendRequest(event.data);
    },
    
    _appendRequest: function(request)
    {
        this._requests.push(request);

        // In case of redirect request id is reassigned to a redirected
        // request and we need to update _requestsById ans search results.
        if (request.redirects && this._requestsById[request.requestId]) {
            var oldRequest = request.redirects[request.redirects.length - 1];
            this._requestsById[oldRequest.requestId] = oldRequest;

            this._updateSearchMatchedListAfterRequestIdChanged(request.requestId, oldRequest.requestId);
        }
        this._requestsById[request.requestId] = request;

        this._requestsByURL[request.url] = request;

        // Pull all the redirects of the main request upon commit load.
        if (request.redirects) {
            for (var i = 0; i < request.redirects.length; ++i)
                this._refreshRequest(request.redirects[i]);
        }

        this._refreshRequest(request);
        if (this._viewInitialised){
            this._threeAddNewObject(request, this._requests.length);    
        }
    },
    
    /**
     * @param {WebInspector.Event} event
     */
    _onRequestUpdated: function(event)
    {
        var request = /** @type {WebInspector.NetworkRequest} */ (event.data);
        this._refreshRequest(request);
    },
    
    /**
     * @param {WebInspector.NetworkRequest} request
     */
    _refreshRequest: function(request)
    {
        this._staleRequests[request.requestId] = request;
        this._scheduleRefresh();
    },
    
    _updateFilter: function(e)
    {
        // TODO this._removeAllNodeHighlights();
        var isMac = WebInspector.isMac();
        var selectMultiple = false;
        if (isMac && e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey)
            selectMultiple = true;
        if (!isMac && e.ctrlKey && !e.metaKey && !e.altKey && !e.shiftKey)
            selectMultiple = true;

        this._filter(e.target, selectMultiple);
        // TODO this.searchCanceled();
        // TODO this._updateSummaryBar();
    },

    _filter: function(target, selectMultiple)
    {
        function unselectAll()
        {
            for (var i = 0; i < this._filterBarElement.childNodes.length; ++i) {
                var child = this._filterBarElement.childNodes[i];
                if (!child.typeName)
                    continue;

                child.removeStyleClass("selected");
                // TODO this._hideCategory(child.typeName);
            }
        }

        if (target === this._filterAllElement) {
            if (target.hasStyleClass("selected")) {
                // We can't unselect All, so we break early here
                return;
            }

            // If All wasn't selected, and now is, unselect everything else.
            unselectAll.call(this);
        } else {
            // Something other than All is being selected, so we want to unselect All.
            if (this._filterAllElement.hasStyleClass("selected")) {
                this._filterAllElement.removeStyleClass("selected");
                // TODO this._hideCategory("all");
            }
        }

        if (!selectMultiple) {
            // If multiple selection is off, we want to unselect everything else
            // and just select ourselves.
            unselectAll.call(this);

            target.addStyleClass("selected");
            // TODO this._showCategory(target.typeName);
            // TODO this._updateOffscreenRows();
            return;
        }

        if (target.hasStyleClass("selected")) {
            // If selectMultiple is turned on, and we were selected, we just
            // want to unselect ourselves.
            target.removeStyleClass("selected");
            // TODO this._hideCategory(target.typeName);
        } else {
            // If selectMultiple is turned on, and we weren't selected, we just
            // want to select ourselves.
            target.addStyleClass("selected");
            // TODO this._showCategory(target.typeName);
        }
        // TODO what do do after we set all filter stuff
        // this._updateOffscreenRows();
    },
    
    _defaultRefreshDelay: 2500,

    _scheduleRefresh: function()
    {
        if (this._needsRefresh)
            return;

        this._needsRefresh = true;

        if (this.isShowing() && !this._refreshTimeout)
            this._refreshTimeout = setTimeout(this.refresh.bind(this), this._defaultRefreshDelay);
    },
    
    refresh: function()
    {
        this._needsRefresh = false;
        if (this._refreshTimeout) {
            clearTimeout(this._refreshTimeout);
            delete this._refreshTimeout;
        }
        if ( this._viewInitialised ){
            // refresh gui 
            this._activateThreeTransform();
        }
    },
    
    _mainFrameNavigated: function(event)
    {
        if (this._preserveLogToggle.toggled)
            return;

        var frame = /** @type {WebInspector.ResourceTreeFrame} */ (event.data);
        var loaderId = frame.loaderId;

        // Preserve provisional load requests.
        var requestsToPreserve = [];
        for (var i = 0; i < this._requests.length; ++i) {
            var request = this._requests[i];
            if (request.loaderId === loaderId)
                requestsToPreserve.push(request);
        }

        this._reset();

        // Restore preserved items.
        for (var i = 0; i < requestsToPreserve.length; ++i)
            this._appendRequest(requestsToPreserve[i]);
    },
    
    _onLoadEventFired: function(event)
    {
        this._mainRequestLoadTime = event.data || -1;
        // Schedule refresh to update boundaries and draw the new line.
        this._scheduleRefresh();
    },

    _domContentLoadedEventFired: function(event)
    {
        this._mainRequestDOMContentTime = event.data || -1;
        // Schedule refresh to update boundaries and draw the new line.
        this._scheduleRefresh();
    },
    
    _reset: function()
    {
        this.dispatchEventToListeners(WebInspector.NetworkLogView.EventTypes.ViewCleared);

        this._clearSearchMatchedList();
        if (this._popoverHelper)
            this._popoverHelper.hidePopover();

        if (this._calculator)
            this._calculator.reset();

        this._requests = [];
        this._requestsById = {};
        this._requestsByURL = {};
        this._staleRequests = {};
        this._requestGridNodes = {};

        if (this._dataGrid) {
            this._dataGrid.rootNode().removeChildren();
            this._updateDividersIfNeeded();
            this._updateSummaryBar();
        }

        this._mainRequestLoadTime = -1;
        this._mainRequestDOMContentTime = -1;
        this._linkifier.reset();
    },
    
    __proto__: WebInspector.View.prototype
}

/**
 * @constructor
 * @extends {WebInspector.Panel}
 * @implements {WebInspector.ContextMenu.Provider}
 */
WebInspector.ThreeDimPanel = function(){
    WebInspector.Panel.call(this, "threeDimPanel");
    this.registerRequiredCSS("three/threeDimPanel.css");
    this._threeDimView = new WebInspector.ThreeDimView();
    this.element.id = "threeDimPanel";
    this._threeDimView.show(this.element);
}

WebInspector.ThreeDimPanel.prototype = {
    get statusBarItems()
    {
        return this._threeDimView.statusBarItems;
    },
    __proto__: WebInspector.Panel.prototype
}

//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js
//@ sourceURL=http://192.168.1.135/devtools/ThreeDimPanel.js