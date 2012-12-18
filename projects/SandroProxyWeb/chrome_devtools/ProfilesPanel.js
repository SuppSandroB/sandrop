


const UserInitiatedProfileName = "org.webkit.profiles.user-initiated";


WebInspector.ProfileType = function(id, name)
{
this._id = id;
this._name = name;

this.treeElement = null;
}

WebInspector.ProfileType.prototype = {
get buttonTooltip()
{
return "";
},

get id()
{
return this._id;
},

get treeItemTitle()
{
return this._name;
},

get name()
{
return this._name;
},


buttonClicked: function(profilesPanel)
{
return false;
},

reset: function()
{
},

get description()
{
return "";
},



createTemporaryProfile: function(title)
{
throw new Error("Needs implemented.");
},


createProfile: function(profile)
{
throw new Error("Not supported for " + this._name + " profiles.");
}
}


WebInspector.ProfileHeader = function(profileType, title, uid)
{
this._profileType = profileType;
this.title = title;
if (uid === undefined) {
this.uid = -1;
this.isTemporary = true;
} else {
this.uid = uid;
this.isTemporary = false;
}
this._fromFile = false;
}

WebInspector.ProfileHeader.prototype = {
profileType: function()
{
return this._profileType;
},


createSidebarTreeElement: function()
{
throw new Error("Needs implemented.");
},

existingView: function()
{
return this._view;
},

view: function()
{
if (!this._view)
this._view = this.createView(WebInspector.ProfilesPanel._instance);
return this._view;
},


createView: function(profilesPanel)
{
throw new Error("Not implemented.");
},


load: function(callback) { },


canSaveToFile: function() { return false; },

saveToFile: function() { throw new Error("Needs implemented"); },


canLoadFromFile: function() { return false; },


loadFromFile: function(file) { throw new Error("Needs implemented"); },


fromFile: function() { return this._fromFile; }
}


WebInspector.ProfilesPanel = function()
{
WebInspector.Panel.call(this, "profiles");
WebInspector.ProfilesPanel._instance = this;
this.registerRequiredCSS("panelEnablerView.css");
this.registerRequiredCSS("heapProfiler.css");
this.registerRequiredCSS("profilesPanel.css");

this.createSidebarViewWithTree();

this.profilesItemTreeElement = new WebInspector.ProfilesSidebarTreeElement(this);
this.sidebarTree.appendChild(this.profilesItemTreeElement);

this._profileTypesByIdMap = {};

var panelEnablerHeading = WebInspector.UIString("You need to enable profiling before you can use the Profiles panel.");
var panelEnablerDisclaimer = WebInspector.UIString("Enabling profiling will make scripts run slower.");
var panelEnablerButton = WebInspector.UIString("Enable Profiling");
this.panelEnablerView = new WebInspector.PanelEnablerView("profiles", panelEnablerHeading, panelEnablerDisclaimer, panelEnablerButton);
this.panelEnablerView.addEventListener("enable clicked", this.enableProfiler, this);

this.profileViews = document.createElement("div");
this.profileViews.id = "profile-views";
this.splitView.mainElement.appendChild(this.profileViews);

this._statusBarButtons = [];

this.enableToggleButton = new WebInspector.StatusBarButton("", "enable-toggle-status-bar-item");
if (Capabilities.profilerCausesRecompilation) {
this._statusBarButtons.push(this.enableToggleButton);
this.enableToggleButton.addEventListener("click", this._toggleProfiling, this);
}
this.recordButton = new WebInspector.StatusBarButton("", "record-profile-status-bar-item");
this.recordButton.addEventListener("click", this.toggleRecordButton, this);
this._statusBarButtons.push(this.recordButton);

this.clearResultsButton = new WebInspector.StatusBarButton(WebInspector.UIString("Clear all profiles."), "clear-status-bar-item");
this.clearResultsButton.addEventListener("click", this._clearProfiles, this);
this._statusBarButtons.push(this.clearResultsButton);

if (WebInspector.experimentsSettings.liveNativeMemoryChart.isEnabled()) {
this.garbageCollectButton = new WebInspector.StatusBarButton(WebInspector.UIString("Collect Garbage"), "garbage-collect-status-bar-item");
this.garbageCollectButton.addEventListener("click", this._garbageCollectButtonClicked, this);
this._statusBarButtons.push(this.garbageCollectButton);
}

this.profileViewStatusBarItemsContainer = document.createElement("div");
this.profileViewStatusBarItemsContainer.className = "status-bar-items";

this._profiles = [];
this._profilerEnabled = !Capabilities.profilerCausesRecompilation;

this._launcherView = new WebInspector.ProfileLauncherView(this);
this._launcherView.addEventListener(WebInspector.ProfileLauncherView.EventTypes.ProfileTypeSelected, this._onProfileTypeSelected, this);
this._reset();

this._registerProfileType(new WebInspector.CPUProfileType());
if (!WebInspector.WorkerManager.isWorkerFrontend())
this._registerProfileType(new WebInspector.CSSSelectorProfileType());
if (Capabilities.heapProfilerPresent)
this._registerProfileType(new WebInspector.HeapSnapshotProfileType());
if (WebInspector.experimentsSettings.nativeMemorySnapshots.isEnabled())
this._registerProfileType(new WebInspector.NativeMemoryProfileType());
if (WebInspector.experimentsSettings.canvasInspection.isEnabled())
this._registerProfileType(new WebInspector.CanvasProfileType());

InspectorBackend.registerProfilerDispatcher(new WebInspector.ProfilerDispatcher(this));

this._createFileSelectorElement();
this.element.addEventListener("contextmenu", this._handleContextMenuEvent.bind(this), true);

WebInspector.ContextMenu.registerProvider(this);
}

WebInspector.ProfilesPanel.prototype = {
_createFileSelectorElement: function()
{
if (this._fileSelectorElement)
this.element.removeChild(this._fileSelectorElement);
this._fileSelectorElement = WebInspector.createFileSelectorElement(this._loadFromFile.bind(this));
this.element.appendChild(this._fileSelectorElement);
},

_loadFromFile: function(file)
{
if (!file.name.endsWith(".heapsnapshot")) {
WebInspector.log(WebInspector.UIString("Only heap snapshots from files with extension '.heapsnapshot' can be loaded."));
return;
}

if (!!this.findTemporaryProfile(WebInspector.HeapSnapshotProfileType.TypeId)) {
WebInspector.log(WebInspector.UIString("Can't load profile when other profile is recording."));
return;
}

var profileType = this.getProfileType(WebInspector.HeapSnapshotProfileType.TypeId);
var temporaryProfile = profileType.createTemporaryProfile(UserInitiatedProfileName + "." + file.name);
this.addProfileHeader(temporaryProfile);

temporaryProfile._fromFile = true;
temporaryProfile.loadFromFile(file);
this._createFileSelectorElement();
},

get statusBarItems()
{
return this._statusBarButtons.select("element").concat([this.profileViewStatusBarItemsContainer]);
},

toggleRecordButton: function()
{
var isProfiling = this._selectedProfileType.buttonClicked(this);
this.recordButton.toggled = isProfiling;
this.recordButton.title = this._selectedProfileType.buttonTooltip;
if (isProfiling)
this._launcherView.profileStarted();
else
this._launcherView.profileFinished();
},

wasShown: function()
{
WebInspector.Panel.prototype.wasShown.call(this);
this._populateProfiles();
},

_profilerWasEnabled: function()
{
if (this._profilerEnabled)
return;

this._profilerEnabled = true;

this._reset();
if (this.isShowing())
this._populateProfiles();
},

_profilerWasDisabled: function()
{
if (!this._profilerEnabled)
return;

this._profilerEnabled = false;
this._reset();
},

_onProfileTypeSelected: function(event)
{
this._selectedProfileType = event.data;
this.recordButton.title = this._selectedProfileType.buttonTooltip;
},

_reset: function()
{
WebInspector.Panel.prototype.reset.call(this);

for (var i = 0; i < this._profiles.length; ++i) {
var view = this._profiles[i].existingView();
if (view) {
view.detach();
if ("dispose" in view)
view.dispose();
}
}
delete this.visibleView;

delete this.currentQuery;
this.searchCanceled();

for (var id in this._profileTypesByIdMap) {
var profileType = this._profileTypesByIdMap[id];
var treeElement = profileType.treeElement;
treeElement.removeChildren();
treeElement.hidden = true;
profileType.reset();
}

this._profiles = [];
this._profilesIdMap = {};
this._profileGroups = {};
this._profileGroupsForLinks = {};
this._profilesWereRequested = false;
this.recordButton.toggled = false;
if (this._selectedProfileType)
this.recordButton.title = this._selectedProfileType.buttonTooltip;
this._launcherView.profileFinished();

this.sidebarTreeElement.removeStyleClass("some-expandable");

this.profileViews.removeChildren();
this.profileViewStatusBarItemsContainer.removeChildren();

this.removeAllListeners();

this._updateInterface();
this.profilesItemTreeElement.select();
this._showLauncherView();
},

_showLauncherView: function()
{
this.closeVisibleView();
this.profileViewStatusBarItemsContainer.removeChildren();
this._launcherView.show(this.splitView.mainElement);
this.visibleView = this._launcherView;
},

_clearProfiles: function()
{
ProfilerAgent.clearProfiles();
this._reset();
},

_garbageCollectButtonClicked: function()
{
ProfilerAgent.collectGarbage();
},


_registerProfileType: function(profileType)
{
this._profileTypesByIdMap[profileType.id] = profileType;
this._launcherView.addProfileType(profileType);
profileType.treeElement = new WebInspector.SidebarSectionTreeElement(profileType.treeItemTitle, null, true);
profileType.treeElement.hidden = true;
this.sidebarTree.appendChild(profileType.treeElement);
profileType.treeElement.childrenListElement.addEventListener("contextmenu", this._handleContextMenuEvent.bind(this), true);
},

_handleContextMenuEvent: function(event)
{
var element = event.srcElement;
while (element && !element.treeElement && element !== this.element)
element = element.parentElement;
if (!element)
return;
if (element.treeElement && element.treeElement.handleContextMenuEvent) {
element.treeElement.handleContextMenuEvent(event);
return;
}
if (element !== this.element || event.srcElement === this.sidebarElement) {
var contextMenu = new WebInspector.ContextMenu(event);
if (this.visibleView instanceof WebInspector.HeapSnapshotView)
this.visibleView.populateContextMenu(contextMenu, event);
contextMenu.appendItem(WebInspector.UIString("Load Heap Snapshot\u2026"), this._fileSelectorElement.click.bind(this._fileSelectorElement));
contextMenu.show();
}

},


_makeTitleKey: function(text, profileTypeId)
{
return escape(text) + '/' + escape(profileTypeId);
},


_makeKey: function(id, profileTypeId)
{
return id + '/' + escape(profileTypeId);
},


addProfileHeader: function(profile)
{
this._removeTemporaryProfile(profile.profileType().id);

var profileType = profile.profileType();
var typeId = profileType.id;
var sidebarParent = profileType.treeElement;
sidebarParent.hidden = false;
var small = false;
var alternateTitle;

this._profiles.push(profile);
this._profilesIdMap[this._makeKey(profile.uid, typeId)] = profile;

if (!profile.title.startsWith(UserInitiatedProfileName)) {
var profileTitleKey = this._makeTitleKey(profile.title, typeId);
if (!(profileTitleKey in this._profileGroups))
this._profileGroups[profileTitleKey] = [];

var group = this._profileGroups[profileTitleKey];
group.push(profile);

if (group.length === 2) {

group._profilesTreeElement = new WebInspector.ProfileGroupSidebarTreeElement(profile.title);


var index = sidebarParent.children.indexOf(group[0]._profilesTreeElement);
sidebarParent.insertChild(group._profilesTreeElement, index);


var selected = group[0]._profilesTreeElement.selected;
sidebarParent.removeChild(group[0]._profilesTreeElement);
group._profilesTreeElement.appendChild(group[0]._profilesTreeElement);
if (selected)
group[0]._profilesTreeElement.revealAndSelect();

group[0]._profilesTreeElement.small = true;
group[0]._profilesTreeElement.mainTitle = WebInspector.UIString("Run %d", 1);

this.sidebarTreeElement.addStyleClass("some-expandable");
}

if (group.length >= 2) {
sidebarParent = group._profilesTreeElement;
alternateTitle = WebInspector.UIString("Run %d", group.length);
small = true;
}
}

var profileTreeElement = profile.createSidebarTreeElement();
profile.sidebarElement = profileTreeElement;
profileTreeElement.small = small;
if (alternateTitle)
profileTreeElement.mainTitle = alternateTitle;
profile._profilesTreeElement = profileTreeElement;

sidebarParent.appendChild(profileTreeElement);
if (!profile.isTemporary) {
if (!this.visibleView)
this.showProfile(profile);
this.dispatchEventToListeners("profile added");
}
},


_removeProfileHeader: function(profile)
{
var sidebarParent = profile.profileType().treeElement;

for (var i = 0; i < this._profiles.length; ++i) {
if (this._profiles[i].uid === profile.uid) {
profile = this._profiles[i];
this._profiles.splice(i, 1);
break;
}
}
delete this._profilesIdMap[this._makeKey(profile.uid, profile.profileType().id)];

var profileTitleKey = this._makeTitleKey(profile.title, profile.profileType().id);
delete this._profileGroups[profileTitleKey];

sidebarParent.removeChild(profile._profilesTreeElement);

if (!profile.isTemporary)
ProfilerAgent.removeProfile(profile.profileType().id, profile.uid);



if (!this._profiles.length)
this.closeVisibleView();
},


showProfile: function(profile)
{
if (!profile || profile.isTemporary)
return;

var view = profile.view();
if (view === this.visibleView)
return;

this.closeVisibleView();

view.show(this.profileViews);

profile._profilesTreeElement._suppressOnSelect = true;
profile._profilesTreeElement.revealAndSelect();
delete profile._profilesTreeElement._suppressOnSelect;

this.visibleView = view;

this.profileViewStatusBarItemsContainer.removeChildren();

var statusBarItems = view.statusBarItems;
if (statusBarItems)
for (var i = 0; i < statusBarItems.length; ++i)
this.profileViewStatusBarItemsContainer.appendChild(statusBarItems[i]);
},


getProfiles: function(typeId)
{
var result = [];
var profilesCount = this._profiles.length;
for (var i = 0; i < profilesCount; ++i) {
var profile = this._profiles[i];
if (!profile.isTemporary && profile.profileType().id === typeId)
result.push(profile);
}
return result;
},


showObject: function(snapshotObjectId, viewName)
{
var heapProfiles = this.getProfiles(WebInspector.HeapSnapshotProfileType.TypeId);
for (var i = 0; i < heapProfiles.length; i++) {
var profile = heapProfiles[i];

if (profile.maxJSObjectId >= snapshotObjectId) {
this.showProfile(profile);
profile.view().changeView(viewName, function() {
profile.view().dataGrid.highlightObjectByHeapSnapshotId(snapshotObjectId);
});
break;
}
}
},


findTemporaryProfile: function(typeId)
{
var profilesCount = this._profiles.length;
for (var i = 0; i < profilesCount; ++i)
if (this._profiles[i].profileType().id === typeId && this._profiles[i].isTemporary)
return this._profiles[i];
return null;
},


_removeTemporaryProfile: function(typeId)
{
var temporaryProfile = this.findTemporaryProfile(typeId);
if (temporaryProfile)
this._removeProfileHeader(temporaryProfile);
},


getProfile: function(typeId, uid)
{
return this._profilesIdMap[this._makeKey(uid, typeId)];
},


_addHeapSnapshotChunk: function(uid, chunk)
{
var profile = this._profilesIdMap[this._makeKey(uid, WebInspector.HeapSnapshotProfileType.TypeId)];
if (!profile)
return;
profile.transferChunk(chunk);
},


_finishHeapSnapshot: function(uid)
{
var profile = this._profilesIdMap[this._makeKey(uid, WebInspector.HeapSnapshotProfileType.TypeId)];
if (!profile)
return;
profile.finishHeapSnapshot();
},


showView: function(view)
{
this.showProfile(view.profile);
},


getProfileType: function(typeId)
{
return this._profileTypesByIdMap[typeId];
},


showProfileForURL: function(url)
{
var match = url.match(WebInspector.ProfileURLRegExp);
if (!match)
return;
this.showProfile(this._profilesIdMap[this._makeKey(Number(match[3]), match[1])]);
},

closeVisibleView: function()
{
if (this.visibleView)
this.visibleView.detach();
delete this.visibleView;
},


displayTitleForProfileLink: function(title, typeId)
{
title = unescape(title);
if (title.startsWith(UserInitiatedProfileName)) {
title = WebInspector.UIString("Profile %d", title.substring(UserInitiatedProfileName.length + 1));
} else {
var titleKey = this._makeTitleKey(title, typeId);
if (!(titleKey in this._profileGroupsForLinks))
this._profileGroupsForLinks[titleKey] = 0;

var groupNumber = ++this._profileGroupsForLinks[titleKey];

if (groupNumber > 2)


title += " " + WebInspector.UIString("Run %d", (groupNumber + 1) / 2);
}

return title;
},


performSearch: function(query)
{
this.searchCanceled();

var searchableViews = this._searchableViews();
if (!searchableViews || !searchableViews.length)
return;

var visibleView = this.visibleView;

var matchesCountUpdateTimeout = null;

function updateMatchesCount()
{
WebInspector.searchController.updateSearchMatchesCount(this._totalSearchMatches, this);
WebInspector.searchController.updateCurrentMatchIndex(this._currentSearchResultIndex, this);
matchesCountUpdateTimeout = null;
}

function updateMatchesCountSoon()
{
if (matchesCountUpdateTimeout)
return;

matchesCountUpdateTimeout = setTimeout(updateMatchesCount.bind(this), 500);
}

function finishedCallback(view, searchMatches)
{
if (!searchMatches)
return;

this._totalSearchMatches += searchMatches;
this._searchResults.push(view);

if (this.searchMatchFound)
this.searchMatchFound(view, searchMatches);

updateMatchesCountSoon.call(this);

if (view === visibleView)
view.jumpToFirstSearchResult();
}

var i = 0;
var panel = this;
var boundFinishedCallback = finishedCallback.bind(this);
var chunkIntervalIdentifier = null;




function processChunk()
{
var view = searchableViews[i];

if (++i >= searchableViews.length) {
if (panel._currentSearchChunkIntervalIdentifier === chunkIntervalIdentifier)
delete panel._currentSearchChunkIntervalIdentifier;
clearInterval(chunkIntervalIdentifier);
}

if (!view)
return;

view.currentQuery = query;
view.performSearch(query, boundFinishedCallback);
}

processChunk();

chunkIntervalIdentifier = setInterval(processChunk, 25);
this._currentSearchChunkIntervalIdentifier = chunkIntervalIdentifier;
},

jumpToNextSearchResult: function()
{
if (!this.showView || !this._searchResults || !this._searchResults.length)
return;

var showFirstResult = false;

this._currentSearchResultIndex = this._searchResults.indexOf(this.visibleView);
if (this._currentSearchResultIndex === -1) {
this._currentSearchResultIndex = 0;
showFirstResult = true;
}

var currentView = this._searchResults[this._currentSearchResultIndex];

if (currentView.showingLastSearchResult()) {
if (++this._currentSearchResultIndex >= this._searchResults.length)
this._currentSearchResultIndex = 0;
currentView = this._searchResults[this._currentSearchResultIndex];
showFirstResult = true;
}

WebInspector.searchController.updateCurrentMatchIndex(this._currentSearchResultIndex, this);

if (currentView !== this.visibleView) {
this.showView(currentView);
WebInspector.searchController.showSearchField();
}

if (showFirstResult)
currentView.jumpToFirstSearchResult();
else
currentView.jumpToNextSearchResult();
},

jumpToPreviousSearchResult: function()
{
if (!this.showView || !this._searchResults || !this._searchResults.length)
return;

var showLastResult = false;

this._currentSearchResultIndex = this._searchResults.indexOf(this.visibleView);
if (this._currentSearchResultIndex === -1) {
this._currentSearchResultIndex = 0;
showLastResult = true;
}

var currentView = this._searchResults[this._currentSearchResultIndex];

if (currentView.showingFirstSearchResult()) {
if (--this._currentSearchResultIndex < 0)
this._currentSearchResultIndex = (this._searchResults.length - 1);
currentView = this._searchResults[this._currentSearchResultIndex];
showLastResult = true;
}

WebInspector.searchController.updateCurrentMatchIndex(this._currentSearchResultIndex, this);

if (currentView !== this.visibleView) {
this.showView(currentView);
WebInspector.searchController.showSearchField();
}

if (showLastResult)
currentView.jumpToLastSearchResult();
else
currentView.jumpToPreviousSearchResult();
},

_searchableViews: function()
{
var views = [];

const visibleView = this.visibleView;
if (visibleView && visibleView.performSearch)
views.push(visibleView);

var profilesLength = this._profiles.length;
for (var i = 0; i < profilesLength; ++i) {
var profile = this._profiles[i];
var view = profile.view();
if (!view.performSearch || view === visibleView)
continue;
views.push(view);
}

return views;
},

searchMatchFound: function(view, matches)
{
view.profile._profilesTreeElement.searchMatches = matches;
},

searchCanceled: function()
{
if (this._searchResults) {
for (var i = 0; i < this._searchResults.length; ++i) {
var view = this._searchResults[i];
if (view.searchCanceled)
view.searchCanceled();
delete view.currentQuery;
}
}

WebInspector.Panel.prototype.searchCanceled.call(this);

if (this._currentSearchChunkIntervalIdentifier) {
clearInterval(this._currentSearchChunkIntervalIdentifier);
delete this._currentSearchChunkIntervalIdentifier;
}

this._totalSearchMatches = 0;
this._currentSearchResultIndex = 0;
this._searchResults = [];

if (!this._profiles)
return;

for (var i = 0; i < this._profiles.length; ++i) {
var profile = this._profiles[i];
profile._profilesTreeElement.searchMatches = 0;
}
},

_updateInterface: function()
{

if (this._profilerEnabled) {
this.enableToggleButton.title = WebInspector.UIString("Profiling enabled. Click to disable.");
this.enableToggleButton.toggled = true;
this.recordButton.visible = true;
this.profileViewStatusBarItemsContainer.removeStyleClass("hidden");
this.clearResultsButton.element.removeStyleClass("hidden");
this.panelEnablerView.detach();
} else {
this.enableToggleButton.title = WebInspector.UIString("Profiling disabled. Click to enable.");
this.enableToggleButton.toggled = false;
this.recordButton.visible = false;
this.profileViewStatusBarItemsContainer.addStyleClass("hidden");
this.clearResultsButton.element.addStyleClass("hidden");
this.panelEnablerView.show(this.element);
}
},

get profilerEnabled()
{
return this._profilerEnabled;
},

enableProfiler: function()
{
if (this._profilerEnabled)
return;
this._toggleProfiling(this.panelEnablerView.alwaysEnabled);
},

disableProfiler: function()
{
if (!this._profilerEnabled)
return;
this._toggleProfiling(this.panelEnablerView.alwaysEnabled);
},

_toggleProfiling: function(optionalAlways)
{
if (this._profilerEnabled) {
WebInspector.settings.profilerEnabled.set(false);
ProfilerAgent.disable(this._profilerWasDisabled.bind(this));
} else {
WebInspector.settings.profilerEnabled.set(!!optionalAlways);
ProfilerAgent.enable(this._profilerWasEnabled.bind(this));
}
},

_populateProfiles: function()
{
if (!this._profilerEnabled || this._profilesWereRequested)
return;


function populateCallback(error, profileHeaders) {
if (error)
return;
profileHeaders.sort(function(a, b) { return a.uid - b.uid; });
var profileHeadersLength = profileHeaders.length;
for (var i = 0; i < profileHeadersLength; ++i) {
var profileHeader = profileHeaders[i];
var profileType = this.getProfileType(profileHeader.typeId);
this.addProfileHeader(profileType.createProfile(profileHeader));
}
}

ProfilerAgent.getProfileHeaders(populateCallback.bind(this));

this._profilesWereRequested = true;
},

sidebarResized: function(event)
{
this.onResize();
},

onResize: function()
{
var minFloatingStatusBarItemsOffset = document.getElementById("panel-status-bar").totalOffsetLeft() + this._statusBarButtons.length * WebInspector.StatusBarButton.width;
this.profileViewStatusBarItemsContainer.style.left = Math.max(minFloatingStatusBarItemsOffset, this.splitView.sidebarWidth()) + "px";
},


setRecordingProfile: function(profileType, isProfiling)
{
var profileTypeObject = this.getProfileType(profileType);
profileTypeObject.setRecordingProfile(isProfiling);
var temporaryProfile = this.findTemporaryProfile(profileType);
if (!!temporaryProfile === isProfiling)
return;
if (!temporaryProfile)
temporaryProfile = profileTypeObject.createTemporaryProfile();
if (isProfiling)
this.addProfileHeader(temporaryProfile);
else
this._removeTemporaryProfile(profileType);
this.recordButton.toggled = isProfiling;
this.recordButton.title = profileTypeObject.buttonTooltip;
if (isProfiling)
this._launcherView.profileStarted();
else
this._launcherView.profileFinished();
},

takeHeapSnapshot: function()
{
var temporaryRecordingProfile = this.findTemporaryProfile(WebInspector.HeapSnapshotProfileType.TypeId);
if (!temporaryRecordingProfile) {
var profileTypeObject = this.getProfileType(WebInspector.HeapSnapshotProfileType.TypeId);
this.addProfileHeader(profileTypeObject.createTemporaryProfile());
}
this._launcherView.profileStarted();
function done() {
this._launcherView.profileFinished();
}
ProfilerAgent.takeHeapSnapshot(done.bind(this));
WebInspector.userMetrics.ProfilesHeapProfileTaken.record();
},


_reportHeapSnapshotProgress: function(done, total)
{
var temporaryProfile = this.findTemporaryProfile(WebInspector.HeapSnapshotProfileType.TypeId);
if (temporaryProfile) {
temporaryProfile.sidebarElement.subtitle = WebInspector.UIString("%.2f%", (done / total) * 100);
temporaryProfile.sidebarElement.wait = true;
if (done >= total)
this._removeTemporaryProfile(WebInspector.HeapSnapshotProfileType.TypeId);
}
},


appendApplicableItems: function(event, contextMenu, target)
{
if (WebInspector.inspectorView.currentPanel() !== this)
return;

var object =   (target);
var objectId = object.objectId;
if (!objectId)
return;

var heapProfiles = this.getProfiles(WebInspector.HeapSnapshotProfileType.TypeId);
if (!heapProfiles.length)
return;

function revealInView(viewName)
{
ProfilerAgent.getHeapObjectId(objectId, didReceiveHeapObjectId.bind(this, viewName));
}

function didReceiveHeapObjectId(viewName, error, result)
{
if (WebInspector.inspectorView.currentPanel() !== this)
return;
if (!error)
this.showObject(result, viewName);
}

contextMenu.appendItem(WebInspector.UIString("Reveal in Dominators View"), revealInView.bind(this, "Dominators"));
contextMenu.appendItem(WebInspector.UIString("Reveal in Summary View"), revealInView.bind(this, "Summary"));
},

__proto__: WebInspector.Panel.prototype
}


WebInspector.ProfilerDispatcher = function(profiler)
{
this._profiler = profiler;
}

WebInspector.ProfilerDispatcher.prototype = {

addProfileHeader: function(profile)
{
var profileType = this._profiler.getProfileType(profile.typeId);
this._profiler.addProfileHeader(profileType.createProfile(profile));
},


addHeapSnapshotChunk: function(uid, chunk)
{
this._profiler._addHeapSnapshotChunk(uid, chunk);
},


finishHeapSnapshot: function(uid)
{
this._profiler._finishHeapSnapshot(uid);
},


setRecordingProfile: function(isProfiling)
{
this._profiler.setRecordingProfile(WebInspector.CPUProfileType.TypeId, isProfiling);
},


resetProfiles: function()
{
this._profiler._reset();
},


reportHeapSnapshotProgress: function(done, total)
{
this._profiler._reportHeapSnapshotProgress(done, total);
}
}


WebInspector.ProfileSidebarTreeElement = function(profile, titleFormat, className)
{
this.profile = profile;
this._titleFormat = titleFormat;

if (this.profile.title.startsWith(UserInitiatedProfileName))
this._profileNumber = this.profile.title.substring(UserInitiatedProfileName.length + 1);

WebInspector.SidebarTreeElement.call(this, className, "", "", profile, false);

this.refreshTitles();
}

WebInspector.ProfileSidebarTreeElement.prototype = {
onselect: function()
{
if (!this._suppressOnSelect)
this.treeOutline.panel.showProfile(this.profile);
},

ondelete: function()
{
this.treeOutline.panel._removeProfileHeader(this.profile);
return true;
},

get mainTitle()
{
if (this._mainTitle)
return this._mainTitle;
if (this.profile.title.startsWith(UserInitiatedProfileName))
return WebInspector.UIString(this._titleFormat, this._profileNumber);
return this.profile.title;
},

set mainTitle(x)
{
this._mainTitle = x;
this.refreshTitles();
},

set searchMatches(matches)
{
if (!matches) {
if (!this.bubbleElement)
return;
this.bubbleElement.removeStyleClass("search-matches");
this.bubbleText = "";
return;
}

this.bubbleText = matches;
this.bubbleElement.addStyleClass("search-matches");
},

handleContextMenuEvent: function(event)
{
var profile = this.profile;
var contextMenu = new WebInspector.ContextMenu(event);
var profilesPanel = WebInspector.ProfilesPanel._instance;

if (profile.canSaveToFile()) {
contextMenu.appendItem(WebInspector.UIString("Save Heap Snapshot\u2026"), profile.saveToFile.bind(profile));
contextMenu.appendItem(WebInspector.UIString("Load Heap Snapshot\u2026"), profilesPanel._fileSelectorElement.click.bind(profilesPanel._fileSelectorElement));
contextMenu.appendItem(WebInspector.UIString("Delete Heap Snapshot"), this.ondelete.bind(this));
} else {
contextMenu.appendItem(WebInspector.UIString("Load Heap Snapshot\u2026"), profilesPanel._fileSelectorElement.click.bind(profilesPanel._fileSelectorElement));
contextMenu.appendItem(WebInspector.UIString("Delete profile"), this.ondelete.bind(this));
}
contextMenu.show();
},

__proto__: WebInspector.SidebarTreeElement.prototype
}


WebInspector.ProfileGroupSidebarTreeElement = function(title, subtitle)
{
WebInspector.SidebarTreeElement.call(this, "profile-group-sidebar-tree-item", title, subtitle, null, true);
}

WebInspector.ProfileGroupSidebarTreeElement.prototype = {
onselect: function()
{
if (this.children.length > 0)
WebInspector.ProfilesPanel._instance.showProfile(this.children[this.children.length - 1].profile);
},

__proto__: WebInspector.SidebarTreeElement.prototype
}


WebInspector.ProfilesSidebarTreeElement = function(panel)
{
this._panel = panel;
this.small = false;

WebInspector.SidebarTreeElement.call(this, "profile-launcher-view-tree-item", WebInspector.UIString("Profiles"), "", null, false);
}

WebInspector.ProfilesSidebarTreeElement.prototype = {
onselect: function()
{
this._panel._showLauncherView();
},

get selectable()
{
return true;
},

__proto__: WebInspector.SidebarTreeElement.prototype
}




WebInspector.ProfileDataGridNode = function(profileView, profileNode, owningTree, hasChildren)
{
this.profileView = profileView;
this.profileNode = profileNode;

WebInspector.DataGridNode.call(this, null, hasChildren);

this.addEventListener("populate", this._populate, this);

this.tree = owningTree;

this.childrenByCallUID = {};
this.lastComparator = null;

this.callUID = profileNode.callUID;
this.selfTime = profileNode.selfTime;
this.totalTime = profileNode.totalTime;
this.functionName = profileNode.functionName;
this.numberOfCalls = profileNode.numberOfCalls;
this.url = profileNode.url;
}

WebInspector.ProfileDataGridNode.prototype = {
get data()
{
function formatMilliseconds(time)
{
return Number.secondsToString(time / 1000, !Capabilities.samplingCPUProfiler);
}

var data = {};

data["function"] = this.functionName;
data["calls"] = this.numberOfCalls;

if (this.profileView.showSelfTimeAsPercent.get())
data["self"] = WebInspector.UIString("%.2f%", this.selfPercent);
else
data["self"] = formatMilliseconds(this.selfTime);

if (this.profileView.showTotalTimeAsPercent.get())
data["total"] = WebInspector.UIString("%.2f%", this.totalPercent);
else
data["total"] = formatMilliseconds(this.totalTime);

if (this.profileView.showAverageTimeAsPercent.get())
data["average"] = WebInspector.UIString("%.2f%", this.averagePercent);
else
data["average"] = formatMilliseconds(this.averageTime);

return data;
},

createCell: function(columnIdentifier)
{
var cell = WebInspector.DataGridNode.prototype.createCell.call(this, columnIdentifier);

if (columnIdentifier === "self" && this._searchMatchedSelfColumn)
cell.addStyleClass("highlight");
else if (columnIdentifier === "total" && this._searchMatchedTotalColumn)
cell.addStyleClass("highlight");
else if (columnIdentifier === "average" && this._searchMatchedAverageColumn)
cell.addStyleClass("highlight");
else if (columnIdentifier === "calls" && this._searchMatchedCallsColumn)
cell.addStyleClass("highlight");

if (columnIdentifier !== "function")
return cell;

if (this.profileNode._searchMatchedFunctionColumn)
cell.addStyleClass("highlight");

if (this.profileNode.url) {

var lineNumber = this.profileNode.lineNumber ? this.profileNode.lineNumber - 1 : 0;
var urlElement = this.profileView._linkifier.linkifyLocation(this.profileNode.url, lineNumber, 0, "profile-node-file");
urlElement.style.maxWidth = "75%";
cell.insertBefore(urlElement, cell.firstChild);
}

return cell;
},

select: function(supressSelectedEvent)
{
WebInspector.DataGridNode.prototype.select.call(this, supressSelectedEvent);
this.profileView._dataGridNodeSelected(this);
},

deselect: function(supressDeselectedEvent)
{
WebInspector.DataGridNode.prototype.deselect.call(this, supressDeselectedEvent);
this.profileView._dataGridNodeDeselected(this);
},

sort: function(  comparator,   force)
{
var gridNodeGroups = [[this]];

for (var gridNodeGroupIndex = 0; gridNodeGroupIndex < gridNodeGroups.length; ++gridNodeGroupIndex) {
var gridNodes = gridNodeGroups[gridNodeGroupIndex];
var count = gridNodes.length;

for (var index = 0; index < count; ++index) {
var gridNode = gridNodes[index];



if (!force && (!gridNode.expanded || gridNode.lastComparator === comparator)) {
if (gridNode.children.length)
gridNode.shouldRefreshChildren = true;
continue;
}

gridNode.lastComparator = comparator;

var children = gridNode.children;
var childCount = children.length;

if (childCount) {
children.sort(comparator);

for (var childIndex = 0; childIndex < childCount; ++childIndex)
children[childIndex]._recalculateSiblings(childIndex);

gridNodeGroups.push(children);
}
}
}
},

insertChild: function(  profileDataGridNode, index)
{
WebInspector.DataGridNode.prototype.insertChild.call(this, profileDataGridNode, index);

this.childrenByCallUID[profileDataGridNode.callUID] = profileDataGridNode;
},

removeChild: function(  profileDataGridNode)
{
WebInspector.DataGridNode.prototype.removeChild.call(this, profileDataGridNode);

delete this.childrenByCallUID[profileDataGridNode.callUID];
},

removeChildren: function(  profileDataGridNode)
{
WebInspector.DataGridNode.prototype.removeChildren.call(this);

this.childrenByCallUID = {};
},

findChild: function(  node)
{
if (!node)
return null;
return this.childrenByCallUID[node.callUID];
},

get averageTime()
{
return this.selfTime / Math.max(1, this.numberOfCalls);
},

get averagePercent()
{
return this.averageTime / this.tree.totalTime * 100.0;
},

get selfPercent()
{
return this.selfTime / this.tree.totalTime * 100.0;
},

get totalPercent()
{
return this.totalTime / this.tree.totalTime * 100.0;
},

get _parent()
{
return this.parent !== this.dataGrid ? this.parent : this.tree;
},

_populate: function()
{
this._sharedPopulate();

if (this._parent) {
var currentComparator = this._parent.lastComparator;

if (currentComparator)
this.sort(currentComparator, true);
}

if (this.removeEventListener)
this.removeEventListener("populate", this._populate, this);
},



_save: function()
{
if (this._savedChildren)
return;

this._savedSelfTime = this.selfTime;
this._savedTotalTime = this.totalTime;
this._savedNumberOfCalls = this.numberOfCalls;

this._savedChildren = this.children.slice();
},



_restore: function()
{
if (!this._savedChildren)
return;

this.selfTime = this._savedSelfTime;
this.totalTime = this._savedTotalTime;
this.numberOfCalls = this._savedNumberOfCalls;

this.removeChildren();

var children = this._savedChildren;
var count = children.length;

for (var index = 0; index < count; ++index) {
children[index]._restore();
this.appendChild(children[index]);
}
},

_merge: function(child, shouldAbsorb)
{
this.selfTime += child.selfTime;

if (!shouldAbsorb) {
this.totalTime += child.totalTime;
this.numberOfCalls += child.numberOfCalls;
}

var children = this.children.slice();

this.removeChildren();

var count = children.length;

for (var index = 0; index < count; ++index) {
if (!shouldAbsorb || children[index] !== child)
this.appendChild(children[index]);
}

children = child.children.slice();
count = children.length;

for (var index = 0; index < count; ++index) {
var orphanedChild = children[index],
existingChild = this.childrenByCallUID[orphanedChild.callUID];

if (existingChild)
existingChild._merge(orphanedChild, false);
else
this.appendChild(orphanedChild);
}
},

__proto__: WebInspector.DataGridNode.prototype
}


WebInspector.ProfileDataGridTree = function(profileView, profileNode)
{
this.tree = this;
this.children = [];

this.profileView = profileView;

this.totalTime = profileNode.totalTime;
this.lastComparator = null;

this.childrenByCallUID = {};
}

WebInspector.ProfileDataGridTree.prototype = {
get expanded()
{
return true;
},

appendChild: function(child)
{
this.insertChild(child, this.children.length);
},

insertChild: function(child, index)
{
this.children.splice(index, 0, child);
this.childrenByCallUID[child.callUID] = child;
},

removeChildren: function()
{
this.children = [];
this.childrenByCallUID = {};
},

findChild: WebInspector.ProfileDataGridNode.prototype.findChild,
sort: WebInspector.ProfileDataGridNode.prototype.sort,

_save: function()
{
if (this._savedChildren)
return;

this._savedTotalTime = this.totalTime;
this._savedChildren = this.children.slice();
},

restore: function()
{
if (!this._savedChildren)
return;

this.children = this._savedChildren;
this.totalTime = this._savedTotalTime;

var children = this.children;
var count = children.length;

for (var index = 0; index < count; ++index)
children[index]._restore();

this._savedChildren = null;
}
}

WebInspector.ProfileDataGridTree.propertyComparators = [{}, {}];

WebInspector.ProfileDataGridTree.propertyComparator = function(  property,   isAscending)
{
var comparator = WebInspector.ProfileDataGridTree.propertyComparators[(isAscending ? 1 : 0)][property];

if (!comparator) {
if (isAscending) {
comparator = function(lhs, rhs)
{
if (lhs[property] < rhs[property])
return -1;

if (lhs[property] > rhs[property])
return 1;

return 0;
}
} else {
comparator = function(lhs, rhs)
{
if (lhs[property] > rhs[property])
return -1;

if (lhs[property] < rhs[property])
return 1;

return 0;
}
}

WebInspector.ProfileDataGridTree.propertyComparators[(isAscending ? 1 : 0)][property] = comparator;
}

return comparator;
}
;









WebInspector.BottomUpProfileDataGridNode = function(  profileView,   profileNode,   owningTree)
{
WebInspector.ProfileDataGridNode.call(this, profileView, profileNode, owningTree, this._willHaveChildren(profileNode));

this._remainingNodeInfos = [];
}

WebInspector.BottomUpProfileDataGridNode.prototype = {
_takePropertiesFromProfileDataGridNode: function(  profileDataGridNode)
{
this._save();

this.selfTime = profileDataGridNode.selfTime;
this.totalTime = profileDataGridNode.totalTime;
this.numberOfCalls = profileDataGridNode.numberOfCalls;
},


_keepOnlyChild: function(  child)
{
this._save();

this.removeChildren();
this.appendChild(child);
},

_exclude: function(aCallUID)
{
if (this._remainingNodeInfos)
this._populate();

this._save();

var children = this.children;
var index = this.children.length;

while (index--)
children[index]._exclude(aCallUID);

var child = this.childrenByCallUID[aCallUID];

if (child)
this._merge(child, true);
},

_restore: function()
{
WebInspector.ProfileDataGridNode.prototype._restore();

if (!this.children.length)
this.hasChildren = this._willHaveChildren(this.profileNode);
},

_merge: function(  child,   shouldAbsorb)
{
this.selfTime -= child.selfTime;

WebInspector.ProfileDataGridNode.prototype._merge.call(this, child, shouldAbsorb);
},

_sharedPopulate: function()
{
var remainingNodeInfos = this._remainingNodeInfos;
var count = remainingNodeInfos.length;

for (var index = 0; index < count; ++index) {
var nodeInfo = remainingNodeInfos[index];
var ancestor = nodeInfo.ancestor;
var focusNode = nodeInfo.focusNode;
var child = this.findChild(ancestor);


if (child) {
var totalTimeAccountedFor = nodeInfo.totalTimeAccountedFor;

child.selfTime += focusNode.selfTime;
child.numberOfCalls += focusNode.numberOfCalls;

if (!totalTimeAccountedFor)
child.totalTime += focusNode.totalTime;
} else {


child = new WebInspector.BottomUpProfileDataGridNode(this.profileView, ancestor, this.tree);

if (ancestor !== focusNode) {

child.selfTime = focusNode.selfTime;
child.totalTime = focusNode.totalTime;
child.numberOfCalls = focusNode.numberOfCalls;
}

this.appendChild(child);
}

var parent = ancestor.parent;
if (parent && parent.parent) {
nodeInfo.ancestor = parent;
child._remainingNodeInfos.push(nodeInfo);
}
}

delete this._remainingNodeInfos;
},

_willHaveChildren: function(profileNode)
{


return !!(profileNode.parent && profileNode.parent.parent);
},

__proto__: WebInspector.ProfileDataGridNode.prototype
}


WebInspector.BottomUpProfileDataGridTree = function(  aProfileView,   aProfileNode)
{
WebInspector.ProfileDataGridTree.call(this, aProfileView, aProfileNode);


var profileNodeUIDs = 0;
var profileNodeGroups = [[], [aProfileNode]];
var visitedProfileNodesForCallUID = {};

this._remainingNodeInfos = [];

for (var profileNodeGroupIndex = 0; profileNodeGroupIndex < profileNodeGroups.length; ++profileNodeGroupIndex) {
var parentProfileNodes = profileNodeGroups[profileNodeGroupIndex];
var profileNodes = profileNodeGroups[++profileNodeGroupIndex];
var count = profileNodes.length;

for (var index = 0; index < count; ++index) {
var profileNode = profileNodes[index];

if (!profileNode.UID)
profileNode.UID = ++profileNodeUIDs;

if (profileNode.head && profileNode !== profileNode.head) {

var visitedNodes = visitedProfileNodesForCallUID[profileNode.callUID];
var totalTimeAccountedFor = false;

if (!visitedNodes) {
visitedNodes = {}
visitedProfileNodesForCallUID[profileNode.callUID] = visitedNodes;
} else {


var parentCount = parentProfileNodes.length;
for (var parentIndex = 0; parentIndex < parentCount; ++parentIndex) {
if (visitedNodes[parentProfileNodes[parentIndex].UID]) {
totalTimeAccountedFor = true;
break;
}
}
}

visitedNodes[profileNode.UID] = true;

this._remainingNodeInfos.push({ ancestor:profileNode, focusNode:profileNode, totalTimeAccountedFor:totalTimeAccountedFor });
}

var children = profileNode.children;
if (children.length) {
profileNodeGroups.push(parentProfileNodes.concat([profileNode]))
profileNodeGroups.push(children);
}
}
}


var any =  this;
var node =  any;
WebInspector.BottomUpProfileDataGridNode.prototype._populate.call(node);

return this;
}

WebInspector.BottomUpProfileDataGridTree.prototype = {

focus: function(  profileDataGridNode)
{
if (!profileDataGridNode)
return;

this._save();

var currentNode = profileDataGridNode;
var focusNode = profileDataGridNode;

while (currentNode.parent && (currentNode instanceof WebInspector.ProfileDataGridNode)) {
currentNode._takePropertiesFromProfileDataGridNode(profileDataGridNode);

focusNode = currentNode;
currentNode = currentNode.parent;

if (currentNode instanceof WebInspector.ProfileDataGridNode)
currentNode._keepOnlyChild(focusNode);
}

this.children = [focusNode];
this.totalTime = profileDataGridNode.totalTime;
},

exclude: function(  profileDataGridNode)
{
if (!profileDataGridNode)
return;

this._save();

var excludedCallUID = profileDataGridNode.callUID;
var excludedTopLevelChild = this.childrenByCallUID[excludedCallUID];



if (excludedTopLevelChild)
this.children.remove(excludedTopLevelChild);

var children = this.children;
var count = children.length;

for (var index = 0; index < count; ++index)
children[index]._exclude(excludedCallUID);

if (this.lastComparator)
this.sort(this.lastComparator, true);
},

_sharedPopulate: WebInspector.BottomUpProfileDataGridNode.prototype._sharedPopulate,

__proto__: WebInspector.ProfileDataGridTree.prototype
}
;



WebInspector.CPUProfileView = function(profile)
{
WebInspector.View.call(this);

this.element.addStyleClass("profile-view");

this.showSelfTimeAsPercent = WebInspector.settings.createSetting("cpuProfilerShowSelfTimeAsPercent", true);
this.showTotalTimeAsPercent = WebInspector.settings.createSetting("cpuProfilerShowTotalTimeAsPercent", true);
this.showAverageTimeAsPercent = WebInspector.settings.createSetting("cpuProfilerShowAverageTimeAsPercent", true);
this._viewType = WebInspector.settings.createSetting("cpuProfilerView", WebInspector.CPUProfileView._TypeHeavy);

var columns = { "self": { title: WebInspector.UIString("Self"), width: "72px", sort: "descending", sortable: true },
"total": { title: WebInspector.UIString("Total"), width: "72px", sortable: true },
"average": { title: WebInspector.UIString("Average"), width: "72px", sortable: true },
"calls": { title: WebInspector.UIString("Calls"), width: "54px", sortable: true },
"function": { title: WebInspector.UIString("Function"), disclosure: true, sortable: true } };

if (Capabilities.samplingCPUProfiler) {
delete columns.average;
delete columns.calls;
}

this.dataGrid = new WebInspector.DataGrid(columns);
this.dataGrid.addEventListener("sorting changed", this._sortProfile, this);
this.dataGrid.element.addEventListener("mousedown", this._mouseDownInDataGrid.bind(this), true);
this.dataGrid.show(this.element);

this.viewSelectComboBox = new WebInspector.StatusBarComboBox(this._changeView.bind(this));

var heavyViewOption = document.createElement("option");
heavyViewOption.label = WebInspector.UIString("Heavy (Bottom Up)");
heavyViewOption.value = WebInspector.CPUProfileView._TypeHeavy;
var treeViewOption = document.createElement("option");
treeViewOption.label = WebInspector.UIString("Tree (Top Down)");
treeViewOption.value = WebInspector.CPUProfileView._TypeTree;

this.viewSelectComboBox.addOption(heavyViewOption);
this.viewSelectComboBox.addOption(treeViewOption);
this.viewSelectComboBox.select(this._viewType.get() === WebInspector.CPUProfileView._TypeHeavy ? heavyViewOption : treeViewOption);

this.percentButton = new WebInspector.StatusBarButton("", "percent-time-status-bar-item");
this.percentButton.addEventListener("click", this._percentClicked, this);

this.focusButton = new WebInspector.StatusBarButton(WebInspector.UIString("Focus selected function."), "focus-profile-node-status-bar-item");
this.focusButton.setEnabled(false);
this.focusButton.addEventListener("click", this._focusClicked, this);

this.excludeButton = new WebInspector.StatusBarButton(WebInspector.UIString("Exclude selected function."), "exclude-profile-node-status-bar-item");
this.excludeButton.setEnabled(false);
this.excludeButton.addEventListener("click", this._excludeClicked, this);

this.resetButton = new WebInspector.StatusBarButton(WebInspector.UIString("Restore all functions."), "reset-profile-status-bar-item");
this.resetButton.visible = false;
this.resetButton.addEventListener("click", this._resetClicked, this);

this.profile = profile;

function profileCallback(error, profile)
{
if (error)
return;

if (!profile.head) {

return;
}
this.profile.head = profile.head;
this._assignParentsInProfile();
this._changeView();
this._updatePercentButton();
}

this._linkifier = new WebInspector.Linkifier(new WebInspector.Linkifier.DefaultFormatter(30));

ProfilerAgent.getProfile(this.profile.profileType().id, this.profile.uid, profileCallback.bind(this));
}

WebInspector.CPUProfileView._TypeTree = "Tree";
WebInspector.CPUProfileView._TypeHeavy = "Heavy";

WebInspector.CPUProfileView.prototype = {
get statusBarItems()
{
return [this.viewSelectComboBox.element, this.percentButton.element, this.focusButton.element, this.excludeButton.element, this.resetButton.element];
},

get bottomUpProfileDataGridTree()
{
if (!this._bottomUpProfileDataGridTree) {
if (this.profile.bottomUpHead)
this._bottomUpProfileDataGridTree = new WebInspector.TopDownProfileDataGridTree(this, this.profile.bottomUpHead);
else
this._bottomUpProfileDataGridTree = new WebInspector.BottomUpProfileDataGridTree(this, this.profile.head);
}
return this._bottomUpProfileDataGridTree;
},

get topDownProfileDataGridTree()
{
if (!this._topDownProfileDataGridTree)
this._topDownProfileDataGridTree = new WebInspector.TopDownProfileDataGridTree(this, this.profile.head);
return this._topDownProfileDataGridTree;
},

get currentTree()
{
return this._currentTree;
},

set currentTree(tree)
{
this._currentTree = tree;
this.refresh();
},

willHide: function()
{
this._currentSearchResultIndex = -1;
},

refresh: function()
{
var selectedProfileNode = this.dataGrid.selectedNode ? this.dataGrid.selectedNode.profileNode : null;

this.dataGrid.rootNode().removeChildren();

var children = this.profileDataGridTree.children;
var count = children.length;

for (var index = 0; index < count; ++index)
this.dataGrid.rootNode().appendChild(children[index]);

if (selectedProfileNode)
selectedProfileNode.selected = true;
},

refreshVisibleData: function()
{
var child = this.dataGrid.rootNode().children[0];
while (child) {
child.refresh();
child = child.traverseNextNode(false, null, true);
}
},

refreshShowAsPercents: function()
{
this._updatePercentButton();
this.refreshVisibleData();
},

searchCanceled: function()
{
if (this._searchResults) {
for (var i = 0; i < this._searchResults.length; ++i) {
var profileNode = this._searchResults[i].profileNode;

delete profileNode._searchMatchedSelfColumn;
delete profileNode._searchMatchedTotalColumn;
delete profileNode._searchMatchedCallsColumn;
delete profileNode._searchMatchedFunctionColumn;

profileNode.refresh();
}
}

delete this._searchFinishedCallback;
this._currentSearchResultIndex = -1;
this._searchResults = [];
},

performSearch: function(query, finishedCallback)
{

this.searchCanceled();

query = query.trim();

if (!query.length)
return;

this._searchFinishedCallback = finishedCallback;

var greaterThan = (query.startsWith(">"));
var lessThan = (query.startsWith("<"));
var equalTo = (query.startsWith("=") || ((greaterThan || lessThan) && query.indexOf("=") === 1));
var percentUnits = (query.lastIndexOf("%") === (query.length - 1));
var millisecondsUnits = (query.length > 2 && query.lastIndexOf("ms") === (query.length - 2));
var secondsUnits = (!millisecondsUnits && query.lastIndexOf("s") === (query.length - 1));

var queryNumber = parseFloat(query);
if (greaterThan || lessThan || equalTo) {
if (equalTo && (greaterThan || lessThan))
queryNumber = parseFloat(query.substring(2));
else
queryNumber = parseFloat(query.substring(1));
}

var queryNumberMilliseconds = (secondsUnits ? (queryNumber * 1000) : queryNumber);


if (!isNaN(queryNumber) && !(greaterThan || lessThan))
equalTo = true;

function matchesQuery(  profileDataGridNode)
{
delete profileDataGridNode._searchMatchedSelfColumn;
delete profileDataGridNode._searchMatchedTotalColumn;
delete profileDataGridNode._searchMatchedAverageColumn;
delete profileDataGridNode._searchMatchedCallsColumn;
delete profileDataGridNode._searchMatchedFunctionColumn;

if (percentUnits) {
if (lessThan) {
if (profileDataGridNode.selfPercent < queryNumber)
profileDataGridNode._searchMatchedSelfColumn = true;
if (profileDataGridNode.totalPercent < queryNumber)
profileDataGridNode._searchMatchedTotalColumn = true;
if (profileDataGridNode.averagePercent < queryNumberMilliseconds)
profileDataGridNode._searchMatchedAverageColumn = true;
} else if (greaterThan) {
if (profileDataGridNode.selfPercent > queryNumber)
profileDataGridNode._searchMatchedSelfColumn = true;
if (profileDataGridNode.totalPercent > queryNumber)
profileDataGridNode._searchMatchedTotalColumn = true;
if (profileDataGridNode.averagePercent < queryNumberMilliseconds)
profileDataGridNode._searchMatchedAverageColumn = true;
}

if (equalTo) {
if (profileDataGridNode.selfPercent == queryNumber)
profileDataGridNode._searchMatchedSelfColumn = true;
if (profileDataGridNode.totalPercent == queryNumber)
profileDataGridNode._searchMatchedTotalColumn = true;
if (profileDataGridNode.averagePercent < queryNumberMilliseconds)
profileDataGridNode._searchMatchedAverageColumn = true;
}
} else if (millisecondsUnits || secondsUnits) {
if (lessThan) {
if (profileDataGridNode.selfTime < queryNumberMilliseconds)
profileDataGridNode._searchMatchedSelfColumn = true;
if (profileDataGridNode.totalTime < queryNumberMilliseconds)
profileDataGridNode._searchMatchedTotalColumn = true;
if (profileDataGridNode.averageTime < queryNumberMilliseconds)
profileDataGridNode._searchMatchedAverageColumn = true;
} else if (greaterThan) {
if (profileDataGridNode.selfTime > queryNumberMilliseconds)
profileDataGridNode._searchMatchedSelfColumn = true;
if (profileDataGridNode.totalTime > queryNumberMilliseconds)
profileDataGridNode._searchMatchedTotalColumn = true;
if (profileDataGridNode.averageTime > queryNumberMilliseconds)
profileDataGridNode._searchMatchedAverageColumn = true;
}

if (equalTo) {
if (profileDataGridNode.selfTime == queryNumberMilliseconds)
profileDataGridNode._searchMatchedSelfColumn = true;
if (profileDataGridNode.totalTime == queryNumberMilliseconds)
profileDataGridNode._searchMatchedTotalColumn = true;
if (profileDataGridNode.averageTime == queryNumberMilliseconds)
profileDataGridNode._searchMatchedAverageColumn = true;
}
} else {
if (equalTo && profileDataGridNode.numberOfCalls == queryNumber)
profileDataGridNode._searchMatchedCallsColumn = true;
if (greaterThan && profileDataGridNode.numberOfCalls > queryNumber)
profileDataGridNode._searchMatchedCallsColumn = true;
if (lessThan && profileDataGridNode.numberOfCalls < queryNumber)
profileDataGridNode._searchMatchedCallsColumn = true;
}

if (profileDataGridNode.functionName.hasSubstring(query, true) || profileDataGridNode.url.hasSubstring(query, true))
profileDataGridNode._searchMatchedFunctionColumn = true;

if (profileDataGridNode._searchMatchedSelfColumn ||
profileDataGridNode._searchMatchedTotalColumn ||
profileDataGridNode._searchMatchedAverageColumn ||
profileDataGridNode._searchMatchedCallsColumn ||
profileDataGridNode._searchMatchedFunctionColumn)
{
profileDataGridNode.refresh();
return true;
}

return false;
}

var current = this.profileDataGridTree.children[0];

while (current) {
if (matchesQuery(current)) {
this._searchResults.push({ profileNode: current });
}

current = current.traverseNextNode(false, null, false);
}

finishedCallback(this, this._searchResults.length);
},

jumpToFirstSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
this._currentSearchResultIndex = 0;
this._jumpToSearchResult(this._currentSearchResultIndex);
},

jumpToLastSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
this._currentSearchResultIndex = (this._searchResults.length - 1);
this._jumpToSearchResult(this._currentSearchResultIndex);
},

jumpToNextSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
if (++this._currentSearchResultIndex >= this._searchResults.length)
this._currentSearchResultIndex = 0;
this._jumpToSearchResult(this._currentSearchResultIndex);
},

jumpToPreviousSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
if (--this._currentSearchResultIndex < 0)
this._currentSearchResultIndex = (this._searchResults.length - 1);
this._jumpToSearchResult(this._currentSearchResultIndex);
},

showingFirstSearchResult: function()
{
return (this._currentSearchResultIndex === 0);
},

showingLastSearchResult: function()
{
return (this._searchResults && this._currentSearchResultIndex === (this._searchResults.length - 1));
},

_jumpToSearchResult: function(index)
{
var searchResult = this._searchResults[index];
if (!searchResult)
return;

var profileNode = searchResult.profileNode;
profileNode.revealAndSelect();
},

_changeView: function()
{
if (!this.profile)
return;

switch (this.viewSelectComboBox.selectedOption().value) {
case WebInspector.CPUProfileView._TypeTree:
this.profileDataGridTree = this.topDownProfileDataGridTree;
this._sortProfile();
this._viewType.set(WebInspector.CPUProfileView._TypeTree);
break;
case WebInspector.CPUProfileView._TypeHeavy:
this.profileDataGridTree = this.bottomUpProfileDataGridTree;
this._sortProfile();
this._viewType.set(WebInspector.CPUProfileView._TypeHeavy);
}

if (!this.currentQuery || !this._searchFinishedCallback || !this._searchResults)
return;




this._searchFinishedCallback(this, -this._searchResults.length);
this.performSearch(this.currentQuery, this._searchFinishedCallback);
},

_percentClicked: function(event)
{
var currentState = this.showSelfTimeAsPercent.get() && this.showTotalTimeAsPercent.get() && this.showAverageTimeAsPercent.get();
this.showSelfTimeAsPercent.set(!currentState);
this.showTotalTimeAsPercent.set(!currentState);
this.showAverageTimeAsPercent.set(!currentState);
this.refreshShowAsPercents();
},

_updatePercentButton: function()
{
if (this.showSelfTimeAsPercent.get() && this.showTotalTimeAsPercent.get() && this.showAverageTimeAsPercent.get()) {
this.percentButton.title = WebInspector.UIString("Show absolute total and self times.");
this.percentButton.toggled = true;
} else {
this.percentButton.title = WebInspector.UIString("Show total and self times as percentages.");
this.percentButton.toggled = false;
}
},

_focusClicked: function(event)
{
if (!this.dataGrid.selectedNode)
return;

this.resetButton.visible = true;
this.profileDataGridTree.focus(this.dataGrid.selectedNode);
this.refresh();
this.refreshVisibleData();
},

_excludeClicked: function(event)
{
var selectedNode = this.dataGrid.selectedNode

if (!selectedNode)
return;

selectedNode.deselect();

this.resetButton.visible = true;
this.profileDataGridTree.exclude(selectedNode);
this.refresh();
this.refreshVisibleData();
},

_resetClicked: function(event)
{
this.resetButton.visible = false;
this.profileDataGridTree.restore();
this._linkifier.reset();
this.refresh();
this.refreshVisibleData();
},

_dataGridNodeSelected: function(node)
{
this.focusButton.setEnabled(true);
this.excludeButton.setEnabled(true);
},

_dataGridNodeDeselected: function(node)
{
this.focusButton.setEnabled(false);
this.excludeButton.setEnabled(false);
},

_sortProfile: function()
{
var sortAscending = this.dataGrid.sortOrder === "ascending";
var sortColumnIdentifier = this.dataGrid.sortColumnIdentifier;
var sortProperty = {
"average": "averageTime",
"self": "selfTime",
"total": "totalTime",
"calls": "numberOfCalls",
"function": "functionName"
}[sortColumnIdentifier];

this.profileDataGridTree.sort(WebInspector.ProfileDataGridTree.propertyComparator(sortProperty, sortAscending));

this.refresh();
},

_mouseDownInDataGrid: function(event)
{
if (event.detail < 2)
return;

var cell = event.target.enclosingNodeOrSelfWithNodeName("td");
if (!cell || (!cell.hasStyleClass("total-column") && !cell.hasStyleClass("self-column") && !cell.hasStyleClass("average-column")))
return;

if (cell.hasStyleClass("total-column"))
this.showTotalTimeAsPercent.set(!this.showTotalTimeAsPercent.get());
else if (cell.hasStyleClass("self-column"))
this.showSelfTimeAsPercent.set(!this.showSelfTimeAsPercent.get());
else if (cell.hasStyleClass("average-column"))
this.showAverageTimeAsPercent.set(!this.showAverageTimeAsPercent.get());

this.refreshShowAsPercents();

event.consume(true);
},

_assignParentsInProfile: function()
{
var head = this.profile.head;
head.parent = null;
head.head = null;
var nodesToTraverse = [ { parent: head, children: head.children } ];
while (nodesToTraverse.length > 0) {
var pair = nodesToTraverse.shift();
var parent = pair.parent;
var children = pair.children;
var length = children.length;
for (var i = 0; i < length; ++i) {
children[i].head = head;
children[i].parent = parent;
if (children[i].children.length > 0)
nodesToTraverse.push({ parent: children[i], children: children[i].children });
}
}
},

__proto__: WebInspector.View.prototype
}


WebInspector.CPUProfileType = function()
{
WebInspector.ProfileType.call(this, WebInspector.CPUProfileType.TypeId, WebInspector.UIString("Collect JavaScript CPU Profile"));
this._recording = false;
WebInspector.CPUProfileType.instance = this;
}

WebInspector.CPUProfileType.TypeId = "CPU";

WebInspector.CPUProfileType.prototype = {
get buttonTooltip()
{
return this._recording ? WebInspector.UIString("Stop CPU profiling.") : WebInspector.UIString("Start CPU profiling.");
},


buttonClicked: function()
{
if (this._recording) {
this.stopRecordingProfile();
return false;
} else {
this.startRecordingProfile();
return true;
}
},

get treeItemTitle()
{
return WebInspector.UIString("CPU PROFILES");
},

get description()
{
return WebInspector.UIString("CPU profiles show where the execution time is spent in your page's JavaScript functions.");
},

isRecordingProfile: function()
{
return this._recording;
},

startRecordingProfile: function()
{
this._recording = true;
WebInspector.userMetrics.ProfilesCPUProfileTaken.record();
ProfilerAgent.start();
},

stopRecordingProfile: function()
{
this._recording = false;
ProfilerAgent.stop();
},

setRecordingProfile: function(isProfiling)
{
this._recording = isProfiling;
},


createTemporaryProfile: function(title)
{
title = title || WebInspector.UIString("Recording\u2026");
return new WebInspector.CPUProfileHeader(this, title);
},


createProfile: function(profile)
{
return new WebInspector.CPUProfileHeader(this, profile.title, profile.uid);
},

__proto__: WebInspector.ProfileType.prototype
}


WebInspector.CPUProfileHeader = function(type, title, uid)
{
WebInspector.ProfileHeader.call(this, type, title, uid);
}

WebInspector.CPUProfileHeader.prototype = {

createSidebarTreeElement: function()
{
return new WebInspector.ProfileSidebarTreeElement(this, WebInspector.UIString("Profile %d"), "profile-sidebar-tree-item");
},


createView: function(profilesPanel)
{
return new WebInspector.CPUProfileView(this);
},

__proto__: WebInspector.ProfileHeader.prototype
}
;



WebInspector.CSSSelectorDataGridNode = function(profileView, data)
{
WebInspector.DataGridNode.call(this, data, false);
this._profileView = profileView;
}

WebInspector.CSSSelectorDataGridNode.prototype = {
get data()
{
var data = {};
data.selector = this._data.selector;
data.matches = this._data.matchCount;

if (this._profileView.showTimeAsPercent.get())
data.time = Number(this._data.timePercent).toFixed(1) + "%";
else
data.time = Number.secondsToString(this._data.time / 1000, true);

return data;
},

get rawData()
{
return this._data;
},

createCell: function(columnIdentifier)
{
var cell = WebInspector.DataGridNode.prototype.createCell.call(this, columnIdentifier);
if (columnIdentifier === "selector" && cell.firstChild) {
cell.firstChild.title = this.rawData.selector;
return cell;
}

if (columnIdentifier !== "source")
return cell;

cell.removeChildren();

if (this.rawData.url) {
var wrapperDiv = cell.createChild("div");
wrapperDiv.appendChild(WebInspector.linkifyResourceAsNode(this.rawData.url, this.rawData.lineNumber));
}

return cell;
},

__proto__: WebInspector.DataGridNode.prototype
}


WebInspector.CSSSelectorProfileView = function(profile)
{
WebInspector.View.call(this);

this.element.addStyleClass("profile-view");

this.showTimeAsPercent = WebInspector.settings.createSetting("selectorProfilerShowTimeAsPercent", true);

var columns = { "selector": { title: WebInspector.UIString("Selector"), width: "550px", sortable: true },
"source": { title: WebInspector.UIString("Source"), width: "100px", sortable: true },
"time": { title: WebInspector.UIString("Total"), width: "72px", sort: "descending", sortable: true },
"matches": { title: WebInspector.UIString("Matches"), width: "72px", sortable: true } };

this.dataGrid = new WebInspector.DataGrid(columns);
this.dataGrid.element.addStyleClass("selector-profile-view");
this.dataGrid.addEventListener("sorting changed", this._sortProfile, this);
this.dataGrid.element.addEventListener("mousedown", this._mouseDownInDataGrid.bind(this), true);
this.dataGrid.show(this.element);

this.percentButton = new WebInspector.StatusBarButton("", "percent-time-status-bar-item");
this.percentButton.addEventListener("click", this._percentClicked, this);

this.profile = profile;

this._createProfileNodes();
this._sortProfile();
this._updatePercentButton();
}

WebInspector.CSSSelectorProfileView.prototype = {
get statusBarItems()
{
return [this.percentButton.element];
},

get profile()
{
return this._profile;
},

set profile(profile)
{
this._profile = profile;
},

_createProfileNodes: function()
{
var data = this.profile.data;
if (!data) {

return;
}

this.profile.children = [];
for (var i = 0; i < data.length; ++i) {
data[i].timePercent = data[i].time * 100 / this.profile.totalTime;
var node = new WebInspector.CSSSelectorDataGridNode(this, data[i]);
this.profile.children.push(node);
}
},

rebuildGridItems: function()
{
this.dataGrid.rootNode().removeChildren();

var children = this.profile.children;
var count = children.length;

for (var index = 0; index < count; ++index)
this.dataGrid.rootNode().appendChild(children[index]);
},

refreshData: function()
{
var child = this.dataGrid.rootNode().children[0];
while (child) {
child.refresh();
child = child.traverseNextNode(false, null, true);
}
},

refreshShowAsPercents: function()
{
this._updatePercentButton();
this.refreshData();
},

_percentClicked: function(event)
{
this.showTimeAsPercent.set(!this.showTimeAsPercent.get());
this.refreshShowAsPercents();
},

_updatePercentButton: function()
{
if (this.showTimeAsPercent.get()) {
this.percentButton.title = WebInspector.UIString("Show absolute times.");
this.percentButton.toggled = true;
} else {
this.percentButton.title = WebInspector.UIString("Show times as percentages.");
this.percentButton.toggled = false;
}
},

_sortProfile: function()
{
var sortAscending = this.dataGrid.sortOrder === "ascending";
var sortColumnIdentifier = this.dataGrid.sortColumnIdentifier;

function selectorComparator(a, b)
{
var result = b.rawData.selector.localeCompare(a.rawData.selector);
return sortAscending ? -result : result;
}

function sourceComparator(a, b)
{
var aRawData = a.rawData;
var bRawData = b.rawData;
var result = bRawData.url.localeCompare(aRawData.url);
if (!result)
result = bRawData.lineNumber - aRawData.lineNumber;
return sortAscending ? -result : result;
}

function timeComparator(a, b)
{
const result = b.rawData.time - a.rawData.time;
return sortAscending ? -result : result;
}

function matchesComparator(a, b)
{
const result = b.rawData.matchCount - a.rawData.matchCount;
return sortAscending ? -result : result;
}

var comparator;
switch (sortColumnIdentifier) {
case "time":
comparator = timeComparator;
break;
case "matches":
comparator = matchesComparator;
break;
case "selector":
comparator = selectorComparator;
break;
case "source":
comparator = sourceComparator;
break;
}

this.profile.children.sort(comparator);

this.rebuildGridItems();
},

_mouseDownInDataGrid: function(event)
{
if (event.detail < 2)
return;

var cell = event.target.enclosingNodeOrSelfWithNodeName("td");
if (!cell)
return;

if (cell.hasStyleClass("time-column"))
this.showTimeAsPercent.set(!this.showTimeAsPercent.get());
else
return;

this.refreshShowAsPercents();

event.consume(true);
},

__proto__: WebInspector.View.prototype
}


WebInspector.CSSSelectorProfileType = function()
{
WebInspector.ProfileType.call(this, WebInspector.CSSSelectorProfileType.TypeId, WebInspector.UIString("Collect CSS Selector Profile"));
this._recording = false;
this._profileUid = 1;
WebInspector.CSSSelectorProfileType.instance = this;
}

WebInspector.CSSSelectorProfileType.TypeId = "SELECTOR";

WebInspector.CSSSelectorProfileType.prototype = {
get buttonTooltip()
{
return this._recording ? WebInspector.UIString("Stop CSS selector profiling.") : WebInspector.UIString("Start CSS selector profiling.");
},


buttonClicked: function(profilesPanel)
{
if (this._recording) {
this._stopRecordingProfile(profilesPanel);
return false;
} else {
this._startRecordingProfile(profilesPanel);
return true;
}
},

get treeItemTitle()
{
return WebInspector.UIString("CSS SELECTOR PROFILES");
},

get description()
{
return WebInspector.UIString("CSS selector profiles show how long the selector matching has taken in total and how many times a certain selector has matched DOM elements (the results are approximate due to matching algorithm optimizations.)");
},

reset: function()
{
this._profileUid = 1;
},

setRecordingProfile: function(isProfiling)
{
this._recording = isProfiling;
},


_startRecordingProfile: function(profilesPanel)
{
this._recording = true;
CSSAgent.startSelectorProfiler();
profilesPanel.setRecordingProfile(WebInspector.CSSSelectorProfileType.TypeId, true);
},


_stopRecordingProfile: function(profilesPanel)
{

function callback(error, profile)
{
if (error)
return;

var uid = this._profileUid++;
var title = WebInspector.UIString("Profile %d", uid) + String.sprintf(" (%s)", Number.secondsToString(profile.totalTime / 1000));
var profileHeader = new WebInspector.CSSProfileHeader(this, title, uid, profile);
profilesPanel.addProfileHeader(profileHeader);
profilesPanel.setRecordingProfile(WebInspector.CSSSelectorProfileType.TypeId, false);
}

this._recording = false;
CSSAgent.stopSelectorProfiler(callback.bind(this));
},


createTemporaryProfile: function(title)
{
title = title || WebInspector.UIString("Recording\u2026");
return new WebInspector.CSSProfileHeader(this, title);
},

__proto__: WebInspector.ProfileType.prototype
}



WebInspector.CSSProfileHeader = function(type, title, uid, protocolData)
{
WebInspector.ProfileHeader.call(this, type, title, uid);
this._protocolData = protocolData;
}

WebInspector.CSSProfileHeader.prototype = {

createSidebarTreeElement: function()
{
return new WebInspector.ProfileSidebarTreeElement(this, this.title, "profile-sidebar-tree-item");
},


createView: function(profilesPanel)
{
var profile =   (this._protocolData);
return new WebInspector.CSSSelectorProfileView(profile);
},

__proto__: WebInspector.ProfileHeader.prototype
}
;



WebInspector.HeapSnapshotArraySlice = function(array, start, end)
{
this._array = array;
this._start = start;
this.length = end - start;
}

WebInspector.HeapSnapshotArraySlice.prototype = {
item: function(index)
{
return this._array[this._start + index];
},

slice: function(start, end)
{
if (typeof end === "undefined")
end = this.length;
return this._array.subarray(this._start + start, this._start + end);
}
}


WebInspector.HeapSnapshotEdge = function(snapshot, edges, edgeIndex)
{
this._snapshot = snapshot;
this._edges = edges;
this.edgeIndex = edgeIndex || 0;
}

WebInspector.HeapSnapshotEdge.prototype = {
clone: function()
{
return new WebInspector.HeapSnapshotEdge(this._snapshot, this._edges, this.edgeIndex);
},

hasStringName: function()
{
if (!this.isShortcut())
return this._hasStringName();
return isNaN(parseInt(this._name(), 10));
},

isElement: function()
{
return this._type() === this._snapshot._edgeElementType;
},

isHidden: function()
{
return this._type() === this._snapshot._edgeHiddenType;
},

isWeak: function()
{
return this._type() === this._snapshot._edgeWeakType;
},

isInternal: function()
{
return this._type() === this._snapshot._edgeInternalType;
},

isInvisible: function()
{
return this._type() === this._snapshot._edgeInvisibleType;
},

isShortcut: function()
{
return this._type() === this._snapshot._edgeShortcutType;
},

name: function()
{
if (!this.isShortcut())
return this._name();
var numName = parseInt(this._name(), 10);
return isNaN(numName) ? this._name() : numName;
},

node: function()
{
return new WebInspector.HeapSnapshotNode(this._snapshot, this.nodeIndex());
},

nodeIndex: function()
{
return this._edges.item(this.edgeIndex + this._snapshot._edgeToNodeOffset);
},

rawEdges: function()
{
return this._edges;
},

toString: function()
{
var name = this.name();
switch (this.type()) {
case "context": return "->" + name;
case "element": return "[" + name + "]";
case "weak": return "[[" + name + "]]";
case "property":
return name.indexOf(" ") === -1 ? "." + name : "[\"" + name + "\"]";
case "shortcut":
if (typeof name === "string")
return name.indexOf(" ") === -1 ? "." + name : "[\"" + name + "\"]";
else
return "[" + name + "]";
case "internal":
case "hidden":
case "invisible":
return "{" + name + "}";
};
return "?" + name + "?";
},

type: function()
{
return this._snapshot._edgeTypes[this._type()];
},

_hasStringName: function()
{
return !this.isElement() && !this.isHidden() && !this.isWeak();
},

_name: function()
{
return this._hasStringName() ? this._snapshot._strings[this._nameOrIndex()] : this._nameOrIndex();
},

_nameOrIndex: function()
{
return this._edges.item(this.edgeIndex + this._snapshot._edgeNameOffset);
},

_type: function()
{
return this._edges.item(this.edgeIndex + this._snapshot._edgeTypeOffset);
}
};


WebInspector.HeapSnapshotEdgeIterator = function(edge)
{
this.edge = edge;
}

WebInspector.HeapSnapshotEdgeIterator.prototype = {
first: function()
{
this.edge.edgeIndex = 0;
},

hasNext: function()
{
return this.edge.edgeIndex < this.edge._edges.length;
},

index: function()
{
return this.edge.edgeIndex;
},

setIndex: function(newIndex)
{
this.edge.edgeIndex = newIndex;
},

item: function()
{
return this.edge;
},

next: function()
{
this.edge.edgeIndex += this.edge._snapshot._edgeFieldsCount;
}
};


WebInspector.HeapSnapshotRetainerEdge = function(snapshot, retainedNodeIndex, retainerIndex)
{
this._snapshot = snapshot;
this._retainedNodeIndex = retainedNodeIndex;

var retainedNodeOrdinal = retainedNodeIndex / snapshot._nodeFieldCount;
this._firstRetainer = snapshot._firstRetainerIndex[retainedNodeOrdinal];
this._retainersCount = snapshot._firstRetainerIndex[retainedNodeOrdinal + 1] - this._firstRetainer;

this.setRetainerIndex(retainerIndex);
}

WebInspector.HeapSnapshotRetainerEdge.prototype = {
clone: function()
{
return new WebInspector.HeapSnapshotRetainerEdge(this._snapshot, this._retainedNodeIndex, this.retainerIndex());
},

hasStringName: function()
{
return this._edge().hasStringName();
},

isElement: function()
{
return this._edge().isElement();
},

isHidden: function()
{
return this._edge().isHidden();
},

isInternal: function()
{
return this._edge().isInternal();
},

isInvisible: function()
{
return this._edge().isInvisible();
},

isShortcut: function()
{
return this._edge().isShortcut();
},

isWeak: function()
{
return this._edge().isWeak();
},

name: function()
{
return this._edge().name();
},

node: function()
{
return this._node();
},

nodeIndex: function()
{
return this._nodeIndex;
},

retainerIndex: function()
{
return this._retainerIndex;
},

setRetainerIndex: function(newIndex)
{
if (newIndex !== this._retainerIndex) {
this._retainerIndex = newIndex;
this.edgeIndex = newIndex;
}
},

set edgeIndex(edgeIndex)
{
var retainerIndex = this._firstRetainer + edgeIndex;
this._globalEdgeIndex = this._snapshot._retainingEdges[retainerIndex];
this._nodeIndex = this._snapshot._retainingNodes[retainerIndex];
delete this._edgeInstance;
delete this._nodeInstance;
},

_node: function()
{
if (!this._nodeInstance)
this._nodeInstance = new WebInspector.HeapSnapshotNode(this._snapshot, this._nodeIndex);
return this._nodeInstance;
},

_edge: function()
{
if (!this._edgeInstance) {
var edgeIndex = this._globalEdgeIndex - this._node()._edgeIndexesStart();
this._edgeInstance = new WebInspector.HeapSnapshotEdge(this._snapshot, this._node().rawEdges(), edgeIndex);
}
return this._edgeInstance;
},

toString: function()
{
return this._edge().toString();
},

type: function()
{
return this._edge().type();
}
}


WebInspector.HeapSnapshotRetainerEdgeIterator = function(retainer)
{
this.retainer = retainer;
}

WebInspector.HeapSnapshotRetainerEdgeIterator.prototype = {
first: function()
{
this.retainer.setRetainerIndex(0);
},

hasNext: function()
{
return this.retainer.retainerIndex() < this.retainer._retainersCount;
},

index: function()
{
return this.retainer.retainerIndex();
},

setIndex: function(newIndex)
{
this.retainer.setRetainerIndex(newIndex);
},

item: function()
{
return this.retainer;
},

next: function()
{
this.retainer.setRetainerIndex(this.retainer.retainerIndex() + 1);
}
};


WebInspector.HeapSnapshotNode = function(snapshot, nodeIndex)
{
this._snapshot = snapshot;
this._firstNodeIndex = nodeIndex;
this.nodeIndex = nodeIndex;
}

WebInspector.HeapSnapshotNode.prototype = {
canBeQueried: function()
{
var flags = this._snapshot._flagsOfNode(this);
return !!(flags & this._snapshot._nodeFlags.canBeQueried);
},

isPageObject: function()
{
var flags = this._snapshot._flagsOfNode(this);
return !!(flags & this._snapshot._nodeFlags.pageObject);
},

distanceToWindow: function()
{
return this._snapshot._distancesToWindow[this.nodeIndex / this._snapshot._nodeFieldCount];
},

className: function()
{
var type = this.type();
switch (type) {
case "hidden":
return WebInspector.UIString("(system)");
case "object":
case "native":
return this.name();
case "code":
return WebInspector.UIString("(compiled code)");
default:
return "(" + type + ")";
}
},

classIndex: function()
{
var snapshot = this._snapshot;
var nodes = snapshot._nodes;
var type = nodes[this.nodeIndex + snapshot._nodeTypeOffset];;
if (type === snapshot._nodeObjectType || type === snapshot._nodeNativeType)
return nodes[this.nodeIndex + snapshot._nodeNameOffset];
return -1 - type;
},

dominatorIndex: function()
{
var nodeFieldCount = this._snapshot._nodeFieldCount;
return this._snapshot._dominatorsTree[this.nodeIndex / this._snapshot._nodeFieldCount] * nodeFieldCount;
},

edges: function()
{
return new WebInspector.HeapSnapshotEdgeIterator(new WebInspector.HeapSnapshotEdge(this._snapshot, this.rawEdges()));
},

edgesCount: function()
{
return (this._edgeIndexesEnd() - this._edgeIndexesStart()) / this._snapshot._edgeFieldsCount;
},

flags: function()
{
return this._snapshot._flagsOfNode(this);
},

id: function()
{
var snapshot = this._snapshot;
return snapshot._nodes[this.nodeIndex + snapshot._nodeIdOffset];
},

isHidden: function()
{
return this._type() === this._snapshot._nodeHiddenType;
},

isNative: function()
{
return this._type() === this._snapshot._nodeNativeType;
},

isSynthetic: function()
{
return this._type() === this._snapshot._nodeSyntheticType;
},

isWindow: function()
{
const windowRE = /^Window/;
return windowRE.test(this.name());
},

isDetachedDOMTreesRoot: function()
{
return this.name() === "(Detached DOM trees)";
},

isDetachedDOMTree: function()
{
const detachedDOMTreeRE = /^Detached DOM tree/;
return detachedDOMTreeRE.test(this.className());
},

isRoot: function()
{
return this.nodeIndex === this._snapshot._rootNodeIndex;
},

name: function()
{
return this._snapshot._strings[this._name()];
},

rawEdges: function()
{
return new WebInspector.HeapSnapshotArraySlice(this._snapshot._containmentEdges, this._edgeIndexesStart(), this._edgeIndexesEnd());
},

retainedSize: function()
{
var snapshot = this._snapshot;
return snapshot._nodes[this.nodeIndex + snapshot._nodeRetainedSizeOffset];
},

retainers: function()
{
return new WebInspector.HeapSnapshotRetainerEdgeIterator(new WebInspector.HeapSnapshotRetainerEdge(this._snapshot, this.nodeIndex, 0));
},

selfSize: function()
{
var snapshot = this._snapshot;
return snapshot._nodes[this.nodeIndex + snapshot._nodeSelfSizeOffset];
},

type: function()
{
return this._snapshot._nodeTypes[this._type()];
},

_name: function()
{
var snapshot = this._snapshot;
return snapshot._nodes[this.nodeIndex + snapshot._nodeNameOffset];
},

_edgeIndexesStart: function()
{
return this._snapshot._firstEdgeIndexes[this._ordinal()];
},

_edgeIndexesEnd: function()
{
return this._snapshot._firstEdgeIndexes[this._ordinal() + 1];
},

_ordinal: function()
{
return this.nodeIndex / this._snapshot._nodeFieldCount;
},

_nextNodeIndex: function()
{
return this.nodeIndex + this._snapshot._nodeFieldCount;
},

_type: function()
{
var snapshot = this._snapshot;
return snapshot._nodes[this.nodeIndex + snapshot._nodeTypeOffset];
}
};


WebInspector.HeapSnapshotNodeIterator = function(node)
{
this.node = node;
this._nodesLength = node._snapshot._nodes.length;
}

WebInspector.HeapSnapshotNodeIterator.prototype = {
first: function()
{
this.node.nodeIndex = this.node._firstNodeIndex;
},

hasNext: function()
{
return this.node.nodeIndex < this._nodesLength;
},

index: function()
{
return this.node.nodeIndex;
},

setIndex: function(newIndex)
{
this.node.nodeIndex = newIndex;
},

item: function()
{
return this.node;
},

next: function()
{
this.node.nodeIndex = this.node._nextNodeIndex();
}
}


WebInspector.HeapSnapshot = function(profile)
{
this.uid = profile.snapshot.uid;
this._nodes = profile.nodes;
this._containmentEdges = profile.edges;

this._metaNode = profile.snapshot.meta;
this._strings = profile.strings;

this._snapshotDiffs = {};
this._aggregatesForDiff = null;

this._init();
}


function HeapSnapshotMetainfo()
{

this.node_fields = [];
this.node_types = [];
this.edge_fields = [];
this.edge_types = [];


this.fields = [];
this.types = [];
}


function HeapSnapshotHeader()
{

this.title = "";
this.uid = 0;
this.meta = new HeapSnapshotMetainfo();
this.node_count = 0;
this.edge_count = 0;
}

WebInspector.HeapSnapshot.prototype = {
_init: function()
{
var meta = this._metaNode;
this._rootNodeIndex = 0;

this._nodeTypeOffset = meta.node_fields.indexOf("type");
this._nodeNameOffset = meta.node_fields.indexOf("name");
this._nodeIdOffset = meta.node_fields.indexOf("id");
this._nodeSelfSizeOffset = meta.node_fields.indexOf("self_size");
this._nodeEdgeCountOffset = meta.node_fields.indexOf("edge_count");
this._nodeFieldCount = meta.node_fields.length;

this._nodeTypes = meta.node_types[this._nodeTypeOffset];
this._nodeHiddenType = this._nodeTypes.indexOf("hidden");
this._nodeObjectType = this._nodeTypes.indexOf("object");
this._nodeNativeType = this._nodeTypes.indexOf("native");
this._nodeCodeType = this._nodeTypes.indexOf("code");
this._nodeSyntheticType = this._nodeTypes.indexOf("synthetic");

this._edgeFieldsCount = meta.edge_fields.length;
this._edgeTypeOffset = meta.edge_fields.indexOf("type");
this._edgeNameOffset = meta.edge_fields.indexOf("name_or_index");
this._edgeToNodeOffset = meta.edge_fields.indexOf("to_node");

this._edgeTypes = meta.edge_types[this._edgeTypeOffset];
this._edgeTypes.push("invisible");
this._edgeElementType = this._edgeTypes.indexOf("element");
this._edgeHiddenType = this._edgeTypes.indexOf("hidden");
this._edgeInternalType = this._edgeTypes.indexOf("internal");
this._edgeShortcutType = this._edgeTypes.indexOf("shortcut");
this._edgeWeakType = this._edgeTypes.indexOf("weak");
this._edgeInvisibleType = this._edgeTypes.indexOf("invisible");

this._nodeFlags = { 
canBeQueried: 1,
detachedDOMTreeNode: 2,
pageObject: 4, 

visitedMarkerMask: 0x0ffff, 
visitedMarker:     0x10000  
};

this.nodeCount = this._nodes.length / this._nodeFieldCount;
this._edgeCount = this._containmentEdges.length / this._edgeFieldsCount;

this._buildEdgeIndexes();
this._markInvisibleEdges();
this._buildRetainers();
this._calculateFlags();
this._calculateObjectToWindowDistance();
var result = this._buildPostOrderIndex();

this._dominatorsTree = this._buildDominatorTree(result.postOrderIndex2NodeOrdinal, result.nodeOrdinal2PostOrderIndex);
this._calculateRetainedSizes(result.postOrderIndex2NodeOrdinal);
this._buildDominatedNodes();
},

_buildEdgeIndexes: function()
{

if (this._nodeEdgeCountOffset === -1) {
var nodes = this._nodes;
var nodeCount = this.nodeCount;
var firstEdgeIndexes = this._firstEdgeIndexes = new Uint32Array(nodeCount + 1);
var nodeFieldCount = this._nodeFieldCount;
var nodeEdgesIndexOffset = this._metaNode.node_fields.indexOf("edges_index");
firstEdgeIndexes[nodeCount] = this._containmentEdges.length;
for (var nodeOrdinal = 0; nodeOrdinal < nodeCount; ++nodeOrdinal) {
firstEdgeIndexes[nodeOrdinal] = nodes[nodeOrdinal * nodeFieldCount + nodeEdgesIndexOffset];
}
return;
}

var nodes = this._nodes;
var nodeCount = this.nodeCount;
var firstEdgeIndexes = this._firstEdgeIndexes = new Uint32Array(nodeCount + 1);
var nodeFieldCount = this._nodeFieldCount;
var edgeFieldsCount = this._edgeFieldsCount;
var nodeEdgeCountOffset = this._nodeEdgeCountOffset;
firstEdgeIndexes[nodeCount] = this._containmentEdges.length;
for (var nodeOrdinal = 0, edgeIndex = 0; nodeOrdinal < nodeCount; ++nodeOrdinal) {
firstEdgeIndexes[nodeOrdinal] = edgeIndex;
edgeIndex += nodes[nodeOrdinal * nodeFieldCount + nodeEdgeCountOffset] * edgeFieldsCount;
}
},

_buildRetainers: function()
{
var retainingNodes = this._retainingNodes = new Uint32Array(this._edgeCount);
var retainingEdges = this._retainingEdges = new Uint32Array(this._edgeCount);


var firstRetainerIndex = this._firstRetainerIndex = new Uint32Array(this.nodeCount + 1);

var containmentEdges = this._containmentEdges;
var edgeFieldsCount = this._edgeFieldsCount;
var nodeFieldCount = this._nodeFieldCount;
var edgeToNodeOffset = this._edgeToNodeOffset;
var nodes = this._nodes;
var firstEdgeIndexes = this._firstEdgeIndexes;
var nodeCount = this.nodeCount;

for (var toNodeFieldIndex = edgeToNodeOffset, l = containmentEdges.length; toNodeFieldIndex < l; toNodeFieldIndex += edgeFieldsCount) {
var toNodeIndex = containmentEdges[toNodeFieldIndex];
if (toNodeIndex % nodeFieldCount)
throw new Error("Invalid toNodeIndex " + toNodeIndex);
++firstRetainerIndex[toNodeIndex / nodeFieldCount];
}
for (var i = 0, firstUnusedRetainerSlot = 0; i < nodeCount; i++) {
var retainersCount = firstRetainerIndex[i];
firstRetainerIndex[i] = firstUnusedRetainerSlot;
retainingNodes[firstUnusedRetainerSlot] = retainersCount;
firstUnusedRetainerSlot += retainersCount;
}
firstRetainerIndex[nodeCount] = retainingNodes.length;

var nextNodeFirstEdgeIndex = firstEdgeIndexes[0];
for (var srcNodeOrdinal = 0; srcNodeOrdinal < nodeCount; ++srcNodeOrdinal) {
var firstEdgeIndex = nextNodeFirstEdgeIndex;
nextNodeFirstEdgeIndex = firstEdgeIndexes[srcNodeOrdinal + 1];
var srcNodeIndex = srcNodeOrdinal * nodeFieldCount;
for (var edgeIndex = firstEdgeIndex; edgeIndex < nextNodeFirstEdgeIndex; edgeIndex += edgeFieldsCount) {
var toNodeIndex = containmentEdges[edgeIndex + edgeToNodeOffset];
if (toNodeIndex % nodeFieldCount)
throw new Error("Invalid toNodeIndex " + toNodeIndex);
var firstRetainerSlotIndex = firstRetainerIndex[toNodeIndex / nodeFieldCount];
var nextUnusedRetainerSlotIndex = firstRetainerSlotIndex + (--retainingNodes[firstRetainerSlotIndex]);
retainingNodes[nextUnusedRetainerSlotIndex] = srcNodeIndex;
retainingEdges[nextUnusedRetainerSlotIndex] = edgeIndex;
}
}
},

dispose: function()
{
delete this._nodes;
delete this._strings;
delete this._retainingEdges;
delete this._retainingNodes;
delete this._firstRetainerIndex;
if (this._aggregates) {
delete this._aggregates;
delete this._aggregatesSortedFlags;
}
delete this._dominatedNodes;
delete this._firstDominatedNodeIndex;
delete this._flags;
delete this._distancesToWindow;
delete this._dominatorsTree;
},

_allNodes: function()
{
return new WebInspector.HeapSnapshotNodeIterator(this.rootNode());
},

rootNode: function()
{
return new WebInspector.HeapSnapshotNode(this, this._rootNodeIndex);
},

get rootNodeIndex()
{
return this._rootNodeIndex;
},

get totalSize()
{
return this.rootNode().retainedSize();
},

_getDominatedIndex: function(nodeIndex)
{
if (nodeIndex % this._nodeFieldCount)
throw new Error("Invalid nodeIndex: " + nodeIndex);
return this._firstDominatedNodeIndex[nodeIndex / this._nodeFieldCount];
},

_dominatedNodesOfNode: function(node)
{
var dominatedIndexFrom = this._getDominatedIndex(node.nodeIndex);
var dominatedIndexTo = this._getDominatedIndex(node._nextNodeIndex());
return new WebInspector.HeapSnapshotArraySlice(this._dominatedNodes, dominatedIndexFrom, dominatedIndexTo);
},

_flagsOfNode: function(node)
{
return this._flags[node.nodeIndex / this._nodeFieldCount];
},


aggregates: function(sortedIndexes, key, filterString)
{
if (!this._aggregates) {
this._aggregates = {};
this._aggregatesSortedFlags = {};
}

var aggregatesByClassName = this._aggregates[key];
if (aggregatesByClassName) {
if (sortedIndexes && !this._aggregatesSortedFlags[key]) {
this._sortAggregateIndexes(aggregatesByClassName);
this._aggregatesSortedFlags[key] = sortedIndexes;
}
return aggregatesByClassName;
}

var filter;
if (filterString)
filter = this._parseFilter(filterString);

var aggregates = this._buildAggregates(filter);
this._calculateClassesRetainedSize(aggregates.aggregatesByClassIndex, filter);
aggregatesByClassName = aggregates.aggregatesByClassName;

if (sortedIndexes)
this._sortAggregateIndexes(aggregatesByClassName);

this._aggregatesSortedFlags[key] = sortedIndexes;
this._aggregates[key] = aggregatesByClassName;

return aggregatesByClassName;
},

aggregatesForDiff: function()
{
if (this._aggregatesForDiff)
return this._aggregatesForDiff;

var aggregatesByClassName = this.aggregates(true, "allObjects");
this._aggregatesForDiff  = {};

var node = new WebInspector.HeapSnapshotNode(this);
for (var className in aggregatesByClassName) {
var aggregate = aggregatesByClassName[className];
var indexes = aggregate.idxs;
var ids = new Array(indexes.length);
var selfSizes = new Array(indexes.length);
for (var i = 0; i < indexes.length; i++) {
node.nodeIndex = indexes[i];
ids[i] = node.id();
selfSizes[i] = node.selfSize();
}

this._aggregatesForDiff[className] = {
indexes: indexes,
ids: ids,
selfSizes: selfSizes
};
}
return this._aggregatesForDiff;
},

_calculateObjectToWindowDistance: function()
{
var nodeFieldCount = this._nodeFieldCount;
var distances = new Uint32Array(this.nodeCount);


var nodesToVisit = new Uint32Array(this.nodeCount);
var nodesToVisitLength = 0;
for (var iter = this.rootNode().edges(); iter.hasNext(); iter.next()) {
var node = iter.edge.node();
if (node.isWindow()) {
nodesToVisit[nodesToVisitLength++] = node.nodeIndex;
distances[node.nodeIndex / nodeFieldCount] = 1;
}
}
this._bfs(nodesToVisit, nodesToVisitLength, distances);


nodesToVisitLength = 0;
nodesToVisit[nodesToVisitLength++] = this._rootNodeIndex;
distances[this._rootNodeIndex / nodeFieldCount] = 1;
this._bfs(nodesToVisit, nodesToVisitLength, distances);
this._distancesToWindow = distances;
},

_bfs: function(nodesToVisit, nodesToVisitLength, distances)
{

var edgeFieldsCount = this._edgeFieldsCount;
var nodeFieldCount = this._nodeFieldCount;
var containmentEdges = this._containmentEdges;
var firstEdgeIndexes = this._firstEdgeIndexes;
var edgeToNodeOffset = this._edgeToNodeOffset;
var edgeTypeOffset = this._edgeTypeOffset;
var nodes = this._nodes;
var nodeCount = this.nodeCount;
var containmentEdgesLength = containmentEdges.length;
var edgeWeakType = this._edgeWeakType;
var edgeShortcutType = this._edgeShortcutType;

var index = 0;
while (index < nodesToVisitLength) {
var nodeIndex = nodesToVisit[index++]; 
var nodeOrdinal = nodeIndex / nodeFieldCount;
var distance = distances[nodeOrdinal] + 1;
var firstEdgeIndex = firstEdgeIndexes[nodeOrdinal];
var edgesEnd = firstEdgeIndexes[nodeOrdinal + 1];
for (var edgeIndex = firstEdgeIndex; edgeIndex < edgesEnd; edgeIndex += edgeFieldsCount) {
var edgeType = containmentEdges[edgeIndex + edgeTypeOffset];
if (edgeType == edgeWeakType)
continue;
var childNodeIndex = containmentEdges[edgeIndex + edgeToNodeOffset];
var childNodeOrdinal = childNodeIndex / nodeFieldCount;
if (distances[childNodeOrdinal])
continue;
distances[childNodeOrdinal] = distance;
nodesToVisit[nodesToVisitLength++] = childNodeIndex;
}
}
if (nodesToVisitLength > nodeCount)
throw new Error("BFS failed. Nodes to visit (" + nodesToVisitLength + ") is more than nodes count (" + nodeCount + ")");
},

_buildAggregates: function(filter)
{
var aggregates = {};
var aggregatesByClassName = {};
var classIndexes = [];
var nodes = this._nodes;
var flags = this._flags;
var nodesLength = nodes.length;
var nodeNativeType = this._nodeNativeType;
var nodeFieldCount = this._nodeFieldCount;
var selfSizeOffset = this._nodeSelfSizeOffset;
var nodeTypeOffset = this._nodeTypeOffset;
var pageObjectFlag = this._nodeFlags.pageObject;
var node = new WebInspector.HeapSnapshotNode(this, this._rootNodeIndex);
var distancesToWindow = this._distancesToWindow;

for (var nodeIndex = this._rootNodeIndex; nodeIndex < nodesLength; nodeIndex += nodeFieldCount) {
var nodeOrdinal = nodeIndex / nodeFieldCount;
if (!(flags[nodeOrdinal] & pageObjectFlag))
continue;
node.nodeIndex = nodeIndex;
if (filter && !filter(node))
continue;
var selfSize = nodes[nodeIndex + selfSizeOffset];
if (!selfSize && nodes[nodeIndex + nodeTypeOffset] !== nodeNativeType)
continue;
var classIndex = node.classIndex();
if (!(classIndex in aggregates)) {
var nodeType = node.type();
var nameMatters = nodeType === "object" || nodeType === "native";
var value = {
count: 1,
distanceToWindow: distancesToWindow[nodeOrdinal],
self: selfSize,
maxRet: 0,
type: nodeType,
name: nameMatters ? node.name() : null,
idxs: [nodeIndex]
};
aggregates[classIndex] = value;
classIndexes.push(classIndex);
aggregatesByClassName[node.className()] = value;
} else {
var clss = aggregates[classIndex];
clss.distanceToWindow = Math.min(clss.distanceToWindow, distancesToWindow[nodeOrdinal]);
++clss.count;
clss.self += selfSize;
clss.idxs.push(nodeIndex);
}
}


for (var i = 0, l = classIndexes.length; i < l; ++i) {
var classIndex = classIndexes[i];
aggregates[classIndex].idxs = aggregates[classIndex].idxs.slice();
}
return {aggregatesByClassName: aggregatesByClassName, aggregatesByClassIndex: aggregates};
},

_calculateClassesRetainedSize: function(aggregates, filter)
{
var rootNodeIndex = this._rootNodeIndex;
var node = new WebInspector.HeapSnapshotNode(this, rootNodeIndex);
var list = [rootNodeIndex];
var sizes = [-1];
var classes = [];
var seenClassNameIndexes = {};
var nodeFieldCount = this._nodeFieldCount;
var nodeTypeOffset = this._nodeTypeOffset;
var nodeNativeType = this._nodeNativeType;
var dominatedNodes = this._dominatedNodes;
var nodes = this._nodes;
var flags = this._flags;
var pageObjectFlag = this._nodeFlags.pageObject;
var firstDominatedNodeIndex = this._firstDominatedNodeIndex;

while (list.length) {
var nodeIndex = list.pop();
node.nodeIndex = nodeIndex;
var classIndex = node.classIndex();
var seen = !!seenClassNameIndexes[classIndex];
var nodeOrdinal = nodeIndex / nodeFieldCount;
var dominatedIndexFrom = firstDominatedNodeIndex[nodeOrdinal];
var dominatedIndexTo = firstDominatedNodeIndex[nodeOrdinal + 1];

if (!seen &&
(flags[nodeOrdinal] & pageObjectFlag) &&
(!filter || filter(node)) &&
(node.selfSize() || nodes[nodeIndex + nodeTypeOffset] === nodeNativeType)
) {
aggregates[classIndex].maxRet += node.retainedSize();
if (dominatedIndexFrom !== dominatedIndexTo) {
seenClassNameIndexes[classIndex] = true;
sizes.push(list.length);
classes.push(classIndex);
}
}
for (var i = dominatedIndexFrom; i < dominatedIndexTo; i++)
list.push(dominatedNodes[i]);

var l = list.length;
while (sizes[sizes.length - 1] === l) {
sizes.pop();
classIndex = classes.pop();
seenClassNameIndexes[classIndex] = false;
}
}
},

_sortAggregateIndexes: function(aggregates)
{
var nodeA = new WebInspector.HeapSnapshotNode(this);
var nodeB = new WebInspector.HeapSnapshotNode(this);
for (var clss in aggregates)
aggregates[clss].idxs.sort(
function(idxA, idxB) {
nodeA.nodeIndex = idxA;
nodeB.nodeIndex = idxB;
return nodeA.id() < nodeB.id() ? -1 : 1;
});
},

_buildPostOrderIndex: function()
{
var nodeFieldCount = this._nodeFieldCount;
var nodes = this._nodes;
var nodeCount = this.nodeCount;
var rootNodeOrdinal = this._rootNodeIndex / nodeFieldCount;

var edgeFieldsCount = this._edgeFieldsCount;
var edgeTypeOffset = this._edgeTypeOffset;
var edgeToNodeOffset = this._edgeToNodeOffset;
var edgeShortcutType = this._edgeShortcutType;
var firstEdgeIndexes = this._firstEdgeIndexes;
var containmentEdges = this._containmentEdges;
var containmentEdgesLength = this._containmentEdges.length;

var flags = this._flags;
var flag = this._nodeFlags.pageObject;

var nodesToVisit = new Uint32Array(nodeCount);
var postOrderIndex2NodeOrdinal = new Uint32Array(nodeCount);
var nodeOrdinal2PostOrderIndex = new Uint32Array(nodeCount);
var painted = new Uint8Array(nodeCount);
var nodesToVisitLength = 0;
var postOrderIndex = 0;
var grey = 1;
var black = 2;

nodesToVisit[nodesToVisitLength++] = rootNodeOrdinal;
painted[rootNodeOrdinal] = grey;

while (nodesToVisitLength) {
var nodeOrdinal = nodesToVisit[nodesToVisitLength - 1];
if (painted[nodeOrdinal] === grey) {
painted[nodeOrdinal] = black;
var nodeFlag = flags[nodeOrdinal] & flag;
var beginEdgeIndex = firstEdgeIndexes[nodeOrdinal];
var endEdgeIndex = firstEdgeIndexes[nodeOrdinal + 1];
for (var edgeIndex = beginEdgeIndex; edgeIndex < endEdgeIndex; edgeIndex += edgeFieldsCount) {
if (nodeOrdinal !== rootNodeOrdinal && containmentEdges[edgeIndex + edgeTypeOffset] === edgeShortcutType)
continue;
var childNodeIndex = containmentEdges[edgeIndex + edgeToNodeOffset];
var childNodeOrdinal = childNodeIndex / nodeFieldCount;
var childNodeFlag = flags[childNodeOrdinal] & flag;


if (nodeOrdinal !== rootNodeOrdinal && childNodeFlag && !nodeFlag)
continue;
if (!painted[childNodeOrdinal]) {
painted[childNodeOrdinal] = grey;
nodesToVisit[nodesToVisitLength++] = childNodeOrdinal;
}
}
} else {
nodeOrdinal2PostOrderIndex[nodeOrdinal] = postOrderIndex;
postOrderIndex2NodeOrdinal[postOrderIndex++] = nodeOrdinal;
--nodesToVisitLength;
}
}

if (postOrderIndex !== nodeCount)
throw new Error("Postordering failed. " + (nodeCount - postOrderIndex) + " hanging nodes");

return {postOrderIndex2NodeOrdinal: postOrderIndex2NodeOrdinal, nodeOrdinal2PostOrderIndex: nodeOrdinal2PostOrderIndex};
},





_buildDominatorTree: function(postOrderIndex2NodeOrdinal, nodeOrdinal2PostOrderIndex)
{
var nodeFieldCount = this._nodeFieldCount;
var nodes = this._nodes;
var firstRetainerIndex = this._firstRetainerIndex;
var retainingNodes = this._retainingNodes;
var retainingEdges = this._retainingEdges;
var edgeFieldsCount = this._edgeFieldsCount;
var edgeTypeOffset = this._edgeTypeOffset;
var edgeToNodeOffset = this._edgeToNodeOffset;
var edgeShortcutType = this._edgeShortcutType;
var firstEdgeIndexes = this._firstEdgeIndexes;
var containmentEdges = this._containmentEdges;
var containmentEdgesLength = this._containmentEdges.length;
var rootNodeIndex = this._rootNodeIndex;

var flags = this._flags;
var flag = this._nodeFlags.pageObject;

var nodesCount = postOrderIndex2NodeOrdinal.length;
var rootPostOrderedIndex = nodesCount - 1;
var noEntry = nodesCount;
var dominators = new Uint32Array(nodesCount);
for (var i = 0; i < rootPostOrderedIndex; ++i)
dominators[i] = noEntry;
dominators[rootPostOrderedIndex] = rootPostOrderedIndex;



var affected = new Uint8Array(nodesCount);
var nodeOrdinal;

{ 
nodeOrdinal = this._rootNodeIndex / nodeFieldCount;
var beginEdgeToNodeFieldIndex = firstEdgeIndexes[nodeOrdinal] + edgeToNodeOffset;
var endEdgeToNodeFieldIndex = firstEdgeIndexes[nodeOrdinal + 1];
for (var toNodeFieldIndex = beginEdgeToNodeFieldIndex;
toNodeFieldIndex < endEdgeToNodeFieldIndex;
toNodeFieldIndex += edgeFieldsCount) {
var childNodeOrdinal = containmentEdges[toNodeFieldIndex] / nodeFieldCount;
affected[nodeOrdinal2PostOrderIndex[childNodeOrdinal]] = 1;
}
}

var changed = true;
while (changed) {
changed = false;
for (var postOrderIndex = rootPostOrderedIndex - 1; postOrderIndex >= 0; --postOrderIndex) {
if (affected[postOrderIndex] === 0)
continue;
affected[postOrderIndex] = 0;


if (dominators[postOrderIndex] === rootPostOrderedIndex)
continue;
nodeOrdinal = postOrderIndex2NodeOrdinal[postOrderIndex];
var nodeFlag = !!(flags[nodeOrdinal] & flag);
var newDominatorIndex = noEntry;
var beginRetainerIndex = firstRetainerIndex[nodeOrdinal];
var endRetainerIndex = firstRetainerIndex[nodeOrdinal + 1];
for (var retainerIndex = beginRetainerIndex; retainerIndex < endRetainerIndex; ++retainerIndex) {
var retainerEdgeIndex = retainingEdges[retainerIndex];
var retainerEdgeType = containmentEdges[retainerEdgeIndex + edgeTypeOffset];
var retainerNodeIndex = retainingNodes[retainerIndex];
if (retainerNodeIndex !== rootNodeIndex && retainerEdgeType === edgeShortcutType)
continue;
var retainerNodeOrdinal = retainerNodeIndex / nodeFieldCount;
var retainerNodeFlag = !!(flags[retainerNodeOrdinal] & flag);


if (retainerNodeIndex !== rootNodeIndex && nodeFlag && !retainerNodeFlag)
continue;
var retanerPostOrderIndex = nodeOrdinal2PostOrderIndex[retainerNodeOrdinal];
if (dominators[retanerPostOrderIndex] !== noEntry) {
if (newDominatorIndex === noEntry)
newDominatorIndex = retanerPostOrderIndex;
else {
while (retanerPostOrderIndex !== newDominatorIndex) {
while (retanerPostOrderIndex < newDominatorIndex)
retanerPostOrderIndex = dominators[retanerPostOrderIndex];
while (newDominatorIndex < retanerPostOrderIndex)
newDominatorIndex = dominators[newDominatorIndex];
}
}


if (newDominatorIndex === rootPostOrderedIndex)
break;
}
}
if (newDominatorIndex !== noEntry && dominators[postOrderIndex] !== newDominatorIndex) {
dominators[postOrderIndex] = newDominatorIndex;
changed = true;
nodeOrdinal = postOrderIndex2NodeOrdinal[postOrderIndex];
beginEdgeToNodeFieldIndex = firstEdgeIndexes[nodeOrdinal] + edgeToNodeOffset;
endEdgeToNodeFieldIndex = firstEdgeIndexes[nodeOrdinal + 1];
for (var toNodeFieldIndex = beginEdgeToNodeFieldIndex;
toNodeFieldIndex < endEdgeToNodeFieldIndex;
toNodeFieldIndex += edgeFieldsCount) {
var childNodeOrdinal = containmentEdges[toNodeFieldIndex] / nodeFieldCount;
affected[nodeOrdinal2PostOrderIndex[childNodeOrdinal]] = 1;
}
}
}
}

var dominatorsTree = new Uint32Array(nodesCount);
for (var postOrderIndex = 0, l = dominators.length; postOrderIndex < l; ++postOrderIndex) {
nodeOrdinal = postOrderIndex2NodeOrdinal[postOrderIndex];
dominatorsTree[nodeOrdinal] = postOrderIndex2NodeOrdinal[dominators[postOrderIndex]];
}
return dominatorsTree;
},

_calculateRetainedSizes: function(postOrderIndex2NodeOrdinal)
{
var nodeCount = this.nodeCount;
var nodes = this._nodes;
var nodeSelfSizeOffset = this._nodeSelfSizeOffset;
var nodeFieldCount = this._nodeFieldCount;
var dominatorsTree = this._dominatorsTree;

var nodeRetainedSizeOffset = this._nodeRetainedSizeOffset = this._nodeEdgeCountOffset;
delete this._nodeEdgeCountOffset;

for (var nodeIndex = 0, l = nodes.length; nodeIndex < l; nodeIndex += nodeFieldCount)
nodes[nodeIndex + nodeRetainedSizeOffset] = nodes[nodeIndex + nodeSelfSizeOffset];


for (var postOrderIndex = 0; postOrderIndex < nodeCount - 1; ++postOrderIndex) {
var nodeOrdinal = postOrderIndex2NodeOrdinal[postOrderIndex];
var nodeIndex = nodeOrdinal * nodeFieldCount;
var dominatorIndex = dominatorsTree[nodeOrdinal] * nodeFieldCount;
nodes[dominatorIndex + nodeRetainedSizeOffset] += nodes[nodeIndex + nodeRetainedSizeOffset];
}
},

_buildDominatedNodes: function()
{





var indexArray = this._firstDominatedNodeIndex = new Uint32Array(this.nodeCount + 1);

var dominatedNodes = this._dominatedNodes = new Uint32Array(this.nodeCount - 1);



var nodeFieldCount = this._nodeFieldCount;
var dominatorsTree = this._dominatorsTree;
for (var nodeOrdinal = 1, l = this.nodeCount; nodeOrdinal < l; ++nodeOrdinal)
++indexArray[dominatorsTree[nodeOrdinal]];


var firstDominatedNodeIndex = 0;
for (var i = 0, l = this.nodeCount; i < l; ++i) {
var dominatedCount = dominatedNodes[firstDominatedNodeIndex] = indexArray[i];
indexArray[i] = firstDominatedNodeIndex;
firstDominatedNodeIndex += dominatedCount;
}
indexArray[this.nodeCount] = dominatedNodes.length;


for (var nodeOrdinal = 1, l = this.nodeCount; nodeOrdinal < l; ++nodeOrdinal) {
var dominatorOrdinal = dominatorsTree[nodeOrdinal];
var dominatedRefIndex = indexArray[dominatorOrdinal];
dominatedRefIndex += (--dominatedNodes[dominatedRefIndex]);
dominatedNodes[dominatedRefIndex] = nodeOrdinal * nodeFieldCount;
}
},

_markInvisibleEdges: function()
{



for (var iter = this.rootNode().edges(); iter.hasNext(); iter.next()) {
var edge = iter.edge;
if (!edge.isShortcut())
continue;
var node = edge.node();
var propNames = {};
for (var innerIter = node.edges(); innerIter.hasNext(); innerIter.next()) {
var globalObjEdge = innerIter.edge;
if (globalObjEdge.isShortcut())
propNames[globalObjEdge._nameOrIndex()] = true;
}
for (innerIter.first(); innerIter.hasNext(); innerIter.next()) {
var globalObjEdge = innerIter.edge;
if (!globalObjEdge.isShortcut()
&& globalObjEdge.node().isHidden()
&& globalObjEdge._hasStringName()
&& (globalObjEdge._nameOrIndex() in propNames))
this._containmentEdges[globalObjEdge._edges._start + globalObjEdge.edgeIndex + this._edgeTypeOffset] = this._edgeInvisibleType;
}
}
},

_numbersComparator: function(a, b)
{
return a < b ? -1 : (a > b ? 1 : 0);
},

_markDetachedDOMTreeNodes: function()
{
var flag = this._nodeFlags.detachedDOMTreeNode;
var detachedDOMTreesRoot;
for (var iter = this.rootNode().edges(); iter.hasNext(); iter.next()) {
var node = iter.edge.node();
if (node.isDetachedDOMTreesRoot()) {
detachedDOMTreesRoot = node;
break;
}
}

if (!detachedDOMTreesRoot)
return;

for (var iter = detachedDOMTreesRoot.edges(); iter.hasNext(); iter.next()) {
var node = iter.edge.node();
if (node.isDetachedDOMTree()) {
for (var edgesIter = node.edges(); edgesIter.hasNext(); edgesIter.next())
this._flags[edgesIter.edge.node().nodeIndex / this._nodeFieldCount] |= flag;
}
}
},

_markPageOwnedNodes: function()
{
var edgeShortcutType = this._edgeShortcutType;
var edgeToNodeOffset = this._edgeToNodeOffset;
var edgeTypeOffset = this._edgeTypeOffset;
var edgeFieldsCount = this._edgeFieldsCount;
var edgeWeakType = this._edgeWeakType;
var firstEdgeIndexes = this._firstEdgeIndexes;
var containmentEdges = this._containmentEdges;
var containmentEdgesLength = containmentEdges.length;
var nodes = this._nodes;
var nodeFieldCount = this._nodeFieldCount;
var nodesCount = this.nodeCount;

var flags = this._flags;
var flag = this._nodeFlags.pageObject;
var visitedMarker = this._nodeFlags.visitedMarker;
var visitedMarkerMask = this._nodeFlags.visitedMarkerMask;
var markerAndFlag = visitedMarker | flag;

var nodesToVisit = new Uint32Array(nodesCount);
var nodesToVisitLength = 0;

var rootNodeOrdinal = this._rootNodeIndex / nodeFieldCount;
for (var edgeIndex = firstEdgeIndexes[rootNodeOrdinal], endEdgeIndex = firstEdgeIndexes[rootNodeOrdinal + 1];
edgeIndex < endEdgeIndex;
edgeIndex += edgeFieldsCount) {
if (containmentEdges[edgeIndex + edgeTypeOffset] === edgeShortcutType) {
var nodeOrdinal = containmentEdges[edgeIndex + edgeToNodeOffset] / nodeFieldCount;
nodesToVisit[nodesToVisitLength++] = nodeOrdinal;
flags[nodeOrdinal] |= visitedMarker;
}
}

while (nodesToVisitLength) {
var nodeOrdinal = nodesToVisit[--nodesToVisitLength];
flags[nodeOrdinal] |= flag;
flags[nodeOrdinal] &= visitedMarkerMask;
var beginEdgeIndex = firstEdgeIndexes[nodeOrdinal];
var endEdgeIndex = firstEdgeIndexes[nodeOrdinal + 1];
for (var edgeIndex = beginEdgeIndex; edgeIndex < endEdgeIndex; edgeIndex += edgeFieldsCount) {
var childNodeIndex = containmentEdges[edgeIndex + edgeToNodeOffset];
var childNodeOrdinal = childNodeIndex / nodeFieldCount;
if (flags[childNodeOrdinal] & markerAndFlag)
continue;
var type = containmentEdges[edgeIndex + edgeTypeOffset];
if (type === edgeWeakType)
continue;
nodesToVisit[nodesToVisitLength++] = childNodeOrdinal;
flags[childNodeOrdinal] |= visitedMarker;
}
}
},

_markQueriableHeapObjects: function()
{



var flag = this._nodeFlags.canBeQueried;
var hiddenEdgeType = this._edgeHiddenType;
var internalEdgeType = this._edgeInternalType;
var invisibleEdgeType = this._edgeInvisibleType;
var weakEdgeType = this._edgeWeakType;
var edgeToNodeOffset = this._edgeToNodeOffset;
var edgeTypeOffset = this._edgeTypeOffset;
var edgeFieldsCount = this._edgeFieldsCount;
var containmentEdges = this._containmentEdges;
var nodes = this._nodes;
var nodeCount = this.nodeCount;
var nodeFieldCount = this._nodeFieldCount;
var firstEdgeIndexes = this._firstEdgeIndexes;

var flags = this._flags;
var list = [];

for (var iter = this.rootNode().edges(); iter.hasNext(); iter.next()) {
if (iter.edge.node().isWindow())
list.push(iter.edge.node().nodeIndex / nodeFieldCount);
}

while (list.length) {
var nodeOrdinal = list.pop();
if (flags[nodeOrdinal] & flag)
continue;
flags[nodeOrdinal] |= flag;
var beginEdgeIndex = firstEdgeIndexes[nodeOrdinal];
var endEdgeIndex = firstEdgeIndexes[nodeOrdinal + 1];
for (var edgeIndex = beginEdgeIndex; edgeIndex < endEdgeIndex; edgeIndex += edgeFieldsCount) {
var childNodeIndex = containmentEdges[edgeIndex + edgeToNodeOffset];
var childNodeOrdinal = childNodeIndex / nodeFieldCount;
if (flags[childNodeOrdinal] & flag)
continue;
var type = containmentEdges[edgeIndex + edgeTypeOffset];
if (type === hiddenEdgeType || type === invisibleEdgeType || type === internalEdgeType || type === weakEdgeType)
continue;
list.push(childNodeOrdinal);
}
}
},

_calculateFlags: function()
{
this._flags = new Uint32Array(this.nodeCount);
this._markDetachedDOMTreeNodes();
this._markQueriableHeapObjects();
this._markPageOwnedNodes();
},

calculateSnapshotDiff: function(baseSnapshotId, baseSnapshotAggregates)
{
var snapshotDiff = this._snapshotDiffs[baseSnapshotId];
if (snapshotDiff)
return snapshotDiff;
snapshotDiff = {};

var aggregates = this.aggregates(true, "allObjects");
for (var className in baseSnapshotAggregates) {
var baseAggregate = baseSnapshotAggregates[className];
var diff = this._calculateDiffForClass(baseAggregate, aggregates[className]);
if (diff)
snapshotDiff[className] = diff;
}
var emptyBaseAggregate = { ids: [], indexes: [], selfSizes: [] };
for (var className in aggregates) {
if (className in baseSnapshotAggregates)
continue;
snapshotDiff[className] = this._calculateDiffForClass(emptyBaseAggregate, aggregates[className]);
}

this._snapshotDiffs[baseSnapshotId] = snapshotDiff;
return snapshotDiff;
},

_calculateDiffForClass: function(baseAggregate, aggregate)
{
var baseIds = baseAggregate.ids;
var baseIndexes = baseAggregate.indexes;
var baseSelfSizes = baseAggregate.selfSizes;

var indexes = aggregate ? aggregate.idxs : [];

var i = 0, l = baseIds.length;
var j = 0, m = indexes.length;
var diff = { addedCount: 0,
removedCount: 0,
addedSize: 0,
removedSize: 0,
deletedIndexes: [],
addedIndexes: [] };

var nodeB = new WebInspector.HeapSnapshotNode(this, indexes[j]);
while (i < l && j < m) {
var nodeAId = baseIds[i];
if (nodeAId < nodeB.id()) {
diff.deletedIndexes.push(baseIndexes[i]);
diff.removedCount++;
diff.removedSize += baseSelfSizes[i];
++i;
} else if (nodeAId > nodeB.id()) { 
diff.addedIndexes.push(indexes[j]);
diff.addedCount++;
diff.addedSize += nodeB.selfSize();
nodeB.nodeIndex = indexes[++j];
} else { 
++i;
nodeB.nodeIndex = indexes[++j];
}
}
while (i < l) {
diff.deletedIndexes.push(baseIndexes[i]);
diff.removedCount++;
diff.removedSize += baseSelfSizes[i];
++i;
}
while (j < m) {
diff.addedIndexes.push(indexes[j]);
diff.addedCount++;
diff.addedSize += nodeB.selfSize();
nodeB.nodeIndex = indexes[++j];
}
diff.countDelta = diff.addedCount - diff.removedCount;
diff.sizeDelta = diff.addedSize - diff.removedSize;
if (!diff.addedCount && !diff.removedCount)
return null;
return diff;
},

_nodeForSnapshotObjectId: function(snapshotObjectId)
{
for (var it = this._allNodes(); it.hasNext(); it.next()) {
if (it.node.id() === snapshotObjectId)
return it.node;
}
return null;
},

nodeClassName: function(snapshotObjectId)
{
var node = this._nodeForSnapshotObjectId(snapshotObjectId);
if (node)
return node.className();
return null;
},

dominatorIdsForNode: function(snapshotObjectId)
{
var node = this._nodeForSnapshotObjectId(snapshotObjectId);
if (!node)
return null;
var result = [];
while (!node.isRoot()) {
result.push(node.id());
node.nodeIndex = node.dominatorIndex();
}
return result;
},

_parseFilter: function(filter)
{
if (!filter)
return null;
var parsedFilter = eval("(function(){return " + filter + "})()");
return parsedFilter.bind(this);
},

createEdgesProvider: function(nodeIndex, filter)
{
var node = new WebInspector.HeapSnapshotNode(this, nodeIndex);
return new WebInspector.HeapSnapshotEdgesProvider(this, this._parseFilter(filter), node.edges());
},

createRetainingEdgesProvider: function(nodeIndex, filter)
{
var node = new WebInspector.HeapSnapshotNode(this, nodeIndex);
return new WebInspector.HeapSnapshotEdgesProvider(this, this._parseFilter(filter), node.retainers());
},

createAddedNodesProvider: function(baseSnapshotId, className)
{
var snapshotDiff = this._snapshotDiffs[baseSnapshotId];
var diffForClass = snapshotDiff[className];
return new WebInspector.HeapSnapshotNodesProvider(this, null, diffForClass.addedIndexes);
},

createDeletedNodesProvider: function(nodeIndexes)
{
return new WebInspector.HeapSnapshotNodesProvider(this, null, nodeIndexes);
},

createNodesProviderForClass: function(className, aggregatesKey)
{
function filter(node) {
return node.isPageObject();
}
return new WebInspector.HeapSnapshotNodesProvider(this, filter, this.aggregates(false, aggregatesKey)[className].idxs);
},

createNodesProviderForDominator: function(nodeIndex)
{
var node = new WebInspector.HeapSnapshotNode(this, nodeIndex);
return new WebInspector.HeapSnapshotNodesProvider(this, null, this._dominatedNodesOfNode(node));
},

updateStaticData: function()
{
return {nodeCount: this.nodeCount, rootNodeIndex: this._rootNodeIndex, totalSize: this.totalSize, uid: this.uid, nodeFlags: this._nodeFlags};
}
};


WebInspector.HeapSnapshotFilteredOrderedIterator = function(iterator, filter, unfilteredIterationOrder)
{
this._filter = filter;
this._iterator = iterator;
this._unfilteredIterationOrder = unfilteredIterationOrder;
this._iterationOrder = null;
this._position = 0;
this._currentComparator = null;
this._sortedPrefixLength = 0;
}

WebInspector.HeapSnapshotFilteredOrderedIterator.prototype = {
_createIterationOrder: function()
{
if (this._iterationOrder)
return;
if (this._unfilteredIterationOrder && !this._filter) {
this._iterationOrder = this._unfilteredIterationOrder.slice(0);
this._unfilteredIterationOrder = null;
return;
}
this._iterationOrder = [];
var iterator = this._iterator;
if (!this._unfilteredIterationOrder && !this._filter) {
for (iterator.first(); iterator.hasNext(); iterator.next())
this._iterationOrder.push(iterator.index());
} else if (!this._unfilteredIterationOrder) {
for (iterator.first(); iterator.hasNext(); iterator.next()) {
if (this._filter(iterator.item()))
this._iterationOrder.push(iterator.index());
}
} else {
var order = this._unfilteredIterationOrder.constructor === Array ?
this._unfilteredIterationOrder : this._unfilteredIterationOrder.slice(0);
for (var i = 0, l = order.length; i < l; ++i) {
iterator.setIndex(order[i]);
if (this._filter(iterator.item()))
this._iterationOrder.push(iterator.index());
}
this._unfilteredIterationOrder = null;
}
},

first: function()
{
this._position = 0;
},

hasNext: function()
{
return this._position < this._iterationOrder.length;
},

isEmpty: function()
{
if (this._iterationOrder)
return !this._iterationOrder.length;
if (this._unfilteredIterationOrder && !this._filter)
return !this._unfilteredIterationOrder.length;
var iterator = this._iterator;
if (!this._unfilteredIterationOrder && !this._filter) {
iterator.first();
return !iterator.hasNext();
} else if (!this._unfilteredIterationOrder) {
for (iterator.first(); iterator.hasNext(); iterator.next())
if (this._filter(iterator.item()))
return false;
} else {
var order = this._unfilteredIterationOrder.constructor === Array ?
this._unfilteredIterationOrder : this._unfilteredIterationOrder.slice(0);
for (var i = 0, l = order.length; i < l; ++i) {
iterator.setIndex(order[i]);
if (this._filter(iterator.item()))
return false;
}
}
return true;
},

item: function()
{
this._iterator.setIndex(this._iterationOrder[this._position]);
return this._iterator.item();
},

get length()
{
this._createIterationOrder();
return this._iterationOrder.length;
},

next: function()
{
++this._position;
},


serializeItemsRange: function(begin, end)
{
this._createIterationOrder();
if (begin > end)
throw new Error("Start position > end position: " + begin + " > " + end);
if (end >= this._iterationOrder.length)
end = this._iterationOrder.length;
if (this._sortedPrefixLength < end) {
this.sort(this._currentComparator, this._sortedPrefixLength, this._iterationOrder.length - 1, end - this._sortedPrefixLength);
this._sortedPrefixLength = end;
}

this._position = begin;
var startPosition = this._position;
var count = end - begin;
var result = new Array(count);
for (var i = 0 ; i < count && this.hasNext(); ++i, this.next())
result[i] = this.serializeItem(this.item());
result.length = i;
result.totalLength = this._iterationOrder.length;

result.startPosition = startPosition;
result.endPosition = this._position;
return result;
},

sortAll: function()
{
this._createIterationOrder();
if (this._sortedPrefixLength === this._iterationOrder.length)
return;
this.sort(this._currentComparator, this._sortedPrefixLength, this._iterationOrder.length - 1, this._iterationOrder.length);
this._sortedPrefixLength = this._iterationOrder.length;
},

sortAndRewind: function(comparator)
{
this._currentComparator = comparator;
this._sortedPrefixLength = 0;
this.first();
}
}

WebInspector.HeapSnapshotFilteredOrderedIterator.prototype.createComparator = function(fieldNames)
{
return {fieldName1:fieldNames[0], ascending1:fieldNames[1], fieldName2:fieldNames[2], ascending2:fieldNames[3]};
}


WebInspector.HeapSnapshotEdgesProvider = function(snapshot, filter, edgesIter)
{
this.snapshot = snapshot;
WebInspector.HeapSnapshotFilteredOrderedIterator.call(this, edgesIter, filter);
}

WebInspector.HeapSnapshotEdgesProvider.prototype = {
serializeItem: function(edge)
{
return {
name: edge.name(),
propertyAccessor: edge.toString(),
node: WebInspector.HeapSnapshotNodesProvider.prototype.serializeItem(edge.node()),
nodeIndex: edge.nodeIndex(),
type: edge.type(),
distanceToWindow: edge.node().distanceToWindow()
};
},

sort: function(comparator, leftBound, rightBound, count)
{
var fieldName1 = comparator.fieldName1;
var fieldName2 = comparator.fieldName2;
var ascending1 = comparator.ascending1;
var ascending2 = comparator.ascending2;

var edgeA = this._iterator.item().clone();
var edgeB = edgeA.clone();
var nodeA = new WebInspector.HeapSnapshotNode(this.snapshot);
var nodeB = new WebInspector.HeapSnapshotNode(this.snapshot);

function compareEdgeFieldName(ascending, indexA, indexB)
{
edgeA.edgeIndex = indexA;
edgeB.edgeIndex = indexB;
if (edgeB.name() === "__proto__") return -1;
if (edgeA.name() === "__proto__") return 1;
var result =
edgeA.hasStringName() === edgeB.hasStringName() ?
(edgeA.name() < edgeB.name() ? -1 : (edgeA.name() > edgeB.name() ? 1 : 0)) :
(edgeA.hasStringName() ? -1 : 1);
return ascending ? result : -result;
}

function compareNodeField(fieldName, ascending, indexA, indexB)
{
edgeA.edgeIndex = indexA;
nodeA.nodeIndex = edgeA.nodeIndex();
var valueA = nodeA[fieldName]();

edgeB.edgeIndex = indexB;
nodeB.nodeIndex = edgeB.nodeIndex();
var valueB = nodeB[fieldName]();

var result = valueA < valueB ? -1 : (valueA > valueB ? 1 : 0);
return ascending ? result : -result;
}

function compareEdgeAndNode(indexA, indexB) {
var result = compareEdgeFieldName(ascending1, indexA, indexB);
if (result === 0)
result = compareNodeField(fieldName2, ascending2, indexA, indexB);
return result;
}

function compareNodeAndEdge(indexA, indexB) {
var result = compareNodeField(fieldName1, ascending1, indexA, indexB);
if (result === 0)
result = compareEdgeFieldName(ascending2, indexA, indexB);
return result;
}

function compareNodeAndNode(indexA, indexB) {
var result = compareNodeField(fieldName1, ascending1, indexA, indexB);
if (result === 0)
result = compareNodeField(fieldName2, ascending2, indexA, indexB);
return result;
}

if (fieldName1 === "!edgeName")
this._iterationOrder.sortRange(compareEdgeAndNode, leftBound, rightBound, count);
else if (fieldName2 === "!edgeName")
this._iterationOrder.sortRange(compareNodeAndEdge, leftBound, rightBound, count);
else
this._iterationOrder.sortRange(compareNodeAndNode, leftBound, rightBound, count);
},

__proto__: WebInspector.HeapSnapshotFilteredOrderedIterator.prototype
}



WebInspector.HeapSnapshotNodesProvider = function(snapshot, filter, nodeIndexes)
{
this.snapshot = snapshot;
WebInspector.HeapSnapshotFilteredOrderedIterator.call(this, snapshot._allNodes(), filter, nodeIndexes);
}

WebInspector.HeapSnapshotNodesProvider.prototype = {
nodePosition: function(snapshotObjectId)
{
this._createIterationOrder();
if (this.isEmpty())
return -1;
this.sortAll();

var node = new WebInspector.HeapSnapshotNode(this.snapshot);
for (var i = 0; i < this._iterationOrder.length; i++) {
node.nodeIndex = this._iterationOrder[i];
if (node.id() === snapshotObjectId)
return i;
}
return -1;
},

serializeItem: function(node)
{
return {
id: node.id(),
name: node.name(),
distanceToWindow: node.distanceToWindow(),
nodeIndex: node.nodeIndex,
retainedSize: node.retainedSize(),
selfSize: node.selfSize(),
type: node.type(),
flags: node.flags()
};
},

sort: function(comparator, leftBound, rightBound, count)
{
var fieldName1 = comparator.fieldName1;
var fieldName2 = comparator.fieldName2;
var ascending1 = comparator.ascending1;
var ascending2 = comparator.ascending2;

var nodeA = new WebInspector.HeapSnapshotNode(this.snapshot);
var nodeB = new WebInspector.HeapSnapshotNode(this.snapshot);

function sortByNodeField(fieldName, ascending)
{
var valueOrFunctionA = nodeA[fieldName];
var valueA = typeof valueOrFunctionA !== "function" ? valueOrFunctionA : valueOrFunctionA.call(nodeA);
var valueOrFunctionB = nodeB[fieldName];
var valueB = typeof valueOrFunctionB !== "function" ? valueOrFunctionB : valueOrFunctionB.call(nodeB);
var result = valueA < valueB ? -1 : (valueA > valueB ? 1 : 0);
return ascending ? result : -result;
}

function sortByComparator(indexA, indexB) {
nodeA.nodeIndex = indexA;
nodeB.nodeIndex = indexB;
var result = sortByNodeField(fieldName1, ascending1);
if (result === 0)
result = sortByNodeField(fieldName2, ascending2);
return result;
}

this._iterationOrder.sortRange(sortByComparator, leftBound, rightBound, count);
},

__proto__: WebInspector.HeapSnapshotFilteredOrderedIterator.prototype
}

;



WebInspector.HeapSnapshotSortableDataGrid = function(columns)
{
WebInspector.DataGrid.call(this, columns);


this._recursiveSortingDepth = 0;

this._highlightedNode = null;

this._populatedAndSorted = false;
this.addEventListener("sorting complete", this._sortingComplete, this);
this.addEventListener("sorting changed", this.sortingChanged, this);
}

WebInspector.HeapSnapshotSortableDataGrid.Events = {
ContentShown: "ContentShown"
}

WebInspector.HeapSnapshotSortableDataGrid.prototype = {

defaultPopulateCount: function()
{
return 100;
},

dispose: function()
{
var children = this.topLevelNodes();
for (var i = 0, l = children.length; i < l; ++i)
children[i].dispose();
},


wasShown: function()
{
if (this._populatedAndSorted)
this.dispatchEventToListeners(WebInspector.HeapSnapshotSortableDataGrid.Events.ContentShown, this);
},

_sortingComplete: function()
{
this.removeEventListener("sorting complete", this._sortingComplete, this);
this._populatedAndSorted = true;
this.dispatchEventToListeners(WebInspector.HeapSnapshotSortableDataGrid.Events.ContentShown, this);
},


willHide: function()
{
this._clearCurrentHighlight();
},


populateContextMenu: function(profilesPanel, contextMenu, event)
{
var td = event.target.enclosingNodeOrSelfWithNodeName("td");
if (!td)
return;
var node = td.heapSnapshotNode;
if (node instanceof WebInspector.HeapSnapshotInstanceNode || node instanceof WebInspector.HeapSnapshotObjectNode) {
function revealInDominatorsView()
{
profilesPanel.showObject(node.snapshotNodeId, "Dominators");
}
contextMenu.appendItem(WebInspector.UIString("Reveal in Dominators View"), revealInDominatorsView.bind(this));
} else if (node instanceof WebInspector.HeapSnapshotDominatorObjectNode) {
function revealInSummaryView()
{
profilesPanel.showObject(node.snapshotNodeId, "Summary");
}
contextMenu.appendItem(WebInspector.UIString("Reveal in Summary View"), revealInSummaryView.bind(this));
}
},

resetSortingCache: function()
{
delete this._lastSortColumnIdentifier;
delete this._lastSortAscending;
},

topLevelNodes: function()
{
return this.rootNode().children;
},


highlightObjectByHeapSnapshotId: function(heapSnapshotObjectId)
{
},


highlightNode: function(node)
{
var prevNode = this._highlightedNode;
this._clearCurrentHighlight();
this._highlightedNode = node;
this._highlightedNode.element.addStyleClass("highlighted-row");

if (node === prevNode) {
var element = node.element;
var parent = element.parentElement;
var nextSibling = element.nextSibling;
parent.removeChild(element);
parent.insertBefore(element, nextSibling);
}
},

nodeWasDetached: function(node)
{
if (this._highlightedNode === node)
this._clearCurrentHighlight();
},

_clearCurrentHighlight: function()
{
if (!this._highlightedNode)
return
this._highlightedNode.element.removeStyleClass("highlighted-row");
this._highlightedNode = null;
},

changeNameFilter: function(filter)
{
filter = filter.toLowerCase();
var children = this.topLevelNodes();
for (var i = 0, l = children.length; i < l; ++i) {
var node = children[i];
if (node.depth === 0)
node.revealed = node._name.toLowerCase().indexOf(filter) !== -1;
}
this.updateVisibleNodes();
},

sortingChanged: function()
{
var sortAscending = this.sortOrder === "ascending";
var sortColumnIdentifier = this.sortColumnIdentifier;
if (this._lastSortColumnIdentifier === sortColumnIdentifier && this._lastSortAscending === sortAscending)
return;
this._lastSortColumnIdentifier = sortColumnIdentifier;
this._lastSortAscending = sortAscending;
var sortFields = this._sortFields(sortColumnIdentifier, sortAscending);

function SortByTwoFields(nodeA, nodeB)
{
var field1 = nodeA[sortFields[0]];
var field2 = nodeB[sortFields[0]];
var result = field1 < field2 ? -1 : (field1 > field2 ? 1 : 0);
if (!sortFields[1])
result = -result;
if (result !== 0)
return result;
field1 = nodeA[sortFields[2]];
field2 = nodeB[sortFields[2]];
result = field1 < field2 ? -1 : (field1 > field2 ? 1 : 0);
if (!sortFields[3])
result = -result;
return result;
}
this._performSorting(SortByTwoFields);
},

_performSorting: function(sortFunction)
{
this.recursiveSortingEnter();
var children = this._topLevelNodes;
this.rootNode().removeChildren();
children.sort(sortFunction);
for (var i = 0, l = children.length; i < l; ++i) {
var child = children[i];
this.appendChildAfterSorting(child);
if (child.expanded)
child.sort();
}
this.updateVisibleNodes();
this.recursiveSortingLeave();
},

appendChildAfterSorting: function(child)
{
var revealed = child.revealed;
this.rootNode().appendChild(child);
child.revealed = revealed;
},

updateVisibleNodes: function()
{
},

recursiveSortingEnter: function()
{
++this._recursiveSortingDepth;
},

recursiveSortingLeave: function()
{
if (!this._recursiveSortingDepth)
return;
if (!--this._recursiveSortingDepth)
this.dispatchEventToListeners("sorting complete");
},

__proto__: WebInspector.DataGrid.prototype
}




WebInspector.HeapSnapshotViewportDataGrid = function(columns)
{
WebInspector.HeapSnapshotSortableDataGrid.call(this, columns);
this.scrollContainer.addEventListener("scroll", this._onScroll.bind(this), true);
this._topLevelNodes = [];
this._topPadding = new WebInspector.HeapSnapshotPaddingNode();
this._bottomPadding = new WebInspector.HeapSnapshotPaddingNode();

this._nodeToHighlightAfterScroll = null;
}

WebInspector.HeapSnapshotViewportDataGrid.prototype = {
topLevelNodes: function()
{
return this._topLevelNodes;
},

appendChildAfterSorting: function(child)
{

},

updateVisibleNodes: function()
{
var scrollTop = this.scrollContainer.scrollTop;

var viewPortHeight = this.scrollContainer.offsetHeight;

this._removePaddingRows();

var children = this._topLevelNodes;

var i = 0;
var topPadding = 0;
while (i < children.length) {
if (children[i].revealed) {
var newTop = topPadding + children[i].nodeHeight();
if (newTop > scrollTop)
break;
topPadding = newTop;
}
++i;
}

this.rootNode().removeChildren();

var heightToFill = viewPortHeight + (scrollTop - topPadding);
var filledHeight = 0;
while (i < children.length && filledHeight < heightToFill) {
if (children[i].revealed) {
this.rootNode().appendChild(children[i]);
filledHeight += children[i].nodeHeight();
}
++i;
}

var bottomPadding = 0;
while (i < children.length) {
bottomPadding += children[i].nodeHeight();
++i;
}

this._addPaddingRows(topPadding, bottomPadding);
},

appendTopLevelNode: function(node)
{
this._topLevelNodes.push(node);
},

removeTopLevelNodes: function()
{
this.rootNode().removeChildren();
this._topLevelNodes = [];
},


highlightNode: function(node)
{
if (this._isScrolledIntoView(node.element))
WebInspector.HeapSnapshotSortableDataGrid.prototype.highlightNode.call(this, node);
else {
node.element.scrollIntoViewIfNeeded(true);
this._nodeToHighlightAfterScroll = node;
}
},

_isScrolledIntoView: function(element)
{
var viewportTop = this.scrollContainer.scrollTop;
var viewportBottom = viewportTop + this.scrollContainer.clientHeight;
var elemTop = element.offsetTop
var elemBottom = elemTop + element.offsetHeight;
return elemBottom <= viewportBottom && elemTop >= viewportTop;
},

_addPaddingRows: function(top, bottom)
{
if (this._topPadding.element.parentNode !== this.dataTableBody)
this.dataTableBody.insertBefore(this._topPadding.element, this.dataTableBody.firstChild);
if (this._bottomPadding.element.parentNode !== this.dataTableBody)
this.dataTableBody.insertBefore(this._bottomPadding.element, this.dataTableBody.lastChild);
this._topPadding.setHeight(top);
this._bottomPadding.setHeight(bottom);
},

_removePaddingRows: function()
{
this._bottomPadding.removeFromTable();
this._topPadding.removeFromTable();
},

onResize: function()
{
WebInspector.HeapSnapshotSortableDataGrid.prototype.onResize.call(this);
this.updateVisibleNodes();
},

_onScroll: function(event)
{
this.updateVisibleNodes();

if (this._nodeToHighlightAfterScroll) {
WebInspector.HeapSnapshotSortableDataGrid.prototype.highlightNode.call(this, this._nodeToHighlightAfterScroll);
this._nodeToHighlightAfterScroll = null;
}
},

__proto__: WebInspector.HeapSnapshotSortableDataGrid.prototype
}


WebInspector.HeapSnapshotPaddingNode = function()
{
this.element = document.createElement("tr");
this.element.addStyleClass("revealed");
}

WebInspector.HeapSnapshotPaddingNode.prototype = {
setHeight: function(height)
{
this.element.style.height = height + "px";
},
removeFromTable: function()
{
var parent = this.element.parentNode;
if (parent)
parent.removeChild(this.element);
}
}



WebInspector.HeapSnapshotContainmentDataGrid = function(columns)
{
columns = columns || {
object: { title: WebInspector.UIString("Object"), disclosure: true, sortable: true },
shallowSize: { title: WebInspector.UIString("Shallow Size"), width: "120px", sortable: true },
retainedSize: { title: WebInspector.UIString("Retained Size"), width: "120px", sortable: true, sort: "descending" }
};
WebInspector.HeapSnapshotSortableDataGrid.call(this, columns);
}

WebInspector.HeapSnapshotContainmentDataGrid.prototype = {
setDataSource: function(snapshotView, snapshot, nodeIndex)
{
this.snapshotView = snapshotView;
this.snapshot = snapshot;
var node = new WebInspector.HeapSnapshotNode(snapshot, nodeIndex || snapshot.rootNodeIndex);
var fakeEdge = { node: node };
this.setRootNode(new WebInspector.HeapSnapshotObjectNode(this, false, fakeEdge, null));
this.rootNode().sort();
},

sortingChanged: function()
{
this.rootNode().sort();
},

__proto__: WebInspector.HeapSnapshotSortableDataGrid.prototype
}



WebInspector.HeapSnapshotRetainmentDataGrid = function()
{
this.showRetainingEdges = true;
var columns = {
object: { title: WebInspector.UIString("Object"), disclosure: true, sortable: true },
shallowSize: { title: WebInspector.UIString("Shallow Size"), width: "120px", sortable: true },
retainedSize: { title: WebInspector.UIString("Retained Size"), width: "120px", sortable: true },
distanceToWindow: { title: WebInspector.UIString("Distance"), width: "80px", sortable: true, sort: "ascending" }
};
WebInspector.HeapSnapshotContainmentDataGrid.call(this, columns);
}

WebInspector.HeapSnapshotRetainmentDataGrid.prototype = {
_sortFields: function(sortColumn, sortAscending)
{
return {
object: ["_name", sortAscending, "_count", false],
count: ["_count", sortAscending, "_name", true],
shallowSize: ["_shallowSize", sortAscending, "_name", true],
retainedSize: ["_retainedSize", sortAscending, "_name", true],
distanceToWindow: ["_distanceToWindow", sortAscending, "_name", true]
}[sortColumn];
},

reset: function()
{
this.rootNode().removeChildren();
this.resetSortingCache();
},

__proto__: WebInspector.HeapSnapshotContainmentDataGrid.prototype
}



WebInspector.HeapSnapshotConstructorsDataGrid = function()
{
var columns = {
object: { title: WebInspector.UIString("Constructor"), disclosure: true, sortable: true },
distanceToWindow: { title: WebInspector.UIString("Distance"), width: "90px", sortable: true },
count: { title: WebInspector.UIString("Objects Count"), width: "90px", sortable: true },
shallowSize: { title: WebInspector.UIString("Shallow Size"), width: "120px", sortable: true },
retainedSize: { title: WebInspector.UIString("Retained Size"), width: "120px", sort: "descending", sortable: true }
};
WebInspector.HeapSnapshotViewportDataGrid.call(this, columns);
this._profileIndex = -1;
this._topLevelNodes = [];

this._objectIdToSelect = null;
}

WebInspector.HeapSnapshotConstructorsDataGrid.prototype = {
_sortFields: function(sortColumn, sortAscending)
{
return {
object: ["_name", sortAscending, "_count", false],
distanceToWindow: ["_distanceToWindow", sortAscending, "_retainedSize", true],
count: ["_count", sortAscending, "_name", true],
shallowSize: ["_shallowSize", sortAscending, "_name", true],
retainedSize: ["_retainedSize", sortAscending, "_name", true]
}[sortColumn];
},


highlightObjectByHeapSnapshotId: function(id)
{
if (!this.snapshot) {
this._objectIdToSelect = id;
return;
}

function didGetClassName(className)
{
var constructorNodes = this.topLevelNodes();
for (var i = 0; i < constructorNodes.length; i++) {
var parent = constructorNodes[i];
if (parent._name === className) {
parent.revealNodeBySnapshotObjectId(parseInt(id, 10));
return;
}
}
}
this.snapshot.nodeClassName(parseInt(id, 10), didGetClassName.bind(this));
},

setDataSource: function(snapshotView, snapshot)
{
this.snapshotView = snapshotView;
this.snapshot = snapshot;
if (this._profileIndex === -1)
this._populateChildren();

if (this._objectIdToSelect) {
this.highlightObjectByHeapSnapshotId(this._objectIdToSelect);
this._objectIdToSelect = null;
}
},

_aggregatesReceived: function(key, aggregates)
{
for (var constructor in aggregates)
this.appendTopLevelNode(new WebInspector.HeapSnapshotConstructorNode(this, constructor, aggregates[constructor], key));
this.sortingChanged();
},

_populateChildren: function()
{

this.dispose();
this.removeTopLevelNodes();
this.resetSortingCache();

var key = this._profileIndex === -1 ? "allObjects" : this._minNodeId + ".." + this._maxNodeId;
var filter = this._profileIndex === -1 ? null : "function(node) { var id = node.id(); return id > " + this._minNodeId + " && id <= " + this._maxNodeId + "; }";

this.snapshot.aggregates(false, key, filter, this._aggregatesReceived.bind(this, key));
},

_filterSelectIndexChanged: function(profiles, profileIndex)
{
this._profileIndex = profileIndex;

delete this._maxNodeId;
delete this._minNodeId;

if (this._profileIndex !== -1) {
this._minNodeId = profileIndex > 0 ? profiles[profileIndex - 1].maxJSObjectId : 0;
this._maxNodeId = profiles[profileIndex].maxJSObjectId;
}

this._populateChildren();
},

__proto__: WebInspector.HeapSnapshotViewportDataGrid.prototype
}



WebInspector.HeapSnapshotDiffDataGrid = function()
{
var columns = {
object: { title: WebInspector.UIString("Constructor"), disclosure: true, sortable: true },
addedCount: { title: WebInspector.UIString("# New"), width: "72px", sortable: true },
removedCount: { title: WebInspector.UIString("# Deleted"), width: "72px", sortable: true },
countDelta: { title: "# Delta", width: "64px", sortable: true },
addedSize: { title: WebInspector.UIString("Alloc. Size"), width: "72px", sortable: true, sort: "descending" },
removedSize: { title: WebInspector.UIString("Freed Size"), width: "72px", sortable: true },
sizeDelta: { title: "Size Delta", width: "72px", sortable: true }
};
WebInspector.HeapSnapshotViewportDataGrid.call(this, columns);
}

WebInspector.HeapSnapshotDiffDataGrid.prototype = {

defaultPopulateCount: function()
{
return 50;
},

_sortFields: function(sortColumn, sortAscending)
{
return {
object: ["_name", sortAscending, "_count", false],
addedCount: ["_addedCount", sortAscending, "_name", true],
removedCount: ["_removedCount", sortAscending, "_name", true],
countDelta: ["_countDelta", sortAscending, "_name", true],
addedSize: ["_addedSize", sortAscending, "_name", true],
removedSize: ["_removedSize", sortAscending, "_name", true],
sizeDelta: ["_sizeDelta", sortAscending, "_name", true]
}[sortColumn];
},

setDataSource: function(snapshotView, snapshot)
{
this.snapshotView = snapshotView;
this.snapshot = snapshot;
},


setBaseDataSource: function(baseSnapshot)
{
this.baseSnapshot = baseSnapshot;
this.dispose();
this.removeTopLevelNodes();
this.resetSortingCache();
if (this.baseSnapshot === this.snapshot) {
this.dispatchEventToListeners("sorting complete");
return;
}
this._populateChildren();
},

_populateChildren: function()
{
function aggregatesForDiffReceived(aggregatesForDiff)
{
this.snapshot.calculateSnapshotDiff(this.baseSnapshot.uid, aggregatesForDiff, didCalculateSnapshotDiff.bind(this));
function didCalculateSnapshotDiff(diffByClassName)
{
for (var className in diffByClassName) {
var diff = diffByClassName[className];
this.appendTopLevelNode(new WebInspector.HeapSnapshotDiffNode(this, className, diff));
}
this.sortingChanged();
}
}



this.baseSnapshot.aggregatesForDiff(aggregatesForDiffReceived.bind(this));
},

__proto__: WebInspector.HeapSnapshotViewportDataGrid.prototype
}



WebInspector.HeapSnapshotDominatorsDataGrid = function()
{
var columns = {
object: { title: WebInspector.UIString("Object"), disclosure: true, sortable: true },
shallowSize: { title: WebInspector.UIString("Shallow Size"), width: "120px", sortable: true },
retainedSize: { title: WebInspector.UIString("Retained Size"), width: "120px", sort: "descending", sortable: true }
};
WebInspector.HeapSnapshotSortableDataGrid.call(this, columns);
this._objectIdToSelect = null;
}

WebInspector.HeapSnapshotDominatorsDataGrid.prototype = {

defaultPopulateCount: function()
{
return 25;
},

setDataSource: function(snapshotView, snapshot)
{
this.snapshotView = snapshotView;
this.snapshot = snapshot;

var fakeNode = { nodeIndex: this.snapshot.rootNodeIndex };
this.setRootNode(new WebInspector.HeapSnapshotDominatorObjectNode(this, fakeNode));
this.rootNode().sort();

if (this._objectIdToSelect) {
this.highlightObjectByHeapSnapshotId(this._objectIdToSelect);
this._objectIdToSelect = null;
}
},

sortingChanged: function()
{
this.rootNode().sort();
},


highlightObjectByHeapSnapshotId: function(id)
{
if (!this.snapshot) {
this._objectIdToSelect = id;
return;
}

function didGetDominators(dominatorIds)
{
if (!dominatorIds) {
WebInspector.log(WebInspector.UIString("Cannot find corresponding heap snapshot node"));
return;
}
var dominatorNode = this.rootNode();
expandNextDominator.call(this, dominatorIds, dominatorNode);
}

function expandNextDominator(dominatorIds, dominatorNode)
{
if (!dominatorNode) {
console.error("Cannot find dominator node");
return;
}
if (!dominatorIds.length) {
this.highlightNode(dominatorNode);
dominatorNode.element.scrollIntoViewIfNeeded(true);
return;
}
var snapshotObjectId = dominatorIds.pop();
dominatorNode.retrieveChildBySnapshotObjectId(snapshotObjectId, expandNextDominator.bind(this, dominatorIds));
}

this.snapshot.dominatorIdsForNode(parseInt(id, 10), didGetDominators.bind(this));
},

__proto__: WebInspector.HeapSnapshotSortableDataGrid.prototype
}

;



WebInspector.HeapSnapshotGridNode = function(tree, hasChildren)
{
WebInspector.DataGridNode.call(this, null, hasChildren);
this._dataGrid = tree;
this._instanceCount = 0;

this._savedChildren = null;

this._retrievedChildrenRanges = [];
this.addEventListener("populate", this._populate, this);
}

WebInspector.HeapSnapshotGridNode.prototype = {

createProvider: function()
{
throw new Error("Needs implemented.");
},


_provider: function()
{
if (!this._providerObject)
this._providerObject = this.createProvider();
return this._providerObject;
},

createCell: function(columnIdentifier)
{
var cell = WebInspector.DataGridNode.prototype.createCell.call(this, columnIdentifier);
if (this._searchMatched)
cell.addStyleClass("highlight");
return cell;
},

collapse: function()
{
WebInspector.DataGridNode.prototype.collapse.call(this);
this._dataGrid.updateVisibleNodes();
},

dispose: function()
{
if (this._provider())
this._provider().dispose();
for (var node = this.children[0]; node; node = node.traverseNextNode(true, this, true))
if (node.dispose)
node.dispose();
},

_reachableFromWindow: false,

queryObjectContent: function(callback)
{
},


wasDetached: function()
{
this._dataGrid.nodeWasDetached(this);
},

_toPercentString: function(num)
{
return num.toFixed(0) + "\u2009%"; 
},


childForPosition: function(nodePosition)
{
var indexOfFirsChildInRange = 0;
for (var i = 0; i < this._retrievedChildrenRanges.length; i++) {
var range = this._retrievedChildrenRanges[i];
if (range.from <= nodePosition && nodePosition < range.to) {
var childIndex = indexOfFirsChildInRange + nodePosition - range.from;
return this.children[childIndex];
}
indexOfFirsChildInRange += range.to - range.from + 1;
}
return null;
},

_createValueCell: function(columnIdentifier)
{
var cell = document.createElement("td");
cell.className = columnIdentifier + "-column";
if (this.dataGrid.snapshot.totalSize !== 0) {
var div = document.createElement("div");
var valueSpan = document.createElement("span");
valueSpan.textContent = this.data[columnIdentifier];
div.appendChild(valueSpan);
var percentColumn = columnIdentifier + "-percent";
if (percentColumn in this.data) {
var percentSpan = document.createElement("span");
percentSpan.className = "percent-column";
percentSpan.textContent = this.data[percentColumn];
div.appendChild(percentSpan);
}
cell.appendChild(div);
}
return cell;
},

_populate: function(event)
{
this.removeEventListener("populate", this._populate, this);
function sorted()
{
this._populateChildren();
}
this._provider().sortAndRewind(this.comparator(), sorted.bind(this));
},

expandWithoutPopulate: function(callback)
{

this.removeEventListener("populate", this._populate, this);
this.expand();
this._provider().sortAndRewind(this.comparator(), callback);
},


_populateChildren: function(fromPosition, toPosition, afterPopulate)
{
fromPosition = fromPosition || 0;
toPosition = toPosition || fromPosition + this._dataGrid.defaultPopulateCount();
var firstNotSerializedPosition = fromPosition;
function serializeNextChunk()
{
if (firstNotSerializedPosition >= toPosition)
return;
var end = Math.min(firstNotSerializedPosition + this._dataGrid.defaultPopulateCount(), toPosition);
this._provider().serializeItemsRange(firstNotSerializedPosition, end, childrenRetrieved.bind(this));
firstNotSerializedPosition = end;
}
function insertRetrievedChild(item, insertionIndex)
{
if (this._savedChildren) {
var hash = this._childHashForEntity(item);
if (hash in this._savedChildren) {
this.insertChild(this._savedChildren[hash], insertionIndex);
return;
}
}
this.insertChild(this._createChildNode(item), insertionIndex);
}
function insertShowMoreButton(from, to, insertionIndex)
{
var button = new WebInspector.ShowMoreDataGridNode(this._populateChildren.bind(this), from, to, this._dataGrid.defaultPopulateCount());
this.insertChild(button, insertionIndex);
}
function childrenRetrieved(items)
{
var itemIndex = 0;
var itemPosition = items.startPosition;
var insertionIndex = 0;

if (!this._retrievedChildrenRanges.length) {
if (items.startPosition > 0) {
this._retrievedChildrenRanges.push({from: 0, to: 0});
insertShowMoreButton.call(this, 0, items.startPosition, insertionIndex++);
}
this._retrievedChildrenRanges.push({from: items.startPosition, to: items.endPosition});
for (var i = 0, l = items.length; i < l; ++i)
insertRetrievedChild.call(this, items[i], insertionIndex++);
if (items.endPosition < items.totalLength)
insertShowMoreButton.call(this, items.endPosition, items.totalLength, insertionIndex++);
} else {
var rangeIndex = 0;
var found = false;
var range;
while (rangeIndex < this._retrievedChildrenRanges.length) {
range = this._retrievedChildrenRanges[rangeIndex];
if (range.to >= itemPosition) {
found = true;
break;
}
insertionIndex += range.to - range.from;

if (range.to < items.totalLength)
insertionIndex += 1;
++rangeIndex;
}

if (!found || items.startPosition < range.from) {

this.children[insertionIndex - 1].setEndPosition(items.startPosition);
insertShowMoreButton.call(this, items.startPosition, found ? range.from : items.totalLength, insertionIndex);
range = {from: items.startPosition, to: items.startPosition};
if (!found)
rangeIndex = this._retrievedChildrenRanges.length;
this._retrievedChildrenRanges.splice(rangeIndex, 0, range);
} else {
insertionIndex += itemPosition - range.from;
}




while (range.to < items.endPosition) {

var skipCount = range.to - itemPosition;
insertionIndex += skipCount;
itemIndex += skipCount;
itemPosition = range.to;


var nextRange = this._retrievedChildrenRanges[rangeIndex + 1];
var newEndOfRange = nextRange ? nextRange.from : items.totalLength;
if (newEndOfRange > items.endPosition)
newEndOfRange = items.endPosition;
while (itemPosition < newEndOfRange) {
insertRetrievedChild.call(this, items[itemIndex++], insertionIndex++);
++itemPosition;
}

if (nextRange && newEndOfRange === nextRange.from) {
range.to = nextRange.to;

this.removeChild(this.children[insertionIndex]);
this._retrievedChildrenRanges.splice(rangeIndex + 1, 1);
} else {
range.to = newEndOfRange;

if (newEndOfRange === items.totalLength)
this.removeChild(this.children[insertionIndex]);
else
this.children[insertionIndex].setStartPosition(items.endPosition);
}
}
}


this._instanceCount += items.length;
if (firstNotSerializedPosition < toPosition) {
serializeNextChunk.call(this);
return;
}

if (afterPopulate)
afterPopulate();
this.dispatchEventToListeners("populate complete");
}
serializeNextChunk.call(this);
},

_saveChildren: function()
{
this._savedChildren = null;
for (var i = 0, childrenCount = this.children.length; i < childrenCount; ++i) {
var child = this.children[i];
if (!child.expanded)
continue;
if (!this._savedChildren)
this._savedChildren = {};
this._savedChildren[this._childHashForNode(child)] = child;
}
},

sort: function()
{
this._dataGrid.recursiveSortingEnter();
function afterSort()
{
this._saveChildren();
this.removeChildren();
this._retrievedChildrenRanges = [];

function afterPopulate()
{
for (var i = 0, l = this.children.length; i < l; ++i) {
var child = this.children[i];
if (child.expanded)
child.sort();
}
this._dataGrid.recursiveSortingLeave();
}
var instanceCount = this._instanceCount;
this._instanceCount = 0;
this._populateChildren(0, instanceCount, afterPopulate.bind(this));
}

this._provider().sortAndRewind(this.comparator(), afterSort.bind(this));
},

__proto__: WebInspector.DataGridNode.prototype
}



WebInspector.HeapSnapshotGenericObjectNode = function(tree, node)
{
this.snapshotNodeIndex = 0;
WebInspector.HeapSnapshotGridNode.call(this, tree, false);

if (!node)
return;
this._name = node.name;
this._type = node.type;
this._distanceToWindow = node.distanceToWindow;
this._shallowSize = node.selfSize;
this._retainedSize = node.retainedSize;
this.snapshotNodeId = node.id;
this.snapshotNodeIndex = node.nodeIndex;
if (this._type === "string")
this._reachableFromWindow = true;
else if (this._type === "object" && this.isWindow(this._name)) {
this._name = this.shortenWindowURL(this._name, false);
this._reachableFromWindow = true;
} else if (node.flags & tree.snapshot.nodeFlags.canBeQueried)
this._reachableFromWindow = true;
if (node.flags & tree.snapshot.nodeFlags.detachedDOMTreeNode)
this.detachedDOMTreeNode = true;
};

WebInspector.HeapSnapshotGenericObjectNode.prototype = {
createCell: function(columnIdentifier)
{
var cell = columnIdentifier !== "object" ? this._createValueCell(columnIdentifier) : this._createObjectCell();
if (this._searchMatched)
cell.addStyleClass("highlight");
return cell;
},

_createObjectCell: function()
{
var cell = document.createElement("td");
cell.className = "object-column";
var div = document.createElement("div");
div.className = "source-code event-properties";
div.style.overflow = "visible";
var data = this.data["object"];
if (this._prefixObjectCell)
this._prefixObjectCell(div, data);
var valueSpan = document.createElement("span");
valueSpan.className = "value console-formatted-" + data.valueStyle;
valueSpan.textContent = data.value;
div.appendChild(valueSpan);
var idSpan = document.createElement("span");
idSpan.className = "console-formatted-id";
idSpan.textContent = " @" + data["nodeId"];
div.appendChild(idSpan);
if (this._postfixObjectCell)
this._postfixObjectCell(div, data);
cell.appendChild(div);
cell.addStyleClass("disclosure");
if (this.depth)
cell.style.setProperty("padding-left", (this.depth * this.dataGrid.indentWidth) + "px");
cell.heapSnapshotNode = this;
return cell;
},

get data()
{
var data = this._emptyData();

var value = this._name;
var valueStyle = "object";
switch (this._type) {
case "string":
value = "\"" + value + "\"";
valueStyle = "string";
break;
case "regexp":
value = "/" + value + "/";
valueStyle = "string";
break;
case "closure":
value = "function" + (value ? " " : "") + value + "()";
valueStyle = "function";
break;
case "number":
valueStyle = "number";
break;
case "hidden":
valueStyle = "null";
break;
case "array":
if (!value)
value = "[]";
else
value += "[]";
break;
};
if (this._reachableFromWindow)
valueStyle += " highlight";
if (value === "Object")
value = "";
if (this.detachedDOMTreeNode)
valueStyle += " detached-dom-tree-node";
data["object"] = { valueStyle: valueStyle, value: value, nodeId: this.snapshotNodeId };

var view = this.dataGrid.snapshotView;
data["distanceToWindow"] =  this._distanceToWindow;
data["shallowSize"] = Number.withThousandsSeparator(this._shallowSize);
data["retainedSize"] = Number.withThousandsSeparator(this._retainedSize);
data["shallowSize-percent"] = this._toPercentString(this._shallowSizePercent);
data["retainedSize-percent"] = this._toPercentString(this._retainedSizePercent);

return this._enhanceData ? this._enhanceData(data) : data;
},

queryObjectContent: function(callback, objectGroupName)
{
if (this._type === "string")
callback(WebInspector.RemoteObject.fromPrimitiveValue(this._name));
else {
function formatResult(error, object)
{
if (!error && object.type)
callback(WebInspector.RemoteObject.fromPayload(object), !!error);
else
callback(WebInspector.RemoteObject.fromPrimitiveValue(WebInspector.UIString("Not available")));
}
ProfilerAgent.getObjectByHeapObjectId(String(this.snapshotNodeId), objectGroupName, formatResult);
}
},

get _retainedSizePercent()
{
return this._retainedSize / this.dataGrid.snapshot.totalSize * 100.0;
},

get _shallowSizePercent()
{
return this._shallowSize / this.dataGrid.snapshot.totalSize * 100.0;
},

updateHasChildren: function()
{
function isEmptyCallback(isEmpty)
{
this.hasChildren = !isEmpty;
}
this._provider().isEmpty(isEmptyCallback.bind(this));
},

isWindow: function(fullName)
{
return fullName.substr(0, 9) === "Window";
},

shortenWindowURL: function(fullName, hasObjectId)
{
var startPos = fullName.indexOf("/");
var endPos = hasObjectId ? fullName.indexOf("@") : fullName.length;
if (startPos !== -1 && endPos !== -1) {
var fullURL = fullName.substring(startPos + 1, endPos).trimLeft();
var url = fullURL.trimURL();
if (url.length > 40)
url = url.trimMiddle(40);
return fullName.substr(0, startPos + 2) + url + fullName.substr(endPos);
} else
return fullName;
},

__proto__: WebInspector.HeapSnapshotGridNode.prototype
}


WebInspector.HeapSnapshotObjectNode = function(tree, isFromBaseSnapshot, edge, parentGridNode)
{
WebInspector.HeapSnapshotGenericObjectNode.call(this, tree, edge.node);
this._referenceName = edge.name;
this._referenceType = edge.type;
this._propertyAccessor = edge.propertyAccessor;
this._distanceToWindow = edge.distanceToWindow;
this.showRetainingEdges = tree.showRetainingEdges;
this._isFromBaseSnapshot = isFromBaseSnapshot;

this._parentGridNode = parentGridNode;
this._cycledWithAncestorGridNode = this._findAncestorWithSameSnapshotNodeId();
if (!this._cycledWithAncestorGridNode)
this.updateHasChildren();
}

WebInspector.HeapSnapshotObjectNode.prototype = {

createProvider: function()
{
var tree = this._dataGrid;
var showHiddenData = WebInspector.settings.showHeapSnapshotObjectsHiddenProperties.get();
var filter = "function(edge) {\n" +
"    return !edge.isInvisible()\n" +
"        && (" + !tree.showRetainingEdges + " || (edge.node().id() !== 1 && !edge.node().isSynthetic() && !edge.isWeak()))\n" +
"        && (" + showHiddenData + " || (!edge.isHidden() && !edge.node().isHidden()));\n" +
"}\n";
var snapshot = this._isFromBaseSnapshot ? tree.baseSnapshot : tree.snapshot;
if (this.showRetainingEdges)
return snapshot.createRetainingEdgesProvider(this.snapshotNodeIndex, filter);
else
return snapshot.createEdgesProvider(this.snapshotNodeIndex, filter);
},

_findAncestorWithSameSnapshotNodeId: function()
{
var ancestor = this._parentGridNode;
while (ancestor) {
if (ancestor.snapshotNodeId === this.snapshotNodeId)
return ancestor;
ancestor = ancestor._parentGridNode;
}
return null;
},

_createChildNode: function(item)
{
return new WebInspector.HeapSnapshotObjectNode(this._dataGrid, this._isFromBaseSnapshot, item, this);
},

_childHashForEntity: function(edge)
{
var prefix = this.showRetainingEdges ? edge.node.id + "#" : "";
return prefix + edge.type + "#" + edge.name;
},

_childHashForNode: function(childNode)
{
var prefix = this.showRetainingEdges ? childNode.snapshotNodeId + "#" : "";
return prefix + childNode._referenceType + "#" + childNode._referenceName;
},

comparator: function()
{
var sortAscending = this._dataGrid.sortOrder === "ascending";
var sortColumnIdentifier = this._dataGrid.sortColumnIdentifier;
var sortFields = {
object: ["!edgeName", sortAscending, "retainedSize", false],
count: ["!edgeName", true, "retainedSize", false],
shallowSize: ["selfSize", sortAscending, "!edgeName", true],
retainedSize: ["retainedSize", sortAscending, "!edgeName", true],
distanceToWindow: ["distanceToWindow", sortAscending, "_name", true]
}[sortColumnIdentifier] || ["!edgeName", true, "retainedSize", false];
return WebInspector.HeapSnapshotFilteredOrderedIterator.prototype.createComparator(sortFields);
},

_emptyData: function()
{
return { count: "", addedCount: "", removedCount: "", countDelta: "", addedSize: "", removedSize: "", sizeDelta: "" };
},

_enhanceData: function(data)
{
var name = this._referenceName;
if (name === "") name = "(empty)";
var nameClass = "name";
switch (this._referenceType) {
case "context":
nameClass = "console-formatted-number";
break;
case "internal":
case "hidden":
nameClass = "console-formatted-null";
break;
case "element":
name = "[" + name + "]";
break;
}
data["object"].nameClass = nameClass;
data["object"].name = name;
data["distanceToWindow"] = this._distanceToWindow;
return data;
},

_prefixObjectCell: function(div, data)
{
if (this._cycledWithAncestorGridNode)
div.className += " cycled-ancessor-node";

var nameSpan = document.createElement("span");
nameSpan.className = data.nameClass;
nameSpan.textContent = data.name;
div.appendChild(nameSpan);

var separatorSpan = document.createElement("span");
separatorSpan.className = "grayed";
separatorSpan.textContent = this.showRetainingEdges ? " in " : " :: ";
div.appendChild(separatorSpan);
},

__proto__: WebInspector.HeapSnapshotGenericObjectNode.prototype
}


WebInspector.HeapSnapshotInstanceNode = function(tree, baseSnapshot, snapshot, node)
{
WebInspector.HeapSnapshotGenericObjectNode.call(this, tree, node);
this._baseSnapshotOrSnapshot = baseSnapshot || snapshot;
this._isDeletedNode = !!baseSnapshot;
this.updateHasChildren();
};

WebInspector.HeapSnapshotInstanceNode.prototype = {
createProvider: function()
{
var showHiddenData = WebInspector.settings.showHeapSnapshotObjectsHiddenProperties.get();
return this._baseSnapshotOrSnapshot.createEdgesProvider(
this.snapshotNodeIndex,
"function(edge) {" +
"    return !edge.isInvisible()" +
"        && (" + showHiddenData + " || (!edge.isHidden() && !edge.node().isHidden()));" +
"}");
},

_createChildNode: function(item)
{
return new WebInspector.HeapSnapshotObjectNode(this._dataGrid, this._isDeletedNode, item, null);
},

_childHashForEntity: function(edge)
{
return edge.type + "#" + edge.name;
},

_childHashForNode: function(childNode)
{
return childNode._referenceType + "#" + childNode._referenceName;
},

comparator: function()
{
var sortAscending = this._dataGrid.sortOrder === "ascending";
var sortColumnIdentifier = this._dataGrid.sortColumnIdentifier;
var sortFields = {
object: ["!edgeName", sortAscending, "retainedSize", false],
distanceToWindow: ["distanceToWindow", sortAscending, "retainedSize", false],
count: ["!edgeName", true, "retainedSize", false],
addedSize: ["selfSize", sortAscending, "!edgeName", true],
removedSize: ["selfSize", sortAscending, "!edgeName", true],
shallowSize: ["selfSize", sortAscending, "!edgeName", true],
retainedSize: ["retainedSize", sortAscending, "!edgeName", true]
}[sortColumnIdentifier] || ["!edgeName", true, "retainedSize", false];
return WebInspector.HeapSnapshotFilteredOrderedIterator.prototype.createComparator(sortFields);
},

_emptyData: function()
{
return {count:"", countDelta:"", sizeDelta: ""};
},

_enhanceData: function(data)
{
if (this._isDeletedNode) {
data["addedCount"] = "";
data["addedSize"] = "";
data["removedCount"] = "\u2022";
data["removedSize"] = Number.withThousandsSeparator(this._shallowSize);
} else {
data["addedCount"] = "\u2022";
data["addedSize"] = Number.withThousandsSeparator(this._shallowSize);
data["removedCount"] = "";
data["removedSize"] = "";
}
return data;
},

get isDeletedNode()
{
return this._isDeletedNode;
},

__proto__: WebInspector.HeapSnapshotGenericObjectNode.prototype
}


WebInspector.HeapSnapshotConstructorNode = function(tree, className, aggregate, aggregatesKey)
{
WebInspector.HeapSnapshotGridNode.call(this, tree, aggregate.count > 0);
this._name = className;
this._aggregatesKey = aggregatesKey;
this._distanceToWindow = aggregate.distanceToWindow;
this._count = aggregate.count;
this._shallowSize = aggregate.self;
this._retainedSize = aggregate.maxRet;
}

WebInspector.HeapSnapshotConstructorNode.prototype = {

createProvider: function()
{
return this._dataGrid.snapshot.createNodesProviderForClass(this._name, this._aggregatesKey)
},


revealNodeBySnapshotObjectId: function(snapshotObjectId)
{
function didExpand()
{
this._provider().nodePosition(snapshotObjectId, didGetNodePosition.bind(this));
}

function didGetNodePosition(nodePosition)
{
if (nodePosition === -1)
this.collapse();
else
this._populateChildren(nodePosition, null, didPopulateChildren.bind(this, nodePosition));
}

function didPopulateChildren(nodePosition)
{
var indexOfFirsChildInRange = 0;
for (var i = 0; i < this._retrievedChildrenRanges.length; i++) {
var range = this._retrievedChildrenRanges[i];
if (range.from <= nodePosition && nodePosition < range.to) {
var childIndex = indexOfFirsChildInRange + nodePosition - range.from;
var instanceNode = this.children[childIndex];
this._dataGrid.highlightNode(instanceNode);
return;
}
indexOfFirsChildInRange += range.to - range.from + 1;
}
}

this.expandWithoutPopulate(didExpand.bind(this));
},

createCell: function(columnIdentifier)
{
var cell = columnIdentifier !== "object" ? this._createValueCell(columnIdentifier) : WebInspector.HeapSnapshotGridNode.prototype.createCell.call(this, columnIdentifier);
if (this._searchMatched)
cell.addStyleClass("highlight");
return cell;
},

_createChildNode: function(item)
{
return new WebInspector.HeapSnapshotInstanceNode(this._dataGrid, null, this._dataGrid.snapshot, item);
},

comparator: function()
{
var sortAscending = this._dataGrid.sortOrder === "ascending";
var sortColumnIdentifier = this._dataGrid.sortColumnIdentifier;
var sortFields = {
object: ["id", sortAscending, "retainedSize", false],
distanceToWindow: ["distanceToWindow", true, "retainedSize", false],
count: ["id", true, "retainedSize", false],
shallowSize: ["selfSize", sortAscending, "id", true],
retainedSize: ["retainedSize", sortAscending, "id", true]
}[sortColumnIdentifier];
return WebInspector.HeapSnapshotFilteredOrderedIterator.prototype.createComparator(sortFields);
},

_childHashForEntity: function(node)
{
return node.id;
},

_childHashForNode: function(childNode)
{
return childNode.snapshotNodeId;
},

get data()
{
var data = { object: this._name };
var view = this.dataGrid.snapshotView;
data["count"] =  Number.withThousandsSeparator(this._count);
data["distanceToWindow"] =  this._distanceToWindow;
data["shallowSize"] = Number.withThousandsSeparator(this._shallowSize);
data["retainedSize"] = Number.withThousandsSeparator(this._retainedSize);
data["count-percent"] =  this._toPercentString(this._countPercent);
data["shallowSize-percent"] = this._toPercentString(this._shallowSizePercent);
data["retainedSize-percent"] = this._toPercentString(this._retainedSizePercent);
return data;
},

get _countPercent()
{
return this._count / this.dataGrid.snapshot.nodeCount * 100.0;
},

get _retainedSizePercent()
{
return this._retainedSize / this.dataGrid.snapshot.totalSize * 100.0;
},

get _shallowSizePercent()
{
return this._shallowSize / this.dataGrid.snapshot.totalSize * 100.0;
},

__proto__: WebInspector.HeapSnapshotGridNode.prototype
}



WebInspector.HeapSnapshotDiffNodesProvider = function(addedNodesProvider, deletedNodesProvider, addedCount, removedCount)
{
this._addedNodesProvider = addedNodesProvider;
this._deletedNodesProvider = deletedNodesProvider;
this._addedCount = addedCount;
this._removedCount = removedCount;
}

WebInspector.HeapSnapshotDiffNodesProvider.prototype = {
dispose: function()
{
this._addedNodesProvider.dispose();
this._deletedNodesProvider.dispose();
},

isEmpty: function(callback)
{
callback(false);
},

serializeItemsRange: function(beginPosition, endPosition, callback)
{
function didReceiveAllItems(items)
{
items.totalLength = this._addedCount + this._removedCount;
callback(items);
}

function didReceiveDeletedItems(addedItems, items)
{
if (!addedItems.length)
addedItems.startPosition = this._addedCount + items.startPosition;
for (var i = 0; i < items.length; i++) {
items[i].isAddedNotRemoved = false;
addedItems.push(items[i]);
}
addedItems.endPosition = this._addedCount + items.endPosition;
didReceiveAllItems.call(this, addedItems);
}

function didReceiveAddedItems(items)
{
for (var i = 0; i < items.length; i++)
items[i].isAddedNotRemoved = true;
if (items.endPosition < endPosition)
return this._deletedNodesProvider.serializeItemsRange(0, endPosition - items.endPosition, didReceiveDeletedItems.bind(this, items));

items.totalLength = this._addedCount + this._removedCount;
didReceiveAllItems.call(this, items);
}

if (beginPosition < this._addedCount)
this._addedNodesProvider.serializeItemsRange(beginPosition, endPosition, didReceiveAddedItems.bind(this));
else
this._deletedNodesProvider.serializeItemsRange(beginPosition - this._addedCount, endPosition - this._addedCount, didReceiveDeletedItems.bind(this, []));
},

sortAndRewind: function(comparator, callback)
{
function afterSort()
{
this._deletedNodesProvider.sortAndRewind(comparator, callback);
}
this._addedNodesProvider.sortAndRewind(comparator, afterSort.bind(this));
}
};


WebInspector.HeapSnapshotDiffNode = function(tree, className, diffForClass)
{
WebInspector.HeapSnapshotGridNode.call(this, tree, true);
this._name = className;

this._addedCount = diffForClass.addedCount;
this._removedCount = diffForClass.removedCount;
this._countDelta = diffForClass.countDelta;
this._addedSize = diffForClass.addedSize;
this._removedSize = diffForClass.removedSize;
this._sizeDelta = diffForClass.sizeDelta;
this._deletedIndexes = diffForClass.deletedIndexes;
}

WebInspector.HeapSnapshotDiffNode.prototype = {

createProvider: function()
{
var tree = this._dataGrid;
return  new WebInspector.HeapSnapshotDiffNodesProvider(
tree.snapshot.createAddedNodesProvider(tree.baseSnapshot.uid, this._name),
tree.baseSnapshot.createDeletedNodesProvider(this._deletedIndexes),
this._addedCount,
this._removedCount);
},

_createChildNode: function(item)
{
if (item.isAddedNotRemoved)
return new WebInspector.HeapSnapshotInstanceNode(this._dataGrid, null, this._dataGrid.snapshot, item);
else
return new WebInspector.HeapSnapshotInstanceNode(this._dataGrid, this._dataGrid.baseSnapshot, null, item);
},

_childHashForEntity: function(node)
{
return node.id;
},

_childHashForNode: function(childNode)
{
return childNode.snapshotNodeId;
},

comparator: function()
{
var sortAscending = this._dataGrid.sortOrder === "ascending";
var sortColumnIdentifier = this._dataGrid.sortColumnIdentifier;
var sortFields = {
object: ["id", sortAscending, "selfSize", false],
addedCount: ["selfSize", sortAscending, "id", true],
removedCount: ["selfSize", sortAscending, "id", true],
countDelta: ["selfSize", sortAscending, "id", true],
addedSize: ["selfSize", sortAscending, "id", true],
removedSize: ["selfSize", sortAscending, "id", true],
sizeDelta: ["selfSize", sortAscending, "id", true]
}[sortColumnIdentifier];
return WebInspector.HeapSnapshotFilteredOrderedIterator.prototype.createComparator(sortFields);
},

_signForDelta: function(delta)
{
if (delta === 0)
return "";
if (delta > 0)
return "+";
else
return "\u2212";  
},

get data()
{
var data = {object: this._name};

data["addedCount"] = Number.withThousandsSeparator(this._addedCount);
data["removedCount"] = Number.withThousandsSeparator(this._removedCount);
data["countDelta"] = this._signForDelta(this._countDelta) + Number.withThousandsSeparator(Math.abs(this._countDelta));
data["addedSize"] = Number.withThousandsSeparator(this._addedSize);
data["removedSize"] = Number.withThousandsSeparator(this._removedSize);
data["sizeDelta"] = this._signForDelta(this._sizeDelta) + Number.withThousandsSeparator(Math.abs(this._sizeDelta));

return data;
},

__proto__: WebInspector.HeapSnapshotGridNode.prototype
}



WebInspector.HeapSnapshotDominatorObjectNode = function(tree, node)
{
WebInspector.HeapSnapshotGenericObjectNode.call(this, tree, node);
this.updateHasChildren();
};

WebInspector.HeapSnapshotDominatorObjectNode.prototype = {

createProvider: function()
{
return this._dataGrid.snapshot.createNodesProviderForDominator(this.snapshotNodeIndex);
},


retrieveChildBySnapshotObjectId: function(snapshotObjectId, callback)
{
function didExpand()
{
this._provider().nodePosition(snapshotObjectId, didGetNodePosition.bind(this));
}

function didGetNodePosition(nodePosition)
{
if (nodePosition === -1) {
this.collapse();
callback(null);
} else
this._populateChildren(nodePosition, null, didPopulateChildren.bind(this, nodePosition));
}

function didPopulateChildren(nodePosition)
{
var child = this.childForPosition(nodePosition);
callback(child);
}



this.hasChildren = true;
this.expandWithoutPopulate(didExpand.bind(this));
},

_createChildNode: function(item)
{
return new WebInspector.HeapSnapshotDominatorObjectNode(this._dataGrid, item);
},

_childHashForEntity: function(node)
{
return node.id;
},

_childHashForNode: function(childNode)
{
return childNode.snapshotNodeId;
},

comparator: function()
{
var sortAscending = this._dataGrid.sortOrder === "ascending";
var sortColumnIdentifier = this._dataGrid.sortColumnIdentifier;
var sortFields = {
object: ["id", sortAscending, "retainedSize", false],
shallowSize: ["selfSize", sortAscending, "id", true],
retainedSize: ["retainedSize", sortAscending, "id", true]
}[sortColumnIdentifier];
return WebInspector.HeapSnapshotFilteredOrderedIterator.prototype.createComparator(sortFields);
},

_emptyData: function()
{
return {};
},

__proto__: WebInspector.HeapSnapshotGenericObjectNode.prototype
}

;



WebInspector.HeapSnapshotLoader = function()
{
this._reset();
}

WebInspector.HeapSnapshotLoader.prototype = {
dispose: function()
{
this._reset();
},

_reset: function()
{
this._json = "";
this._state = "find-snapshot-info";
this._snapshot = {};
},

close: function()
{
if (this._json)
this._parseStringsArray();
},

buildSnapshot: function()
{
var result = new WebInspector.HeapSnapshot(this._snapshot);
this._reset();
return result;
},

_parseUintArray: function()
{
var index = 0;
var char0 = "0".charCodeAt(0), char9 = "9".charCodeAt(0), closingBracket = "]".charCodeAt(0);
var length = this._json.length;
while (true) {
while (index < length) {
var code = this._json.charCodeAt(index);
if (char0 <= code && code <= char9)
break;
else if (code === closingBracket) {
this._json = this._json.slice(index + 1);
return false;
}
++index;
}
if (index === length) {
this._json = "";
return true;
}
var nextNumber = 0;
var startIndex = index;
while (index < length) {
var code = this._json.charCodeAt(index);
if (char0 > code || code > char9)
break;
nextNumber *= 10;
nextNumber += (code - char0);
++index;
}
if (index === length) {
this._json = this._json.slice(startIndex);
return true;
}
this._array[this._arrayIndex++] = nextNumber;
}
},

_parseStringsArray: function()
{
var closingBracketIndex = this._json.lastIndexOf("]");
if (closingBracketIndex === -1)
throw new Error("Incomplete JSON");
this._json = this._json.slice(0, closingBracketIndex + 1);
this._snapshot.strings = JSON.parse(this._json);
},


write: function(chunk)
{
this._json += chunk;
switch (this._state) {
case "find-snapshot-info": {
var snapshotToken = "\"snapshot\"";
var snapshotTokenIndex = this._json.indexOf(snapshotToken);
if (snapshotTokenIndex === -1)
throw new Error("Snapshot token not found");
this._json = this._json.slice(snapshotTokenIndex + snapshotToken.length + 1);
this._state = "parse-snapshot-info";
}
case "parse-snapshot-info": {
var closingBracketIndex = WebInspector.findBalancedCurlyBrackets(this._json);
if (closingBracketIndex === -1)
return;
this._snapshot.snapshot =   (JSON.parse(this._json.slice(0, closingBracketIndex)));
this._json = this._json.slice(closingBracketIndex);
this._state = "find-nodes";
}
case "find-nodes": {
var nodesToken = "\"nodes\"";
var nodesTokenIndex = this._json.indexOf(nodesToken);
if (nodesTokenIndex === -1)
return;
var bracketIndex = this._json.indexOf("[", nodesTokenIndex);
if (bracketIndex === -1)
return;
this._json = this._json.slice(bracketIndex + 1);
var node_fields_count = this._snapshot.snapshot.meta.node_fields.length;
var nodes_length = this._snapshot.snapshot.node_count * node_fields_count;
this._array = new Uint32Array(nodes_length);
this._arrayIndex = 0;
this._state = "parse-nodes";
}
case "parse-nodes": {
if (this._parseUintArray())
return;
this._snapshot.nodes = this._array;
this._state = "find-edges";
this._array = null;
}
case "find-edges": {
var edgesToken = "\"edges\"";
var edgesTokenIndex = this._json.indexOf(edgesToken);
if (edgesTokenIndex === -1)
return;
var bracketIndex = this._json.indexOf("[", edgesTokenIndex);
if (bracketIndex === -1)
return;
this._json = this._json.slice(bracketIndex + 1);
var edge_fields_count = this._snapshot.snapshot.meta.edge_fields.length;
var edges_length = this._snapshot.snapshot.edge_count * edge_fields_count;
this._array = new Uint32Array(edges_length);
this._arrayIndex = 0;
this._state = "parse-edges";
}
case "parse-edges": {
if (this._parseUintArray())
return;
this._snapshot.edges = this._array;
this._array = null;
this._state = "find-strings";
}
case "find-strings": {
var stringsToken = "\"strings\"";
var stringsTokenIndex = this._json.indexOf(stringsToken);
if (stringsTokenIndex === -1)
return;
var bracketIndex = this._json.indexOf("[", stringsTokenIndex);
if (bracketIndex === -1)
return;
this._json = this._json.slice(bracketIndex);
this._state = "accumulate-strings";
break;
}
case "accumulate-strings":
break;
}
}
};
;



WebInspector.HeapSnapshotWorkerWrapper = function()
{
}

WebInspector.HeapSnapshotWorkerWrapper.prototype =  {
postMessage: function(message)
{
},
terminate: function()
{
},

__proto__: WebInspector.Object.prototype
}


WebInspector.HeapSnapshotRealWorker = function()
{
this._worker = new Worker("HeapSnapshotWorker.js");
this._worker.addEventListener("message", this._messageReceived.bind(this), false);
}

WebInspector.HeapSnapshotRealWorker.prototype = {
_messageReceived: function(event)
{
var message = event.data;
if ("callId" in message)
this.dispatchEventToListeners("message", message);
else {
if (message.object !== "console") {
console.log(WebInspector.UIString("Worker asks to call a method '%s' on an unsupported object '%s'.", message.method, message.object));
return;
}
if (message.method !== "log" && message.method !== "info" && message.method !== "error") {
console.log(WebInspector.UIString("Worker asks to call an unsupported method '%s' on the console object.", message.method));
return;
}
console[message.method].apply(window[message.object], message.arguments);
}
},

postMessage: function(message)
{
this._worker.postMessage(message);
},

terminate: function()
{
this._worker.terminate();
},

__proto__: WebInspector.HeapSnapshotWorkerWrapper.prototype
}



WebInspector.AsyncTaskQueue = function()
{
this._queue = [];
this._isTimerSheduled = false;
}

WebInspector.AsyncTaskQueue.prototype = {

addTask: function(task)
{
this._queue.push(task);
this._scheduleTimer();
},

_onTimeout: function()
{
this._isTimerSheduled = false;
var queue = this._queue;
this._queue = [];
for (var i = 0; i < queue.length; i++) {
try {
queue[i]();
} catch (e) {
console.error("Exception while running task: " + e.stack);
}
}
this._scheduleTimer();
},

_scheduleTimer: function()
{
if (this._queue.length && !this._isTimerSheduled) {
setTimeout(this._onTimeout.bind(this), 0);
this._isTimerSheduled = true;
}
}
}


WebInspector.HeapSnapshotFakeWorker = function()
{
this._dispatcher = new WebInspector.HeapSnapshotWorkerDispatcher(window, this._postMessageFromWorker.bind(this));
this._asyncTaskQueue = new WebInspector.AsyncTaskQueue();
}

WebInspector.HeapSnapshotFakeWorker.prototype = {
postMessage: function(message)
{
function dispatch()
{
if (this._dispatcher)
this._dispatcher.dispatchMessage({data: message});
}
this._asyncTaskQueue.addTask(dispatch.bind(this));
},

terminate: function()
{
this._dispatcher = null;
},

_postMessageFromWorker: function(message)
{
function send()
{
this.dispatchEventToListeners("message", message);
}
this._asyncTaskQueue.addTask(send.bind(this));
},

__proto__: WebInspector.HeapSnapshotWorkerWrapper.prototype
}



WebInspector.HeapSnapshotWorker = function()
{
this._nextObjectId = 1;
this._nextCallId = 1;
this._callbacks = [];
this._previousCallbacks = [];

this._worker = typeof InspectorTest === "undefined" ? new WebInspector.HeapSnapshotRealWorker() : new WebInspector.HeapSnapshotFakeWorker();
this._worker.addEventListener("message", this._messageReceived, this);
}

WebInspector.HeapSnapshotWorker.prototype = {
createObject: function(constructorName)
{
var proxyConstructorFunction = this._findFunction(constructorName + "Proxy");
var objectId = this._nextObjectId++;
var proxy = new proxyConstructorFunction(this, objectId);
this._postMessage({callId: this._nextCallId++, disposition: "create", objectId: objectId, methodName: constructorName});
return proxy;
},

dispose: function()
{
this._worker.terminate();
if (this._interval)
clearInterval(this._interval);
},

disposeObject: function(objectId)
{
this._postMessage({callId: this._nextCallId++, disposition: "dispose", objectId: objectId});
},

callGetter: function(callback, objectId, getterName)
{
var callId = this._nextCallId++;
this._callbacks[callId] = callback;
this._postMessage({callId: callId, disposition: "getter", objectId: objectId, methodName: getterName});
},

callFactoryMethod: function(callback, objectId, methodName, proxyConstructorName)
{
var callId = this._nextCallId++;
var methodArguments = Array.prototype.slice.call(arguments, 4);
var newObjectId = this._nextObjectId++;
var proxyConstructorFunction = this._findFunction(proxyConstructorName);
if (callback) {
function wrapCallback(remoteResult)
{
callback(remoteResult ? new proxyConstructorFunction(this, newObjectId) : null);
}
this._callbacks[callId] = wrapCallback.bind(this);
this._postMessage({callId: callId, disposition: "factory", objectId: objectId, methodName: methodName, methodArguments: methodArguments, newObjectId: newObjectId});
return null;
} else {
this._postMessage({callId: callId, disposition: "factory", objectId: objectId, methodName: methodName, methodArguments: methodArguments, newObjectId: newObjectId});
return new proxyConstructorFunction(this, newObjectId);
}
},

callMethod: function(callback, objectId, methodName)
{
var callId = this._nextCallId++;
var methodArguments = Array.prototype.slice.call(arguments, 3);
if (callback)
this._callbacks[callId] = callback;
this._postMessage({callId: callId, disposition: "method", objectId: objectId, methodName: methodName, methodArguments: methodArguments});
},

startCheckingForLongRunningCalls: function()
{
if (this._interval)
return;
this._checkLongRunningCalls();
this._interval = setInterval(this._checkLongRunningCalls.bind(this), 300);
},

_checkLongRunningCalls: function()
{
for (var callId in this._previousCallbacks)
if (!(callId in this._callbacks))
delete this._previousCallbacks[callId];
var hasLongRunningCalls = false;
for (callId in this._previousCallbacks) {
hasLongRunningCalls = true;
break;
}
this.dispatchEventToListeners("wait", hasLongRunningCalls);
for (callId in this._callbacks)
this._previousCallbacks[callId] = true;
},

_findFunction: function(name)
{
var path = name.split(".");
var result = window;
for (var i = 0; i < path.length; ++i)
result = result[path[i]];
return result;
},

_messageReceived: function(event)
{
var data = event.data;
if (event.data.error) {
if (event.data.errorMethodName)
WebInspector.log(WebInspector.UIString("An error happened when a call for method '%s' was requested", event.data.errorMethodName));
WebInspector.log(event.data.errorCallStack);
delete this._callbacks[data.callId];
return;
}
if (!this._callbacks[data.callId])
return;
var callback = this._callbacks[data.callId];
delete this._callbacks[data.callId];
callback(data.result);
},

_postMessage: function(message)
{
this._worker.postMessage(message);
},

__proto__: WebInspector.Object.prototype
}



WebInspector.HeapSnapshotProxyObject = function(worker, objectId)
{
this._worker = worker;
this._objectId = objectId;
}

WebInspector.HeapSnapshotProxyObject.prototype = {
_callWorker: function(workerMethodName, args)
{
args.splice(1, 0, this._objectId);
return this._worker[workerMethodName].apply(this._worker, args);
},

dispose: function()
{
this._worker.disposeObject(this._objectId);
},

disposeWorker: function()
{
this._worker.dispose();
},


callFactoryMethod: function(callback, methodName, proxyConstructorName, var_args)
{
return this._callWorker("callFactoryMethod", Array.prototype.slice.call(arguments, 0));
},

callGetter: function(callback, getterName)
{
return this._callWorker("callGetter", Array.prototype.slice.call(arguments, 0));
},


callMethod: function(callback, methodName, var_args)
{
return this._callWorker("callMethod", Array.prototype.slice.call(arguments, 0));
},

get worker() {
return this._worker;
}
};


WebInspector.HeapSnapshotLoaderProxy = function(worker, objectId)
{
WebInspector.HeapSnapshotProxyObject.call(this, worker, objectId);
this._pendingSnapshotConsumers = [];
}

WebInspector.HeapSnapshotLoaderProxy.prototype = {

addConsumer: function(callback)
{
this._pendingSnapshotConsumers.push(callback);
},


write: function(chunk, callback)
{
this.callMethod(callback, "write", chunk);
},

close: function()
{
function buildSnapshot()
{
this.callFactoryMethod(updateStaticData.bind(this), "buildSnapshot", "WebInspector.HeapSnapshotProxy");
}
function updateStaticData(snapshotProxy)
{
this.dispose();
snapshotProxy.updateStaticData(notifyPendingConsumers.bind(this));
}
function notifyPendingConsumers(snapshotProxy)
{
for (var i = 0; i < this._pendingSnapshotConsumers.length; ++i)
this._pendingSnapshotConsumers[i](snapshotProxy);
this._pendingSnapshotConsumers = [];
}
this.callMethod(buildSnapshot.bind(this), "close");
},

__proto__: WebInspector.HeapSnapshotProxyObject.prototype
}



WebInspector.HeapSnapshotProxy = function(worker, objectId)
{
WebInspector.HeapSnapshotProxyObject.call(this, worker, objectId);
}

WebInspector.HeapSnapshotProxy.prototype = {
aggregates: function(sortedIndexes, key, filter, callback)
{
this.callMethod(callback, "aggregates", sortedIndexes, key, filter);
},

aggregatesForDiff: function(callback)
{
this.callMethod(callback, "aggregatesForDiff");
},

calculateSnapshotDiff: function(baseSnapshotId, baseSnapshotAggregates, callback)
{
this.callMethod(callback, "calculateSnapshotDiff", baseSnapshotId, baseSnapshotAggregates);
},

nodeClassName: function(snapshotObjectId, callback)
{
this.callMethod(callback, "nodeClassName", snapshotObjectId);
},

dominatorIdsForNode: function(nodeIndex, callback)
{
this.callMethod(callback, "dominatorIdsForNode", nodeIndex);
},

createEdgesProvider: function(nodeIndex, filter)
{
return this.callFactoryMethod(null, "createEdgesProvider", "WebInspector.HeapSnapshotProviderProxy", nodeIndex, filter);
},

createRetainingEdgesProvider: function(nodeIndex, filter)
{
return this.callFactoryMethod(null, "createRetainingEdgesProvider", "WebInspector.HeapSnapshotProviderProxy", nodeIndex, filter);
},

createAddedNodesProvider: function(baseSnapshotId, className)
{
return this.callFactoryMethod(null, "createAddedNodesProvider", "WebInspector.HeapSnapshotProviderProxy", baseSnapshotId, className);
},

createDeletedNodesProvider: function(nodeIndexes)
{
return this.callFactoryMethod(null, "createDeletedNodesProvider", "WebInspector.HeapSnapshotProviderProxy", nodeIndexes);
},

createNodesProvider: function(filter)
{
return this.callFactoryMethod(null, "createNodesProvider", "WebInspector.HeapSnapshotProviderProxy", filter);
},

createNodesProviderForClass: function(className, aggregatesKey)
{
return this.callFactoryMethod(null, "createNodesProviderForClass", "WebInspector.HeapSnapshotProviderProxy", className, aggregatesKey);
},

createNodesProviderForDominator: function(nodeIndex)
{
return this.callFactoryMethod(null, "createNodesProviderForDominator", "WebInspector.HeapSnapshotProviderProxy", nodeIndex);
},

dispose: function()
{
this.disposeWorker();
},

get nodeCount()
{
return this._staticData.nodeCount;
},

get nodeFlags()
{
return this._staticData.nodeFlags;
},

get rootNodeIndex()
{
return this._staticData.rootNodeIndex;
},

updateStaticData: function(callback)
{
function dataReceived(staticData)
{
this._staticData = staticData;
callback(this);
}
this.callMethod(dataReceived.bind(this), "updateStaticData");
},

get totalSize()
{
return this._staticData.totalSize;
},

get uid()
{
return this._staticData.uid;
},

__proto__: WebInspector.HeapSnapshotProxyObject.prototype
}



WebInspector.HeapSnapshotProviderProxy = function(worker, objectId)
{
WebInspector.HeapSnapshotProxyObject.call(this, worker, objectId);
}

WebInspector.HeapSnapshotProviderProxy.prototype = {
nodePosition: function(snapshotObjectId, callback)
{
this.callMethod(callback, "nodePosition", snapshotObjectId);
},

isEmpty: function(callback)
{
this.callMethod(callback, "isEmpty");
},

serializeItemsRange: function(startPosition, endPosition, callback)
{
this.callMethod(callback, "serializeItemsRange", startPosition, endPosition);
},

sortAndRewind: function(comparator, callback)
{
this.callMethod(callback, "sortAndRewind", comparator);
},

__proto__: WebInspector.HeapSnapshotProxyObject.prototype
}

;



WebInspector.HeapSnapshotView = function(parent, profile)
{
WebInspector.View.call(this);

this.element.addStyleClass("heap-snapshot-view");

this.parent = parent;
this.parent.addEventListener("profile added", this._updateBaseOptions, this);
this.parent.addEventListener("profile added", this._updateFilterOptions, this);

this.viewsContainer = document.createElement("div");
this.viewsContainer.addStyleClass("views-container");
this.element.appendChild(this.viewsContainer);

this.containmentView = new WebInspector.View();
this.containmentView.element.addStyleClass("view");
this.containmentDataGrid = new WebInspector.HeapSnapshotContainmentDataGrid();
this.containmentDataGrid.element.addEventListener("mousedown", this._mouseDownInContentsGrid.bind(this), true);
this.containmentDataGrid.show(this.containmentView.element);
this.containmentDataGrid.addEventListener(WebInspector.DataGrid.Events.SelectedNode, this._selectionChanged, this);

this.constructorsView = new WebInspector.View();
this.constructorsView.element.addStyleClass("view");
this.constructorsView.element.appendChild(this._createToolbarWithClassNameFilter());

this.constructorsDataGrid = new WebInspector.HeapSnapshotConstructorsDataGrid();
this.constructorsDataGrid.element.addStyleClass("class-view-grid");
this.constructorsDataGrid.element.addEventListener("mousedown", this._mouseDownInContentsGrid.bind(this), true);
this.constructorsDataGrid.show(this.constructorsView.element);
this.constructorsDataGrid.addEventListener(WebInspector.DataGrid.Events.SelectedNode, this._selectionChanged, this);

this.diffView = new WebInspector.View();
this.diffView.element.addStyleClass("view");
this.diffView.element.appendChild(this._createToolbarWithClassNameFilter());

this.diffDataGrid = new WebInspector.HeapSnapshotDiffDataGrid();
this.diffDataGrid.element.addStyleClass("class-view-grid");
this.diffDataGrid.show(this.diffView.element);
this.diffDataGrid.addEventListener(WebInspector.DataGrid.Events.SelectedNode, this._selectionChanged, this);

this.dominatorView = new WebInspector.View();
this.dominatorView.element.addStyleClass("view");
this.dominatorDataGrid = new WebInspector.HeapSnapshotDominatorsDataGrid();
this.dominatorDataGrid.element.addEventListener("mousedown", this._mouseDownInContentsGrid.bind(this), true);
this.dominatorDataGrid.show(this.dominatorView.element);
this.dominatorDataGrid.addEventListener(WebInspector.DataGrid.Events.SelectedNode, this._selectionChanged, this);

this.retainmentViewHeader = document.createElement("div");
this.retainmentViewHeader.addStyleClass("retainers-view-header");
WebInspector.installDragHandle(this.retainmentViewHeader, this._startRetainersHeaderDragging.bind(this), this._retainersHeaderDragging.bind(this), this._endRetainersHeaderDragging.bind(this), "row-resize");
var retainingPathsTitleDiv = document.createElement("div");
retainingPathsTitleDiv.className = "title";
var retainingPathsTitle = document.createElement("span");
retainingPathsTitle.textContent = WebInspector.UIString("Object's retaining tree");
retainingPathsTitleDiv.appendChild(retainingPathsTitle);
this.retainmentViewHeader.appendChild(retainingPathsTitleDiv);
this.element.appendChild(this.retainmentViewHeader);

this.retainmentView = new WebInspector.View();
this.retainmentView.element.addStyleClass("view");
this.retainmentView.element.addStyleClass("retaining-paths-view");
this.retainmentDataGrid = new WebInspector.HeapSnapshotRetainmentDataGrid();
this.retainmentDataGrid.show(this.retainmentView.element);
this.retainmentDataGrid.addEventListener(WebInspector.DataGrid.Events.SelectedNode, this._inspectedObjectChanged, this);
this.retainmentView.show(this.element);
this.retainmentDataGrid.reset();

this.dataGrid = this.constructorsDataGrid;
this.currentView = this.constructorsView;

this.viewSelectElement = document.createElement("select");
this.viewSelectElement.className = "status-bar-item";
this.viewSelectElement.addEventListener("change", this._onSelectedViewChanged.bind(this), false);

this.views = [{title: "Summary", view: this.constructorsView, grid: this.constructorsDataGrid},
{title: "Comparison", view: this.diffView, grid: this.diffDataGrid},
{title: "Containment", view: this.containmentView, grid: this.containmentDataGrid},
{title: "Dominators", view: this.dominatorView, grid: this.dominatorDataGrid}];
this.views.current = 0;
for (var i = 0; i < this.views.length; ++i) {
var view = this.views[i];
var option = document.createElement("option");
option.label = WebInspector.UIString(view.title);
this.viewSelectElement.appendChild(option);
}

this._profileUid = profile.uid;

this.baseSelectElement = document.createElement("select");
this.baseSelectElement.className = "status-bar-item";
this.baseSelectElement.addEventListener("change", this._changeBase.bind(this), false);
this._updateBaseOptions();

this.filterSelectElement = document.createElement("select");
this.filterSelectElement.className = "status-bar-item";
this.filterSelectElement.addEventListener("change", this._changeFilter.bind(this), false);
this._updateFilterOptions();

this.helpButton = new WebInspector.StatusBarButton("", "heap-snapshot-help-status-bar-item status-bar-item");
this.helpButton.addEventListener("click", this._helpClicked, this);

this._popoverHelper = new WebInspector.ObjectPopoverHelper(this.element, this._getHoverAnchor.bind(this), this._resolveObjectForPopover.bind(this), undefined, true);

this.profile.load(profileCallback.bind(this));

function profileCallback(heapSnapshotProxy)
{
var list = this._profiles();
var profileIndex;
for (var i = 0; i < list.length; ++i) {
if (list[i].uid === this._profileUid) {
profileIndex = i;
break;
}
}

if (profileIndex > 0)
this.baseSelectElement.selectedIndex = profileIndex - 1;
else
this.baseSelectElement.selectedIndex = profileIndex;
this.dataGrid.setDataSource(this, heapSnapshotProxy);
}
}

WebInspector.HeapSnapshotView.prototype = {
dispose: function()
{
this.profile.dispose();
if (this.baseProfile)
this.baseProfile.dispose();
this.containmentDataGrid.dispose();
this.constructorsDataGrid.dispose();
this.diffDataGrid.dispose();
this.dominatorDataGrid.dispose();
this.retainmentDataGrid.dispose();
},

get statusBarItems()
{

function appendArrowImage(element, hidden)
{
var span = document.createElement("span");
span.className = "status-bar-select-container" + (hidden ? " hidden" : "");
span.appendChild(element);
return span;
}
return [appendArrowImage(this.viewSelectElement), appendArrowImage(this.baseSelectElement, true), appendArrowImage(this.filterSelectElement), this.helpButton.element];
},

get profile()
{
return this.parent.getProfile(WebInspector.HeapSnapshotProfileType.TypeId, this._profileUid);
},

get baseProfile()
{
return this.parent.getProfile(WebInspector.HeapSnapshotProfileType.TypeId, this._baseProfileUid);
},

wasShown: function()
{

this.profile.load(profileCallback1.bind(this));

function profileCallback1() {
if (this.baseProfile)
this.baseProfile.load(profileCallback2.bind(this));
else
profileCallback2.call(this);
}

function profileCallback2() {
this.currentView.show(this.viewsContainer);
}
},

willHide: function()
{
this._currentSearchResultIndex = -1;
this._popoverHelper.hidePopover();
if (this.helpPopover && this.helpPopover.isShowing())
this.helpPopover.hide();
},

onResize: function()
{
var height = this.retainmentView.element.clientHeight;
this._updateRetainmentViewHeight(height);
},

searchCanceled: function()
{
if (this._searchResults) {
for (var i = 0; i < this._searchResults.length; ++i) {
var node = this._searchResults[i].node;
delete node._searchMatched;
node.refresh();
}
}

delete this._searchFinishedCallback;
this._currentSearchResultIndex = -1;
this._searchResults = [];
},

performSearch: function(query, finishedCallback)
{

this.searchCanceled();

query = query.trim();

if (!query.length)
return;
if (this.currentView !== this.constructorsView && this.currentView !== this.diffView)
return;

this._searchFinishedCallback = finishedCallback;

function matchesByName(gridNode) {
return ("_name" in gridNode) && gridNode._name.hasSubstring(query, true);
}

function matchesById(gridNode) {
return ("snapshotNodeId" in gridNode) && gridNode.snapshotNodeId === query;
}

var matchPredicate;
if (query.charAt(0) !== "@")
matchPredicate = matchesByName;
else {
query = parseInt(query.substring(1), 10);
matchPredicate = matchesById;
}

function matchesQuery(gridNode)
{
delete gridNode._searchMatched;
if (matchPredicate(gridNode)) {
gridNode._searchMatched = true;
gridNode.refresh();
return true;
}
return false;
}

var current = this.dataGrid.rootNode().children[0];
var depth = 0;
var info = {};


const maxDepth = 1;

while (current) {
if (matchesQuery(current))
this._searchResults.push({ node: current });
current = current.traverseNextNode(false, null, (depth >= maxDepth), info);
depth += info.depthChange;
}

finishedCallback(this, this._searchResults.length);
},

jumpToFirstSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
this._currentSearchResultIndex = 0;
this._jumpToSearchResult(this._currentSearchResultIndex);
},

jumpToLastSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
this._currentSearchResultIndex = (this._searchResults.length - 1);
this._jumpToSearchResult(this._currentSearchResultIndex);
},

jumpToNextSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
if (++this._currentSearchResultIndex >= this._searchResults.length)
this._currentSearchResultIndex = 0;
this._jumpToSearchResult(this._currentSearchResultIndex);
},

jumpToPreviousSearchResult: function()
{
if (!this._searchResults || !this._searchResults.length)
return;
if (--this._currentSearchResultIndex < 0)
this._currentSearchResultIndex = (this._searchResults.length - 1);
this._jumpToSearchResult(this._currentSearchResultIndex);
},

showingFirstSearchResult: function()
{
return (this._currentSearchResultIndex === 0);
},

showingLastSearchResult: function()
{
return (this._searchResults && this._currentSearchResultIndex === (this._searchResults.length - 1));
},

_jumpToSearchResult: function(index)
{
var searchResult = this._searchResults[index];
if (!searchResult)
return;

var node = searchResult.node;
node.revealAndSelect();
},

refreshVisibleData: function()
{
var child = this.dataGrid.rootNode().children[0];
while (child) {
child.refresh();
child = child.traverseNextNode(false, null, true);
}
},

_changeBase: function()
{
if (this._baseProfileUid === this._profiles()[this.baseSelectElement.selectedIndex].uid)
return;

this._baseProfileUid = this._profiles()[this.baseSelectElement.selectedIndex].uid;
var dataGrid =   (this.dataGrid);

if (dataGrid.snapshot)
this.baseProfile.load(dataGrid.setBaseDataSource.bind(dataGrid));

if (!this.currentQuery || !this._searchFinishedCallback || !this._searchResults)
return;




this._searchFinishedCallback(this, -this._searchResults.length);
this.performSearch(this.currentQuery, this._searchFinishedCallback);
},

_changeFilter: function()
{
var profileIndex = this.filterSelectElement.selectedIndex - 1;
this.dataGrid._filterSelectIndexChanged(this._profiles(), profileIndex);

if (!this.currentQuery || !this._searchFinishedCallback || !this._searchResults)
return;




this._searchFinishedCallback(this, -this._searchResults.length);
this.performSearch(this.currentQuery, this._searchFinishedCallback);
},

_createToolbarWithClassNameFilter: function()
{
var toolbar = document.createElement("div");
toolbar.addStyleClass("class-view-toolbar");
var classNameFilter = document.createElement("input");
classNameFilter.addStyleClass("class-name-filter");
classNameFilter.setAttribute("placeholder", WebInspector.UIString("Class filter"));
classNameFilter.addEventListener("keyup", this._changeNameFilter.bind(this, classNameFilter), false);
toolbar.appendChild(classNameFilter);
return toolbar;
},

_changeNameFilter: function(classNameInputElement)
{
var filter = classNameInputElement.value;
this.dataGrid.changeNameFilter(filter);
},

_profiles: function()
{
return this.parent.getProfiles(WebInspector.HeapSnapshotProfileType.TypeId);
},

processLoadedSnapshot: function(profile, snapshot)
{
profile.nodes = snapshot.nodes;
profile.strings = snapshot.strings;
var s = new WebInspector.HeapSnapshot(profile);
profile.sidebarElement.subtitle = Number.bytesToString(s.totalSize);
},


populateContextMenu: function(contextMenu, event)
{
this.dataGrid.populateContextMenu(this.parent, contextMenu, event);
},

_selectionChanged: function(event)
{
var selectedNode = event.target.selectedNode;
this._setRetainmentDataGridSource(selectedNode);
this._inspectedObjectChanged(event);
},

_inspectedObjectChanged: function(event)
{
var selectedNode = event.target.selectedNode;
if (!this.profile.fromFile() && selectedNode instanceof WebInspector.HeapSnapshotGenericObjectNode)
ConsoleAgent.addInspectedHeapObject(selectedNode.snapshotNodeId);
},

_setRetainmentDataGridSource: function(nodeItem)
{
if (nodeItem && nodeItem.snapshotNodeIndex)
this.retainmentDataGrid.setDataSource(this, nodeItem.isDeletedNode ? nodeItem.dataGrid.baseSnapshot : nodeItem.dataGrid.snapshot, nodeItem.snapshotNodeIndex);
else
this.retainmentDataGrid.reset();
},

_mouseDownInContentsGrid: function(event)
{
if (event.detail < 2)
return;

var cell = event.target.enclosingNodeOrSelfWithNodeName("td");
if (!cell || (!cell.hasStyleClass("count-column") && !cell.hasStyleClass("shallowSize-column") && !cell.hasStyleClass("retainedSize-column")))
return;

event.consume(true);
},

changeView: function(viewTitle, callback)
{
var viewIndex = null;
for (var i = 0; i < this.views.length; ++i)
if (this.views[i].title === viewTitle) {
viewIndex = i;
break;
}
if (this.views.current === viewIndex) {
setTimeout(callback, 0);
return;
}

function dataGridContentShown(event)
{
var dataGrid = event.data;
dataGrid.removeEventListener(WebInspector.HeapSnapshotSortableDataGrid.Events.ContentShown, dataGridContentShown, this);
if (dataGrid === this.dataGrid)
callback();
}
this.views[viewIndex].grid.addEventListener(WebInspector.HeapSnapshotSortableDataGrid.Events.ContentShown, dataGridContentShown, this);

this.viewSelectElement.selectedIndex = viewIndex;
this._changeView(viewIndex);
},

_updateDataSourceAndView: function()
{
var dataGrid = this.dataGrid;
if (dataGrid.snapshotView)
return;

this.profile.load(didLoadSnapshot.bind(this));
function didLoadSnapshot(snapshotProxy)
{
if (this.dataGrid !== dataGrid)
return;
if (dataGrid.snapshot !== snapshotProxy)
dataGrid.setDataSource(this, snapshotProxy);
if (dataGrid === this.diffDataGrid) {
if (!this._baseProfileUid)
this._baseProfileUid = this._profiles()[this.baseSelectElement.selectedIndex].uid;
this.baseProfile.load(didLoadBaseSnaphot.bind(this));
}
}

function didLoadBaseSnaphot(baseSnapshotProxy)
{
if (this.diffDataGrid.baseSnapshot !== baseSnapshotProxy)
this.diffDataGrid.setBaseDataSource(baseSnapshotProxy);
}
},

_onSelectedViewChanged: function(event)
{
this._changeView(event.target.selectedIndex);
},

_updateSelectorsVisibility: function()
{
if (this.currentView === this.diffView)
this.baseSelectElement.parentElement.removeStyleClass("hidden");
else
this.baseSelectElement.parentElement.addStyleClass("hidden");

if (this.currentView === this.constructorsView)
this.filterSelectElement.parentElement.removeStyleClass("hidden");
else
this.filterSelectElement.parentElement.addStyleClass("hidden");
},

_changeView: function(selectedIndex)
{
if (selectedIndex === this.views.current)
return;

this.views.current = selectedIndex;
this.currentView.detach();
var view = this.views[this.views.current];
this.currentView = view.view;
this.dataGrid = view.grid;
this.currentView.show(this.viewsContainer);
this.refreshVisibleData();
this.dataGrid.updateWidths();

this._updateSelectorsVisibility();

this._updateDataSourceAndView();

if (!this.currentQuery || !this._searchFinishedCallback || !this._searchResults)
return;




this._searchFinishedCallback(this, -this._searchResults.length);
this.performSearch(this.currentQuery, this._searchFinishedCallback);
},

_getHoverAnchor: function(target)
{
var span = target.enclosingNodeOrSelfWithNodeName("span");
if (!span)
return;
var row = target.enclosingNodeOrSelfWithNodeName("tr");
if (!row)
return;
span.node = row._dataGridNode;
return span;
},

_resolveObjectForPopover: function(element, showCallback, objectGroupName)
{
if (this.profile.fromFile())
return;
element.node.queryObjectContent(showCallback, objectGroupName);
},

_helpClicked: function(event)
{
if (!this._helpPopoverContentElement) {
var refTypes = ["a:", "console-formatted-name", WebInspector.UIString("property"),
"0:", "console-formatted-name", WebInspector.UIString("element"),
"a:", "console-formatted-number", WebInspector.UIString("context var"),
"a:", "console-formatted-null", WebInspector.UIString("system prop")];
var objTypes = [" a ", "console-formatted-object", "Object",
"\"a\"", "console-formatted-string", "String",
"/a/", "console-formatted-string", "RegExp",
"a()", "console-formatted-function", "Function",
"a[]", "console-formatted-object", "Array",
"num", "console-formatted-number", "Number",
" a ", "console-formatted-null", "System"];

var contentElement = document.createElement("table");
contentElement.className = "heap-snapshot-help";
var headerRow = document.createElement("tr");
var propsHeader = document.createElement("th");
propsHeader.textContent = WebInspector.UIString("Property types:");
headerRow.appendChild(propsHeader);
var objsHeader = document.createElement("th");
objsHeader.textContent = WebInspector.UIString("Object types:");
headerRow.appendChild(objsHeader);
contentElement.appendChild(headerRow);

function appendHelp(help, index, cell)
{
var div = document.createElement("div");
div.className = "source-code event-properties";
var name = document.createElement("span");
name.textContent = help[index];
name.className = help[index + 1];
div.appendChild(name);
var desc = document.createElement("span");
desc.textContent = " " + help[index + 2];
div.appendChild(desc);
cell.appendChild(div);
}

var len = Math.max(refTypes.length, objTypes.length);
for (var i = 0; i < len; i += 3) {
var row = document.createElement("tr");
var refCell = document.createElement("td");
if (refTypes[i])
appendHelp(refTypes, i, refCell);
row.appendChild(refCell);
var objCell = document.createElement("td");
if (objTypes[i])
appendHelp(objTypes, i, objCell);
row.appendChild(objCell);
contentElement.appendChild(row);
}
this._helpPopoverContentElement = contentElement;
this.helpPopover = new WebInspector.Popover();
}
if (this.helpPopover.isShowing())
this.helpPopover.hide();
else
this.helpPopover.show(this._helpPopoverContentElement, this.helpButton.element);
},


_startRetainersHeaderDragging: function(event)
{
if (!this.isShowing())
return false;

this._previousDragPosition = event.pageY;
return true;
},

_retainersHeaderDragging: function(event)
{
var height = this.retainmentView.element.clientHeight;
height += this._previousDragPosition - event.pageY;
this._previousDragPosition = event.pageY;
this._updateRetainmentViewHeight(height);
event.consume(true);
},

_endRetainersHeaderDragging: function(event)
{
delete this._previousDragPosition;
event.consume();
},

_updateRetainmentViewHeight: function(height)
{
height = Number.constrain(height, Preferences.minConsoleHeight, this.element.clientHeight - Preferences.minConsoleHeight);
this.viewsContainer.style.bottom = (height + this.retainmentViewHeader.clientHeight) + "px";
this.retainmentView.element.style.height = height + "px";
this.retainmentViewHeader.style.bottom = height + "px";
},

_updateBaseOptions: function()
{
var list = this._profiles();

if (this.baseSelectElement.length === list.length)
return;

for (var i = this.baseSelectElement.length, n = list.length; i < n; ++i) {
var baseOption = document.createElement("option");
var title = list[i].title;
if (!title.indexOf(UserInitiatedProfileName))
title = WebInspector.UIString("Snapshot %d", title.substring(UserInitiatedProfileName.length + 1));
baseOption.label = title;
this.baseSelectElement.appendChild(baseOption);
}
},

_updateFilterOptions: function()
{
var list = this._profiles();

if (this.filterSelectElement.length - 1 === list.length)
return;

if (!this.filterSelectElement.length) {
var filterOption = document.createElement("option");
filterOption.label = WebInspector.UIString("All objects");
this.filterSelectElement.appendChild(filterOption);
}

if (this.profile.fromFile())
return;
for (var i = this.filterSelectElement.length - 1, n = list.length; i < n; ++i) {
var profile = list[i];
var filterOption = document.createElement("option");
var title = list[i].title;
if (!title.indexOf(UserInitiatedProfileName)) {
if (!i)
title = WebInspector.UIString("Objects allocated before Snapshot %d", title.substring(UserInitiatedProfileName.length + 1));
else
title = WebInspector.UIString("Objects allocated between Snapshots %d and %d", title.substring(UserInitiatedProfileName.length + 1) - 1, title.substring(UserInitiatedProfileName.length + 1));
}
filterOption.label = title;
this.filterSelectElement.appendChild(filterOption);
}
},

__proto__: WebInspector.View.prototype
}



WebInspector.HeapSnapshotProfileType = function()
{
WebInspector.ProfileType.call(this, WebInspector.HeapSnapshotProfileType.TypeId, WebInspector.UIString("Take Heap Snapshot"));
}

WebInspector.HeapSnapshotProfileType.TypeId = "HEAP";

WebInspector.HeapSnapshotProfileType.prototype = {
get buttonTooltip()
{
return WebInspector.UIString("Take heap snapshot.");
},


buttonClicked: function(profilesPanel)
{
profilesPanel.takeHeapSnapshot();
return false;
},

get treeItemTitle()
{
return WebInspector.UIString("HEAP SNAPSHOTS");
},

get description()
{
return WebInspector.UIString("Heap snapshot profiles show memory distribution among your page's JavaScript objects and related DOM nodes.");
},


createTemporaryProfile: function(title)
{
title = title || WebInspector.UIString("Snapshotting\u2026");
return new WebInspector.HeapProfileHeader(this, title);
},


createProfile: function(profile)
{
return new WebInspector.HeapProfileHeader(this, profile.title, profile.uid, profile.maxJSObjectId || 0);
},

__proto__: WebInspector.ProfileType.prototype
}


WebInspector.HeapProfileHeader = function(type, title, uid, maxJSObjectId)
{
WebInspector.ProfileHeader.call(this, type, title, uid);
this.maxJSObjectId = maxJSObjectId;

this._receiver = null;

this._snapshotProxy = null;
this._totalNumberOfChunks = 0;
}

WebInspector.HeapProfileHeader.prototype = {

createSidebarTreeElement: function()
{
return new WebInspector.ProfileSidebarTreeElement(this, WebInspector.UIString("Snapshot %d"), "heap-snapshot-sidebar-tree-item");
},


createView: function(profilesPanel)
{
return new WebInspector.HeapSnapshotView(profilesPanel, this);
},

snapshotProxy: function()
{
return this._snapshotProxy;
},


load: function(callback)
{
if (this._snapshotProxy) {
callback(this._snapshotProxy);
return;
}

this._numberOfChunks = 0;
this._savedChunks = 0;
this._savingToFile = false;
if (!this._receiver) {
this._setupWorker();
this.sidebarElement.subtitle = WebInspector.UIString("Loading\u2026");
this.sidebarElement.wait = true;
ProfilerAgent.getProfile(this.profileType().id, this.uid);
}
var loaderProxy =   (this._receiver);
loaderProxy.addConsumer(callback);
},

_setupWorker: function()
{
function setProfileWait(event)
{
this.sidebarElement.wait = event.data;
}
var worker = new WebInspector.HeapSnapshotWorker();
worker.addEventListener("wait", setProfileWait, this);
var loaderProxy = worker.createObject("WebInspector.HeapSnapshotLoader");
loaderProxy.addConsumer(this._snapshotReceived.bind(this));
this._receiver = loaderProxy;
},

dispose: function()
{
if (this._receiver)
this._receiver.close();
else if (this._snapshotProxy)
this._snapshotProxy.dispose();
},


_updateTransferProgress: function(value, maxValue)
{
var percentValue = ((maxValue ? (value / maxValue) : 0) * 100).toFixed(2);
if (this._savingToFile)
this.sidebarElement.subtitle = WebInspector.UIString("Saving\u2026 %d\%", percentValue);
else
this.sidebarElement.subtitle = WebInspector.UIString("Loading\u2026 %d\%", percentValue);
},

_updateSnapshotStatus: function()
{
this.sidebarElement.subtitle = Number.bytesToString(this._snapshotProxy.totalSize);
this.sidebarElement.wait = false;
},


transferChunk: function(chunk)
{
++this._numberOfChunks;
this._receiver.write(chunk, callback.bind(this));
function callback()
{
this._updateTransferProgress(++this._savedChunks, this._totalNumberOfChunks);
if (this._totalNumberOfChunks === this._savedChunks) {
if (this._savingToFile)
this._updateSnapshotStatus();
else
this.sidebarElement.subtitle = WebInspector.UIString("Parsing\u2026");

this._receiver.close();
}
}
},

_snapshotReceived: function(snapshotProxy)
{
this._receiver = null;
if (snapshotProxy)
this._snapshotProxy = snapshotProxy;
this._updateSnapshotStatus();
var worker =   (this._snapshotProxy.worker);
this.isTemporary = false;
worker.startCheckingForLongRunningCalls();
},

finishHeapSnapshot: function()
{
this._totalNumberOfChunks = this._numberOfChunks;
},


canSaveToFile: function()
{
return !this.fromFile() && !!this._snapshotProxy && !this._receiver;
},


saveToFile: function()
{
this._numberOfChunks = 0;
function onOpen()
{
this._savedChunks = 0;
this._updateTransferProgress(0, this._totalNumberOfChunks);
ProfilerAgent.getProfile(this.profileType().id, this.uid);
}
this._savingToFile = true;
this._fileName = this._fileName || "Heap-" + new Date().toISO8601Compact() + ".heapsnapshot";
this._receiver = new WebInspector.FileOutputStream();
this._receiver.open(this._fileName, onOpen.bind(this));
},


canLoadFromFile: function()
{
return false;
},


loadFromFile: function(file)
{
this.title = file.name;
this.sidebarElement.subtitle = WebInspector.UIString("Loading\u2026");
this.sidebarElement.wait = true;
this._setupWorker();
this._numberOfChunks = 0;
this._savingToFile = false;

var delegate = new WebInspector.HeapSnapshotLoadFromFileDelegate(this);
var fileReader = this._createFileReader(file, delegate);
fileReader.start(this._receiver);
},

_createFileReader: function(file, delegate)
{
return new WebInspector.ChunkedFileReader(file, 10000000, delegate);
},

__proto__: WebInspector.ProfileHeader.prototype
}


WebInspector.HeapSnapshotLoadFromFileDelegate = function(snapshotHeader)
{
this._snapshotHeader = snapshotHeader;
}

WebInspector.HeapSnapshotLoadFromFileDelegate.prototype = {
onTransferStarted: function()
{
},


onChunkTransferred: function(reader)
{
this._snapshotHeader._updateTransferProgress(reader.loadedSize(), reader.fileSize());
},

onTransferFinished: function()
{
this._snapshotHeader.finishHeapSnapshot();
},


onError: function (reader, e)
{
switch(e.target.error.code) {
case e.target.error.NOT_FOUND_ERR:
this._snapshotHeader.sidebarElement.subtitle = WebInspector.UIString("'%s' not found.", reader.fileName());
break;
case e.target.error.NOT_READABLE_ERR:
this._snapshotHeader.sidebarElement.subtitle = WebInspector.UIString("'%s' is not readable", reader.fileName());
break;
case e.target.error.ABORT_ERR:
break;
default:
this._snapshotHeader.sidebarElement.subtitle = WebInspector.UIString("'%s' error %d", reader.fileName(), e.target.error.code);
}
}
}
;



WebInspector.HeapSnapshotWorkerDispatcher = function(globalObject, postMessage)
{
this._objects = [];
this._global = globalObject;
this._postMessage = postMessage;
}

WebInspector.HeapSnapshotWorkerDispatcher.prototype = {
_findFunction: function(name)
{
var path = name.split(".");
var result = this._global;
for (var i = 0; i < path.length; ++i)
result = result[path[i]];
return result;
},

dispatchMessage: function(event)
{
var data = event.data;
var response = {callId: data.callId};
try {
switch (data.disposition) {
case "create": {
var constructorFunction = this._findFunction(data.methodName);
this._objects[data.objectId] = new constructorFunction();
break;
}
case "dispose": {
delete this._objects[data.objectId];
break;
}
case "getter": {
var object = this._objects[data.objectId];
var result = object[data.methodName];
response.result = result;
break;
}
case "factory": {
var object = this._objects[data.objectId];
var result = object[data.methodName].apply(object, data.methodArguments);
if (result)
this._objects[data.newObjectId] = result;
response.result = !!result;
break;
}
case "method": {
var object = this._objects[data.objectId];
response.result = object[data.methodName].apply(object, data.methodArguments);
break;
}
}
} catch (e) {
response.error = e.toString();
response.errorCallStack = e.stack;
if (data.methodName)
response.errorMethodName = data.methodName;
}
this._postMessage(response);
}
};
;



WebInspector.NativeMemorySnapshotView = function(profile)
{
WebInspector.View.call(this);
this.registerRequiredCSS("nativeMemoryProfiler.css");

this.element.addStyleClass("native-snapshot-view");
this.containmentDataGrid = new WebInspector.NativeSnapshotDataGrid(profile._memoryBlock);
this.containmentDataGrid.show(this.element);
}

WebInspector.NativeMemorySnapshotView.prototype = {
__proto__: WebInspector.View.prototype
}


WebInspector.NativeSnapshotDataGrid = function(profile)
{
var columns = {
object: { title: WebInspector.UIString("Object"), width: "200px", disclosure: true, sortable: false },
size: { title: WebInspector.UIString("Size"), sortable: false },
};
WebInspector.DataGrid.call(this, columns);
this.setRootNode(new WebInspector.DataGridNode(null, true));
var totalNode = new WebInspector.NativeSnapshotNode(profile, profile);
this.rootNode().appendChild(totalNode);
totalNode.expand();
}

WebInspector.NativeSnapshotDataGrid.prototype = {
__proto__: WebInspector.DataGrid.prototype
}


WebInspector.NativeSnapshotNode = function(nodeData, profile)
{
this._nodeData = nodeData;
this._profile = profile;
var viewProperties = WebInspector.MemoryBlockViewProperties._forMemoryBlock(nodeData);
var data = { object: viewProperties._description, size: this._nodeData.size };
var hasChildren = !!nodeData.children && nodeData.children.length !== 0;
WebInspector.DataGridNode.call(this, data, hasChildren);
this.addEventListener("populate", this._populate, this);
}

WebInspector.NativeSnapshotNode.prototype = {

createCell: function(columnIdentifier)
{
var cell = columnIdentifier === "size" ?
this._createSizeCell(columnIdentifier) :
WebInspector.DataGridNode.prototype.createCell.call(this, columnIdentifier);
return cell;
},


_createSizeCell: function(columnIdentifier)
{
var node = this;
var viewProperties = null;
while (!viewProperties || viewProperties._fillStyle === "inherit") {
viewProperties = WebInspector.MemoryBlockViewProperties._forMemoryBlock(node._nodeData);
node = node.parent;
}

var sizeKiB = this._nodeData.size / 1024;
var totalSize = this._profile.size;
var percentage = this._nodeData.size / totalSize  * 100;

var cell = document.createElement("td");
cell.className = columnIdentifier + "-column";

var textDiv = document.createElement("div");
textDiv.textContent = Number.withThousandsSeparator(sizeKiB.toFixed(0)) + "\u2009" + WebInspector.UIString("KiB");
textDiv.className = "size-text";
cell.appendChild(textDiv);

var barDiv = document.createElement("div");
barDiv.className = "size-bar";
barDiv.style.width = percentage + "%";
barDiv.style.backgroundColor = viewProperties._fillStyle;

var fillerDiv = document.createElement("div");
fillerDiv.className = "percent-text"
barDiv.appendChild(fillerDiv);
var percentDiv = document.createElement("div");
percentDiv.textContent = percentage.toFixed(1) + "%";
percentDiv.className = "percent-text"
barDiv.appendChild(percentDiv);

var barHolderDiv = document.createElement("div");
barHolderDiv.appendChild(barDiv);
cell.appendChild(barHolderDiv);

return cell;
},

_populate: function() {
this.removeEventListener("populate", this._populate, this);
function comparator(a, b) {
return b.size - a.size;
}
if (this._nodeData !== this._profile)
this._nodeData.children.sort(comparator);
for (var node in this._nodeData.children) {
var nodeData = this._nodeData.children[node];
if (WebInspector.settings.showNativeSnapshotUninstrumentedSize.get() || nodeData.name !== "Other")
this.appendChild(new WebInspector.NativeSnapshotNode(nodeData, this._profile));
}
},

__proto__: WebInspector.DataGridNode.prototype
}


WebInspector.NativeMemoryProfileType = function()
{
WebInspector.ProfileType.call(this, WebInspector.NativeMemoryProfileType.TypeId, WebInspector.UIString("Take Native Memory Snapshot"));
this._nextProfileUid = 1;
}

WebInspector.NativeMemoryProfileType.TypeId = "NATIVE_MEMORY";

WebInspector.NativeMemoryProfileType.prototype = {
get buttonTooltip()
{
return WebInspector.UIString("Take native memory snapshot.");
},


buttonClicked: function(profilesPanel)
{
var profileHeader = new WebInspector.NativeMemoryProfileHeader(this, WebInspector.UIString("Snapshot %d", this._nextProfileUid), this._nextProfileUid);
++this._nextProfileUid;
profileHeader.isTemporary = true;
profilesPanel.addProfileHeader(profileHeader);
function didReceiveMemorySnapshot(error, memoryBlock)
{
if (memoryBlock.size && memoryBlock.children) {
var knownSize = 0;
for (var i = 0; i < memoryBlock.children.length; i++) {
var size = memoryBlock.children[i].size;
if (size)
knownSize += size;
}
var otherSize = memoryBlock.size - knownSize;

if (otherSize) {
memoryBlock.children.push({
name: "Other",
size: otherSize
});
}
}
profileHeader._memoryBlock = memoryBlock;
profileHeader.isTemporary = false;
profileHeader.sidebarElement.subtitle = Number.bytesToString(memoryBlock.size);
}
MemoryAgent.getProcessMemoryDistribution(didReceiveMemorySnapshot.bind(this));
return false;
},

get treeItemTitle()
{
return WebInspector.UIString("MEMORY DISTRIBUTION");
},

get description()
{
return WebInspector.UIString("Native memory snapshot profiles show memory distribution among browser subsystems");
},


createTemporaryProfile: function(title)
{
title = title || WebInspector.UIString("Snapshotting\u2026");
return new WebInspector.NativeMemoryProfileHeader(this, title);
},


createProfile: function(profile)
{
return new WebInspector.NativeMemoryProfileHeader(this, profile.title, -1);
},

__proto__: WebInspector.ProfileType.prototype
}


WebInspector.NativeMemoryProfileHeader = function(type, title, uid)
{
WebInspector.ProfileHeader.call(this, type, title, uid);


this._memoryBlock = null;
}

WebInspector.NativeMemoryProfileHeader.prototype = {

createSidebarTreeElement: function()
{
return new WebInspector.ProfileSidebarTreeElement(this, WebInspector.UIString("Snapshot %d"), "heap-snapshot-sidebar-tree-item");
},


createView: function(profilesPanel)
{
return new WebInspector.NativeMemorySnapshotView(this);
},

__proto__: WebInspector.ProfileHeader.prototype
}


WebInspector.MemoryBlockViewProperties = function(fillStyle, name, description)
{
this._fillStyle = fillStyle;
this._name = name;
this._description = description;
}


WebInspector.MemoryBlockViewProperties._standardBlocks = null;

WebInspector.MemoryBlockViewProperties._initialize = function()
{
if (WebInspector.MemoryBlockViewProperties._standardBlocks)
return;
WebInspector.MemoryBlockViewProperties._standardBlocks = {};
function addBlock(fillStyle, name, description)
{
WebInspector.MemoryBlockViewProperties._standardBlocks[name] = new WebInspector.MemoryBlockViewProperties(fillStyle, name, WebInspector.UIString(description));
}
addBlock("hsl(  0,  0%,  60%)", "ProcessPrivateMemory", "Total");
addBlock("hsl(  0,  0%,  80%)", "OwnersTypePlaceholder", "OwnersTypePlaceholder");
addBlock("hsl(  0,  0%,  60%)", "Other", "Other");
addBlock("hsl(220, 80%,  70%)", "Page", "Page structures");
addBlock("hsl(100, 60%,  50%)", "JSHeap", "JavaScript heap");
addBlock("hsl( 90, 40%,  80%)", "JSExternalResources", "JavaScript external resources");
addBlock("hsl( 90, 60%,  80%)", "JSExternalArrays", "JavaScript external arrays");
addBlock("hsl( 90, 60%,  80%)", "JSExternalStrings", "JavaScript external strings");
addBlock("hsl(  0, 80%,  60%)", "WebInspector", "Inspector data");
addBlock("hsl( 36, 90%,  50%)", "MemoryCache", "Memory cache resources");
addBlock("hsl( 40, 80%,  80%)", "GlyphCache", "Glyph cache resources");
addBlock("hsl( 35, 80%,  80%)", "DOMStorageCache", "DOM storage cache");
addBlock("hsl( 60, 80%,  60%)", "RenderTree", "Render tree");
}

WebInspector.MemoryBlockViewProperties._forMemoryBlock = function(memoryBlock)
{
WebInspector.MemoryBlockViewProperties._initialize();
var result = WebInspector.MemoryBlockViewProperties._standardBlocks[memoryBlock.name];
if (result)
return result;
return new WebInspector.MemoryBlockViewProperties("inherit", memoryBlock.name, memoryBlock.name);
}



WebInspector.NativeMemoryPieChart = function(memorySnapshot)
{
WebInspector.View.call(this);
this._memorySnapshot = memorySnapshot;
this.element = document.createElement("div");
this.element.addStyleClass("memory-pie-chart-container");
this._memoryBlockList = this.element.createChild("div", "memory-blocks-list");

this._canvasContainer = this.element.createChild("div", "memory-pie-chart");
this._canvas = this._canvasContainer.createChild("canvas");
this._addBlockLabels(memorySnapshot, true);
}

WebInspector.NativeMemoryPieChart.prototype = {

onResize: function()
{
this._updateSize();
this._paint();
},

_updateSize: function()
{
var width = this._canvasContainer.clientWidth - 5;
var height = this._canvasContainer.clientHeight - 5;
this._canvas.width = width;
this._canvas.height = height;
},

_addBlockLabels: function(memoryBlock, includeChildren)
{
var viewProperties = WebInspector.MemoryBlockViewProperties._forMemoryBlock(memoryBlock);
var title = viewProperties._description + ": " + Number.bytesToString(memoryBlock.size);

var swatchElement = this._memoryBlockList.createChild("div", "item");
swatchElement.createChild("div", "swatch").style.backgroundColor = viewProperties._fillStyle;
swatchElement.createChild("span", "title").textContent = title;

if (!memoryBlock.children || !includeChildren)
return;
for (var i = 0; i < memoryBlock.children.length; i++)
this._addBlockLabels(memoryBlock.children[i], false);
},

_paint: function()
{
this._clear();
var width = this._canvas.width;
var height = this._canvas.height;

var x = width / 2;
var y = height / 2;
var radius = 200;

var ctx = this._canvas.getContext("2d");
ctx.beginPath();
ctx.arc(x, y, radius, 0, Math.PI*2, false);
ctx.lineWidth = 1;
ctx.strokeStyle = "rgba(130, 130, 130, 0.8)";
ctx.stroke();
ctx.closePath();

var currentAngle = 0;
var memoryBlock = this._memorySnapshot;

function paintPercentAndLabel(fraction, title, midAngle)
{
ctx.beginPath();
ctx.font = "13px Arial";
ctx.fillStyle = "rgba(10, 10, 10, 0.8)";

var textX = x + (radius + 10) * Math.cos(midAngle);
var textY = y + (radius + 10) * Math.sin(midAngle);
var relativeOffset = -Math.cos(midAngle) / Math.sin(Math.PI / 12);
relativeOffset = Number.constrain(relativeOffset, -1, 1);
var metrics = ctx.measureText(title);
textX -= metrics.width * (relativeOffset + 1) / 2;
textY += 5;
ctx.fillText(title, textX, textY);


if (fraction > 0.03) {
textX = x + radius * Math.cos(midAngle) / 2;
textY = y + radius * Math.sin(midAngle) / 2;
ctx.fillText((100 * fraction).toFixed(0) + "%", textX - 8, textY + 5);
}

ctx.closePath();
}

if (!memoryBlock.children)
return;
var total = memoryBlock.size;
for (var i = 0; i < memoryBlock.children.length; i++) {
var child = memoryBlock.children[i];
if (!child.size)
continue;
var viewProperties = WebInspector.MemoryBlockViewProperties._forMemoryBlock(child);
var angleSpan = Math.PI * 2 * (child.size / total);
ctx.beginPath();
ctx.moveTo(x, y);
ctx.lineTo(x + radius * Math.cos(currentAngle), y + radius * Math.sin(currentAngle));
ctx.arc(x, y, radius, currentAngle, currentAngle + angleSpan, false);
ctx.lineWidth = 0.5;
ctx.lineTo(x, y);
ctx.fillStyle = viewProperties._fillStyle;
ctx.strokeStyle = "rgba(100, 100, 100, 0.8)";
ctx.fill();
ctx.stroke();
ctx.closePath();

paintPercentAndLabel(child.size / total, viewProperties._description, currentAngle + angleSpan / 2);

currentAngle += angleSpan;
}
},

_clear: function() {
var ctx = this._canvas.getContext("2d");
ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
},

__proto__: WebInspector.View.prototype
}


WebInspector.NativeMemoryBarChart = function()
{
WebInspector.View.call(this);
this.registerRequiredCSS("nativeMemoryProfiler.css");
this._memorySnapshot = null;
this.element = document.createElement("div");
this._table = this.element.createChild("table");
this._divs = {};
var row = this._table.insertRow();
this._totalDiv = row.insertCell().createChild("div");
this._totalDiv.addStyleClass("memory-bar-chart-total");
row.insertCell();
}

WebInspector.NativeMemoryBarChart.prototype = {
_updateStats: function()
{
function didReceiveMemorySnapshot(error, memoryBlock)
{
if (memoryBlock.size && memoryBlock.children) {
var knownSize = 0;
for (var i = 0; i < memoryBlock.children.length; i++) {
var size = memoryBlock.children[i].size;
if (size)
knownSize += size;
}
var otherSize = memoryBlock.size - knownSize;

if (otherSize) {
memoryBlock.children.push({
name: "Other",
size: otherSize
});
}
}
this._memorySnapshot = memoryBlock;
this._updateView();
}
MemoryAgent.getProcessMemoryDistribution(didReceiveMemorySnapshot.bind(this));
},


willHide: function()
{
clearInterval(this._timerId);
},


wasShown: function()
{
this._timerId = setInterval(this._updateStats.bind(this), 1000);
},

_updateView: function()
{
var memoryBlock = this._memorySnapshot;
if (!memoryBlock)
return;

var MB = 1024 * 1024;
var maxSize = 100 * MB;
for (var i = 0; i < memoryBlock.children.length; ++i)
maxSize = Math.max(maxSize, memoryBlock.children[i].size);
var maxBarLength = 500;
var barLengthSizeRatio = maxBarLength / maxSize;

for (var i = memoryBlock.children.length - 1; i >= 0 ; --i) {
var child = memoryBlock.children[i];
var name = child.name;
var divs = this._divs[name];
if (!divs) {
var row = this._table.insertRow();
var nameDiv = row.insertCell(-1).createChild("div");
var viewProperties = WebInspector.MemoryBlockViewProperties._forMemoryBlock(child);
var title = viewProperties._description;
nameDiv.textContent = title;
nameDiv.addStyleClass("memory-bar-chart-name");
var barCell = row.insertCell(-1);
var barDiv = barCell.createChild("div");
barDiv.addStyleClass("memory-bar-chart-bar");
viewProperties = WebInspector.MemoryBlockViewProperties._forMemoryBlock(child);
barDiv.style.backgroundColor = viewProperties._fillStyle;
var unusedDiv = barDiv.createChild("div");
unusedDiv.addStyleClass("memory-bar-chart-unused");
var percentDiv = barDiv.createChild("div");
percentDiv.addStyleClass("memory-bar-chart-percent");
var sizeDiv = barCell.createChild("div");
sizeDiv.addStyleClass("memory-bar-chart-size");
divs = this._divs[name] = { barDiv: barDiv, unusedDiv: unusedDiv, percentDiv: percentDiv, sizeDiv: sizeDiv };
}
var unusedSize = 0;
if (!!child.children) {
var unusedName = name + ".Unused";
for (var j = 0; j < child.children.length; ++j) {
if (child.children[j].name === unusedName) {
unusedSize = child.children[j].size;
break;
}
}
}
var unusedLength = unusedSize * barLengthSizeRatio;
var barLength = child.size * barLengthSizeRatio;

divs.barDiv.style.width = barLength + "px";
divs.unusedDiv.style.width = unusedLength + "px";
divs.percentDiv.textContent = barLength > 20 ? (child.size / memoryBlock.size * 100).toFixed(0) + "%" : "";
divs.sizeDiv.textContent = (child.size / MB).toFixed(1) + "\u2009MB";
}

var memoryBlockViewProperties = WebInspector.MemoryBlockViewProperties._forMemoryBlock(memoryBlock);
this._totalDiv.textContent = memoryBlockViewProperties._description + ": " + (memoryBlock.size / MB).toFixed(1) + "\u2009MB";
},

__proto__: WebInspector.View.prototype
}
;



WebInspector.ProfileLauncherView = function(profilesPanel)
{
WebInspector.View.call(this);

this._panel = profilesPanel;
this._profileRunning = false;

this.element.addStyleClass("profile-launcher-view");
this.element.addStyleClass("panel-enabler-view");

this._contentElement = document.createElement("div");
this._contentElement.className = "profile-launcher-view-content";
this.element.appendChild(this._contentElement);

var header = this._contentElement.createChild("h1");
header.textContent = WebInspector.UIString("Select profiling type");

this._profileTypeSelectorForm = this._contentElement.createChild("form");

if (WebInspector.experimentsSettings.liveNativeMemoryChart.isEnabled()) {
this._nativeMemoryElement = document.createElement("div");
this._contentElement.appendChild(this._nativeMemoryElement);
this._nativeMemoryLiveChart = new WebInspector.NativeMemoryBarChart();
this._nativeMemoryLiveChart.show(this._nativeMemoryElement);
}

this._contentElement.createChild("div", "flexible-space");

this._controlButton = this._contentElement.createChild("button", "control-profiling");
this._controlButton.addEventListener("click", this._controlButtonClicked.bind(this), false);
this._updateControls();
}

WebInspector.ProfileLauncherView.EventTypes = {
ProfileTypeSelected: "profile-type-selected"
}

WebInspector.ProfileLauncherView.prototype = {

addProfileType: function(profileType)
{
var checked = !this._profileTypeSelectorForm.children.length;
var labelElement = this._profileTypeSelectorForm.createChild("label");
labelElement.textContent = profileType.name;
var optionElement = document.createElement("input");
labelElement.insertBefore(optionElement, labelElement.firstChild);
optionElement.type = "radio";
optionElement.name = "profile-type";
if (checked) {
optionElement.checked = checked;
this.dispatchEventToListeners(WebInspector.ProfileLauncherView.EventTypes.ProfileTypeSelected, profileType);
}
optionElement.addEventListener("change", this._profileTypeChanged.bind(this, profileType), false);
var descriptionElement = labelElement.createChild("p");
descriptionElement.textContent = profileType.description;
},

_controlButtonClicked: function()
{
this._panel.toggleRecordButton();
},

_updateControls: function()
{
if (this._isProfiling) {
this._profileTypeSelectorForm.disabled = true;
this._controlButton.addStyleClass("running");
this._controlButton.textContent = WebInspector.UIString("Stop");
} else {
this._profileTypeSelectorForm.disabled = false;
this._controlButton.removeStyleClass("running");
this._controlButton.textContent = WebInspector.UIString("Start");
}
},


_profileTypeChanged: function(profileType, event)
{
this.dispatchEventToListeners(WebInspector.ProfileLauncherView.EventTypes.ProfileTypeSelected, profileType);
},

profileStarted: function()
{
this._isProfiling = true;
this._updateControls();
},

profileFinished: function()
{
this._isProfiling = false;
this._updateControls();
},

__proto__: WebInspector.View.prototype
}
;



WebInspector.TopDownProfileDataGridNode = function(  profileView,   profileNode,   owningTree)
{
var hasChildren = (profileNode.children && profileNode.children.length);

WebInspector.ProfileDataGridNode.call(this, profileView, profileNode, owningTree, hasChildren);

this._remainingChildren = profileNode.children;
}

WebInspector.TopDownProfileDataGridNode.prototype = {
_sharedPopulate: function()
{
var children = this._remainingChildren;
var childrenLength = children.length;

for (var i = 0; i < childrenLength; ++i)
this.appendChild(new WebInspector.TopDownProfileDataGridNode(this.profileView, children[i], this.tree));

this._remainingChildren = null;
},

_exclude: function(aCallUID)
{
if (this._remainingChildren)
this._populate();

this._save();

var children = this.children;
var index = this.children.length;

while (index--)
children[index]._exclude(aCallUID);

var child = this.childrenByCallUID[aCallUID];

if (child)
this._merge(child, true);
},

__proto__: WebInspector.ProfileDataGridNode.prototype
}


WebInspector.TopDownProfileDataGridTree = function(  profileView,   profileNode)
{
WebInspector.ProfileDataGridTree.call(this, profileView, profileNode);

this._remainingChildren = profileNode.children;

var any =  this;
var node =  any;
WebInspector.TopDownProfileDataGridNode.prototype._populate.call(node);
}

WebInspector.TopDownProfileDataGridTree.prototype = {
focus: function(  profileDataGrideNode)
{
if (!profileDataGrideNode)
return;

this._save();
profileDataGrideNode.savePosition();

this.children = [profileDataGrideNode];
this.totalTime = profileDataGrideNode.totalTime;
},

exclude: function(  profileDataGrideNode)
{
if (!profileDataGrideNode)
return;

this._save();

var excludedCallUID = profileDataGrideNode.callUID;

var any =  this;
var node =  any;
WebInspector.TopDownProfileDataGridNode.prototype._exclude.call(node, excludedCallUID);

if (this.lastComparator)
this.sort(this.lastComparator, true);
},

restore: function()
{
if (!this._savedChildren)
return;

this.children[0].restorePosition();

WebInspector.ProfileDataGridTree.prototype.restore.call(this);
},

_merge: WebInspector.TopDownProfileDataGridNode.prototype._merge,

_sharedPopulate: WebInspector.TopDownProfileDataGridNode.prototype._sharedPopulate,

__proto__: WebInspector.ProfileDataGridTree.prototype
}
;



WebInspector.CanvasProfileView = function(profile)
{
WebInspector.View.call(this);
this.registerRequiredCSS("canvasProfiler.css");
this._profile = profile;
this._traceLogId = profile.traceLogId();
this.element.addStyleClass("canvas-profile-view");

this._linkifier = new WebInspector.Linkifier();
this._splitView = new WebInspector.SplitView(false, "canvasProfileViewSplitLocation", 300);

var columns = { 0: {}, 1: {}, 2: {} };
columns[0].title = "#";
columns[0].sortable = true;
columns[0].width = "5%";
columns[1].title = WebInspector.UIString("Call");
columns[1].sortable = true;
columns[1].width = "75%";
columns[2].title = WebInspector.UIString("Location");
columns[2].sortable = true;
columns[2].width = "20%";

this._logGrid = new WebInspector.DataGrid(columns);
this._logGrid.element.addStyleClass("fill");
this._logGrid.show(this._splitView.secondElement());
this._logGrid.addEventListener(WebInspector.DataGrid.Events.SelectedNode, this._replayTraceLog.bind(this));

var replayImageContainer = this._splitView.firstElement();
replayImageContainer.id = "canvas-replay-image-container";

this._replayImageElement = document.createElement("image");
this._replayImageElement.id = "canvas-replay-image";

replayImageContainer.appendChild(this._replayImageElement);
this._debugInfoElement = document.createElement("div");
replayImageContainer.appendChild(this._debugInfoElement);

this._splitView.show(this.element);

this._enableWaitIcon(true);
CanvasAgent.getTraceLog(this._traceLogId, this._didReceiveTraceLog.bind(this));
}

WebInspector.CanvasProfileView.prototype = {
dispose: function()
{
this._linkifier.reset();
CanvasAgent.dropTraceLog(this._traceLogId);
},

get statusBarItems()
{
return [];
},

get profile()
{
return this._profile;
},


elementsToRestoreScrollPositionsFor: function()
{
return [this._logGrid.scrollContainer];
},


_enableWaitIcon: function(enable)
{
this._replayImageElement.className = enable ? "wait" : "";
},

_replayTraceLog: function()
{
var callNode = this._logGrid.selectedNode;
if (!callNode)
return;
var time = Date.now();
function didReplayTraceLog(error, dataURL)
{
this._enableWaitIcon(false);
if (error)
return;
this._debugInfoElement.textContent = "Replay time: " + (Date.now() - time) + "ms";
this._replayImageElement.src = dataURL;
}
this._enableWaitIcon(true);
CanvasAgent.replayTraceLog(this._traceLogId, callNode.index, didReplayTraceLog.bind(this));
},

_didReceiveTraceLog: function(error, traceLog)
{
this._enableWaitIcon(false);
this._logGrid.rootNode().removeChildren();
if (error || !traceLog)
return;
var calls = traceLog.calls;
for (var i = 0, n = calls.length; i < n; ++i)
this._logGrid.rootNode().appendChild(this._createCallNode(i, calls[i]));
var lastNode = this._logGrid.rootNode().children[calls.length - 1];
if (lastNode) {
lastNode.reveal();
lastNode.select();
}
},

_createCallNode: function(index, call)
{
var traceLogItem = document.createElement("div");
var data = {};
data[0] = index + 1;
data[1] = call.functionName || "context." + call.property;
data[2] = "";
if (call.sourceURL) {

var lineNumber = Math.max(0, call.lineNumber - 1) || 0;
var columnNumber = Math.max(0, call.columnNumber - 1) || 0;
data[2] = this._linkifier.linkifyLocation(call.sourceURL, lineNumber, columnNumber);
}

if (call.arguments)
data[1] += "(" + call.arguments.join(", ") + ")";
else
data[1] += " = " + call.value;

if (typeof call.result !== "undefined")
data[1] += " => " + call.result;

var node = new WebInspector.DataGridNode(data);
node.call = call;
node.index = index;
node.selectable = true;
return node;
},

__proto__: WebInspector.View.prototype
}


WebInspector.CanvasProfileType = function()
{
WebInspector.ProfileType.call(this, WebInspector.CanvasProfileType.TypeId, WebInspector.UIString("Capture Canvas Frame"));
this._nextProfileUid = 1;

CanvasAgent.enable();
}

WebInspector.CanvasProfileType.TypeId = "CANVAS_PROFILE";

WebInspector.CanvasProfileType.prototype = {
get buttonTooltip()
{
return WebInspector.UIString("Capture Canvas Frame.");
},


buttonClicked: function(profilesPanel)
{
var profileHeader = new WebInspector.CanvasProfileHeader(this, WebInspector.UIString("Trace Log %d", this._nextProfileUid), this._nextProfileUid);
++this._nextProfileUid;
profileHeader.isTemporary = true;
profilesPanel.addProfileHeader(profileHeader);
function didStartCapturingFrame(error, traceLogId)
{
profileHeader._traceLogId = traceLogId;
profileHeader.isTemporary = false;
}
CanvasAgent.captureFrame(didStartCapturingFrame.bind(this));
return false;
},

get treeItemTitle()
{
return WebInspector.UIString("CANVAS PROFILE");
},

get description()
{
return WebInspector.UIString("Canvas calls instrumentation");
},


reset: function()
{
this._nextProfileUid = 1;
},


createTemporaryProfile: function(title)
{
title = title || WebInspector.UIString("Capturing\u2026");
return new WebInspector.CanvasProfileHeader(this, title);
},


createProfile: function(profile)
{
return new WebInspector.CanvasProfileHeader(this, profile.title, -1);
},

__proto__: WebInspector.ProfileType.prototype
}


WebInspector.CanvasProfileHeader = function(type, title, uid)
{
WebInspector.ProfileHeader.call(this, type, title, uid);


this._traceLogId = null;
}

WebInspector.CanvasProfileHeader.prototype = {

traceLogId: function()
{
return this._traceLogId;
},


createSidebarTreeElement: function()
{
return new WebInspector.ProfileSidebarTreeElement(this, WebInspector.UIString("Trace Log %d"), "profile-sidebar-tree-item");
},


createView: function(profilesPanel)
{
return new WebInspector.CanvasProfileView(this);
},

__proto__: WebInspector.ProfileHeader.prototype
}
;
