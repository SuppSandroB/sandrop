





WebInspector.JavaScriptBreakpointsSidebarPane = function(breakpointManager, showSourceLineDelegate)
{
WebInspector.SidebarPane.call(this, WebInspector.UIString("Breakpoints"));

this._breakpointManager = breakpointManager;
this._showSourceLineDelegate = showSourceLineDelegate;

this.listElement = document.createElement("ol");
this.listElement.className = "breakpoint-list";

this.emptyElement = document.createElement("div");
this.emptyElement.className = "info";
this.emptyElement.textContent = WebInspector.UIString("No Breakpoints");

this.bodyElement.appendChild(this.emptyElement);

this._items = new Map();

var breakpointLocations = this._breakpointManager.allBreakpointLocations();
for (var i = 0; i < breakpointLocations.length; ++i)
this._addBreakpoint(breakpointLocations[i].breakpoint, breakpointLocations[i].uiLocation);

this._breakpointManager.addEventListener(WebInspector.BreakpointManager.Events.BreakpointAdded, this._breakpointAdded, this);
this._breakpointManager.addEventListener(WebInspector.BreakpointManager.Events.BreakpointRemoved, this._breakpointRemoved, this);

this.emptyElement.addEventListener("contextmenu", this._emptyElementContextMenu.bind(this), true);
}

WebInspector.JavaScriptBreakpointsSidebarPane.prototype = {
_emptyElementContextMenu: function(event)
{
var contextMenu = new WebInspector.ContextMenu(event);
var breakpointActive = WebInspector.debuggerModel.breakpointsActive();
var breakpointActiveTitle = WebInspector.UIString(breakpointActive ? "Deactivate Breakpoints" : "Activate Breakpoints");
contextMenu.appendItem(breakpointActiveTitle, WebInspector.debuggerModel.setBreakpointsActive.bind(WebInspector.debuggerModel, !breakpointActive));
contextMenu.show();
},


_breakpointAdded: function(event)
{
this._breakpointRemoved(event);

var breakpoint =   (event.data.breakpoint);
var uiLocation =   (event.data.uiLocation);
this._addBreakpoint(breakpoint, uiLocation);
},


_addBreakpoint: function(breakpoint, uiLocation)
{
var element = document.createElement("li");
element.addStyleClass("cursor-pointer");
element.addEventListener("contextmenu", this._breakpointContextMenu.bind(this, breakpoint), true);
element.addEventListener("click", this._breakpointClicked.bind(this, uiLocation), false);

var checkbox = document.createElement("input");
checkbox.className = "checkbox-elem";
checkbox.type = "checkbox";
checkbox.checked = breakpoint.enabled();
checkbox.addEventListener("click", this._breakpointCheckboxClicked.bind(this, breakpoint), false);
element.appendChild(checkbox);

var labelElement = document.createTextNode(WebInspector.formatLinkText(uiLocation.uiSourceCode.url, uiLocation.lineNumber));
element.appendChild(labelElement);

var snippetElement = document.createElement("div");
snippetElement.className = "source-text monospace";
element.appendChild(snippetElement);


function didRequestContent(content, contentEncoded, mimeType)
{
var lineEndings = content.lineEndings();
if (uiLocation.lineNumber < lineEndings.length)
snippetElement.textContent = content.substring(lineEndings[uiLocation.lineNumber - 1], lineEndings[uiLocation.lineNumber]);
}
uiLocation.uiSourceCode.requestContent(didRequestContent.bind(this));

element._data = uiLocation;
var currentElement = this.listElement.firstChild;
while (currentElement) {
if (currentElement._data && this._compareBreakpoints(currentElement._data, element._data) > 0)
break;
currentElement = currentElement.nextSibling;
}
this._addListElement(element, currentElement);

var breakpointItem = {};
breakpointItem.element = element;
breakpointItem.checkbox = checkbox;
this._items.put(breakpoint, breakpointItem);

if (!this.expanded)
this.expanded = true;
},


_breakpointRemoved: function(event)
{
var breakpoint =   (event.data.breakpoint);
var uiLocation =   (event.data.uiLocation);
var breakpointItem = this._items.get(breakpoint);
if (!breakpointItem)
return;
this._items.remove(breakpoint);
this._removeListElement(breakpointItem.element);
},


highlightBreakpoint: function(breakpoint)
{
var breakpointItem = this._items.get(breakpoint);
if (!breakpointItem)
return;
breakpointItem.element.addStyleClass("breakpoint-hit");
this._highlightedBreakpointItem = breakpointItem;
},

clearBreakpointHighlight: function()
{
if (this._highlightedBreakpointItem) {
this._highlightedBreakpointItem.element.removeStyleClass("breakpoint-hit");
delete this._highlightedBreakpointItem;
}
},

_breakpointClicked: function(uiLocation, event)
{
this._showSourceLineDelegate(uiLocation.uiSourceCode, uiLocation.lineNumber);
},


_breakpointCheckboxClicked: function(breakpoint, event)
{

event.consume();
breakpoint.setEnabled(event.target.checked);
},


_breakpointContextMenu: function(breakpoint, event)
{
var breakpoints = this._items.values();
var contextMenu = new WebInspector.ContextMenu(event);
contextMenu.appendItem(WebInspector.UIString("Remove Breakpoint"), breakpoint.remove.bind(breakpoint));
if (breakpoints.length > 1) {
var removeAllTitle = WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Remove all breakpoints" : "Remove All Breakpoints");
contextMenu.appendItem(removeAllTitle, this._breakpointManager.removeAllBreakpoints.bind(this._breakpointManager));
}

contextMenu.appendSeparator();
var breakpointActive = WebInspector.debuggerModel.breakpointsActive();
var breakpointActiveTitle = WebInspector.UIString(breakpointActive ? "Deactivate Breakpoints" : "Activate Breakpoints");
contextMenu.appendItem(breakpointActiveTitle, WebInspector.debuggerModel.setBreakpointsActive.bind(WebInspector.debuggerModel, !breakpointActive));

function enabledBreakpointCount(breakpoints)
{
var count = 0;
for (var i = 0; i < breakpoints.length; ++i) {
if (breakpoints[i].checkbox.checked)
count++;
}
return count;
}
if (breakpoints.length > 1) {
var enableBreakpointCount = enabledBreakpointCount(breakpoints);
var enableTitle = WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Enable all breakpoints" : "Enable All Breakpoints");
var disableTitle = WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Disable all breakpoints" : "Disable All Breakpoints");

contextMenu.appendSeparator();

contextMenu.appendItem(enableTitle, this._breakpointManager.toggleAllBreakpoints.bind(this._breakpointManager, true), !(enableBreakpointCount != breakpoints.length));
contextMenu.appendItem(disableTitle, this._breakpointManager.toggleAllBreakpoints.bind(this._breakpointManager, false), !(enableBreakpointCount > 1));
}

contextMenu.show();
},

_addListElement: function(element, beforeElement)
{
if (beforeElement)
this.listElement.insertBefore(element, beforeElement);
else {
if (!this.listElement.firstChild) {
this.bodyElement.removeChild(this.emptyElement);
this.bodyElement.appendChild(this.listElement);
}
this.listElement.appendChild(element);
}
},

_removeListElement: function(element)
{
this.listElement.removeChild(element);
if (!this.listElement.firstChild) {
this.bodyElement.removeChild(this.listElement);
this.bodyElement.appendChild(this.emptyElement);
}
},

_compare: function(x, y)
{
if (x !== y)
return x < y ? -1 : 1;
return 0;
},

_compareBreakpoints: function(b1, b2)
{
return this._compare(b1.url, b2.url) || this._compare(b1.lineNumber, b2.lineNumber);
},

reset: function()
{
this.listElement.removeChildren();
if (this.listElement.parentElement) {
this.bodyElement.removeChild(this.listElement);
this.bodyElement.appendChild(this.emptyElement);
}
this._items.clear();
},

__proto__: WebInspector.SidebarPane.prototype
}


WebInspector.XHRBreakpointsSidebarPane = function()
{
WebInspector.NativeBreakpointsSidebarPane.call(this, WebInspector.UIString("XHR Breakpoints"));

this._breakpointElements = {};

var addButton = document.createElement("button");
addButton.className = "pane-title-button add";
addButton.addEventListener("click", this._addButtonClicked.bind(this), false);
addButton.title = WebInspector.UIString("Add XHR breakpoint");
this.titleElement.appendChild(addButton);

this.emptyElement.addEventListener("contextmenu", this._emptyElementContextMenu.bind(this), true);

this._restoreBreakpoints();
}

WebInspector.XHRBreakpointsSidebarPane.prototype = {
_emptyElementContextMenu: function(event)
{
var contextMenu = new WebInspector.ContextMenu(event);
contextMenu.appendItem(WebInspector.UIString("Add Breakpoint"), this._addButtonClicked.bind(this));
contextMenu.show();
},

_addButtonClicked: function(event)
{
if (event)
event.consume();

this.expanded = true;

var inputElementContainer = document.createElement("p");
inputElementContainer.className = "breakpoint-condition";
var inputElement = document.createElement("span");
inputElementContainer.textContent = WebInspector.UIString("Break when URL contains:");
inputElement.className = "editing";
inputElement.id = "breakpoint-condition-input";
inputElementContainer.appendChild(inputElement);
this._addListElement(inputElementContainer, this.listElement.firstChild);

function finishEditing(accept, e, text)
{
this._removeListElement(inputElementContainer);
if (accept) {
this._setBreakpoint(text, true);
this._saveBreakpoints();
}
}

var config = new WebInspector.EditingConfig(finishEditing.bind(this, true), finishEditing.bind(this, false));
WebInspector.startEditing(inputElement, config);
},

_setBreakpoint: function(url, enabled)
{
if (url in this._breakpointElements)
return;

var element = document.createElement("li");
element._url = url;
element.addEventListener("contextmenu", this._contextMenu.bind(this, url), true);

var checkboxElement = document.createElement("input");
checkboxElement.className = "checkbox-elem";
checkboxElement.type = "checkbox";
checkboxElement.checked = enabled;
checkboxElement.addEventListener("click", this._checkboxClicked.bind(this, url), false);
element._checkboxElement = checkboxElement;
element.appendChild(checkboxElement);

var labelElement = document.createElement("span");
if (!url)
labelElement.textContent = WebInspector.UIString("Any XHR");
else
labelElement.textContent = WebInspector.UIString("URL contains \"%s\"", url);
labelElement.addStyleClass("cursor-auto");
labelElement.addEventListener("dblclick", this._labelClicked.bind(this, url), false);
element.appendChild(labelElement);

var currentElement = this.listElement.firstChild;
while (currentElement) {
if (currentElement._url && currentElement._url < element._url)
break;
currentElement = currentElement.nextSibling;
}
this._addListElement(element, currentElement);
this._breakpointElements[url] = element;
if (enabled)
DOMDebuggerAgent.setXHRBreakpoint(url);
},

_removeBreakpoint: function(url)
{
var element = this._breakpointElements[url];
if (!element)
return;

this._removeListElement(element);
delete this._breakpointElements[url];
if (element._checkboxElement.checked)
DOMDebuggerAgent.removeXHRBreakpoint(url);
},

_contextMenu: function(url, event)
{
var contextMenu = new WebInspector.ContextMenu(event);
function removeBreakpoint()
{
this._removeBreakpoint(url);
this._saveBreakpoints();
}
function removeAllBreakpoints()
{
for (var url in this._breakpointElements)
this._removeBreakpoint(url);
this._saveBreakpoints();
}
var removeAllTitle = WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Remove all breakpoints" : "Remove All Breakpoints");

contextMenu.appendItem(WebInspector.UIString("Add Breakpoint"), this._addButtonClicked.bind(this));
contextMenu.appendItem(WebInspector.UIString("Remove Breakpoint"), removeBreakpoint.bind(this));
contextMenu.appendItem(removeAllTitle, removeAllBreakpoints.bind(this));
contextMenu.show();
},

_checkboxClicked: function(url, event)
{
if (event.target.checked)
DOMDebuggerAgent.setXHRBreakpoint(url);
else
DOMDebuggerAgent.removeXHRBreakpoint(url);
this._saveBreakpoints();
},

_labelClicked: function(url)
{
var element = this._breakpointElements[url];
var inputElement = document.createElement("span");
inputElement.className = "breakpoint-condition editing";
inputElement.textContent = url;
this.listElement.insertBefore(inputElement, element);
element.addStyleClass("hidden");

function finishEditing(accept, e, text)
{
this._removeListElement(inputElement);
if (accept) {
this._removeBreakpoint(url);
this._setBreakpoint(text, element._checkboxElement.checked);
this._saveBreakpoints();
} else
element.removeStyleClass("hidden");
}

WebInspector.startEditing(inputElement, new WebInspector.EditingConfig(finishEditing.bind(this, true), finishEditing.bind(this, false)));
},

highlightBreakpoint: function(url)
{
var element = this._breakpointElements[url];
if (!element)
return;
this.expanded = true;
element.addStyleClass("breakpoint-hit");
this._highlightedElement = element;
},

clearBreakpointHighlight: function()
{
if (this._highlightedElement) {
this._highlightedElement.removeStyleClass("breakpoint-hit");
delete this._highlightedElement;
}
},

_saveBreakpoints: function()
{
var breakpoints = [];
for (var url in this._breakpointElements)
breakpoints.push({ url: url, enabled: this._breakpointElements[url]._checkboxElement.checked });
WebInspector.settings.xhrBreakpoints.set(breakpoints);
},

_restoreBreakpoints: function()
{
var breakpoints = WebInspector.settings.xhrBreakpoints.get();
for (var i = 0; i < breakpoints.length; ++i) {
var breakpoint = breakpoints[i];
if (breakpoint && typeof breakpoint.url === "string")
this._setBreakpoint(breakpoint.url, breakpoint.enabled);
}
},

__proto__: WebInspector.NativeBreakpointsSidebarPane.prototype
}


WebInspector.EventListenerBreakpointsSidebarPane = function()
{
WebInspector.SidebarPane.call(this, WebInspector.UIString("Event Listener Breakpoints"));

this.categoriesElement = document.createElement("ol");
this.categoriesElement.tabIndex = 0;
this.categoriesElement.addStyleClass("properties-tree");
this.categoriesElement.addStyleClass("event-listener-breakpoints");
this.categoriesTreeOutline = new TreeOutline(this.categoriesElement);
this.bodyElement.appendChild(this.categoriesElement);

this._breakpointItems = {};



this._createCategory(WebInspector.UIString("Animation"), false, ["requestAnimationFrame", "cancelAnimationFrame", "animationFrameFired"]);
this._createCategory(WebInspector.UIString("Control"), true, ["resize", "scroll", "zoom", "focus", "blur", "select", "change", "submit", "reset"]);
this._createCategory(WebInspector.UIString("Clipboard"), true, ["copy", "cut", "paste", "beforecopy", "beforecut", "beforepaste"]);
this._createCategory(WebInspector.UIString("DOM Mutation"), true, ["DOMActivate", "DOMFocusIn", "DOMFocusOut", "DOMAttrModified", "DOMCharacterDataModified", "DOMNodeInserted", "DOMNodeInsertedIntoDocument", "DOMNodeRemoved", "DOMNodeRemovedFromDocument", "DOMSubtreeModified", "DOMContentLoaded"]);
this._createCategory(WebInspector.UIString("Device"), true, ["deviceorientation", "devicemotion"]);
this._createCategory(WebInspector.UIString("Keyboard"), true, ["keydown", "keyup", "keypress", "input"]);
this._createCategory(WebInspector.UIString("Load"), true, ["load", "unload", "abort", "error"]);
this._createCategory(WebInspector.UIString("Mouse"), true, ["click", "dblclick", "mousedown", "mouseup", "mouseover", "mousemove", "mouseout", "mousewheel"]);
this._createCategory(WebInspector.UIString("Timer"), false, ["setTimer", "clearTimer", "timerFired"]);
this._createCategory(WebInspector.UIString("Touch"), true, ["touchstart", "touchmove", "touchend", "touchcancel"]);

this._restoreBreakpoints();
}

WebInspector.EventListenerBreakpointsSidebarPane.categotyListener = "listener:";
WebInspector.EventListenerBreakpointsSidebarPane.categotyInstrumentation = "instrumentation:";

WebInspector.EventListenerBreakpointsSidebarPane.eventNameForUI = function(eventName)
{
if (!WebInspector.EventListenerBreakpointsSidebarPane._eventNamesForUI) {
WebInspector.EventListenerBreakpointsSidebarPane._eventNamesForUI = {
"instrumentation:setTimer": WebInspector.UIString("Set Timer"),
"instrumentation:clearTimer": WebInspector.UIString("Clear Timer"),
"instrumentation:timerFired": WebInspector.UIString("Timer Fired"),
"instrumentation:requestAnimationFrame": WebInspector.UIString("Request Animation Frame"),
"instrumentation:cancelAnimationFrame": WebInspector.UIString("Cancel Animation Frame"),
"instrumentation:animationFrameFired": WebInspector.UIString("Animation Frame Fired")
};
}
return WebInspector.EventListenerBreakpointsSidebarPane._eventNamesForUI[eventName] || eventName.substring(eventName.indexOf(":") + 1);
}

WebInspector.EventListenerBreakpointsSidebarPane.prototype = {
_createCategory: function(name, isDOMEvent, eventNames)
{
var categoryItem = {};
categoryItem.element = new TreeElement(name);
this.categoriesTreeOutline.appendChild(categoryItem.element);
categoryItem.element.listItemElement.addStyleClass("event-category");
categoryItem.element.selectable = true;

categoryItem.checkbox = this._createCheckbox(categoryItem.element);
categoryItem.checkbox.addEventListener("click", this._categoryCheckboxClicked.bind(this, categoryItem), true);

categoryItem.children = {};
for (var i = 0; i < eventNames.length; ++i) {
var eventName = (isDOMEvent ? WebInspector.EventListenerBreakpointsSidebarPane.categotyListener :  WebInspector.EventListenerBreakpointsSidebarPane.categotyInstrumentation) + eventNames[i];

var breakpointItem = {};
var title = WebInspector.EventListenerBreakpointsSidebarPane.eventNameForUI(eventName);
breakpointItem.element = new TreeElement(title);
categoryItem.element.appendChild(breakpointItem.element);
var hitMarker = document.createElement("div");
hitMarker.className = "breakpoint-hit-marker";
breakpointItem.element.listItemElement.appendChild(hitMarker);
breakpointItem.element.listItemElement.addStyleClass("source-code");
breakpointItem.element.selectable = true;

breakpointItem.checkbox = this._createCheckbox(breakpointItem.element);
breakpointItem.checkbox.addEventListener("click", this._breakpointCheckboxClicked.bind(this, eventName), true);
breakpointItem.parent = categoryItem;

this._breakpointItems[eventName] = breakpointItem;
categoryItem.children[eventName] = breakpointItem;
}
},

_createCheckbox: function(treeElement)
{
var checkbox = document.createElement("input");
checkbox.className = "checkbox-elem";
checkbox.type = "checkbox";
treeElement.listItemElement.insertBefore(checkbox, treeElement.listItemElement.firstChild);
return checkbox;
},

_categoryCheckboxClicked: function(categoryItem)
{
var checked = categoryItem.checkbox.checked;
for (var eventName in categoryItem.children) {
var breakpointItem = categoryItem.children[eventName];
if (breakpointItem.checkbox.checked === checked)
continue;
if (checked)
this._setBreakpoint(eventName);
else
this._removeBreakpoint(eventName);
}
this._saveBreakpoints();
},

_breakpointCheckboxClicked: function(eventName, event)
{
if (event.target.checked)
this._setBreakpoint(eventName);
else
this._removeBreakpoint(eventName);
this._saveBreakpoints();
},

_setBreakpoint: function(eventName)
{
var breakpointItem = this._breakpointItems[eventName];
if (!breakpointItem)
return;
breakpointItem.checkbox.checked = true;
if (eventName.startsWith(WebInspector.EventListenerBreakpointsSidebarPane.categotyListener))
DOMDebuggerAgent.setEventListenerBreakpoint(eventName.substring(WebInspector.EventListenerBreakpointsSidebarPane.categotyListener.length));
else if (eventName.startsWith(WebInspector.EventListenerBreakpointsSidebarPane.categotyInstrumentation))
DOMDebuggerAgent.setInstrumentationBreakpoint(eventName.substring(WebInspector.EventListenerBreakpointsSidebarPane.categotyInstrumentation.length));
this._updateCategoryCheckbox(breakpointItem.parent);
},

_removeBreakpoint: function(eventName)
{
var breakpointItem = this._breakpointItems[eventName];
if (!breakpointItem)
return;
breakpointItem.checkbox.checked = false;
if (eventName.startsWith(WebInspector.EventListenerBreakpointsSidebarPane.categotyListener))
DOMDebuggerAgent.removeEventListenerBreakpoint(eventName.substring(WebInspector.EventListenerBreakpointsSidebarPane.categotyListener.length));
else if (eventName.startsWith(WebInspector.EventListenerBreakpointsSidebarPane.categotyInstrumentation))
DOMDebuggerAgent.removeInstrumentationBreakpoint(eventName.substring(WebInspector.EventListenerBreakpointsSidebarPane.categotyInstrumentation.length));
this._updateCategoryCheckbox(breakpointItem.parent);
},

_updateCategoryCheckbox: function(categoryItem)
{
var hasEnabled = false, hasDisabled = false;
for (var eventName in categoryItem.children) {
var breakpointItem = categoryItem.children[eventName];
if (breakpointItem.checkbox.checked)
hasEnabled = true;
else
hasDisabled = true;
}
categoryItem.checkbox.checked = hasEnabled;
categoryItem.checkbox.indeterminate = hasEnabled && hasDisabled;
},

highlightBreakpoint: function(eventName)
{
var breakpointItem = this._breakpointItems[eventName];
if (!breakpointItem)
return;
this.expanded = true;
breakpointItem.parent.element.expand();
breakpointItem.element.listItemElement.addStyleClass("breakpoint-hit");
this._highlightedElement = breakpointItem.element.listItemElement;
},

clearBreakpointHighlight: function()
{
if (this._highlightedElement) {
this._highlightedElement.removeStyleClass("breakpoint-hit");
delete this._highlightedElement;
}
},

_saveBreakpoints: function()
{
var breakpoints = [];
for (var eventName in this._breakpointItems) {
if (this._breakpointItems[eventName].checkbox.checked)
breakpoints.push({ eventName: eventName });
}
WebInspector.settings.eventListenerBreakpoints.set(breakpoints);
},

_restoreBreakpoints: function()
{
var breakpoints = WebInspector.settings.eventListenerBreakpoints.get();
for (var i = 0; i < breakpoints.length; ++i) {
var breakpoint = breakpoints[i];
if (breakpoint && typeof breakpoint.eventName === "string")
this._setBreakpoint(breakpoint.eventName);
}
},

__proto__: WebInspector.SidebarPane.prototype
}
;



WebInspector.CallStackSidebarPane = function()
{
WebInspector.SidebarPane.call(this, WebInspector.UIString("Call Stack"));
this._model = WebInspector.debuggerModel;

this.bodyElement.addEventListener("keydown", this._keyDown.bind(this), true);
this.bodyElement.tabIndex = 0;
}

WebInspector.CallStackSidebarPane.prototype = {
update: function(callFrames)
{
this.bodyElement.removeChildren();
this.placards = [];

if (!callFrames) {
var infoElement = document.createElement("div");
infoElement.className = "info";
infoElement.textContent = WebInspector.UIString("Not Paused");
this.bodyElement.appendChild(infoElement);
return;
}

for (var i = 0; i < callFrames.length; ++i) {
var callFrame = callFrames[i];
var placard = new WebInspector.CallStackSidebarPane.Placard(callFrame, this);
placard.element.addEventListener("click", this._placardSelected.bind(this, placard), false);
this.placards.push(placard);
this.bodyElement.appendChild(placard.element);
}
},

setSelectedCallFrame: function(x)
{
for (var i = 0; i < this.placards.length; ++i) {
var placard = this.placards[i];
placard.selected = (placard._callFrame === x);
}
},

_selectNextCallFrameOnStack: function()
{
var index = this._selectedCallFrameIndex();
if (index == -1)
return;
this._selectedPlacardByIndex(index + 1);
},

_selectPreviousCallFrameOnStack: function()
{
var index = this._selectedCallFrameIndex();
if (index == -1)
return;
this._selectedPlacardByIndex(index - 1);
},

_selectedPlacardByIndex: function(index)
{
if (index < 0 || index >= this.placards.length)
return;
this._placardSelected(this.placards[index])
},

_selectedCallFrameIndex: function()
{
if (!this._model.selectedCallFrame())
return -1;
for (var i = 0; i < this.placards.length; ++i) {
var placard = this.placards[i];
if (placard._callFrame === this._model.selectedCallFrame())
return i;
}
return -1;
},

_placardSelected: function(placard)
{
this._model.setSelectedCallFrame(placard._callFrame);
},

_copyStackTrace: function()
{
var text = "";
for (var i = 0; i < this.placards.length; ++i)
text += this.placards[i].title + " (" + this.placards[i].subtitle + ")\n";
InspectorFrontendHost.copyText(text);
},

registerShortcuts: function(section, registerShortcutDelegate)
{
var nextCallFrame = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Period,
WebInspector.KeyboardShortcut.Modifiers.Ctrl);
registerShortcutDelegate(nextCallFrame.key, this._selectNextCallFrameOnStack.bind(this));

var prevCallFrame = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Comma,
WebInspector.KeyboardShortcut.Modifiers.Ctrl);
registerShortcutDelegate(prevCallFrame.key, this._selectPreviousCallFrameOnStack.bind(this));

section.addRelatedKeys([ nextCallFrame.name, prevCallFrame.name ], WebInspector.UIString("Next/previous call frame"));
},

setStatus: function(status)
{
if (!this._statusMessageElement) {
this._statusMessageElement = document.createElement("div");
this._statusMessageElement.className = "info";
this.bodyElement.appendChild(this._statusMessageElement);
}
if (typeof status === "string")
this._statusMessageElement.textContent = status;
else {
this._statusMessageElement.removeChildren();
this._statusMessageElement.appendChild(status);
}
},

_keyDown: function(event)
{
if (event.altKey || event.shiftKey || event.metaKey || event.ctrlKey)
return;

if (event.keyIdentifier === "Up") {
this._selectPreviousCallFrameOnStack();
event.consume();
} else if (event.keyIdentifier === "Down") {
this._selectNextCallFrameOnStack();
event.consume();
}
},

__proto__: WebInspector.SidebarPane.prototype
}


WebInspector.CallStackSidebarPane.Placard = function(callFrame, pane)
{
WebInspector.Placard.call(this, callFrame.functionName || WebInspector.UIString("(anonymous function)"), "");
callFrame.createLiveLocation(this._update.bind(this));
this.element.addEventListener("contextmenu", this._placardContextMenu.bind(this), true);
this._callFrame = callFrame;
this._pane = pane;
}

WebInspector.CallStackSidebarPane.Placard.prototype = {
_update: function(uiLocation)
{
this.subtitle = WebInspector.formatLinkText(uiLocation.uiSourceCode.url, uiLocation.lineNumber).trimMiddle(100);
},

_placardContextMenu: function(event)
{
var contextMenu = new WebInspector.ContextMenu(event);

if (WebInspector.debuggerModel.canSetScriptSource()) {
contextMenu.appendItem(WebInspector.UIString("Restart Frame"), this._restartFrame.bind(this));
contextMenu.appendSeparator();
}
contextMenu.appendItem(WebInspector.UIString("Copy Stack Trace"), this._pane._copyStackTrace.bind(this._pane));

contextMenu.show();
},

_restartFrame: function()
{
this._callFrame.restart(undefined);
},

__proto__: WebInspector.Placard.prototype
}
;



WebInspector.FilteredItemSelectionDialog = function(delegate)
{
WebInspector.DialogDelegate.call(this);

var xhr = new XMLHttpRequest();
xhr.open("GET", "filteredItemSelectionDialog.css", false);
xhr.send(null);

this.element = document.createElement("div");
this.element.className = "js-outline-dialog";
this.element.addEventListener("keydown", this._onKeyDown.bind(this), false);
this.element.addEventListener("mousemove", this._onMouseMove.bind(this), false);
this.element.addEventListener("click", this._onClick.bind(this), false);
var styleElement = this.element.createChild("style");
styleElement.type = "text/css";
styleElement.textContent = xhr.responseText;

this._itemElements = [];
this._elementIndexes = new Map();
this._elementHighlightChanges = new Map();

this._promptElement = this.element.createChild("input", "monospace");
this._promptElement.type = "text";
this._promptElement.setAttribute("spellcheck", "false");

this._progressElement = this.element.createChild("div", "progress");

this._itemElementsContainer = document.createElement("div");
this._itemElementsContainer.className = "container monospace";
this._itemElementsContainer.addEventListener("scroll", this._onScroll.bind(this), false);
this.element.appendChild(this._itemElementsContainer);

this._delegate = delegate;

this._delegate.requestItems(this._itemsLoaded.bind(this));
}

WebInspector.FilteredItemSelectionDialog.prototype = {

position: function(element, relativeToElement)
{
const minWidth = 500;
const minHeight = 204;
var width = Math.max(relativeToElement.offsetWidth * 2 / 3, minWidth);
var height = Math.max(relativeToElement.offsetHeight * 2 / 3, minHeight);

this.element.style.width = width + "px";
this.element.style.height = height + "px";

const shadowPadding = 20; 
element.positionAt(
relativeToElement.totalOffsetLeft() + Math.max((relativeToElement.offsetWidth - width - 2 * shadowPadding) / 2, shadowPadding),
relativeToElement.totalOffsetTop() + Math.max((relativeToElement.offsetHeight - height - 2 * shadowPadding) / 2, shadowPadding));
},

focus: function()
{
WebInspector.setCurrentFocusElement(this._promptElement);
},

willHide: function()
{
if (this._isHiding)
return;
this._isHiding = true;
if (this._filterTimer)
clearTimeout(this._filterTimer);
},

onEnter: function()
{
if (!this._selectedElement)
return;
this._delegate.selectItem(this._elementIndexes.get(this._selectedElement), this._promptElement.value.trim());
},


_itemsLoaded: function(index, chunkLength, chunkIndex, chunkCount)
{
for (var i = index; i < index + chunkLength; ++i)
this._itemElementsContainer.appendChild(this._createItemElement(i));
this._filterItems();

if (chunkIndex === chunkCount)
this._progressElement.style.backgroundImage = "";
else {
const color = "rgb(66, 129, 235)";
const percent = ((chunkIndex / chunkCount) * 100) + "%";
this._progressElement.style.backgroundImage = "-webkit-linear-gradient(left, " + color + ", " + color + " " + percent + ",  transparent " + percent + ")";
}
},


_createItemElement: function(index)
{
if (this._itemElements[index])
return this._itemElements[index];

var itemElement = document.createElement("div");
itemElement.className = "item";
itemElement._titleElement = itemElement.createChild("span");
itemElement._titleElement.textContent = this._delegate.itemTitleAt(index);
itemElement._titleSuffixElement = itemElement.createChild("span");
itemElement._subtitleElement = itemElement.createChild("span", "subtitle");
itemElement._subtitleElement.textContent = this._delegate.itemSubtitleAt(index);
this._elementIndexes.put(itemElement, index);
this._itemElements.push(itemElement);
return itemElement;
},


_hideItemElement: function(itemElement)
{
itemElement.style.display = "none";
},


_itemElementVisible: function(itemElement)
{
return itemElement.style.display !== "none";
},


_showItemElement: function(itemElement)
{
itemElement.style.display = "";
},


_createSearchRegExp: function(query, isGlobal)
{
return this._innerCreateSearchRegExp(this._delegate.rewriteQuery(query), isGlobal);
},


_innerCreateSearchRegExp: function(query, isGlobal)
{
if (!query)
return new RegExp(".*");
query = query.trim();

var ignoreCase = (query === query.toLowerCase());
var regExpString = query.escapeForRegExp().replace(/\\\*/g, ".*").replace(/\\\?/g, ".")
if (ignoreCase)
regExpString = regExpString.replace(/(?!^)(\\\.|[_:-])/g, "[^._:-]*$1");
else
regExpString = regExpString.replace(/(?!^)(\\\.|[A-Z_:-])/g, "[^.A-Z_:-]*$1");
regExpString = regExpString;
return new RegExp(regExpString, (ignoreCase ? "i" : "") + (isGlobal ? "g" : ""));
},

_filterItems: function()
{
delete this._filterTimer;

var query = this._promptElement.value;
query = query.trim();
var regex = this._createSearchRegExp(query);

var firstElement;
for (var i = 0; i < this._itemElements.length; ++i) {
var itemElement = this._itemElements[i];
itemElement._titleSuffixElement.textContent = this._delegate.itemSuffixAt(i);
if (regex.test(this._delegate.itemKeyAt(i))) {
this._showItemElement(itemElement);
if (!firstElement)
firstElement = itemElement;
} else
this._hideItemElement(itemElement);
}

if (!this._selectedElement || !this._itemElementVisible(this._selectedElement))
this._updateSelection(firstElement);

if (query) {
this._highlightItems(query);
this._query = query;
} else {
this._clearHighlight();
delete this._query;
}
},

_onKeyDown: function(event)
{
function nextItem(itemElement, isPageScroll, forward)
{
var scrollItemsLeft = isPageScroll && this._rowsPerViewport ? this._rowsPerViewport : 1;
var candidate = itemElement;
var lastVisibleCandidate = candidate;
do {
candidate = forward ? candidate.nextSibling : candidate.previousSibling;
if (!candidate) {
if (isPageScroll)
return lastVisibleCandidate;
else
candidate = forward ? this._itemElementsContainer.firstChild : this._itemElementsContainer.lastChild;
}
if (!this._itemElementVisible(candidate))
continue;
lastVisibleCandidate = candidate;
--scrollItemsLeft;
} while (scrollItemsLeft && candidate !== this._selectedElement);

return candidate;
}

if (this._selectedElement) {
var candidate;
switch (event.keyCode) {
case WebInspector.KeyboardShortcut.Keys.Down.code:
candidate = nextItem.call(this, this._selectedElement, false, true);
break;
case WebInspector.KeyboardShortcut.Keys.Up.code:
candidate = nextItem.call(this, this._selectedElement, false, false);
break;
case WebInspector.KeyboardShortcut.Keys.PageDown.code:
candidate = nextItem.call(this, this._selectedElement, true, true);
break;
case WebInspector.KeyboardShortcut.Keys.PageUp.code:
candidate = nextItem.call(this, this._selectedElement, true, false);
break;
}

if (candidate) {
this._updateSelection(candidate);
event.preventDefault();
return;
}
}

if (event.keyIdentifier !== "Shift" && event.keyIdentifier !== "Ctrl" && event.keyIdentifier !== "Meta" && event.keyIdentifier !== "Left" && event.keyIdentifier !== "Right")
this._scheduleFilter();
},

_scheduleFilter: function()
{
if (this._filterTimer)
return;
this._filterTimer = setTimeout(this._filterItems.bind(this), 0);
},


_updateSelection: function(newSelectedElement)
{
if (this._selectedElement === newSelectedElement)
return;
if (this._selectedElement)
this._selectedElement.removeStyleClass("selected");

this._selectedElement = newSelectedElement;
if (newSelectedElement) {
newSelectedElement.addStyleClass("selected");
newSelectedElement.scrollIntoViewIfNeeded(false);
if (!this._itemHeight) {
this._itemHeight = newSelectedElement.offsetHeight;
this._rowsPerViewport = Math.floor(this._itemElementsContainer.offsetHeight / this._itemHeight);
}
}
},

_onClick: function(event)
{
var itemElement = event.target.enclosingNodeOrSelfWithClass("item");
if (!itemElement)
return;
this._updateSelection(itemElement);
this._delegate.selectItem(this._elementIndexes.get(this._selectedElement), this._promptElement.value.trim());
WebInspector.Dialog.hide();
},

_onMouseMove: function(event)
{
var itemElement = event.target.enclosingNodeOrSelfWithClass("item");
if (!itemElement)
return;
this._updateSelection(itemElement);
},

_onScroll: function()
{
if (this._query)
this._highlightItems(this._query);
else
this._clearHighlight();
},


_highlightItems: function(query)
{
var regex = this._createSearchRegExp(query, true);
for (var i = 0; i < this._delegate.itemsCount(); ++i) {
var itemElement = this._itemElements[i];
if (this._itemElementVisible(itemElement) && this._itemElementInViewport(itemElement))
this._highlightItem(itemElement, regex);
}
},

_clearHighlight: function()
{
for (var i = 0; i < this._delegate.itemsCount(); ++i)
this._clearElementHighlight(this._itemElements[i]);
},


_clearElementHighlight: function(itemElement)
{
var changes = this._elementHighlightChanges.get(itemElement)
if (changes) {
WebInspector.revertDomChanges(changes);
this._elementHighlightChanges.remove(itemElement);
}
},


_highlightItem: function(itemElement, regex)
{
this._clearElementHighlight(itemElement);

var key = this._delegate.itemKeyAt(this._elementIndexes.get(itemElement));
var ranges = [];

var match;
while ((match = regex.exec(key)) !== null && match[0]) {
ranges.push({ offset: match.index, length: regex.lastIndex - match.index });
}

var changes = [];
WebInspector.highlightRangesWithStyleClass(itemElement, ranges, "highlight", changes);

if (changes.length)
this._elementHighlightChanges.put(itemElement, changes);
},


_itemElementInViewport: function(itemElement)
{
if (itemElement.offsetTop + this._itemHeight < this._itemElementsContainer.scrollTop)
return false;
if (itemElement.offsetTop > this._itemElementsContainer.scrollTop + this._itemHeight * (this._rowsPerViewport + 1))
return false;
return true;
},

__proto__: WebInspector.DialogDelegate.prototype
}


WebInspector.SelectionDialogContentProvider = function()
{
}

WebInspector.SelectionDialogContentProvider.prototype = {

itemTitleAt: function(itemIndex) { },


itemSuffixAt: function(itemIndex) { },


itemSubtitleAt: function(itemIndex) { },


itemKeyAt: function(itemIndex) { },


itemsCount: function() { },


requestItems: function(callback) { },


selectItem: function(itemIndex, promptValue) { },


rewriteQuery: function(query) { },
}


WebInspector.JavaScriptOutlineDialog = function(view, contentProvider)
{
WebInspector.SelectionDialogContentProvider.call(this);

this._functionItems = [];
this._view = view;
this._contentProvider = contentProvider;
}


WebInspector.JavaScriptOutlineDialog.show = function(view, contentProvider)
{
if (WebInspector.Dialog.currentInstance())
return null;
var delegate = new WebInspector.JavaScriptOutlineDialog(view, contentProvider);
var filteredItemSelectionDialog = new WebInspector.FilteredItemSelectionDialog(delegate);
WebInspector.Dialog.show(view.element, filteredItemSelectionDialog);
}

WebInspector.JavaScriptOutlineDialog.prototype = {

itemTitleAt: function(itemIndex)
{
var functionItem = this._functionItems[itemIndex];
return functionItem.name + (functionItem.arguments ? functionItem.arguments : "");
},


itemSuffixAt: function(itemIndex)
{
return "";
},


itemSubtitleAt: function(itemIndex)
{
return ":" + (this._functionItems[itemIndex].line + 1);
},


itemKeyAt: function(itemIndex)
{
return this._functionItems[itemIndex].name;
},


itemsCount: function()
{
return this._functionItems.length;
},


requestItems: function(callback)
{

function contentCallback(content, contentEncoded, mimeType)
{
if (this._outlineWorker)
this._outlineWorker.terminate();
this._outlineWorker = new Worker("ScriptFormatterWorker.js");
this._outlineWorker.onmessage = this._didBuildOutlineChunk.bind(this, callback);
const method = "outline";
this._outlineWorker.postMessage({ method: method, params: { content: content } });
}
this._contentProvider.requestContent(contentCallback.bind(this));
},

_didBuildOutlineChunk: function(callback, event)
{
var data = event.data;

var index = this._functionItems.length;
var chunk = data["chunk"];
for (var i = 0; i < chunk.length; ++i)
this._functionItems.push(chunk[i]);
callback(index, chunk.length, data.index, data.total);

if (data.total === data.index && this._outlineWorker) {
this._outlineWorker.terminate();
delete this._outlineWorker;
}
},


selectItem: function(itemIndex, promptValue)
{
var lineNumber = this._functionItems[itemIndex].line;
if (!isNaN(lineNumber) && lineNumber >= 0)
this._view.highlightLine(lineNumber);
this._view.focus();
},


rewriteQuery: function(query)
{
return query;
},

__proto__: WebInspector.SelectionDialogContentProvider.prototype
}


WebInspector.OpenResourceDialog = function(panel, uiSourceCodeProvider)
{
WebInspector.SelectionDialogContentProvider.call(this);
this._panel = panel;

this._uiSourceCodes = uiSourceCodeProvider.uiSourceCodes();

function filterOutEmptyURLs(uiSourceCode)
{
return !!uiSourceCode.parsedURL.lastPathComponent;
}
this._uiSourceCodes = this._uiSourceCodes.filter(filterOutEmptyURLs);

function compareFunction(uiSourceCode1, uiSourceCode2)
{
return uiSourceCode1.parsedURL.lastPathComponent.localeCompare(uiSourceCode2.parsedURL.lastPathComponent);
}
this._uiSourceCodes.sort(compareFunction);
}

WebInspector.OpenResourceDialog.prototype = {

itemTitleAt: function(itemIndex)
{
return this._uiSourceCodes[itemIndex].parsedURL.lastPathComponent;
},


itemSuffixAt: function(itemIndex)
{
return this._queryLineNumber || "";
},


itemSubtitleAt: function(itemIndex)
{
return this._uiSourceCodes[itemIndex].parsedURL.folderPathComponents;
},


itemKeyAt: function(itemIndex)
{
return this._uiSourceCodes[itemIndex].parsedURL.lastPathComponent;
},


itemsCount: function()
{
return this._uiSourceCodes.length;
},


requestItems: function(callback)
{
callback(0, this._uiSourceCodes.length, 1, 1);
},


selectItem: function(itemIndex, promptValue)
{
var lineNumberMatch = promptValue.match(/[^:]+\:([\d]*)$/);
var lineNumber = lineNumberMatch ? Math.max(parseInt(lineNumberMatch[1], 10) - 1, 0) : 0;
this._panel.showUISourceCode(this._uiSourceCodes[itemIndex], lineNumber);
},


rewriteQuery: function(query)
{
if (!query)
return query;
query = query.trim();
var lineNumberMatch = query.match(/([^:]+)(\:[\d]*)$/);
this._queryLineNumber = lineNumberMatch ? lineNumberMatch[2] : "";
return lineNumberMatch ? lineNumberMatch[1] : query;
},

__proto__: WebInspector.SelectionDialogContentProvider.prototype
}


WebInspector.OpenResourceDialog.show = function(panel, uiSourceCodeProvider, relativeToElement)
{
if (WebInspector.Dialog.currentInstance())
return;

var filteredItemSelectionDialog = new WebInspector.FilteredItemSelectionDialog(new WebInspector.OpenResourceDialog(panel, uiSourceCodeProvider));
WebInspector.Dialog.show(relativeToElement, filteredItemSelectionDialog);
}
;



WebInspector.JavaScriptSourceFrame = function(scriptsPanel, uiSourceCode)
{
this._scriptsPanel = scriptsPanel;
this._breakpointManager = WebInspector.breakpointManager;
this._uiSourceCode = uiSourceCode;

var locations = this._breakpointManager.breakpointLocationsForUISourceCode(this._uiSourceCode);
for (var i = 0; i < locations.length; ++i)
this._breakpointAdded({data:locations[i]});

WebInspector.SourceFrame.call(this, uiSourceCode);

this._popoverHelper = new WebInspector.ObjectPopoverHelper(this.textEditor.element,
this._getPopoverAnchor.bind(this), this._resolveObjectForPopover.bind(this), this._onHidePopover.bind(this), true);

this.textEditor.element.addEventListener("keydown", this._onKeyDown.bind(this), true);

this.textEditor.addEventListener(WebInspector.TextEditor.Events.GutterClick, this._handleGutterClick.bind(this), this);

this._breakpointManager.addEventListener(WebInspector.BreakpointManager.Events.BreakpointAdded, this._breakpointAdded, this);
this._breakpointManager.addEventListener(WebInspector.BreakpointManager.Events.BreakpointRemoved, this._breakpointRemoved, this);

this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.FormattedChanged, this._onFormattedChanged, this);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.WorkingCopyChanged, this._onWorkingCopyChanged, this);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.ConsoleMessageAdded, this._consoleMessageAdded, this);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.ConsoleMessageRemoved, this._consoleMessageRemoved, this);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.ConsoleMessagesCleared, this._consoleMessagesCleared, this);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.SourceMappingChanged, this._onSourceMappingChanged, this);

this._updateScriptFile();
}

WebInspector.JavaScriptSourceFrame.prototype = {

wasShown: function()
{
WebInspector.SourceFrame.prototype.wasShown.call(this);
},

willHide: function()
{
WebInspector.SourceFrame.prototype.willHide.call(this);
this._popoverHelper.hidePopover();
},


canEditSource: function()
{
return this._uiSourceCode.isEditable();
},


commitEditing: function(text)
{
if (!this._uiSourceCode.isDirty())
return;

this._isCommittingEditing = true;
this._uiSourceCode.commitWorkingCopy(function() { });
delete this._isCommittingEditing;
},


_onFormattedChanged: function(event)
{
var content =   (event.data.content);
this._textEditor.setReadOnly(this._uiSourceCode.formatted());
this._innerSetContent(content);
},


_onWorkingCopyChanged: function(event)
{
this._innerSetContent(this._uiSourceCode.workingCopy());
},

_innerSetContent: function(content)
{
if (this._isSettingWorkingCopy || this._isCommittingEditing)
return;

this.setContent(content, false, this._uiSourceCode.mimeType());
},

populateLineGutterContextMenu: function(contextMenu, lineNumber)
{
contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Continue to here" : "Continue to Here"), this._continueToLine.bind(this, lineNumber));

var breakpoint = this._breakpointManager.findBreakpoint(this._uiSourceCode, lineNumber);
if (!breakpoint) {

contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Add breakpoint" : "Add Breakpoint"), this._setBreakpoint.bind(this, lineNumber, "", true));
contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Add conditional breakpoint…" : "Add Conditional Breakpoint…"), this._editBreakpointCondition.bind(this, lineNumber));
} else {

contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Remove breakpoint" : "Remove Breakpoint"), breakpoint.remove.bind(breakpoint));
contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Edit breakpoint…" : "Edit Breakpoint…"), this._editBreakpointCondition.bind(this, lineNumber, breakpoint));
if (breakpoint.enabled())
contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Disable breakpoint" : "Disable Breakpoint"), breakpoint.setEnabled.bind(breakpoint, false));
else
contextMenu.appendItem(WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Enable breakpoint" : "Enable Breakpoint"), breakpoint.setEnabled.bind(breakpoint, true));
}
},

populateTextAreaContextMenu: function(contextMenu, lineNumber)
{
WebInspector.SourceFrame.prototype.populateTextAreaContextMenu.call(this, contextMenu, lineNumber);
var selection = window.getSelection();
if (selection.type === "Range" && !selection.isCollapsed) {
var addToWatchLabel = WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Add to watch" : "Add to Watch");
contextMenu.appendItem(addToWatchLabel, this._scriptsPanel.addToWatch.bind(this._scriptsPanel, selection.toString()));
var evaluateLabel = WebInspector.UIString(WebInspector.useLowerCaseMenuTitles() ? "Evaluate in console" : "Evaluate in Console");
contextMenu.appendItem(evaluateLabel, WebInspector.evaluateInConsole.bind(WebInspector, selection.toString()));
contextMenu.appendSeparator();
}
contextMenu.appendApplicableItems(this._uiSourceCode);
},

onTextChanged: function(oldRange, newRange)
{
WebInspector.SourceFrame.prototype.onTextChanged.call(this, oldRange, newRange);
this._isSettingWorkingCopy = true;
this._uiSourceCode.setWorkingCopy(this._textEditor.text());
delete this._isSettingWorkingCopy;
},

_willMergeToVM: function()
{
if (this._supportsEnabledBreakpointsWhileEditing())
return;
this._preserveDecorations = true;
},

_didMergeToVM: function()
{
if (this._supportsEnabledBreakpointsWhileEditing())
return;
delete this._preserveDecorations;
this._restoreBreakpointsAfterEditing();
},

_willDivergeFromVM: function()
{
if (this._supportsEnabledBreakpointsWhileEditing())
return;
this._preserveDecorations = true;
},

_didDivergeFromVM: function()
{
if (this._supportsEnabledBreakpointsWhileEditing())
return;
delete this._preserveDecorations;
this._muteBreakpointsWhileEditing();
},

_muteBreakpointsWhileEditing: function()
{
for (var lineNumber = 0; lineNumber < this._textEditor.linesCount; ++lineNumber) {
var breakpointDecoration = this._textEditor.getAttribute(lineNumber, "breakpoint");
if (!breakpointDecoration)
continue;
this._removeBreakpointDecoration(lineNumber);
this._addBreakpointDecoration(lineNumber, breakpointDecoration.condition, breakpointDecoration.enabled, true);
}
},

_supportsEnabledBreakpointsWhileEditing: function()
{
return this._uiSourceCode.isSnippet;
},

_restoreBreakpointsAfterEditing: function()
{
var breakpoints = {};

for (var lineNumber = 0; lineNumber < this._textEditor.linesCount; ++lineNumber) {
var breakpointDecoration = this._textEditor.getAttribute(lineNumber, "breakpoint");
if (breakpointDecoration) {
breakpoints[lineNumber] = breakpointDecoration;
this._removeBreakpointDecoration(lineNumber);
}
}


var breakpointLocations = this._breakpointManager.breakpointLocationsForUISourceCode(this._uiSourceCode);
var lineNumbers = {};
for (var i = 0; i < breakpointLocations.length; ++i) {
var breakpoint = breakpointLocations[i].breakpoint;
breakpointLocations[i].breakpoint.remove();
}


for (var lineNumberString in breakpoints) {
var lineNumber = parseInt(lineNumberString, 10);
if (isNaN(lineNumber))
continue;
var breakpointDecoration = breakpoints[lineNumberString];
this._setBreakpoint(lineNumber, breakpointDecoration.condition, breakpointDecoration.enabled);
}
},

_getPopoverAnchor: function(element, event)
{
if (!WebInspector.debuggerModel.isPaused())
return null;
if (window.getSelection().type === "Range")
return null;
var lineElement = element.enclosingNodeOrSelfWithClass("webkit-line-content");
if (!lineElement)
return null;

if (element.hasStyleClass("webkit-javascript-ident"))
return element;

if (element.hasStyleClass("source-frame-token"))
return element;


if (element.hasStyleClass("webkit-javascript-keyword"))
return element.textContent === "this" ? element : null;

if (element !== lineElement || lineElement.childElementCount)
return null;



var lineContent = lineElement.textContent;
var ranges = [];
var regex = new RegExp("[a-zA-Z_\$0-9]+", "g");
var match;
while (regex.lastIndex < lineContent.length && (match = regex.exec(lineContent)))
ranges.push({offset: match.index, length: regex.lastIndex - match.index});


var changes = [];
WebInspector.highlightRangesWithStyleClass(lineElement, ranges, "source-frame-token", changes);
var lineOffsetLeft = lineElement.totalOffsetLeft();
for (var child = lineElement.firstChild; child; child = child.nextSibling) {
if (child.nodeType !== Node.ELEMENT_NODE || !child.hasStyleClass("source-frame-token"))
continue;
if (event.x > lineOffsetLeft + child.offsetLeft && event.x < lineOffsetLeft + child.offsetLeft + child.offsetWidth) {
var text = child.textContent;
return (text === "this" || !WebInspector.SourceJavaScriptTokenizer.Keywords[text]) ? child : null;
}
}
return null;
},

_resolveObjectForPopover: function(element, showCallback, objectGroupName)
{
this._highlightElement = this._highlightExpression(element);


function showObjectPopover(result, wasThrown)
{
if (!WebInspector.debuggerModel.isPaused()) {
this._popoverHelper.hidePopover();
return;
}
showCallback(WebInspector.RemoteObject.fromPayload(result), wasThrown, this._highlightElement);

if (this._highlightElement)
this._highlightElement.addStyleClass("source-frame-eval-expression");
}

if (!WebInspector.debuggerModel.isPaused()) {
this._popoverHelper.hidePopover();
return;
}
var selectedCallFrame = WebInspector.debuggerModel.selectedCallFrame();
selectedCallFrame.evaluate(this._highlightElement.textContent, objectGroupName, false, true, false, false, showObjectPopover.bind(this));
},

_onHidePopover: function()
{

var highlightElement = this._highlightElement;
if (!highlightElement)
return;


var parentElement = highlightElement.parentElement;
if (parentElement) {
var child = highlightElement.firstChild;
while (child) {
var nextSibling = child.nextSibling;
parentElement.insertBefore(child, highlightElement);
child = nextSibling;
}
parentElement.removeChild(highlightElement);
}
delete this._highlightElement;
},

_highlightExpression: function(element)
{

var tokens = [ element ];
var token = element.previousSibling;
while (token && (token.className === "webkit-javascript-ident" || token.className === "source-frame-token" || token.className === "webkit-javascript-keyword" || token.textContent.trim() === ".")) {
tokens.push(token);
token = token.previousSibling;
}
tokens.reverse();


var parentElement = element.parentElement;
var nextElement = element.nextSibling;
var container = document.createElement("span");
for (var i = 0; i < tokens.length; ++i)
container.appendChild(tokens[i]);
parentElement.insertBefore(container, nextElement);
return container;
},


_addBreakpointDecoration: function(lineNumber, condition, enabled, mutedWhileEditing)
{
if (this._preserveDecorations)
return;
var breakpoint = {
condition: condition,
enabled: enabled
};
this.textEditor.setAttribute(lineNumber, "breakpoint", breakpoint);

var disabled = !enabled || mutedWhileEditing;
this.textEditor.addBreakpoint(lineNumber, disabled, !!condition);
},

_removeBreakpointDecoration: function(lineNumber)
{
if (this._preserveDecorations)
return;
this.textEditor.removeAttribute(lineNumber, "breakpoint");
this.textEditor.removeBreakpoint(lineNumber);
},

_onKeyDown: function(event)
{
if (event.keyIdentifier === "U+001B") { 
if (this._popoverHelper.isPopoverVisible()) {
this._popoverHelper.hidePopover();
event.consume();
}
}
},


_editBreakpointCondition: function(lineNumber, breakpoint)
{
this._conditionElement = this._createConditionElement(lineNumber);
this.textEditor.addDecoration(lineNumber, this._conditionElement);

function finishEditing(committed, element, newText)
{
this.textEditor.removeDecoration(lineNumber, this._conditionElement);
delete this._conditionEditorElement;
delete this._conditionElement;
if (breakpoint)
breakpoint.setCondition(newText);
else
this._setBreakpoint(lineNumber, newText, true);
}

var config = new WebInspector.EditingConfig(finishEditing.bind(this, true), finishEditing.bind(this, false));
WebInspector.startEditing(this._conditionEditorElement, config);
this._conditionEditorElement.value = breakpoint ? breakpoint.condition() : "";
this._conditionEditorElement.select();
},

_createConditionElement: function(lineNumber)
{
var conditionElement = document.createElement("div");
conditionElement.className = "source-frame-breakpoint-condition";

var labelElement = document.createElement("label");
labelElement.className = "source-frame-breakpoint-message";
labelElement.htmlFor = "source-frame-breakpoint-condition";
labelElement.appendChild(document.createTextNode(WebInspector.UIString("The breakpoint on line %d will stop only if this expression is true:", lineNumber)));
conditionElement.appendChild(labelElement);

var editorElement = document.createElement("input");
editorElement.id = "source-frame-breakpoint-condition";
editorElement.className = "monospace";
editorElement.type = "text";
conditionElement.appendChild(editorElement);
this._conditionEditorElement = editorElement;

return conditionElement;
},


setExecutionLine: function(lineNumber)
{
this._executionLineNumber = lineNumber;
if (this.loaded) {
this.textEditor.setExecutionLine(lineNumber);
this.revealLine(this._executionLineNumber);
if (this.canEditSource())
this.setSelection(WebInspector.TextRange.createFromLocation(lineNumber, 0));
}
},

clearExecutionLine: function()
{
if (this.loaded && typeof this._executionLineNumber === "number")
this.textEditor.clearExecutionLine();
delete this._executionLineNumber;
},

_lineNumberAfterEditing: function(lineNumber, oldRange, newRange)
{
var shiftOffset = lineNumber <= oldRange.startLine ? 0 : newRange.linesCount - oldRange.linesCount;


if (lineNumber === oldRange.startLine) {
var whiteSpacesRegex = /^[\s\xA0]*$/;
for (var i = 0; lineNumber + i <= newRange.endLine; ++i) {
if (!whiteSpacesRegex.test(this.textEditor.line(lineNumber + i))) {
shiftOffset = i;
break;
}
}
}

var newLineNumber = Math.max(0, lineNumber + shiftOffset);
if (oldRange.startLine < lineNumber && lineNumber < oldRange.endLine)
newLineNumber = oldRange.startLine;
return newLineNumber;
},

_breakpointAdded: function(event)
{
var uiLocation =   (event.data.uiLocation);

if (uiLocation.uiSourceCode !== this._uiSourceCode)
return;

var breakpoint =   (event.data.breakpoint);
if (this.loaded)
this._addBreakpointDecoration(uiLocation.lineNumber, breakpoint.condition(), breakpoint.enabled(), false);
},

_breakpointRemoved: function(event)
{
var uiLocation =   (event.data.uiLocation);
if (uiLocation.uiSourceCode !== this._uiSourceCode)
return;

var breakpoint =   (event.data.breakpoint);
var remainingBreakpoint = this._breakpointManager.findBreakpoint(this._uiSourceCode, uiLocation.lineNumber);
if (!remainingBreakpoint && this.loaded)
this._removeBreakpointDecoration(uiLocation.lineNumber);
},

_consoleMessageAdded: function(event)
{
var message =   (event.data);
if (this.loaded)
this.addMessageToSource(message.lineNumber, message.originalMessage);
},

_consoleMessageRemoved: function(event)
{
var message =   (event.data);
if (this.loaded)
this.removeMessageFromSource(message.lineNumber, message.originalMessage);
},

_consoleMessagesCleared: function(event)
{
this.clearMessages();
},


_onSourceMappingChanged: function(event)
{
this._updateScriptFile();
},

_updateScriptFile: function()
{
if (this._scriptFile) {
this._scriptFile.removeEventListener(WebInspector.ScriptFile.Events.WillMergeToVM, this._willMergeToVM, this);
this._scriptFile.removeEventListener(WebInspector.ScriptFile.Events.DidMergeToVM, this._didMergeToVM, this);
this._scriptFile.removeEventListener(WebInspector.ScriptFile.Events.WillDivergeFromVM, this._willDivergeFromVM, this);
this._scriptFile.removeEventListener(WebInspector.ScriptFile.Events.DidDivergeFromVM, this._didDivergeFromVM, this);
}
this._scriptFile = this._uiSourceCode.scriptFile();
if (this._scriptFile) {
this._scriptFile.addEventListener(WebInspector.ScriptFile.Events.WillMergeToVM, this._willMergeToVM, this);
this._scriptFile.addEventListener(WebInspector.ScriptFile.Events.DidMergeToVM, this._didMergeToVM, this);
this._scriptFile.addEventListener(WebInspector.ScriptFile.Events.WillDivergeFromVM, this._willDivergeFromVM, this);
this._scriptFile.addEventListener(WebInspector.ScriptFile.Events.DidDivergeFromVM, this._didDivergeFromVM, this);
}
},

onTextEditorContentLoaded: function()
{
if (typeof this._executionLineNumber === "number")
this.setExecutionLine(this._executionLineNumber);

var breakpointLocations = this._breakpointManager.breakpointLocationsForUISourceCode(this._uiSourceCode);
for (var i = 0; i < breakpointLocations.length; ++i) {
var breakpoint = breakpointLocations[i].breakpoint;
this._addBreakpointDecoration(breakpointLocations[i].uiLocation.lineNumber, breakpoint.condition(), breakpoint.enabled(), false);
}

var messages = this._uiSourceCode.consoleMessages();
for (var i = 0; i < messages.length; ++i) {
var message = messages[i];
this.addMessageToSource(message.lineNumber, message.originalMessage);
}
},


_handleGutterClick: function(event)
{
if (this._uiSourceCode.isDirty() && !this._supportsEnabledBreakpointsWhileEditing())
return;

var lineNumber = event.data.lineNumber;
var eventObject =   (event.data.event);

if (eventObject.button != 0 || eventObject.altKey || eventObject.ctrlKey || eventObject.metaKey)
return;

this._toggleBreakpoint(lineNumber, eventObject.shiftKey);
eventObject.consume(true);
},


_toggleBreakpoint: function(lineNumber, onlyDisable)
{
var breakpoint = this._breakpointManager.findBreakpoint(this._uiSourceCode, lineNumber);
if (breakpoint) {
if (onlyDisable)
breakpoint.setEnabled(!breakpoint.enabled());
else
breakpoint.remove();
} else
this._setBreakpoint(lineNumber, "", true);
},

toggleBreakpointOnCurrentLine: function()
{
var selection = this.textEditor.selection();
if (!selection)
return;
this._toggleBreakpoint(selection.startLine, false);
},


_setBreakpoint: function(lineNumber, condition, enabled)
{
this._breakpointManager.setBreakpoint(this._uiSourceCode, lineNumber, condition, enabled);
},


_continueToLine: function(lineNumber)
{
var rawLocation =   (this._uiSourceCode.uiLocationToRawLocation(lineNumber, 0));
WebInspector.debuggerModel.continueToLocation(rawLocation);
},

__proto__: WebInspector.SourceFrame.prototype
}
;



WebInspector.NavigatorOverlayController = function(parentSidebarView, navigatorView, editorView)
{
this._parentSidebarView = parentSidebarView;
this._navigatorView = navigatorView;
this._editorView = editorView;

this._navigatorSidebarResizeWidgetElement = document.createElement("div");
this._navigatorSidebarResizeWidgetElement.addStyleClass("scripts-navigator-resizer-widget");
this._parentSidebarView.installResizer(this._navigatorSidebarResizeWidgetElement);
this._navigatorView.element.appendChild(this._navigatorSidebarResizeWidgetElement);

this._navigatorShowHideButton = new WebInspector.StatusBarButton(WebInspector.UIString("Hide navigator"), "scripts-navigator-show-hide-button", 3);
this._navigatorShowHideButton.state = "pinned";
this._navigatorShowHideButton.addEventListener("click", this._toggleNavigator, this);
this._editorView.element.appendChild(this._navigatorShowHideButton.element);

WebInspector.settings.navigatorHidden = WebInspector.settings.createSetting("navigatorHidden", true);
if (WebInspector.settings.navigatorHidden.get())
this._toggleNavigator();
}

WebInspector.NavigatorOverlayController.prototype = {
wasShown: function()
{
window.setTimeout(this._maybeShowNavigatorOverlay.bind(this), 0);
},

_maybeShowNavigatorOverlay: function()
{
if (WebInspector.settings.navigatorHidden.get() && !WebInspector.settings.navigatorWasOnceHidden.get())
this.showNavigatorOverlay();
},

_toggleNavigator: function()
{
if (this._navigatorShowHideButton.state === "overlay")
this._pinNavigator();
else if (this._navigatorShowHideButton.state === "hidden")
this.showNavigatorOverlay();
else
this._hidePinnedNavigator();
},

_hidePinnedNavigator: function()
{
this._navigatorShowHideButton.state = "hidden";
this._navigatorShowHideButton.title = WebInspector.UIString("Show navigator");
this._parentSidebarView.element.appendChild(this._navigatorShowHideButton.element);

this._editorView.element.addStyleClass("navigator-hidden");
this._navigatorSidebarResizeWidgetElement.addStyleClass("hidden");

this._parentSidebarView.hideSidebarElement();
this._navigatorView.detach();
this._editorView.focus();

WebInspector.settings.navigatorWasOnceHidden.set(true);
WebInspector.settings.navigatorHidden.set(true);
},

_pinNavigator: function()
{
this._navigatorShowHideButton.state = "pinned";
this._navigatorShowHideButton.title = WebInspector.UIString("Hide navigator");

this._editorView.element.removeStyleClass("navigator-hidden");
this._navigatorSidebarResizeWidgetElement.removeStyleClass("hidden");
this._editorView.element.appendChild(this._navigatorShowHideButton.element);

this._innerHideNavigatorOverlay();
this._parentSidebarView.showSidebarElement();
this._navigatorView.show(this._parentSidebarView.sidebarElement);
this._navigatorView.focus();
WebInspector.settings.navigatorHidden.set(false);
},

showNavigatorOverlay: function()
{
if (this._navigatorShowHideButton.state === "overlay")
return;

this._navigatorShowHideButton.state = "overlay";
this._navigatorShowHideButton.title = WebInspector.UIString("Pin navigator");

this._sidebarOverlay = new WebInspector.SidebarOverlay(this._navigatorView, "scriptsPanelNavigatorOverlayWidth", Preferences.minScriptsSidebarWidth);
this._boundKeyDown = this._keyDown.bind(this);
this._sidebarOverlay.element.addEventListener("keydown", this._boundKeyDown, false);
var navigatorOverlayResizeWidgetElement = document.createElement("div");
navigatorOverlayResizeWidgetElement.addStyleClass("scripts-navigator-resizer-widget");
this._sidebarOverlay.resizerWidgetElement = navigatorOverlayResizeWidgetElement;

this._navigatorView.element.appendChild(this._navigatorShowHideButton.element);
this._boundContainingElementFocused = this._containingElementFocused.bind(this);
this._parentSidebarView.element.addEventListener("mousedown", this._boundContainingElementFocused, false);

this._sidebarOverlay.show(this._parentSidebarView.element);
this._navigatorView.focus();
},

_keyDown: function(event)
{
if (event.handled)
return;

if (event.keyCode === WebInspector.KeyboardShortcut.Keys.Esc.code) {
this.hideNavigatorOverlay();
event.consume(true);
}
},

hideNavigatorOverlay: function()
{
if (this._navigatorShowHideButton.state !== "overlay")
return;

this._navigatorShowHideButton.state = "hidden";
this._navigatorShowHideButton.title = WebInspector.UIString("Show navigator");
this._parentSidebarView.element.appendChild(this._navigatorShowHideButton.element);

this._innerHideNavigatorOverlay();
this._editorView.focus();
},

_innerHideNavigatorOverlay: function()
{
this._parentSidebarView.element.removeEventListener("mousedown", this._boundContainingElementFocused, false);
this._sidebarOverlay.element.removeEventListener("keydown", this._boundKeyDown, false);
this._sidebarOverlay.hide();
},

_containingElementFocused: function(event)
{
if (!event.target.isSelfOrDescendant(this._sidebarOverlay.element))
this.hideNavigatorOverlay();
},

isNavigatorPinned: function()
{
return this._navigatorShowHideButton.state === "pinned";
},

isNavigatorHidden: function()
{
return this._navigatorShowHideButton.state === "hidden";
}
}
;



WebInspector.NavigatorView = function()
{
WebInspector.View.call(this);
this.registerRequiredCSS("navigatorView.css");

this._treeSearchBoxElement = document.createElement("div");
this._treeSearchBoxElement.className = "navigator-tree-search-box";
this.element.appendChild(this._treeSearchBoxElement);

var scriptsTreeElement = document.createElement("ol");
this._scriptsTree = new WebInspector.NavigatorTreeOutline(this._treeSearchBoxElement, scriptsTreeElement);

var scriptsOutlineElement = document.createElement("div");
scriptsOutlineElement.addStyleClass("outline-disclosure");
scriptsOutlineElement.addStyleClass("navigator");
scriptsOutlineElement.appendChild(scriptsTreeElement);

this.element.addStyleClass("fill");
this.element.addStyleClass("navigator-container");
this.element.appendChild(scriptsOutlineElement);
this.setDefaultFocusedElement(this._scriptsTree.element);

this._folderTreeElements = {};
this._scriptTreeElementsByUISourceCode = new Map();

WebInspector.settings.showScriptFolders.addChangeListener(this._showScriptFoldersSettingChanged.bind(this));
}


WebInspector.NavigatorView.Events = {
ItemSelected: "ItemSelected",
FileRenamed: "FileRenamed"
}

WebInspector.NavigatorView.prototype = {

addUISourceCode: function(uiSourceCode)
{
if (this._scriptTreeElementsByUISourceCode.get(uiSourceCode))
return;

var scriptTreeElement = new WebInspector.NavigatorSourceTreeElement(this, uiSourceCode, "");
this._scriptTreeElementsByUISourceCode.put(uiSourceCode, scriptTreeElement);
this._updateScriptTitle(uiSourceCode);
this._addUISourceCodeListeners(uiSourceCode);

var folderTreeElement = this.getOrCreateFolderTreeElement(uiSourceCode);
folderTreeElement.appendChild(scriptTreeElement);
},

_uiSourceCodeTitleChanged: function(event)
{
var uiSourceCode =   (event.target);
this._updateScriptTitle(uiSourceCode)
},

_uiSourceCodeWorkingCopyChanged: function(event)
{
var uiSourceCode =   (event.target);
this._updateScriptTitle(uiSourceCode)
},

_uiSourceCodeWorkingCopyCommitted: function(event)
{
var uiSourceCode =   (event.target);
this._updateScriptTitle(uiSourceCode)
},

_uiSourceCodeFormattedChanged: function(event)
{
var uiSourceCode =   (event.target);
this._updateScriptTitle(uiSourceCode);
},


_updateScriptTitle: function(uiSourceCode, ignoreIsDirty)
{
var scriptTreeElement = this._scriptTreeElementsByUISourceCode.get(uiSourceCode);
if (!scriptTreeElement)
return;

var titleText;
if (uiSourceCode.parsedURL.isValid) {
titleText = uiSourceCode.parsedURL.lastPathComponent;
if (uiSourceCode.parsedURL.queryParams)
titleText += "?" + uiSourceCode.parsedURL.queryParams;
} else if (uiSourceCode.parsedURL)
titleText = uiSourceCode.parsedURL.url;
if (!titleText)
titleText = WebInspector.UIString("(program)");
if (!ignoreIsDirty && uiSourceCode.isDirty())
titleText = "*" + titleText;
scriptTreeElement.titleText = titleText;
},


isScriptSourceAdded: function(uiSourceCode)
{
var scriptTreeElement = this._scriptTreeElementsByUISourceCode.get(uiSourceCode);
return !!scriptTreeElement;
},


revealUISourceCode: function(uiSourceCode)
{
if (this._scriptsTree.selectedTreeElement)
this._scriptsTree.selectedTreeElement.deselect();

this._lastSelectedUISourceCode = uiSourceCode;

var scriptTreeElement = this._scriptTreeElementsByUISourceCode.get(uiSourceCode);
scriptTreeElement.revealAndSelect(true);
},


_scriptSelected: function(uiSourceCode, focusSource)
{
this._lastSelectedUISourceCode = uiSourceCode;
var data = { uiSourceCode: uiSourceCode, focusSource: focusSource};
this.dispatchEventToListeners(WebInspector.NavigatorView.Events.ItemSelected, data);
},


removeUISourceCode: function(uiSourceCode)
{
var treeElement = this._scriptTreeElementsByUISourceCode.get(uiSourceCode);
while (treeElement) {
var parent = treeElement.parent;
if (parent) {
if (treeElement instanceof WebInspector.NavigatorFolderTreeElement)
delete this._folderTreeElements[treeElement.folderIdentifier];
parent.removeChild(treeElement);
if (parent.children.length)
break;
}
treeElement = parent;
}
this._scriptTreeElementsByUISourceCode.remove(uiSourceCode);
this._removeUISourceCodeListeners(uiSourceCode);
},


_addUISourceCodeListeners: function(uiSourceCode)
{
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.TitleChanged, this._uiSourceCodeTitleChanged, this);
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.WorkingCopyChanged, this._uiSourceCodeWorkingCopyChanged, this);
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.WorkingCopyCommitted, this._uiSourceCodeWorkingCopyCommitted, this);
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.FormattedChanged, this._uiSourceCodeFormattedChanged, this);
},


_removeUISourceCodeListeners: function(uiSourceCode)
{
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.TitleChanged, this._uiSourceCodeTitleChanged, this);
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.WorkingCopyChanged, this._uiSourceCodeWorkingCopyChanged, this);
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.WorkingCopyCommitted, this._uiSourceCodeWorkingCopyCommitted, this);
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.FormattedChanged, this._uiSourceCodeFormattedChanged, this);
},

_showScriptFoldersSettingChanged: function()
{
var uiSourceCodes = this._scriptsTree.scriptTreeElements();
this.reset();

for (var i = 0; i < uiSourceCodes.length; ++i)
this.addUISourceCode(uiSourceCodes[i]);

if (this._lastSelectedUISourceCode)
this.revealUISourceCode(this._lastSelectedUISourceCode);
},

_fileRenamed: function(uiSourceCode, newTitle)
{    
var data = { uiSourceCode: uiSourceCode, name: newTitle };
this.dispatchEventToListeners(WebInspector.NavigatorView.Events.FileRenamed, data);
},


rename: function(uiSourceCode, callback)
{
var scriptTreeElement = this._scriptTreeElementsByUISourceCode.get(uiSourceCode);
if (!scriptTreeElement)
return;


var treeOutlineElement = scriptTreeElement.treeOutline.element;
WebInspector.markBeingEdited(treeOutlineElement, true);

function commitHandler(element, newTitle, oldTitle)
{
if (newTitle && newTitle !== oldTitle)
this._fileRenamed(uiSourceCode, newTitle);
afterEditing.call(this, true);
}

function cancelHandler()
{
afterEditing.call(this, false);
}


function afterEditing(committed)
{
WebInspector.markBeingEdited(treeOutlineElement, false);
this._updateScriptTitle(uiSourceCode);
if (callback)
callback(committed);
}

var editingConfig = new WebInspector.EditingConfig(commitHandler.bind(this), cancelHandler.bind(this));
this._updateScriptTitle(uiSourceCode, true);
WebInspector.startEditing(scriptTreeElement.titleElement, editingConfig);
window.getSelection().setBaseAndExtent(scriptTreeElement.titleElement, 0, scriptTreeElement.titleElement, 1);
},

reset: function()
{
var uiSourceCodes = this._scriptsTree.scriptTreeElements;
for (var i = 0; i < uiSourceCodes.length; ++i)
this._removeUISourceCodeListeners(uiSourceCodes[i]);

this._scriptsTree.stopSearch();
this._scriptsTree.removeChildren();
this._folderTreeElements = {};
this._scriptTreeElementsByUISourceCode.clear();
},


createFolderTreeElement: function(parentFolderElement, folderIdentifier, domain, folderName)
{
var folderTreeElement = new WebInspector.NavigatorFolderTreeElement(folderIdentifier, domain, folderName);
parentFolderElement.appendChild(folderTreeElement);
this._folderTreeElements[folderIdentifier] = folderTreeElement;
return folderTreeElement;
},


getOrCreateFolderTreeElement: function(uiSourceCode)
{
return this._getOrCreateFolderTreeElement(uiSourceCode.parsedURL.host, uiSourceCode.parsedURL.folderPathComponents);
},


_getOrCreateFolderTreeElement: function(domain, folderName)
{
var folderIdentifier = domain + "/" + folderName;

if (this._folderTreeElements[folderIdentifier])
return this._folderTreeElements[folderIdentifier];

var showScriptFolders = WebInspector.settings.showScriptFolders.get();

if ((!domain && !folderName) || !showScriptFolders)
return this._scriptsTree;

var parentFolderElement;
if (!folderName)
parentFolderElement = this._scriptsTree;
else
parentFolderElement = this._getOrCreateFolderTreeElement(domain, "");

return this.createFolderTreeElement(parentFolderElement, folderIdentifier, domain, folderName);
},

handleContextMenu: function(event, uiSourceCode)
{
var contextMenu = new WebInspector.ContextMenu(event);
contextMenu.appendApplicableItems(uiSourceCode);
contextMenu.show();
},

__proto__: WebInspector.View.prototype
}


WebInspector.NavigatorTreeOutline = function(treeSearchBoxElement, element)
{
TreeOutline.call(this, element);
this.element = element;

this._treeSearchBoxElement = treeSearchBoxElement;

this.comparator = WebInspector.NavigatorTreeOutline._treeElementsCompare;

this.searchable = true;
this.searchInputElement = document.createElement("input");
}

WebInspector.NavigatorTreeOutline._treeElementsCompare = function compare(treeElement1, treeElement2)
{

function typeWeight(treeElement)
{
if (treeElement instanceof WebInspector.NavigatorFolderTreeElement) {
if (treeElement.isDomain) {
if (treeElement.titleText === WebInspector.inspectedPageDomain)
return 1;
return 2;
}
return 3;
}
return 4;
}

var typeWeight1 = typeWeight(treeElement1);
var typeWeight2 = typeWeight(treeElement2);

var result;
if (typeWeight1 > typeWeight2)
result = 1;
else if (typeWeight1 < typeWeight2)
result = -1;
else {
var title1 = treeElement1.titleText;
var title2 = treeElement2.titleText;
result = title1.localeCompare(title2);
}
return result;
}

WebInspector.NavigatorTreeOutline.prototype = {

scriptTreeElements: function()
{
var result = [];
if (this.children.length) {
for (var treeElement = this.children[0]; treeElement; treeElement = treeElement.traverseNextTreeElement(false, this, true)) {
if (treeElement instanceof WebInspector.NavigatorSourceTreeElement)
result.push(treeElement.uiSourceCode);
}
}
return result;
},

searchStarted: function()
{
this._treeSearchBoxElement.appendChild(this.searchInputElement);
this._treeSearchBoxElement.addStyleClass("visible");
},

searchFinished: function()
{
this._treeSearchBoxElement.removeChild(this.searchInputElement);
this._treeSearchBoxElement.removeStyleClass("visible");
},

__proto__: TreeOutline.prototype
}


WebInspector.BaseNavigatorTreeElement = function(title, iconClasses, hasChildren, noIcon)
{
TreeElement.call(this, "", null, hasChildren);
this._titleText = title;
this._iconClasses = iconClasses;
this._noIcon = noIcon;
}

WebInspector.BaseNavigatorTreeElement.prototype = {
onattach: function()
{
this.listItemElement.removeChildren();
if (this._iconClasses) {
for (var i = 0; i < this._iconClasses.length; ++i)
this.listItemElement.addStyleClass(this._iconClasses[i]);
}

var selectionElement = document.createElement("div");
selectionElement.className = "selection";
this.listItemElement.appendChild(selectionElement);

if (!this._noIcon) {
this.imageElement = document.createElement("img");
this.imageElement.className = "icon";
this.listItemElement.appendChild(this.imageElement);
}

this.titleElement = document.createElement("div");
this.titleElement.className = "base-navigator-tree-element-title";
this._titleTextNode = document.createTextNode("");
this._titleTextNode.textContent = this._titleText;
this.titleElement.appendChild(this._titleTextNode);
this.listItemElement.appendChild(this.titleElement);
this.expand();
},

onreveal: function()
{
if (this.listItemElement)
this.listItemElement.scrollIntoViewIfNeeded(true);
},


get titleText()
{
return this._titleText;
},

set titleText(titleText)
{
this._titleText = titleText || "";
if (this.titleElement)
this.titleElement.textContent = this._titleText;
},


matchesSearchText: function(searchText)
{
return this.titleText.match(new RegExp("^" + searchText.escapeForRegExp(), "i"));
},

__proto__: TreeElement.prototype
}


WebInspector.NavigatorFolderTreeElement = function(folderIdentifier, domain, folderName)
{
this._folderIdentifier = folderIdentifier;
this._folderName = folderName;

var iconClass = this.isDomain ? "navigator-domain-tree-item" : "navigator-folder-tree-item";
var title = this.isDomain ? domain : folderName.substring(1);

WebInspector.BaseNavigatorTreeElement.call(this, title, [iconClass], true);
this.tooltip = folderName;
}

WebInspector.NavigatorFolderTreeElement.prototype = {

get folderIdentifier()
{
return this._folderIdentifier;
},


get isDomain()
{
return this._folderName === "";
},

onattach: function()
{
WebInspector.BaseNavigatorTreeElement.prototype.onattach.call(this);
if (this.isDomain && this.titleText != WebInspector.inspectedPageDomain)
this.collapse();
else
this.expand();
},

__proto__: WebInspector.BaseNavigatorTreeElement.prototype
}


WebInspector.NavigatorSourceTreeElement = function(navigatorView, uiSourceCode, title)
{
WebInspector.BaseNavigatorTreeElement.call(this, title, ["navigator-" + uiSourceCode.contentType().name() + "-tree-item"], false);
this._navigatorView = navigatorView;
this._uiSourceCode = uiSourceCode;
this.tooltip = uiSourceCode.url;
}

WebInspector.NavigatorSourceTreeElement.prototype = {

get uiSourceCode()
{
return this._uiSourceCode;
},

onattach: function()
{
WebInspector.BaseNavigatorTreeElement.prototype.onattach.call(this);
this.listItemElement.draggable = true;
this.listItemElement.addEventListener("click", this._onclick.bind(this), false);
this.listItemElement.addEventListener("contextmenu", this._handleContextMenuEvent.bind(this), false);
this.listItemElement.addEventListener("mousedown", this._onmousedown.bind(this), false);
this.listItemElement.addEventListener("dragstart", this._ondragstart.bind(this), false);
},

_onmousedown: function(event)
{
if (event.which === 1) 
this._uiSourceCode.requestContent(callback.bind(this));

function callback(content, contentEncoded, mimeType)
{
this._warmedUpContent = content;
}
},

_ondragstart: function(event)
{
event.dataTransfer.setData("text/plain", this._warmedUpContent);
event.dataTransfer.effectAllowed = "copy";
return true;
},

onspace: function()
{
this._navigatorView._scriptSelected(this.uiSourceCode, true);
return true;
},


_onclick: function(event)
{
this._navigatorView._scriptSelected(this.uiSourceCode, false);
},


ondblclick: function(event)
{
var middleClick = event.button === 1;
this._navigatorView._scriptSelected(this.uiSourceCode, !middleClick);
},

onenter: function()
{
this._navigatorView._scriptSelected(this.uiSourceCode, true);
return true;
},


_handleContextMenuEvent: function(event)
{
this._navigatorView.handleContextMenu(event, this._uiSourceCode);
},

__proto__: WebInspector.BaseNavigatorTreeElement.prototype
}
;



WebInspector.RevisionHistoryView = function()
{
WebInspector.View.call(this);
this.registerRequiredCSS("revisionHistory.css");
this.element.addStyleClass("revision-history-drawer");
this.element.addStyleClass("fill");
this.element.addStyleClass("outline-disclosure");
this._uiSourceCodeItems = new Map();

var olElement = this.element.createChild("ol");
this._treeOutline = new TreeOutline(olElement);


function populateRevisions(uiSourceCode)
{
if (uiSourceCode.history.length)
this._createUISourceCodeItem(uiSourceCode);
}

WebInspector.workspace.uiSourceCodes().forEach(populateRevisions.bind(this));
WebInspector.workspace.addEventListener(WebInspector.Workspace.Events.UISourceCodeContentCommitted, this._revisionAdded, this);
WebInspector.workspace.addEventListener(WebInspector.UISourceCodeProvider.Events.UISourceCodeRemoved, this._uiSourceCodeRemoved, this);
WebInspector.workspace.addEventListener(WebInspector.UISourceCodeProvider.Events.TemporaryUISourceCodeRemoved, this._uiSourceCodeRemoved, this);
WebInspector.workspace.addEventListener(WebInspector.Workspace.Events.ProjectWillReset, this._reset, this);

this._statusElement = document.createElement("span");
this._statusElement.textContent = WebInspector.UIString("Local modifications");

}


WebInspector.RevisionHistoryView.showHistory = function(uiSourceCode)
{
if (!WebInspector.RevisionHistoryView._view) 
WebInspector.RevisionHistoryView._view = new WebInspector.RevisionHistoryView();
var view = WebInspector.RevisionHistoryView._view;
WebInspector.showViewInDrawer(view._statusElement, view);
view._revealUISourceCode(uiSourceCode);
}

WebInspector.RevisionHistoryView.prototype = {

_createUISourceCodeItem: function(uiSourceCode)
{
var uiSourceCodeItem = new TreeElement(uiSourceCode.parsedURL.displayName, null, true);
uiSourceCodeItem.selectable = false;


for (var i = 0; i < this._treeOutline.children.length; ++i) {
if (this._treeOutline.children[i].title.localeCompare(uiSourceCode.parsedURL.displayName) > 0) {
this._treeOutline.insertChild(uiSourceCodeItem, i);
break;
}
}
if (i === this._treeOutline.children.length)
this._treeOutline.appendChild(uiSourceCodeItem);

this._uiSourceCodeItems.put(uiSourceCode, uiSourceCodeItem);

var revisionCount = uiSourceCode.history.length;
for (var i = revisionCount - 1; i >= 0; --i) {
var revision = uiSourceCode.history[i];
var historyItem = new WebInspector.RevisionHistoryTreeElement(revision, uiSourceCode.history[i - 1], i !== revisionCount - 1);
uiSourceCodeItem.appendChild(historyItem);
}

var linkItem = new TreeElement("", null, false);
linkItem.selectable = false;
uiSourceCodeItem.appendChild(linkItem);

var revertToOriginal = linkItem.listItemElement.createChild("span", "revision-history-link revision-history-link-row");
revertToOriginal.textContent = WebInspector.UIString("apply original content");
revertToOriginal.addEventListener("click", uiSourceCode.revertToOriginal.bind(uiSourceCode));

var clearHistoryElement = uiSourceCodeItem.listItemElement.createChild("span", "revision-history-link");
clearHistoryElement.textContent = WebInspector.UIString("revert");
clearHistoryElement.addEventListener("click", this._clearHistory.bind(this, uiSourceCode));
return uiSourceCodeItem;
},


_clearHistory: function(uiSourceCode)
{
uiSourceCode.revertAndClearHistory(this._removeUISourceCode.bind(this));
},

_revisionAdded: function(event)
{
var uiSourceCode =   (event.data.uiSourceCode);
var uiSourceCodeItem = this._uiSourceCodeItems.get(uiSourceCode);
if (!uiSourceCodeItem) {
uiSourceCodeItem = this._createUISourceCodeItem(uiSourceCode);
return;
}

var historyLength = uiSourceCode.history.length;
var historyItem = new WebInspector.RevisionHistoryTreeElement(uiSourceCode.history[historyLength - 1], uiSourceCode.history[historyLength - 2], false);
if (uiSourceCodeItem.children.length)
uiSourceCodeItem.children[0].allowRevert();
uiSourceCodeItem.insertChild(historyItem, 0);
},


_revealUISourceCode: function(uiSourceCode)
{
var uiSourceCodeItem = this._uiSourceCodeItems.get(uiSourceCode);
if (uiSourceCodeItem) {
uiSourceCodeItem.reveal();
uiSourceCodeItem.expand();
}
},

_uiSourceCodeRemoved: function(event)
{
var uiSourceCode =   (event.data);
this._removeUISourceCode(uiSourceCode);
},


_removeUISourceCode: function(uiSourceCode)
{
var uiSourceCodeItem = this._uiSourceCodeItems.get(uiSourceCode);
if (!uiSourceCodeItem)
return;
this._treeOutline.removeChild(uiSourceCodeItem);
this._uiSourceCodeItems.remove(uiSourceCode);
},

_reset: function()
{
this._treeOutline.removeChildren();
this._uiSourceCodeItems.clear();
},

__proto__: WebInspector.View.prototype
}


WebInspector.RevisionHistoryTreeElement = function(revision, baseRevision, allowRevert)
{
TreeElement.call(this, revision.timestamp.toLocaleTimeString(), null, true);
this.selectable = false;

this._revision = revision;
this._baseRevision = baseRevision;

this._revertElement = document.createElement("span");
this._revertElement.className = "revision-history-link";
this._revertElement.textContent = WebInspector.UIString("apply revision content");
this._revertElement.addEventListener("click", this._revision.revertToThis.bind(this._revision), false);
if (!allowRevert)
this._revertElement.addStyleClass("hidden");
}

WebInspector.RevisionHistoryTreeElement.prototype = {
onattach: function()
{
this.listItemElement.addStyleClass("revision-history-revision");
},

onexpand: function()
{
this.listItemElement.appendChild(this._revertElement);

if (this._wasExpandedOnce)
return;
this._wasExpandedOnce = true;

this.childrenListElement.addStyleClass("source-code");
if (this._baseRevision)
this._baseRevision.requestContent(step1.bind(this));
else
this._revision.uiSourceCode.requestOriginalContent(step1.bind(this));

function step1(baseContent)
{
this._revision.requestContent(step2.bind(this, baseContent));
}

function step2(baseContent, newContent)
{
var baseLines = difflib.stringAsLines(baseContent);
var newLines = difflib.stringAsLines(newContent);
var sm = new difflib.SequenceMatcher(baseLines, newLines);
var opcodes = sm.get_opcodes();
var lastWasSeparator = false;

for (var idx = 0; idx < opcodes.length; idx++) {
var code = opcodes[idx];
var change = code[0];
var b = code[1];
var be = code[2];
var n = code[3];
var ne = code[4];
var rowCount = Math.max(be - b, ne - n);
var topRows = [];
var bottomRows = [];
for (var i = 0; i < rowCount; i++) {
if (change === "delete" || (change === "replace" && b < be)) {
var lineNumber = b++;
this._createLine(lineNumber, null, baseLines[lineNumber], "removed");
lastWasSeparator = false;
}

if (change === "insert" || (change === "replace" && n < ne)) {
var lineNumber = n++;
this._createLine(null, lineNumber, newLines[lineNumber], "added");
lastWasSeparator = false;
}

if (change === "equal") {
b++;
n++;
if (!lastWasSeparator)
this._createLine(null, null, "    \u2026", "separator");
lastWasSeparator = true;
}
}
}
}
},

oncollapse: function()
{
if (this._revertElement.parentElement)
this._revertElement.parentElement.removeChild(this._revertElement);
},


_createLine: function(baseLineNumber, newLineNumber, lineContent, changeType)
{
var child = new TreeElement("", null, false);
child.selectable = false;
this.appendChild(child);
var lineElement = document.createElement("span");

function appendLineNumber(lineNumber)
{
var numberString = lineNumber !== null ? numberToStringWithSpacesPadding(lineNumber + 1, 4) : "    ";
var lineNumberSpan = document.createElement("span");
lineNumberSpan.addStyleClass("webkit-line-number");
lineNumberSpan.textContent = numberString;
child.listItemElement.appendChild(lineNumberSpan);
}

appendLineNumber(baseLineNumber);
appendLineNumber(newLineNumber);

var contentSpan = document.createElement("span");
contentSpan.textContent = lineContent;
child.listItemElement.appendChild(contentSpan);
child.listItemElement.addStyleClass("revision-history-line");
child.listItemElement.addStyleClass("revision-history-line-" + changeType);
},

allowRevert: function()
{
this._revertElement.removeStyleClass("hidden");
},

__proto__: TreeElement.prototype
}
;



WebInspector.ScopeChainSidebarPane = function()
{
WebInspector.SidebarPane.call(this, WebInspector.UIString("Scope Variables"));
this._sections = [];
this._expandedSections = {};
this._expandedProperties = [];
}

WebInspector.ScopeChainSidebarPane.prototype = {
update: function(callFrame)
{
this.bodyElement.removeChildren();

if (!callFrame) {
var infoElement = document.createElement("div");
infoElement.className = "info";
infoElement.textContent = WebInspector.UIString("Not Paused");
this.bodyElement.appendChild(infoElement);
return;
}

for (var i = 0; i < this._sections.length; ++i) {
var section = this._sections[i];
if (!section.title)
continue;
if (section.expanded)
this._expandedSections[section.title] = true;
else
delete this._expandedSections[section.title];
}

this._sections = [];

var foundLocalScope = false;
var scopeChain = callFrame.scopeChain;
for (var i = 0; i < scopeChain.length; ++i) {
var scope = scopeChain[i];
var title = null;
var subtitle = scope.object.description;
var emptyPlaceholder = null;
var extraProperties = null;

switch (scope.type) {
case "local":
foundLocalScope = true;
title = WebInspector.UIString("Local");
emptyPlaceholder = WebInspector.UIString("No Variables");
subtitle = null;
if (callFrame.this)
extraProperties = [ new WebInspector.RemoteObjectProperty("this", WebInspector.RemoteObject.fromPayload(callFrame.this)) ];
if (i == 0) {
var details = WebInspector.debuggerModel.debuggerPausedDetails();
var exception = details.reason === WebInspector.DebuggerModel.BreakReason.Exception ? details.auxData : 0;
if (exception) {
extraProperties = extraProperties || [];
var exceptionObject =   (exception);
extraProperties.push(new WebInspector.RemoteObjectProperty("<exception>", WebInspector.RemoteObject.fromPayload(exceptionObject)));
}
}
break;
case "closure":
title = WebInspector.UIString("Closure");
emptyPlaceholder = WebInspector.UIString("No Variables");
subtitle = null;
break;
case "catch":
title = WebInspector.UIString("Catch");
subtitle = null;
break;
case "with":
title = WebInspector.UIString("With Block");
break;
case "global":
title = WebInspector.UIString("Global");
break;
}

if (!title || title === subtitle)
subtitle = null;

var section = new WebInspector.ObjectPropertiesSection(WebInspector.RemoteObject.fromPayload(scope.object), title, subtitle, emptyPlaceholder, true, extraProperties, WebInspector.ScopeVariableTreeElement);
section.editInSelectedCallFrameWhenPaused = true;
section.pane = this;

if (scope.type === "global")
section.expanded = false;
else if (!foundLocalScope || scope.type === "local" || title in this._expandedSections)
section.expanded = true;

this._sections.push(section);
this.bodyElement.appendChild(section.element);
}
},

__proto__: WebInspector.SidebarPane.prototype
}


WebInspector.ScopeVariableTreeElement = function(property)
{
WebInspector.ObjectPropertyTreeElement.call(this, property);
}

WebInspector.ScopeVariableTreeElement.prototype = {
onattach: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.onattach.call(this);
if (this.hasChildren && this.propertyIdentifier in this.treeOutline.section.pane._expandedProperties)
this.expand();
},

onexpand: function()
{
this.treeOutline.section.pane._expandedProperties[this.propertyIdentifier] = true;
},

oncollapse: function()
{
delete this.treeOutline.section.pane._expandedProperties[this.propertyIdentifier];
},

get propertyIdentifier()
{
if ("_propertyIdentifier" in this)
return this._propertyIdentifier;
var section = this.treeOutline.section;
this._propertyIdentifier = section.title + ":" + (section.subtitle ? section.subtitle + ":" : "") + this.propertyPath();
return this._propertyIdentifier;
},

__proto__: WebInspector.ObjectPropertyTreeElement.prototype
}
;



WebInspector.ScriptsNavigator = function()
{
WebInspector.Object.call(this);

this._tabbedPane = new WebInspector.TabbedPane();
this._tabbedPane.shrinkableTabs = true;
this._tabbedPane.element.addStyleClass("navigator-tabbed-pane");

this._scriptsView = new WebInspector.NavigatorView();
this._scriptsView.addEventListener(WebInspector.NavigatorView.Events.ItemSelected, this._scriptSelected, this);

this._contentScriptsView = new WebInspector.NavigatorView();
this._contentScriptsView.addEventListener(WebInspector.NavigatorView.Events.ItemSelected, this._scriptSelected, this);

this._snippetsView = new WebInspector.SnippetsNavigatorView();
this._snippetsView.addEventListener(WebInspector.NavigatorView.Events.ItemSelected, this._scriptSelected, this);
this._snippetsView.addEventListener(WebInspector.NavigatorView.Events.FileRenamed, this._fileRenamed, this);
this._snippetsView.addEventListener(WebInspector.SnippetsNavigatorView.Events.SnippetCreationRequested, this._snippetCreationRequested, this);
this._snippetsView.addEventListener(WebInspector.SnippetsNavigatorView.Events.ItemRenamingRequested, this._itemRenamingRequested, this);

this._tabbedPane.appendTab(WebInspector.ScriptsNavigator.ScriptsTab, WebInspector.UIString("Sources"), this._scriptsView);
this._tabbedPane.selectTab(WebInspector.ScriptsNavigator.ScriptsTab);
this._tabbedPane.appendTab(WebInspector.ScriptsNavigator.ContentScriptsTab, WebInspector.UIString("Content scripts"), this._contentScriptsView);
if (WebInspector.experimentsSettings.snippetsSupport.isEnabled())
this._tabbedPane.appendTab(WebInspector.ScriptsNavigator.SnippetsTab, WebInspector.UIString("Snippets"), this._snippetsView);
}

WebInspector.ScriptsNavigator.Events = {
ScriptSelected: "ScriptSelected",
SnippetCreationRequested: "SnippetCreationRequested",
ItemRenamingRequested: "ItemRenamingRequested",
FileRenamed: "FileRenamed"
}

WebInspector.ScriptsNavigator.ScriptsTab = "scripts";
WebInspector.ScriptsNavigator.ContentScriptsTab = "contentScripts";
WebInspector.ScriptsNavigator.SnippetsTab = "snippets";

WebInspector.ScriptsNavigator.prototype = {

get view()
{
return this._tabbedPane;
},


_snippetsNavigatorViewForUISourceCode: function(uiSourceCode)
{
if (uiSourceCode.isContentScript)
return this._contentScriptsView;
else if (uiSourceCode.isSnippet)
return this._snippetsView;
else
return this._scriptsView;
},


addUISourceCode: function(uiSourceCode)
{
this._snippetsNavigatorViewForUISourceCode(uiSourceCode).addUISourceCode(uiSourceCode);
},


removeUISourceCode: function(uiSourceCode)
{
this._snippetsNavigatorViewForUISourceCode(uiSourceCode).removeUISourceCode(uiSourceCode);
},


isScriptSourceAdded: function(uiSourceCode)
{
return this._snippetsNavigatorViewForUISourceCode(uiSourceCode).isScriptSourceAdded(uiSourceCode);
},


revealUISourceCode: function(uiSourceCode)
{
this._snippetsNavigatorViewForUISourceCode(uiSourceCode).revealUISourceCode(uiSourceCode);
if (uiSourceCode.isContentScript)
this._tabbedPane.selectTab(WebInspector.ScriptsNavigator.ContentScriptsTab);
else if (uiSourceCode.isSnippet)
this._tabbedPane.selectTab(WebInspector.ScriptsNavigator.SnippetsTab);
else
this._tabbedPane.selectTab(WebInspector.ScriptsNavigator.ScriptsTab);
},


rename: function(uiSourceCode, callback)
{
this._snippetsNavigatorViewForUISourceCode(uiSourceCode).rename(uiSourceCode, callback);
},


_scriptSelected: function(event)
{
this.dispatchEventToListeners(WebInspector.ScriptsNavigator.Events.ScriptSelected, event.data);
},


_fileRenamed: function(event)
{    
this.dispatchEventToListeners(WebInspector.ScriptsNavigator.Events.FileRenamed, event.data);
},


_itemRenamingRequested: function(event)
{
this.dispatchEventToListeners(WebInspector.ScriptsNavigator.Events.ItemRenamingRequested, event.data);
},


_snippetCreationRequested: function(event)
{    
this.dispatchEventToListeners(WebInspector.ScriptsNavigator.Events.SnippetCreationRequested, event.data);
},

reset: function()
{
this._scriptsView.reset();
this._contentScriptsView.reset();
this._snippetsView.reset();
},

__proto__: WebInspector.Object.prototype
}


WebInspector.SnippetsNavigatorView = function()
{
WebInspector.NavigatorView.call(this);
this.element.addEventListener("contextmenu", this.handleContextMenu.bind(this), false);
}

WebInspector.SnippetsNavigatorView.Events = {
SnippetCreationRequested: "SnippetCreationRequested",
ItemRenamingRequested: "ItemRenamingRequested"
}

WebInspector.SnippetsNavigatorView.prototype = {

getOrCreateFolderTreeElement: function(uiSourceCode)
{
return this._scriptsTree;
},


handleContextMenu: function(event, uiSourceCode)
{
var contextMenu = new WebInspector.ContextMenu(event);
if (uiSourceCode) {
contextMenu.appendItem(WebInspector.UIString("Run"), this._handleEvaluateSnippet.bind(this, uiSourceCode));
contextMenu.appendItem(WebInspector.UIString("Rename"), this._handleRenameSnippet.bind(this, uiSourceCode));
contextMenu.appendItem(WebInspector.UIString("Remove"), this._handleRemoveSnippet.bind(this, uiSourceCode));
contextMenu.appendSeparator();
}
contextMenu.appendItem(WebInspector.UIString("New"), this._handleCreateSnippet.bind(this));
contextMenu.show();
},


_handleEvaluateSnippet: function(uiSourceCode, event)
{
if (!uiSourceCode.isSnippet)
return;
WebInspector.scriptSnippetModel.evaluateScriptSnippet(uiSourceCode);
},


_handleRenameSnippet: function(uiSourceCode, event)
{
this.dispatchEventToListeners(WebInspector.ScriptsNavigator.Events.ItemRenamingRequested, uiSourceCode);
},


_handleRemoveSnippet: function(uiSourceCode, event)
{
if (!uiSourceCode.isSnippet)
return;
WebInspector.scriptSnippetModel.deleteScriptSnippet(uiSourceCode);
},


_handleCreateSnippet: function(event)
{
this._snippetCreationRequested();
},

_snippetCreationRequested: function()
{
this.dispatchEventToListeners(WebInspector.SnippetsNavigatorView.Events.SnippetCreationRequested, null);
},

__proto__: WebInspector.NavigatorView.prototype
}
;



WebInspector.ScriptsSearchScope = function(uiSourceCodeProvider)
{

WebInspector.SearchScope.call(this)
this._searchId = 0;
this._uiSourceCodeProvider = uiSourceCodeProvider;
}

WebInspector.ScriptsSearchScope.prototype = {

performSearch: function(searchConfig, searchResultCallback, searchFinishedCallback)
{
this.stopSearch();

var uiSourceCodes = this._sortedUISourceCodes();
var uiSourceCodeIndex = 0;

function filterOutContentScripts(uiSourceCode)
{
return !uiSourceCode.isContentScript;
}

if (!WebInspector.settings.searchInContentScripts.get())
uiSourceCodes = uiSourceCodes.filter(filterOutContentScripts);

function continueSearch()
{


if (uiSourceCodeIndex < uiSourceCodes.length) {
var uiSourceCode = uiSourceCodes[uiSourceCodeIndex++];
uiSourceCode.searchInContent(searchConfig.query, !searchConfig.ignoreCase, searchConfig.isRegex, searchCallbackWrapper.bind(this, this._searchId, uiSourceCode));
} else 
searchFinishedCallback(true);
}

function searchCallbackWrapper(searchId, uiSourceCode, searchMatches)
{
if (searchId !== this._searchId) {
searchFinishedCallback(false);
return;
}
var searchResult = new WebInspector.FileBasedSearchResultsPane.SearchResult(uiSourceCode, searchMatches);
searchResultCallback(searchResult);
if (searchId !== this._searchId) {
searchFinishedCallback(false);
return;
}
continueSearch.call(this);
}

continueSearch.call(this);
return uiSourceCodes.length;
},

stopSearch: function()
{
++this._searchId;
},


createSearchResultsPane: function(searchConfig)
{
return new WebInspector.FileBasedSearchResultsPane(searchConfig);
},


_sortedUISourceCodes: function()
{
function filterOutAnonymous(uiSourceCode)
{
return !!uiSourceCode.url;
}

function comparator(a, b)
{
return a.url.localeCompare(b.url);   
}

var uiSourceCodes = this._uiSourceCodeProvider.uiSourceCodes();

uiSourceCodes = uiSourceCodes.filter(filterOutAnonymous);
uiSourceCodes.sort(comparator);

return uiSourceCodes;
},

__proto__: WebInspector.SearchScope.prototype
}
;



WebInspector.SnippetJavaScriptSourceFrame = function(scriptsPanel, uiSourceCode)
{
WebInspector.JavaScriptSourceFrame.call(this, scriptsPanel, uiSourceCode);

this._uiSourceCode = uiSourceCode;
this._runButton = new WebInspector.StatusBarButton(WebInspector.UIString("Run"), "evaluate-snippet-status-bar-item");
this._runButton.addEventListener("click", this._runButtonClicked, this);
}

WebInspector.SnippetJavaScriptSourceFrame.prototype = {

statusBarItems: function()
{
return [this._runButton.element];
},

_runButtonClicked: function()
{
WebInspector.scriptSnippetModel.evaluateScriptSnippet(this._uiSourceCode);
},

__proto__: WebInspector.JavaScriptSourceFrame.prototype
}
;



WebInspector.StyleSheetOutlineDialog = function(view, uiSourceCode)
{
WebInspector.SelectionDialogContentProvider.call(this);

this._rules = [];
this._view = view;
this._uiSourceCode = uiSourceCode;
}


WebInspector.StyleSheetOutlineDialog.show = function(view, uiSourceCode)
{
if (WebInspector.Dialog.currentInstance())
return null;
var delegate = new WebInspector.StyleSheetOutlineDialog(view, uiSourceCode);
var filteredItemSelectionDialog = new WebInspector.FilteredItemSelectionDialog(delegate);
WebInspector.Dialog.show(view.element, filteredItemSelectionDialog);
}

WebInspector.StyleSheetOutlineDialog.prototype = {

itemTitleAt: function(itemIndex)
{
return this._rules[itemIndex].selectorText;
},


itemSuffixAt: function(itemIndex)
{
return "";
},


itemSubtitleAt: function(itemIndex)
{
return ":" + (this._rules[itemIndex].sourceLine + 1);
},


itemKeyAt: function(itemIndex)
{
return this._rules[itemIndex].selectorText;
},


itemsCount: function()
{
return this._rules.length;
},


requestItems: function(callback)
{
function didGetAllStyleSheets(error, infos)
{
if (error) {
callback(0, 0, 0, 0);
return;
}

for (var i = 0; i < infos.length; ++i) {
var info = infos[i];
if (info.sourceURL === this._uiSourceCode.contentURL()) {
WebInspector.CSSStyleSheet.createForId(info.styleSheetId, didGetStyleSheet.bind(this));
return;
}
}
callback(0, 0, 0, 0);
}

CSSAgent.getAllStyleSheets(didGetAllStyleSheets.bind(this));


function didGetStyleSheet(styleSheet)
{
if (!styleSheet) {
callback(0, 0, 0, 0);
return;
}

this._rules = styleSheet.rules;
callback(0, this._rules.length, 0, 1);
}
},


selectItem: function(itemIndex, promptValue)
{
var lineNumber = this._rules[itemIndex].sourceLine;
if (!isNaN(lineNumber) && lineNumber >= 0)
this._view.highlightLine(lineNumber);
this._view.focus();
},


rewriteQuery: function(query)
{
return query;
},

__proto__: WebInspector.SelectionDialogContentProvider.prototype
}
;



WebInspector.TabbedEditorContainerDelegate = function() { }

WebInspector.TabbedEditorContainerDelegate.prototype = {

viewForFile: function(uiSourceCode) { }
}


WebInspector.TabbedEditorContainer = function(delegate, settingName)
{
WebInspector.Object.call(this);
this._delegate = delegate;

this._tabbedPane = new WebInspector.TabbedPane();
this._tabbedPane.closeableTabs = true;
this._tabbedPane.element.id = "scripts-editor-container-tabbed-pane";

this._tabbedPane.addEventListener(WebInspector.TabbedPane.EventTypes.TabClosed, this._tabClosed, this);
this._tabbedPane.addEventListener(WebInspector.TabbedPane.EventTypes.TabSelected, this._tabSelected, this);

this._tabIds = new Map();
this._files = {};
this._loadedURLs = {};

this._previouslyViewedFilesSetting = WebInspector.settings.createSetting(settingName, []);
this._history = WebInspector.TabbedEditorContainer.History.fromObject(this._previouslyViewedFilesSetting.get());
}


WebInspector.TabbedEditorContainer.Events = {
EditorSelected: "EditorSelected",
EditorClosed: "EditorClosed"
}

WebInspector.TabbedEditorContainer._tabId = 0;

WebInspector.TabbedEditorContainer.maximalPreviouslyViewedFilesCount = 30;

WebInspector.TabbedEditorContainer.prototype = {

get view()
{
return this._tabbedPane;
},


get visibleView()
{
return this._tabbedPane.visibleView;
},


show: function(parentElement)
{
this._tabbedPane.show(parentElement);
},


showFile: function(uiSourceCode)
{
this._innerShowFile(uiSourceCode, true);
},

_addScrollAndSelectionListeners: function()
{
console.assert(this._currentFile);
var sourceFrame = this._delegate.viewForFile(this._currentFile);
sourceFrame.addEventListener(WebInspector.SourceFrame.Events.ScrollChanged, this._scrollChanged, this);
sourceFrame.addEventListener(WebInspector.SourceFrame.Events.SelectionChanged, this._selectionChanged, this);
},

_removeScrollAndSelectionListeners: function()
{
if (!this._currentFile)
return;
var sourceFrame = this._delegate.viewForFile(this._currentFile);
sourceFrame.removeEventListener(WebInspector.SourceFrame.Events.ScrollChanged, this._scrollChanged, this);
sourceFrame.removeEventListener(WebInspector.SourceFrame.Events.SelectionChanged, this._selectionChanged, this);
},

_scrollChanged: function(event)
{
var lineNumber =   (event.data);
this._history.updateScrollLineNumber(this._currentFile.url, lineNumber);
this._history.save(this._previouslyViewedFilesSetting);
},

_selectionChanged: function(event)
{
var range =   (event.data);
this._history.updateSelectionRange(this._currentFile.url, range);
this._history.save(this._previouslyViewedFilesSetting);
},


_innerShowFile: function(uiSourceCode, userGesture)
{
if (this._currentFile === uiSourceCode)
return;
this._removeScrollAndSelectionListeners();
this._currentFile = uiSourceCode;

var tabId = this._tabIds.get(uiSourceCode) || this._appendFileTab(uiSourceCode, userGesture);

this._tabbedPane.selectTab(tabId, userGesture);
if (userGesture)
this._editorSelectedByUserAction();

this._addScrollAndSelectionListeners();

this.dispatchEventToListeners(WebInspector.TabbedEditorContainer.Events.EditorSelected, this._currentFile);
},


_titleForFile: function(uiSourceCode)
{
const maxDisplayNameLength = 30;
const minDisplayQueryParamLength = 5;

var title;
var parsedURL = uiSourceCode.parsedURL;
if (!parsedURL.isValid)
title = parsedURL.url ? parsedURL.url.trimMiddle(maxDisplayNameLength) : WebInspector.UIString("(program)");
else {
var maxDisplayQueryParamLength = Math.max(minDisplayQueryParamLength, maxDisplayNameLength - parsedURL.lastPathComponent.length);
var displayQueryParams = parsedURL.queryParams ? "?" + parsedURL.queryParams.trimEnd(maxDisplayQueryParamLength - 1) : "";
var displayLastPathComponent = parsedURL.lastPathComponent.trimMiddle(maxDisplayNameLength - displayQueryParams.length);
var displayName = displayLastPathComponent + displayQueryParams;
title = displayName || WebInspector.UIString("(program)");
}

if (uiSourceCode.isDirty())
title += "*";
return title;
},


addUISourceCode: function(uiSourceCode)
{
if (this._userSelectedFiles || this._loadedURLs[uiSourceCode.url])
return;
this._loadedURLs[uiSourceCode.url] = true;

var index = this._history.index(uiSourceCode.url)
if (index === -1)
return;

var tabId = this._tabIds.get(uiSourceCode) || this._appendFileTab(uiSourceCode, false);


if (index === 0)
this._innerShowFile(uiSourceCode, false);
},


removeUISourceCode: function(uiSourceCode)
{
var wasCurrent = this._currentFile === uiSourceCode;

var tabId = this._tabIds.get(uiSourceCode);
if (tabId)
this._tabbedPane.closeTab(tabId);

if (wasCurrent && uiSourceCode.isTemporary) {
var newUISourceCode = WebInspector.workspace.uiSourceCodeForURL(uiSourceCode.url);
if (newUISourceCode)
this._innerShowFile(newUISourceCode, false);
}
},


_editorClosedByUserAction: function(uiSourceCode)
{
this._userSelectedFiles = true;
this._history.remove(uiSourceCode.url);
this._updateHistory();
},

_editorSelectedByUserAction: function()
{
this._userSelectedFiles = true;
this._updateHistory();
},

_updateHistory: function()
{
var tabIds = this._tabbedPane.lastOpenedTabIds(WebInspector.TabbedEditorContainer.maximalPreviouslyViewedFilesCount);

function tabIdToURL(tabId)
{
return this._files[tabId].url;
}

this._history.update(tabIds.map(tabIdToURL.bind(this)));
this._history.save(this._previouslyViewedFilesSetting);
},


_tooltipForFile: function(uiSourceCode)
{
return uiSourceCode.url;
},


_appendFileTab: function(uiSourceCode, userGesture)
{
var view = this._delegate.viewForFile(uiSourceCode);
var title = this._titleForFile(uiSourceCode);
var tooltip = this._tooltipForFile(uiSourceCode);

var tabId = this._generateTabId();
this._tabIds.put(uiSourceCode, tabId);
this._files[tabId] = uiSourceCode;

var savedScrollLineNumber = this._history.scrollLineNumber(uiSourceCode.url);
if (savedScrollLineNumber)
view.scrollToLine(savedScrollLineNumber);
var savedSelectionRange = this._history.selectionRange(uiSourceCode.url);
if (savedSelectionRange)
view.setSelection(savedSelectionRange);

this._tabbedPane.appendTab(tabId, title, view, tooltip, userGesture);

this._addUISourceCodeListeners(uiSourceCode);
return tabId;
},


_tabClosed: function(event)
{
var tabId =   (event.data.tabId);
var userGesture =   (event.data.isUserGesture);

var uiSourceCode = this._files[tabId];
if (this._currentFile === uiSourceCode) {
this._removeScrollAndSelectionListeners();
delete this._currentFile;
}
this._tabIds.remove(uiSourceCode);
delete this._files[tabId];

this._removeUISourceCodeListeners(uiSourceCode);

this.dispatchEventToListeners(WebInspector.TabbedEditorContainer.Events.EditorClosed, uiSourceCode);

if (userGesture)
this._editorClosedByUserAction(uiSourceCode);
},


_tabSelected: function(event)
{
var tabId =   (event.data.tabId);
var userGesture =   (event.data.isUserGesture);

var uiSourceCode = this._files[tabId];
this._innerShowFile(uiSourceCode, userGesture);
},


_addUISourceCodeListeners: function(uiSourceCode)
{
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.TitleChanged, this._uiSourceCodeTitleChanged, this);
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.WorkingCopyChanged, this._uiSourceCodeWorkingCopyChanged, this);
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.WorkingCopyCommitted, this._uiSourceCodeWorkingCopyCommitted, this);
uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.FormattedChanged, this._uiSourceCodeFormattedChanged, this);
},


_removeUISourceCodeListeners: function(uiSourceCode)
{
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.TitleChanged, this._uiSourceCodeTitleChanged, this);
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.WorkingCopyChanged, this._uiSourceCodeWorkingCopyChanged, this);
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.WorkingCopyCommitted, this._uiSourceCodeWorkingCopyCommitted, this);
uiSourceCode.removeEventListener(WebInspector.UISourceCode.Events.FormattedChanged, this._uiSourceCodeFormattedChanged, this);
},


_updateFileTitle: function(uiSourceCode)
{
var tabId = this._tabIds.get(uiSourceCode);
if (tabId) {
var title = this._titleForFile(uiSourceCode);
this._tabbedPane.changeTabTitle(tabId, title);
}
},

_uiSourceCodeTitleChanged: function(event)
{
var uiSourceCode =   (event.target);
this._updateFileTitle(uiSourceCode);
},

_uiSourceCodeWorkingCopyChanged: function(event)
{
var uiSourceCode =   (event.target);
this._updateFileTitle(uiSourceCode);
},

_uiSourceCodeWorkingCopyCommitted: function(event)
{
var uiSourceCode =   (event.target);
this._updateFileTitle(uiSourceCode);
},

_uiSourceCodeFormattedChanged: function(event)
{
var uiSourceCode =   (event.target);
this._updateFileTitle(uiSourceCode);
},

reset: function()
{
this._tabbedPane.closeAllTabs();
this._tabIds = new Map();
this._files = {};
delete this._currentFile;
delete this._userSelectedFiles;
this._loadedURLs = {};
},


_generateTabId: function()
{
return "tab_" + (WebInspector.TabbedEditorContainer._tabId++);
},


currentFile: function()
{
return this._currentFile;
},

__proto__: WebInspector.Object.prototype
}


WebInspector.TabbedEditorContainer.HistoryItem = function(url, selectionRange, scrollLineNumber)
{
this.url = url;
this.selectionRange = selectionRange;
this.scrollLineNumber = scrollLineNumber;
}


WebInspector.TabbedEditorContainer.HistoryItem.fromObject = function (serializedHistoryItem)
{
var selectionRange = serializedHistoryItem.selectionRange ? WebInspector.TextRange.fromObject(serializedHistoryItem.selectionRange) : null;
return new WebInspector.TabbedEditorContainer.HistoryItem(serializedHistoryItem.url, selectionRange, serializedHistoryItem.scrollLineNumber);
}

WebInspector.TabbedEditorContainer.HistoryItem.prototype = {

serializeToObject: function()
{
var serializedHistoryItem = {};
serializedHistoryItem.url = this.url;
serializedHistoryItem.selectionRange = this.selectionRange;
serializedHistoryItem.scrollLineNumber = this.scrollLineNumber;
return serializedHistoryItem;
},

__proto__: WebInspector.Object.prototype
}


WebInspector.TabbedEditorContainer.History = function(items)
{
this._items = items;
}


WebInspector.TabbedEditorContainer.History.fromObject = function(serializedHistory)
{
var items = [];
for (var i = 0; i < serializedHistory.length; ++i)
items.push(WebInspector.TabbedEditorContainer.HistoryItem.fromObject(serializedHistory[i]));
return new WebInspector.TabbedEditorContainer.History(items);
}

WebInspector.TabbedEditorContainer.History.prototype = {

index: function(url)
{
for (var i = 0; i < this._items.length; ++i) {
if (this._items[i].url === url)
return i;
}
return -1;
},


selectionRange: function(url)
{
var index = this.index(url);
return index !== -1 ? this._items[index].selectionRange : undefined;
},


updateSelectionRange: function(url, selectionRange)
{
if (!selectionRange)
return;
var index = this.index(url);
if (index === -1)
return;
this._items[index].selectionRange = selectionRange;
},


scrollLineNumber: function(url)
{
var index = this.index(url);
return index !== -1 ? this._items[index].scrollLineNumber : undefined;
},


updateScrollLineNumber: function(url, scrollLineNumber)
{
var index = this.index(url);
if (index === -1)
return;
this._items[index].scrollLineNumber = scrollLineNumber;
},


update: function(urls)
{
for (var i = urls.length - 1; i >= 0; --i) {
var index = this.index(urls[i]);
var item;
if (index !== -1) {
item = this._items[index];
this._items.splice(index, 1);
} else
item = new WebInspector.TabbedEditorContainer.HistoryItem(urls[i]);
this._items.unshift(item);
}
},


remove: function(url)
{
var index = this.index(url);
if (index !== -1)
this._items.splice(index, 1);
},


save: function(setting)
{
setting.set(this._serializeToObject());
},


_serializeToObject: function()
{
var serializedHistory = [];
for (var i = 0; i < this._items.length; ++i)
serializedHistory.push(this._items[i].serializeToObject());
return serializedHistory;
},

__proto__: WebInspector.Object.prototype
}
;



WebInspector.UISourceCodeFrame = function(uiSourceCode)
{
this._uiSourceCode = uiSourceCode;
WebInspector.SourceFrame.call(this, this._uiSourceCode);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.FormattedChanged, this._onFormattedChanged, this);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.WorkingCopyChanged, this._onWorkingCopyChanged, this);
this._uiSourceCode.addEventListener(WebInspector.UISourceCode.Events.WorkingCopyCommitted, this._onWorkingCopyCommitted, this);
}

WebInspector.UISourceCodeFrame.prototype = {

canEditSource: function()
{
return true;
},


commitEditing: function(text)
{
if (!this._uiSourceCode.isDirty())
return;

this._isCommittingEditing = true;
this._uiSourceCode.commitWorkingCopy(this._didEditContent.bind(this));
delete this._isCommittingEditing;
},

onTextChanged: function(oldRange, newRange)
{
this._isSettingWorkingCopy = true;
this._uiSourceCode.setWorkingCopy(this._textEditor.text());
delete this._isSettingWorkingCopy;
},

_didEditContent: function(error)
{
if (error) {
WebInspector.log(error, WebInspector.ConsoleMessage.MessageLevel.Error, true);
return;
}
},


_onFormattedChanged: function(event)
{
var content =   (event.data.content);
this._textEditor.setReadOnly(this._uiSourceCode.formatted());
this._innerSetContent(content);
},


_onWorkingCopyChanged: function(event)
{
this._innerSetContent(this._uiSourceCode.workingCopy());
},


_onWorkingCopyCommitted: function(event)
{
this._innerSetContent(this._uiSourceCode.workingCopy());
},

_innerSetContent: function(content)
{
if (this._isSettingWorkingCopy || this._isCommittingEditing)
return;

this.setContent(this._uiSourceCode.content() || "", false, this._uiSourceCode.contentType().canonicalMimeType());
},

populateTextAreaContextMenu: function(contextMenu, lineNumber)
{
WebInspector.SourceFrame.prototype.populateTextAreaContextMenu.call(this, contextMenu, lineNumber);
contextMenu.appendApplicableItems(this._uiSourceCode);
contextMenu.appendSeparator();
},

__proto__: WebInspector.SourceFrame.prototype
}
;



WebInspector.WatchExpressionsSidebarPane = function()
{
WebInspector.SidebarPane.call(this, WebInspector.UIString("Watch Expressions"));
}

WebInspector.WatchExpressionsSidebarPane.prototype = {
show: function()
{
this._visible = true;


if (this._wasShown) {
this._refreshExpressionsIfNeeded();
return;
}

this._wasShown = true;

this.section = new WebInspector.WatchExpressionsSection();
this.bodyElement.appendChild(this.section.element);

var refreshButton = document.createElement("button");
refreshButton.className = "pane-title-button refresh";
refreshButton.addEventListener("click", this._refreshButtonClicked.bind(this), false);
refreshButton.title = WebInspector.UIString("Refresh");
this.titleElement.appendChild(refreshButton);

var addButton = document.createElement("button");
addButton.className = "pane-title-button add";
addButton.addEventListener("click", this._addButtonClicked.bind(this), false);
this.titleElement.appendChild(addButton);
addButton.title = WebInspector.UIString("Add watch expression");
this._requiresUpdate = true;

if (WebInspector.settings.watchExpressions.get().length > 0)
this.expanded = true;
},

hide: function()
{
this._visible = false;
},

reset: function()
{
this.refreshExpressions();
},

refreshExpressions: function()
{
this._requiresUpdate = true;
this._refreshExpressionsIfNeeded();
},

addExpression: function(expression)
{
this.section.addExpression(expression);
this.expanded = true;
},

_refreshExpressionsIfNeeded: function()
{
if (this._requiresUpdate && this._visible) {
this.section.update();
delete this._requiresUpdate;
} else
this._requiresUpdate = true;
},

_addButtonClicked: function(event)
{
event.consume();
this.expanded = true;
this.section.addNewExpressionAndEdit();
},

_refreshButtonClicked: function(event)
{
event.consume();
this.refreshExpressions();
},

__proto__: WebInspector.SidebarPane.prototype
}


WebInspector.WatchExpressionsSection = function()
{
this._watchObjectGroupId = "watch-group";

WebInspector.ObjectPropertiesSection.call(this, WebInspector.RemoteObject.fromPrimitiveValue(""));

this.treeElementConstructor = WebInspector.WatchedPropertyTreeElement;
this._expandedExpressions = {};
this._expandedProperties = {};

this.emptyElement = document.createElement("div");
this.emptyElement.className = "info";
this.emptyElement.textContent = WebInspector.UIString("No Watch Expressions");

this.watchExpressions = WebInspector.settings.watchExpressions.get();

this.headerElement.className = "hidden";
this.editable = true;
this.expanded = true;
this.propertiesElement.addStyleClass("watch-expressions");

this.element.addEventListener("mousemove", this._mouseMove.bind(this), true);
this.element.addEventListener("mouseout", this._mouseOut.bind(this), true);
this.element.addEventListener("dblclick", this._sectionDoubleClick.bind(this), false);
this.emptyElement.addEventListener("contextmenu", this._emptyElementContextMenu.bind(this), false);
}

WebInspector.WatchExpressionsSection.NewWatchExpression = "\xA0";

WebInspector.WatchExpressionsSection.prototype = {
update: function(e)
{
if (e)
e.consume();

function appendResult(expression, watchIndex, result, wasThrown)
{
if (!result)
return;

var property = new WebInspector.RemoteObjectProperty(expression, result);
property.watchIndex = watchIndex;
property.wasThrown = wasThrown;








properties.push(property);

if (properties.length == propertyCount) {
this.updateProperties(properties, WebInspector.WatchExpressionTreeElement, WebInspector.WatchExpressionsSection.CompareProperties);



if (this._newExpressionAdded) {
delete this._newExpressionAdded;

var treeElement = this.findAddedTreeElement();
if (treeElement)
treeElement.startEditing();
}


if (this._lastMouseMovePageY)
this._updateHoveredElement(this._lastMouseMovePageY);
}
}


RuntimeAgent.releaseObjectGroup(this._watchObjectGroupId)
var properties = [];



var propertyCount = 0;
for (var i = 0; i < this.watchExpressions.length; ++i) {
if (!this.watchExpressions[i])
continue;
++propertyCount;
}



for (var i = 0; i < this.watchExpressions.length; ++i) {
var expression = this.watchExpressions[i];
if (!expression)
continue;

WebInspector.runtimeModel.evaluate(expression, this._watchObjectGroupId, false, true, false, false, appendResult.bind(this, expression, i));
}

if (!propertyCount) {
if (!this.emptyElement.parentNode)
this.element.appendChild(this.emptyElement);
} else {
if (this.emptyElement.parentNode)
this.element.removeChild(this.emptyElement);
}




this.expanded = (propertyCount != 0);
},

addExpression: function(expression)
{
this.watchExpressions.push(expression);
this.saveExpressions();
this.update();
},

addNewExpressionAndEdit: function()
{
this._newExpressionAdded = true;
this.watchExpressions.push(WebInspector.WatchExpressionsSection.NewWatchExpression);
this.update();
},

_sectionDoubleClick: function(event)
{
if (event.target !== this.element && event.target !== this.propertiesElement && event.target !== this.emptyElement)
return;
event.consume();
this.addNewExpressionAndEdit();
},

updateExpression: function(element, value)
{
if (value === null) {
var index = element.property.watchIndex;
this.watchExpressions.splice(index, 1);
}
else
this.watchExpressions[element.property.watchIndex] = value;
this.saveExpressions();
this.update();
},

_deleteAllExpressions: function()
{
this.watchExpressions = [];
this.saveExpressions();
this.update();
},

findAddedTreeElement: function()
{
var children = this.propertiesTreeOutline.children;
for (var i = 0; i < children.length; ++i) {
if (children[i].property.name === WebInspector.WatchExpressionsSection.NewWatchExpression)
return children[i];
}
},

saveExpressions: function()
{
var toSave = [];
for (var i = 0; i < this.watchExpressions.length; i++)
if (this.watchExpressions[i])
toSave.push(this.watchExpressions[i]);

WebInspector.settings.watchExpressions.set(toSave);
return toSave.length;
},

_mouseMove: function(e)
{
if (this.propertiesElement.firstChild)
this._updateHoveredElement(e.pageY);
},

_mouseOut: function()
{
if (this._hoveredElement) {
this._hoveredElement.removeStyleClass("hovered");
delete this._hoveredElement;
}
delete this._lastMouseMovePageY;
},

_updateHoveredElement: function(pageY)
{
var candidateElement = this.propertiesElement.firstChild;
while (true) {
var next = candidateElement.nextSibling;
while (next && !next.clientHeight)
next = next.nextSibling;
if (!next || next.totalOffsetTop() > pageY)
break;
candidateElement = next;
}

if (this._hoveredElement !== candidateElement) {
if (this._hoveredElement)
this._hoveredElement.removeStyleClass("hovered");
if (candidateElement)
candidateElement.addStyleClass("hovered");
this._hoveredElement = candidateElement;
}

this._lastMouseMovePageY = pageY;
},

_emptyElementContextMenu: function(event)
{
var contextMenu = new WebInspector.ContextMenu(event);
contextMenu.appendItem(WebInspector.UIString("Add watch expression"), this.addNewExpressionAndEdit.bind(this));
contextMenu.show();
},

__proto__: WebInspector.ObjectPropertiesSection.prototype
}

WebInspector.WatchExpressionsSection.CompareProperties = function(propertyA, propertyB)
{
if (propertyA.watchIndex == propertyB.watchIndex)
return 0;
else if (propertyA.watchIndex < propertyB.watchIndex)
return -1;
else
return 1;
}


WebInspector.WatchExpressionTreeElement = function(property)
{
WebInspector.ObjectPropertyTreeElement.call(this, property);
}

WebInspector.WatchExpressionTreeElement.prototype = {
onexpand: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.onexpand.call(this);
this.treeOutline.section._expandedExpressions[this._expression()] = true;
},

oncollapse: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.oncollapse.call(this);
delete this.treeOutline.section._expandedExpressions[this._expression()];
},

onattach: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.onattach.call(this);
if (this.treeOutline.section._expandedExpressions[this._expression()])
this.expanded = true;
},

_expression: function()
{
return this.property.name;
},

update: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.update.call(this);

if (this.property.wasThrown) {
this.valueElement.textContent = WebInspector.UIString("<not available>");
this.listItemElement.addStyleClass("dimmed");
} else
this.listItemElement.removeStyleClass("dimmed");

var deleteButton = document.createElement("input");
deleteButton.type = "button";
deleteButton.title = WebInspector.UIString("Delete watch expression.");
deleteButton.addStyleClass("enabled-button");
deleteButton.addStyleClass("delete-button");
deleteButton.addEventListener("click", this._deleteButtonClicked.bind(this), false);
this.listItemElement.addEventListener("contextmenu", this._contextMenu.bind(this), false);
this.listItemElement.insertBefore(deleteButton, this.listItemElement.firstChild);
},


populateContextMenu: function(contextMenu)
{
if (!this.isEditing()) {
contextMenu.appendItem(WebInspector.UIString("Add watch expression"), this.treeOutline.section.addNewExpressionAndEdit.bind(this.treeOutline.section));
contextMenu.appendItem(WebInspector.UIString("Delete watch expression"), this._deleteButtonClicked.bind(this));
}
if (this.treeOutline.section.watchExpressions.length > 1)
contextMenu.appendItem(WebInspector.UIString("Delete all watch expressions"), this._deleteAllButtonClicked.bind(this));
},

_contextMenu: function(event)
{
var contextMenu = new WebInspector.ContextMenu(event);
this.populateContextMenu(contextMenu);
contextMenu.show();
},

_deleteAllButtonClicked: function()
{
this.treeOutline.section._deleteAllExpressions();
},

_deleteButtonClicked: function()
{
this.treeOutline.section.updateExpression(this, null);
},

renderPromptAsBlock: function()
{
return true;
},


elementAndValueToEdit: function(event)
{
return [this.nameElement, this.property.name.trim()];
},

editingCancelled: function(element, context)
{
if (!context.elementToEdit.textContent)
this.treeOutline.section.updateExpression(this, null);

WebInspector.ObjectPropertyTreeElement.prototype.editingCancelled.call(this, element, context);
},

applyExpression: function(expression, updateInterface)
{
expression = expression.trim();

if (!expression)
expression = null;

this.property.name = expression;
this.treeOutline.section.updateExpression(this, expression);
},

__proto__: WebInspector.ObjectPropertyTreeElement.prototype
}



WebInspector.WatchedPropertyTreeElement = function(property)
{
WebInspector.ObjectPropertyTreeElement.call(this, property);
}

WebInspector.WatchedPropertyTreeElement.prototype = {
onattach: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.onattach.call(this);
if (this.hasChildren && this.propertyPath() in this.treeOutline.section._expandedProperties)
this.expand();
},

onexpand: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.onexpand.call(this);
this.treeOutline.section._expandedProperties[this.propertyPath()] = true;
},

oncollapse: function()
{
WebInspector.ObjectPropertyTreeElement.prototype.oncollapse.call(this);
delete this.treeOutline.section._expandedProperties[this.propertyPath()];
},

__proto__: WebInspector.ObjectPropertyTreeElement.prototype
}
;



WebInspector.Worker = function(id, url, shared)
{
this.id = id;
this.url = url;
this.shared = shared;
}


WebInspector.WorkersSidebarPane = function(workerManager)
{
WebInspector.SidebarPane.call(this, WebInspector.UIString("Workers"));

this._enableWorkersCheckbox = new WebInspector.Checkbox(
WebInspector.UIString("Pause on start"),
"sidebar-label",
WebInspector.UIString("Automatically attach to new workers and pause them. Enabling this option will force opening inspector for all new workers."));
this._enableWorkersCheckbox.element.id = "pause-workers-checkbox";
this.bodyElement.appendChild(this._enableWorkersCheckbox.element);
this._enableWorkersCheckbox.addEventListener(this._autoattachToWorkersClicked.bind(this));
this._enableWorkersCheckbox.checked = false;

if (Preferences.sharedWorkersDebugNote) {
var note = this.bodyElement.createChild("div");
note.id = "shared-workers-list";
note.addStyleClass("sidebar-label")
note.textContent = Preferences.sharedWorkersDebugNote;
}

var separator = this.bodyElement.createChild("div", "sidebar-separator");
separator.textContent = WebInspector.UIString("Dedicated worker inspectors");

this._workerListElement = document.createElement("ol");
this._workerListElement.tabIndex = 0;
this._workerListElement.addStyleClass("properties-tree");
this._workerListElement.addStyleClass("sidebar-label");
this.bodyElement.appendChild(this._workerListElement);

this._idToWorkerItem = {};
this._workerManager = workerManager;

workerManager.addEventListener(WebInspector.WorkerManager.Events.WorkerAdded, this._workerAdded, this);
workerManager.addEventListener(WebInspector.WorkerManager.Events.WorkerRemoved, this._workerRemoved, this);
workerManager.addEventListener(WebInspector.WorkerManager.Events.WorkersCleared, this._workersCleared, this);
}

WebInspector.WorkersSidebarPane.prototype = {
_workerAdded: function(event)
{
this._addWorker(event.data.workerId, event.data.url, event.data.inspectorConnected);
},

_workerRemoved: function(event)
{
var workerItem = this._idToWorkerItem[event.data];
delete this._idToWorkerItem[event.data];
workerItem.parentElement.removeChild(workerItem);
},

_workersCleared: function(event)
{
this._idToWorkerItem = {};
this._workerListElement.removeChildren();
},

_addWorker: function(workerId, url, inspectorConnected)
{
var item = this._workerListElement.createChild("div", "dedicated-worker-item");
var link = item.createChild("a");
link.textContent = url;
link.href = "#";
link.target = "_blank";
link.addEventListener("click", this._workerItemClicked.bind(this, workerId), true);
this._idToWorkerItem[workerId] = item;
},

_workerItemClicked: function(workerId, event)
{
event.preventDefault();
this._workerManager.openWorkerInspector(workerId);
},

_autoattachToWorkersClicked: function(event)
{
WorkerAgent.setAutoconnectToWorkers(this._enableWorkersCheckbox.checked);
},

__proto__: WebInspector.SidebarPane.prototype
}
;


WebInspector.ScriptsPanel = function(workspaceForTest)
{
WebInspector.Panel.call(this, "scripts");
this.registerRequiredCSS("scriptsPanel.css");

WebInspector.settings.navigatorWasOnceHidden = WebInspector.settings.createSetting("navigatorWasOnceHidden", false);
WebInspector.settings.debuggerSidebarHidden = WebInspector.settings.createSetting("debuggerSidebarHidden", false);

this._workspace = workspaceForTest || WebInspector.workspace;

function viewGetter()
{
return this.visibleView;
}
WebInspector.GoToLineDialog.install(this, viewGetter.bind(this));

var helpSection = WebInspector.shortcutsScreen.section(WebInspector.UIString("Sources Panel"));
this.debugToolbar = this._createDebugToolbar(helpSection);

const initialDebugSidebarWidth = 225;
const minimumDebugSidebarWidthPercent = 50;
this.createSidebarView(this.element, WebInspector.SidebarView.SidebarPosition.Right, initialDebugSidebarWidth);
this.splitView.element.id = "scripts-split-view";
this.splitView.setMinimumSidebarWidth(Preferences.minScriptsSidebarWidth);
this.splitView.setMinimumMainWidthPercent(minimumDebugSidebarWidthPercent);

this.sidebarElement.appendChild(this.debugToolbar);

this.debugSidebarResizeWidgetElement = document.createElement("div");
this.debugSidebarResizeWidgetElement.id = "scripts-debug-sidebar-resizer-widget";
this.splitView.installResizer(this.debugSidebarResizeWidgetElement);


const initialNavigatorWidth = 225;
const minimumViewsContainerWidthPercent = 50;
this.editorView = new WebInspector.SidebarView(WebInspector.SidebarView.SidebarPosition.Left, "scriptsPanelNavigatorSidebarWidth", initialNavigatorWidth);
this.editorView.element.tabIndex = 0;

this.editorView.setMinimumSidebarWidth(Preferences.minScriptsSidebarWidth);
this.editorView.setMinimumMainWidthPercent(minimumViewsContainerWidthPercent);
this.editorView.show(this.splitView.mainElement);

this._navigator = new WebInspector.ScriptsNavigator();
this._navigator.view.show(this.editorView.sidebarElement);

this._editorContainer = new WebInspector.TabbedEditorContainer(this, "previouslyViewedFiles");
this._editorContainer.show(this.editorView.mainElement);

this._navigatorController = new WebInspector.NavigatorOverlayController(this.editorView, this._navigator.view, this._editorContainer.view);

this._navigator.addEventListener(WebInspector.ScriptsNavigator.Events.ScriptSelected, this._scriptSelected, this);
this._navigator.addEventListener(WebInspector.ScriptsNavigator.Events.SnippetCreationRequested, this._snippetCreationRequested, this);
this._navigator.addEventListener(WebInspector.ScriptsNavigator.Events.ItemRenamingRequested, this._itemRenamingRequested, this);
this._navigator.addEventListener(WebInspector.ScriptsNavigator.Events.FileRenamed, this._fileRenamed, this);

this._editorContainer.addEventListener(WebInspector.TabbedEditorContainer.Events.EditorSelected, this._editorSelected, this);
this._editorContainer.addEventListener(WebInspector.TabbedEditorContainer.Events.EditorClosed, this._editorClosed, this);

this.splitView.mainElement.appendChild(this.debugSidebarResizeWidgetElement);

this.sidebarPanes = {};
this.sidebarPanes.watchExpressions = new WebInspector.WatchExpressionsSidebarPane();
this.sidebarPanes.callstack = new WebInspector.CallStackSidebarPane();
this.sidebarPanes.scopechain = new WebInspector.ScopeChainSidebarPane();
this.sidebarPanes.jsBreakpoints = new WebInspector.JavaScriptBreakpointsSidebarPane(WebInspector.breakpointManager, this._showSourceLine.bind(this));
this.sidebarPanes.domBreakpoints = WebInspector.domBreakpointsSidebarPane;
this.sidebarPanes.xhrBreakpoints = new WebInspector.XHRBreakpointsSidebarPane();
this.sidebarPanes.eventListenerBreakpoints = new WebInspector.EventListenerBreakpointsSidebarPane();

if (InspectorFrontendHost.canInspectWorkers() && !WebInspector.WorkerManager.isWorkerFrontend()) {
WorkerAgent.enable();
this.sidebarPanes.workerList = new WebInspector.WorkersSidebarPane(WebInspector.workerManager);
}

this._debugSidebarContentsElement = document.createElement("div");
this._debugSidebarContentsElement.id = "scripts-debug-sidebar-contents";
this.sidebarElement.appendChild(this._debugSidebarContentsElement);

for (var pane in this.sidebarPanes) {
if (this.sidebarPanes[pane] === this.sidebarPanes.domBreakpoints)
continue;
this._debugSidebarContentsElement.appendChild(this.sidebarPanes[pane].element);
}

this.sidebarPanes.callstack.expanded = true;

this.sidebarPanes.scopechain.expanded = true;
this.sidebarPanes.jsBreakpoints.expanded = true;

this.sidebarPanes.callstack.registerShortcuts(helpSection, this.registerShortcut.bind(this));
var evaluateInConsoleShortcut = WebInspector.KeyboardShortcut.makeDescriptor("e", WebInspector.KeyboardShortcut.Modifiers.Shift | WebInspector.KeyboardShortcut.Modifiers.Ctrl);
helpSection.addKey(evaluateInConsoleShortcut.name, WebInspector.UIString("Evaluate selection in console"));
this.registerShortcut(evaluateInConsoleShortcut.key, this._evaluateSelectionInConsole.bind(this));

var outlineShortcut = WebInspector.KeyboardShortcut.makeDescriptor("o", WebInspector.KeyboardShortcut.Modifiers.CtrlOrMeta | WebInspector.KeyboardShortcut.Modifiers.Shift);
helpSection.addKey(outlineShortcut.name, WebInspector.UIString("Go to member"));
this.registerShortcut(outlineShortcut.key, this._showOutlineDialog.bind(this));

var createBreakpointShortcut = WebInspector.KeyboardShortcut.makeDescriptor("b", WebInspector.KeyboardShortcut.Modifiers.CtrlOrMeta);
helpSection.addKey(createBreakpointShortcut.name, WebInspector.UIString("Toggle breakpoint"));
this.registerShortcut(createBreakpointShortcut.key, this._toggleBreakpoint.bind(this));

var panelEnablerHeading = WebInspector.UIString("You need to enable debugging before you can use the Scripts panel.");
var panelEnablerDisclaimer = WebInspector.UIString("Enabling debugging will make scripts run slower.");
var panelEnablerButton = WebInspector.UIString("Enable Debugging");

this.panelEnablerView = new WebInspector.PanelEnablerView("scripts", panelEnablerHeading, panelEnablerDisclaimer, panelEnablerButton);
this.panelEnablerView.addEventListener("enable clicked", this._enableDebugging, this);

this.enableToggleButton = new WebInspector.StatusBarButton("", "enable-toggle-status-bar-item");
this.enableToggleButton.addEventListener("click", this._toggleDebugging, this);
if (!Capabilities.debuggerCausesRecompilation)
this.enableToggleButton.element.addStyleClass("hidden");

this._pauseOnExceptionButton = new WebInspector.StatusBarButton("", "scripts-pause-on-exceptions-status-bar-item", 3);
this._pauseOnExceptionButton.addEventListener("click", this._togglePauseOnExceptions, this);

this._toggleFormatSourceButton = new WebInspector.StatusBarButton(WebInspector.UIString("Pretty print"), "scripts-toggle-pretty-print-status-bar-item");
this._toggleFormatSourceButton.toggled = false;
this._toggleFormatSourceButton.addEventListener("click", this._toggleFormatSource, this);

this._scriptViewStatusBarItemsContainer = document.createElement("div");
this._scriptViewStatusBarItemsContainer.style.display = "inline-block";

this._installDebuggerSidebarController();

this._sourceFramesByUISourceCode = new Map();
this._updateDebuggerButtons();
this._pauseOnExceptionStateChanged();
if (WebInspector.debuggerModel.isPaused())
this._debuggerPaused();

WebInspector.settings.pauseOnExceptionStateString.addChangeListener(this._pauseOnExceptionStateChanged, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerWasEnabled, this._debuggerWasEnabled, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerWasDisabled, this._debuggerWasDisabled, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerPaused, this._debuggerPaused, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerResumed, this._debuggerResumed, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.CallFrameSelected, this._callFrameSelected, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.ConsoleCommandEvaluatedInSelectedCallFrame, this._consoleCommandEvaluatedInSelectedCallFrame, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.ExecutionLineChanged, this._executionLineChanged, this);
WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.BreakpointsActiveStateChanged, this._breakpointsActiveStateChanged, this);

WebInspector.startBatchUpdate();
var uiSourceCodes = this._workspace.uiSourceCodes();
for (var i = 0; i < uiSourceCodes.length; ++i)
this._addUISourceCode(uiSourceCodes[i]);
WebInspector.endBatchUpdate();

this._workspace.addEventListener(WebInspector.UISourceCodeProvider.Events.UISourceCodeAdded, this._uiSourceCodeAdded, this);
this._workspace.addEventListener(WebInspector.UISourceCodeProvider.Events.UISourceCodeRemoved, this._uiSourceCodeRemoved, this);
this._workspace.addEventListener(WebInspector.UISourceCodeProvider.Events.TemporaryUISourceCodeRemoved, this._uiSourceCodeRemoved, this);
this._workspace.addEventListener(WebInspector.Workspace.Events.ProjectWillReset, this._reset.bind(this), this);

WebInspector.advancedSearchController.registerSearchScope(new WebInspector.ScriptsSearchScope(this._workspace));
}

WebInspector.ScriptsPanel.prototype = {
get statusBarItems()
{
return [this.enableToggleButton.element, this._pauseOnExceptionButton.element, this._toggleFormatSourceButton.element, this._scriptViewStatusBarItemsContainer];
},

defaultFocusedElement: function()
{
return this._navigator.view.defaultFocusedElement();
},

get paused()
{
return this._paused;
},

wasShown: function()
{
WebInspector.Panel.prototype.wasShown.call(this);
this._debugSidebarContentsElement.insertBefore(this.sidebarPanes.domBreakpoints.element, this.sidebarPanes.xhrBreakpoints.element);
this.sidebarPanes.watchExpressions.show();

this._navigatorController.wasShown();
},

willHide: function()
{
WebInspector.Panel.prototype.willHide.call(this);
WebInspector.closeViewInDrawer();
},


_uiSourceCodeAdded: function(event)
{
var uiSourceCode =   (event.data);
this._addUISourceCode(uiSourceCode);
},


_addUISourceCode: function(uiSourceCode)
{
if (this._toggleFormatSourceButton.toggled)
uiSourceCode.setFormatted(true);

this._navigator.addUISourceCode(uiSourceCode);
this._editorContainer.addUISourceCode(uiSourceCode);
},

_uiSourceCodeRemoved: function(event)
{
var uiSourceCode =   (event.data);
this._editorContainer.removeUISourceCode(uiSourceCode);
this._navigator.removeUISourceCode(uiSourceCode);
this._removeSourceFrame(uiSourceCode);
},

_consoleCommandEvaluatedInSelectedCallFrame: function(event)
{
this.sidebarPanes.scopechain.update(WebInspector.debuggerModel.selectedCallFrame());
},

_debuggerPaused: function()
{
var details = WebInspector.debuggerModel.debuggerPausedDetails();

this._paused = true;
this._waitingToPause = false;
this._stepping = false;

this._updateDebuggerButtons();

WebInspector.inspectorView.setCurrentPanel(this);
this.sidebarPanes.callstack.update(details.callFrames);

if (details.reason === WebInspector.DebuggerModel.BreakReason.DOM) {
this.sidebarPanes.domBreakpoints.highlightBreakpoint(details.auxData);
function didCreateBreakpointHitStatusMessage(element)
{
this.sidebarPanes.callstack.setStatus(element);
}
this.sidebarPanes.domBreakpoints.createBreakpointHitStatusMessage(details.auxData, didCreateBreakpointHitStatusMessage.bind(this));
} else if (details.reason === WebInspector.DebuggerModel.BreakReason.EventListener) {
var eventName = details.auxData.eventName;
this.sidebarPanes.eventListenerBreakpoints.highlightBreakpoint(details.auxData.eventName);
var eventNameForUI = WebInspector.EventListenerBreakpointsSidebarPane.eventNameForUI(eventName);
this.sidebarPanes.callstack.setStatus(WebInspector.UIString("Paused on a \"%s\" Event Listener.", eventNameForUI));
} else if (details.reason === WebInspector.DebuggerModel.BreakReason.XHR) {
this.sidebarPanes.xhrBreakpoints.highlightBreakpoint(details.auxData["breakpointURL"]);
this.sidebarPanes.callstack.setStatus(WebInspector.UIString("Paused on a XMLHttpRequest."));
} else if (details.reason === WebInspector.DebuggerModel.BreakReason.Exception)
this.sidebarPanes.callstack.setStatus(WebInspector.UIString("Paused on exception: '%s'.", details.auxData.description));
else if (details.reason === WebInspector.DebuggerModel.BreakReason.Assert)
this.sidebarPanes.callstack.setStatus(WebInspector.UIString("Paused on assertion."));
else if (details.reason === WebInspector.DebuggerModel.BreakReason.CSPViolation)
this.sidebarPanes.callstack.setStatus(WebInspector.UIString("Paused on a script blocked due to Content Security Policy directive: \"%s\".", details.auxData["directiveText"]));
else {
function didGetUILocation(uiLocation)
{
var breakpoint = WebInspector.breakpointManager.findBreakpoint(uiLocation.uiSourceCode, uiLocation.lineNumber);
if (!breakpoint)
return;
this.sidebarPanes.jsBreakpoints.highlightBreakpoint(breakpoint);
this.sidebarPanes.callstack.setStatus(WebInspector.UIString("Paused on a JavaScript breakpoint."));
}
details.callFrames[0].createLiveLocation(didGetUILocation.bind(this));
}

this._showDebuggerSidebar();
this._toggleDebuggerSidebarButton.setEnabled(false);
window.focus();
InspectorFrontendHost.bringToFront();
},

_debuggerResumed: function()
{
this._paused = false;
this._waitingToPause = false;
this._stepping = false;

this._clearInterface();
this._toggleDebuggerSidebarButton.setEnabled(true);
},

_debuggerWasEnabled: function()
{
this._updateDebuggerButtons();
},

_debuggerWasDisabled: function()
{
this._reset();
},

_reset: function()
{
delete this.currentQuery;
this.searchCanceled();

this._debuggerResumed();

delete this._currentUISourceCode;
this._navigator.reset();
this._editorContainer.reset();
this._updateScriptViewStatusBarItems();
this.sidebarPanes.jsBreakpoints.reset();
this.sidebarPanes.watchExpressions.reset();

var uiSourceCodes = this._workspace.uiSourceCodes();
for (var i = 0; i < uiSourceCodes.length; ++i)
this._removeSourceFrame(uiSourceCodes[i]);
},

get visibleView()
{
return this._editorContainer.visibleView;
},

_updateScriptViewStatusBarItems: function()
{
this._scriptViewStatusBarItemsContainer.removeChildren();

var sourceFrame = this.visibleView;
if (sourceFrame) {
var statusBarItems = sourceFrame.statusBarItems() || [];
for (var i = 0; i < statusBarItems.length; ++i)
this._scriptViewStatusBarItemsContainer.appendChild(statusBarItems[i]);
}
},

canShowAnchorLocation: function(anchor)
{
if (WebInspector.debuggerModel.debuggerEnabled() && anchor.uiSourceCode)
return true;
var uiSourceCodes = this._workspace.uiSourceCodes();
for (var i = 0; i < uiSourceCodes.length; ++i) {
if (uiSourceCodes[i].url === anchor.href) {
anchor.uiSourceCode = uiSourceCodes[i];
return true;
}
}
return false;
},

showAnchorLocation: function(anchor)
{
this._showSourceLine(anchor.uiSourceCode, anchor.lineNumber);
},


showUISourceCode: function(uiSourceCode, lineNumber)
{
this._showSourceLine(uiSourceCode, lineNumber);
},


_showSourceLine: function(uiSourceCode, lineNumber)
{
var sourceFrame = this._showFile(uiSourceCode);
if (typeof lineNumber === "number")
sourceFrame.highlightLine(lineNumber);
sourceFrame.focus();
},


_showFile: function(uiSourceCode)
{
var sourceFrame = this._getOrCreateSourceFrame(uiSourceCode);
if (this._currentUISourceCode === uiSourceCode)
return sourceFrame;
this._currentUISourceCode = uiSourceCode;

if (this._navigator.isScriptSourceAdded(uiSourceCode))
this._navigator.revealUISourceCode(uiSourceCode);
this._editorContainer.showFile(uiSourceCode);
this._updateScriptViewStatusBarItems();

return sourceFrame;
},


_createSourceFrame: function(uiSourceCode)
{
var sourceFrame;
switch (uiSourceCode.contentType()) {
case WebInspector.resourceTypes.Script:
if (uiSourceCode.isSnippet && !uiSourceCode.isTemporary)
sourceFrame = new WebInspector.SnippetJavaScriptSourceFrame(this, uiSourceCode);
else
sourceFrame = new WebInspector.JavaScriptSourceFrame(this, uiSourceCode);
break;
case WebInspector.resourceTypes.Document:
sourceFrame = new WebInspector.JavaScriptSourceFrame(this, uiSourceCode);
break;
case WebInspector.resourceTypes.Stylesheet:
default:
sourceFrame = new WebInspector.UISourceCodeFrame(uiSourceCode);
break;
}
this._sourceFramesByUISourceCode.put(uiSourceCode, sourceFrame);
return sourceFrame;
},


_getOrCreateSourceFrame: function(uiSourceCode)
{
return this._sourceFramesByUISourceCode.get(uiSourceCode) || this._createSourceFrame(uiSourceCode);
},


viewForFile: function(uiSourceCode)
{
return this._getOrCreateSourceFrame(uiSourceCode);
},


_removeSourceFrame: function(uiSourceCode)
{
var sourceFrame = this._sourceFramesByUISourceCode.get(uiSourceCode);
if (!sourceFrame)
return;
this._sourceFramesByUISourceCode.remove(uiSourceCode);
sourceFrame.detach();
},

_clearCurrentExecutionLine: function()
{
if (this._executionSourceFrame)
this._executionSourceFrame.clearExecutionLine();
delete this._executionSourceFrame;
},

_executionLineChanged: function(event)
{
var uiLocation = event.data;

this._clearCurrentExecutionLine();
if (!uiLocation)
return;
var sourceFrame = this._getOrCreateSourceFrame(uiLocation.uiSourceCode);
sourceFrame.setExecutionLine(uiLocation.lineNumber);
this._executionSourceFrame = sourceFrame;
},

_revealExecutionLine: function(uiLocation)
{
var uiSourceCode = uiLocation.uiSourceCode;

if (uiSourceCode.isTemporary) {
if (this._currentUISourceCode && this._currentUISourceCode.scriptFile() && this._currentUISourceCode.scriptFile().isDivergingFromVM())
return;
this._editorContainer.addUISourceCode(uiSourceCode);
if (uiSourceCode.formatted() !== this._toggleFormatSourceButton.toggled)
uiSourceCode.setFormatted(this._toggleFormatSourceButton.toggled);
}
var sourceFrame = this._showFile(uiSourceCode);
sourceFrame.revealLine(uiLocation.lineNumber);
sourceFrame.focus();
},

_callFrameSelected: function(event)
{
var callFrame = event.data;

if (!callFrame)
return;

this.sidebarPanes.scopechain.update(callFrame);
this.sidebarPanes.watchExpressions.refreshExpressions();
this.sidebarPanes.callstack.setSelectedCallFrame(callFrame);
callFrame.createLiveLocation(this._revealExecutionLine.bind(this));
},

_editorClosed: function(event)
{
this._navigatorController.hideNavigatorOverlay();
var uiSourceCode =   (event.data);

if (this._currentUISourceCode === uiSourceCode)
delete this._currentUISourceCode;


this._updateScriptViewStatusBarItems();
WebInspector.searchController.resetSearch();
},

_editorSelected: function(event)
{
var uiSourceCode =   (event.data);
var sourceFrame = this._showFile(uiSourceCode);
this._navigatorController.hideNavigatorOverlay();
sourceFrame.focus();
WebInspector.searchController.resetSearch();
},

_scriptSelected: function(event)
{
var uiSourceCode =   (event.data.uiSourceCode);
var sourceFrame = this._showFile(uiSourceCode);
this._navigatorController.hideNavigatorOverlay();
if (sourceFrame && event.data.focusSource)
sourceFrame.focus();
},

_pauseOnExceptionStateChanged: function()
{
var pauseOnExceptionsState = WebInspector.settings.pauseOnExceptionStateString.get();
switch (pauseOnExceptionsState) {
case WebInspector.DebuggerModel.PauseOnExceptionsState.DontPauseOnExceptions:
this._pauseOnExceptionButton.title = WebInspector.UIString("Don't pause on exceptions.\nClick to Pause on all exceptions.");
break;
case WebInspector.DebuggerModel.PauseOnExceptionsState.PauseOnAllExceptions:
this._pauseOnExceptionButton.title = WebInspector.UIString("Pause on all exceptions.\nClick to Pause on uncaught exceptions.");
break;
case WebInspector.DebuggerModel.PauseOnExceptionsState.PauseOnUncaughtExceptions:
this._pauseOnExceptionButton.title = WebInspector.UIString("Pause on uncaught exceptions.\nClick to Not pause on exceptions.");
break;
}
this._pauseOnExceptionButton.state = pauseOnExceptionsState;
},

_updateDebuggerButtons: function()
{
if (WebInspector.debuggerModel.debuggerEnabled()) {
this.enableToggleButton.title = WebInspector.UIString("Debugging enabled. Click to disable.");
this.enableToggleButton.toggled = true;
this._pauseOnExceptionButton.visible = true;
this.panelEnablerView.detach();
} else {
this.enableToggleButton.title = WebInspector.UIString("Debugging disabled. Click to enable.");
this.enableToggleButton.toggled = false;
this._pauseOnExceptionButton.visible = false;
this.panelEnablerView.show(this.element);
}

if (this._paused) {
this._updateButtonTitle(this.pauseButton, WebInspector.UIString("Resume script execution (%s)."))
this.pauseButton.addStyleClass("paused");

this.pauseButton.disabled = false;
this.stepOverButton.disabled = false;
this.stepIntoButton.disabled = false;
this.stepOutButton.disabled = false;

this.debuggerStatusElement.textContent = WebInspector.UIString("Paused");
} else {
this._updateButtonTitle(this.pauseButton, WebInspector.UIString("Pause script execution (%s)."))
this.pauseButton.removeStyleClass("paused");

this.pauseButton.disabled = this._waitingToPause;
this.stepOverButton.disabled = true;
this.stepIntoButton.disabled = true;
this.stepOutButton.disabled = true;

if (this._waitingToPause)
this.debuggerStatusElement.textContent = WebInspector.UIString("Pausing");
else if (this._stepping)
this.debuggerStatusElement.textContent = WebInspector.UIString("Stepping");
else
this.debuggerStatusElement.textContent = "";
}
},

_clearInterface: function()
{
this.sidebarPanes.callstack.update(null);
this.sidebarPanes.scopechain.update(null);
this.sidebarPanes.jsBreakpoints.clearBreakpointHighlight();
this.sidebarPanes.domBreakpoints.clearBreakpointHighlight();
this.sidebarPanes.eventListenerBreakpoints.clearBreakpointHighlight();
this.sidebarPanes.xhrBreakpoints.clearBreakpointHighlight();

this._clearCurrentExecutionLine();
this._updateDebuggerButtons();
},

_enableDebugging: function()
{
this._toggleDebugging(this.panelEnablerView.alwaysEnabled);
},

_toggleDebugging: function(optionalAlways)
{
this._paused = false;
this._waitingToPause = false;
this._stepping = false;

if (WebInspector.debuggerModel.debuggerEnabled()) {
WebInspector.settings.debuggerEnabled.set(false);
WebInspector.debuggerModel.disableDebugger();
} else {
WebInspector.settings.debuggerEnabled.set(!!optionalAlways);
WebInspector.debuggerModel.enableDebugger();
}
},

_togglePauseOnExceptions: function()
{
var nextStateMap = {};
var stateEnum = WebInspector.DebuggerModel.PauseOnExceptionsState;
nextStateMap[stateEnum.DontPauseOnExceptions] = stateEnum.PauseOnAllExceptions;
nextStateMap[stateEnum.PauseOnAllExceptions] = stateEnum.PauseOnUncaughtExceptions;
nextStateMap[stateEnum.PauseOnUncaughtExceptions] = stateEnum.DontPauseOnExceptions;
WebInspector.settings.pauseOnExceptionStateString.set(nextStateMap[this._pauseOnExceptionButton.state]);
},

_togglePause: function()
{
if (this._paused) {
this._paused = false;
this._waitingToPause = false;
DebuggerAgent.resume();
} else {
this._stepping = false;
this._waitingToPause = true;
DebuggerAgent.pause();
}

this._clearInterface();
},

_stepOverClicked: function()
{
if (!this._paused)
return;

this._paused = false;
this._stepping = true;

this._clearInterface();

DebuggerAgent.stepOver();
},

_stepIntoClicked: function()
{
if (!this._paused)
return;

this._paused = false;
this._stepping = true;

this._clearInterface();

DebuggerAgent.stepInto();
},

_stepOutClicked: function()
{
if (!this._paused)
return;

this._paused = false;
this._stepping = true;

this._clearInterface();

DebuggerAgent.stepOut();
},

_toggleBreakpointsClicked: function(event)
{
WebInspector.debuggerModel.setBreakpointsActive(!WebInspector.debuggerModel.breakpointsActive());
},

_breakpointsActiveStateChanged: function(event)
{
var active = event.data;
this._toggleBreakpointsButton.toggled = active;
if (active) {
this._toggleBreakpointsButton.title = WebInspector.UIString("Deactivate breakpoints.");
WebInspector.inspectorView.element.removeStyleClass("breakpoints-deactivated");
this.sidebarPanes.jsBreakpoints.listElement.removeStyleClass("breakpoints-list-deactivated");
} else {
this._toggleBreakpointsButton.title = WebInspector.UIString("Activate breakpoints.");
WebInspector.inspectorView.element.addStyleClass("breakpoints-deactivated");
this.sidebarPanes.jsBreakpoints.listElement.addStyleClass("breakpoints-list-deactivated");
}
},

_evaluateSelectionInConsole: function()
{
var selection = window.getSelection();
if (selection.type === "Range" && !selection.isCollapsed)
WebInspector.evaluateInConsole(selection.toString());
},

_createDebugToolbar: function(section)
{
var debugToolbar = document.createElement("div");
debugToolbar.className = "status-bar";
debugToolbar.id = "scripts-debug-toolbar";

var title, handler, shortcuts;
var platformSpecificModifier = WebInspector.KeyboardShortcut.Modifiers.CtrlOrMeta;


handler = this._togglePause.bind(this);
shortcuts = [];
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F8));
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Slash, platformSpecificModifier));
this.pauseButton = this._createButtonAndRegisterShortcuts(section, "scripts-pause", "", handler, shortcuts, WebInspector.UIString("Pause/Continue"));
debugToolbar.appendChild(this.pauseButton);


title = WebInspector.UIString("Step over next function call (%s).");
handler = this._stepOverClicked.bind(this);
shortcuts = [];
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F10));
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.SingleQuote, platformSpecificModifier));
this.stepOverButton = this._createButtonAndRegisterShortcuts(section, "scripts-step-over", title, handler, shortcuts, WebInspector.UIString("Step over"));
debugToolbar.appendChild(this.stepOverButton);


title = WebInspector.UIString("Step into next function call (%s).");
handler = this._stepIntoClicked.bind(this);
shortcuts = [];
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F11));
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Semicolon, platformSpecificModifier));
this.stepIntoButton = this._createButtonAndRegisterShortcuts(section, "scripts-step-into", title, handler, shortcuts, WebInspector.UIString("Step into"));
debugToolbar.appendChild(this.stepIntoButton);


title = WebInspector.UIString("Step out of current function (%s).");
handler = this._stepOutClicked.bind(this);
shortcuts = [];
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F11, WebInspector.KeyboardShortcut.Modifiers.Shift));
shortcuts.push(WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Semicolon, WebInspector.KeyboardShortcut.Modifiers.Shift | platformSpecificModifier));
this.stepOutButton = this._createButtonAndRegisterShortcuts(section, "scripts-step-out", title, handler, shortcuts, WebInspector.UIString("Step out"));
debugToolbar.appendChild(this.stepOutButton);

this._toggleBreakpointsButton = new WebInspector.StatusBarButton(WebInspector.UIString("Deactivate breakpoints."), "toggle-breakpoints");
this._toggleBreakpointsButton.toggled = true;
this._toggleBreakpointsButton.addEventListener("click", this._toggleBreakpointsClicked, this);
debugToolbar.appendChild(this._toggleBreakpointsButton.element);

this.debuggerStatusElement = document.createElement("div");
this.debuggerStatusElement.id = "scripts-debugger-status";
debugToolbar.appendChild(this.debuggerStatusElement);

return debugToolbar;
},

_updateButtonTitle: function(button, buttonTitle)
{
button.buttonTitle = buttonTitle;
var hasShortcuts = button.shortcuts && button.shortcuts.length;
if (hasShortcuts)
button.title = String.vsprintf(buttonTitle, [button.shortcuts[0].name]);
else
button.title = buttonTitle;
},

_createButtonAndRegisterShortcuts: function(section, buttonId, buttonTitle, handler, shortcuts, shortcutDescription)
{
var button = document.createElement("button");
button.className = "status-bar-item";
button.id = buttonId;
button.shortcuts = shortcuts;
this._updateButtonTitle(button, buttonTitle);
button.disabled = true;
button.appendChild(document.createElement("img"));
button.addEventListener("click", handler, false);

var shortcutNames = [];
for (var i = 0; i < shortcuts.length; ++i) {
this.registerShortcut(shortcuts[i].key, handler);
shortcutNames.push(shortcuts[i].name);
}
section.addAlternateKeys(shortcutNames, shortcutDescription);

return button;
},

searchCanceled: function()
{
if (this._searchView)
this._searchView.searchCanceled();

delete this._searchView;
delete this._searchQuery;
},


performSearch: function(query)
{
WebInspector.searchController.updateSearchMatchesCount(0, this);

if (!this.visibleView)
return;


this.searchCanceled();

this._searchView = this.visibleView;
this._searchQuery = query;

function finishedCallback(view, searchMatches)
{
if (!searchMatches)
return;

WebInspector.searchController.updateSearchMatchesCount(searchMatches, this);
view.jumpToNextSearchResult();
WebInspector.searchController.updateCurrentMatchIndex(view.currentSearchResultIndex, this);
}

this._searchView.performSearch(query, finishedCallback.bind(this));
},

jumpToNextSearchResult: function()
{
if (!this._searchView)
return;

if (this._searchView !== this.visibleView) {
this.performSearch(this._searchQuery);
return;
}

if (this._searchView.showingLastSearchResult())
this._searchView.jumpToFirstSearchResult();
else
this._searchView.jumpToNextSearchResult();
WebInspector.searchController.updateCurrentMatchIndex(this._searchView.currentSearchResultIndex, this);
return true;
},

jumpToPreviousSearchResult: function()
{
if (!this._searchView)
return false;

if (this._searchView !== this.visibleView) {
this.performSearch(this._searchQuery);
if (this._searchView)
this._searchView.jumpToLastSearchResult();
return;
}

if (this._searchView.showingFirstSearchResult())
this._searchView.jumpToLastSearchResult();
else
this._searchView.jumpToPreviousSearchResult();
WebInspector.searchController.updateCurrentMatchIndex(this._searchView.currentSearchResultIndex, this);
},


canSearchAndReplace: function()
{
var view =   (this.visibleView);
return !!view && view.canEditSource();
},


replaceSelectionWith: function(text)
{
var view =   (this.visibleView);
view.replaceSearchMatchWith(text);
},


replaceAllWith: function(query, text)
{
var view =   (this.visibleView);
view.replaceAllWith(query, text);
},

_toggleFormatSource: function()
{
this._toggleFormatSourceButton.toggled = !this._toggleFormatSourceButton.toggled;
var uiSourceCodes = this._workspace.uiSourceCodes();
for (var i = 0; i < uiSourceCodes.length; ++i)
uiSourceCodes[i].setFormatted(this._toggleFormatSourceButton.toggled);
},

addToWatch: function(expression)
{
this.sidebarPanes.watchExpressions.addExpression(expression);
},

_toggleBreakpoint: function()
{
var sourceFrame = this.visibleView;
if (!sourceFrame)
return;

if (sourceFrame instanceof WebInspector.JavaScriptSourceFrame) {
var javaScriptSourceFrame =   (sourceFrame);
javaScriptSourceFrame.toggleBreakpointOnCurrentLine();
}            
},

_showOutlineDialog: function()
{
var uiSourceCode = this._editorContainer.currentFile();
if (!uiSourceCode)
return;

switch (uiSourceCode.contentType()) {
case WebInspector.resourceTypes.Document:
case WebInspector.resourceTypes.Script:
WebInspector.JavaScriptOutlineDialog.show(this.visibleView, uiSourceCode);
break;
case WebInspector.resourceTypes.Stylesheet:
WebInspector.StyleSheetOutlineDialog.show(this.visibleView, uiSourceCode);
break;
}
},

_installDebuggerSidebarController: function()
{
this._toggleDebuggerSidebarButton = new WebInspector.StatusBarButton(WebInspector.UIString("Hide debugger"), "scripts-debugger-show-hide-button", 3);
this._toggleDebuggerSidebarButton.state = "shown";
this._toggleDebuggerSidebarButton.addEventListener("click", clickHandler, this);

function clickHandler()
{
if (this._toggleDebuggerSidebarButton.state === "shown")
this._hideDebuggerSidebar();
else
this._showDebuggerSidebar();
}
this.editorView.element.appendChild(this._toggleDebuggerSidebarButton.element);

if (WebInspector.settings.debuggerSidebarHidden.get())
this._hideDebuggerSidebar();

},

_showDebuggerSidebar: function()
{
if (this._toggleDebuggerSidebarButton.state === "shown")
return;
this._toggleDebuggerSidebarButton.state = "shown";
this._toggleDebuggerSidebarButton.title = WebInspector.UIString("Hide debugger");
this.splitView.showSidebarElement();
this.debugSidebarResizeWidgetElement.removeStyleClass("hidden");
WebInspector.settings.debuggerSidebarHidden.set(false);
},

_hideDebuggerSidebar: function()
{
if (this._toggleDebuggerSidebarButton.state === "hidden")
return;
this._toggleDebuggerSidebarButton.state = "hidden";
this._toggleDebuggerSidebarButton.title = WebInspector.UIString("Show debugger");
this.splitView.hideSidebarElement();
this.debugSidebarResizeWidgetElement.addStyleClass("hidden");
WebInspector.settings.debuggerSidebarHidden.set(true);
},

_fileRenamed: function(event)
{
var uiSourceCode =   (event.data.uiSourceCode);
var name =   (event.data.name);
if (!uiSourceCode.isSnippet)
return;
WebInspector.scriptSnippetModel.renameScriptSnippet(uiSourceCode, name);
},


_snippetCreationRequested: function(event)
{
var uiSourceCode = WebInspector.scriptSnippetModel.createScriptSnippet();
this._showSourceLine(uiSourceCode);

var shouldHideNavigator = !this._navigatorController.isNavigatorPinned();
if (this._navigatorController.isNavigatorHidden())
this._navigatorController.showNavigatorOverlay();
this._navigator.rename(uiSourceCode, callback.bind(this));


function callback(committed)
{
if (shouldHideNavigator)
this._navigatorController.hideNavigatorOverlay();

if (!committed) {
WebInspector.scriptSnippetModel.deleteScriptSnippet(uiSourceCode);
return;
}

this._showSourceLine(uiSourceCode);
}
},


_itemRenamingRequested: function(event)
{
var uiSourceCode =   (event.data);

var shouldHideNavigator = !this._navigatorController.isNavigatorPinned();
if (this._navigatorController.isNavigatorHidden())
this._navigatorController.showNavigatorOverlay();
this._navigator.rename(uiSourceCode, callback.bind(this));


function callback(committed)
{
if (shouldHideNavigator && committed) {
this._navigatorController.hideNavigatorOverlay();
this._showSourceLine(uiSourceCode);
}
}
},


_showLocalHistory: function(uiSourceCode)
{
WebInspector.RevisionHistoryView.showHistory(uiSourceCode);
},


appendApplicableItems: function(event, contextMenu, target)
{
this._appendUISourceCodeItems(contextMenu, target);
this._appendFunctionItems(contextMenu, target);
},


_appendUISourceCodeItems: function(contextMenu, target)
{
if (!(target instanceof WebInspector.UISourceCode))
return;

var uiSourceCode =   (target);
contextMenu.appendItem(WebInspector.UIString("Local modifications..."), this._showLocalHistory.bind(this, uiSourceCode));
var resource = WebInspector.resourceForURL(uiSourceCode.url);
if (resource && resource.request)
contextMenu.appendApplicableItems(resource.request);
},


_appendFunctionItems: function(contextMenu, target)
{
if (!(target instanceof WebInspector.RemoteObject))
return;
var remoteObject =   (target);
if (remoteObject.type !== "function")
return;

function didGetDetails(error, response)
{
if (error) {
console.error(error);
return;
}
WebInspector.inspectorView.showPanelForAnchorNavigation(this);
var uiLocation = WebInspector.debuggerModel.rawLocationToUILocation(response.location);
this._showSourceLine(uiLocation.uiSourceCode, uiLocation.lineNumber);
}

function revealFunction()
{
DebuggerAgent.getFunctionDetails(remoteObject.objectId, didGetDetails.bind(this));
}

contextMenu.appendItem(WebInspector.UIString("Show function definition"), revealFunction.bind(this));
},

showGoToSourceDialog: function()
{
WebInspector.OpenResourceDialog.show(this, this._workspace, this.editorView.mainElement);
},

__proto__: WebInspector.Panel.prototype
}
