





WebInspector.MemoryStatistics = function(timelinePanel, model, sidebarWidth)
{
this._timelinePanel = timelinePanel;
this._counters = [];

model.addEventListener(WebInspector.TimelineModel.Events.RecordAdded, this._onRecordAdded, this);
model.addEventListener(WebInspector.TimelineModel.Events.RecordsCleared, this._onRecordsCleared, this);

this._containerAnchor = timelinePanel.element.lastChild;
this._memorySidebarView = new WebInspector.SidebarView(WebInspector.SidebarView.SidebarPosition.Left, undefined, sidebarWidth);
this._memorySidebarView.sidebarElement.addStyleClass("sidebar");
this._memorySidebarView.element.id = "memory-graphs-container";

this._memorySidebarView.addEventListener(WebInspector.SidebarView.EventTypes.Resized, this._sidebarResized.bind(this));

this._canvasContainer = this._memorySidebarView.mainElement;
this._canvasContainer.id = "memory-graphs-canvas-container";
this._currentValuesBar = this._canvasContainer.createChild("div");
this._currentValuesBar.id = "counter-values-bar";
this._canvas = this._canvasContainer.createChild("canvas");
this._canvas.id = "memory-counters-graph";
this._lastMarkerXPosition = 0;

this._canvas.addEventListener("mouseover", this._onMouseOver.bind(this), true);
this._canvas.addEventListener("mousemove", this._onMouseMove.bind(this), true);
this._canvas.addEventListener("mouseout", this._onMouseOut.bind(this), true);
this._canvas.addEventListener("click", this._onClick.bind(this), true);

this._timelineGrid = new WebInspector.TimelineGrid();
this._canvasContainer.appendChild(this._timelineGrid.dividersElement);


this._memorySidebarView.sidebarElement.createChild("div", "sidebar-tree sidebar-tree-section").textContent = WebInspector.UIString("COUNTERS");
function getDocumentCount(entry)
{
return entry.documentCount;
}
function getNodeCount(entry)
{
return entry.nodeCount;
}
function getListenerCount(entry)
{
return entry.listenerCount;
}
this._counterUI = [
new WebInspector.CounterUI(this, "Document Count", "Documents: %d", [100,0,0], getDocumentCount),
new WebInspector.CounterUI(this, "DOM Node Count", "Nodes: %d", [0,100,0], getNodeCount),
new WebInspector.CounterUI(this, "Event Listener Count", "Listeners: %d", [0,0,100], getListenerCount)
];

TimelineAgent.setIncludeMemoryDetails(true);
}


WebInspector.SwatchCheckbox = function(title, color)
{
this.element = document.createElement("div");
this._swatch = this.element.createChild("div", "swatch");
this.element.createChild("span", "title").textContent = title;
this._color = color;
this.checked = true;

this.element.addEventListener("click", this._toggleCheckbox.bind(this), true);
}

WebInspector.SwatchCheckbox.Events = {
Changed: "Changed"
}

WebInspector.SwatchCheckbox.prototype = {
get checked()
{
return this._checked;
},

set checked(v)
{
this._checked = v;
if (this._checked)
this._swatch.style.backgroundColor = this._color;
else
this._swatch.style.backgroundColor = "";
},

_toggleCheckbox: function(event)
{
this.checked = !this.checked;
this.dispatchEventToListeners(WebInspector.SwatchCheckbox.Events.Changed);
},

__proto__: WebInspector.Object.prototype
}


WebInspector.CounterUI = function(memoryCountersPane, title, currentValueLabel, rgb, valueGetter)
{
this._memoryCountersPane = memoryCountersPane;
this.valueGetter = valueGetter;
var container = memoryCountersPane._memorySidebarView.sidebarElement.createChild("div", "memory-counter-sidebar-info");
var swatchColor = "rgb(" + rgb.join(",") + ")";
this._swatch = new WebInspector.SwatchCheckbox(WebInspector.UIString(title), swatchColor);
this._swatch.addEventListener(WebInspector.SwatchCheckbox.Events.Changed, this._toggleCounterGraph.bind(this));
container.appendChild(this._swatch.element);
this._range = this._swatch.element.createChild("span");

this._value = memoryCountersPane._currentValuesBar.createChild("span", "memory-counter-value");
this._value.style.color = swatchColor;
this._currentValueLabel = currentValueLabel;

this.graphColor = "rgba(" + rgb.join(",") + ",0.8)";
this.graphYValues = [];
}

WebInspector.CounterUI.prototype = {
_toggleCounterGraph: function(event)
{
if (this._swatch.checked)
this._value.removeStyleClass("hidden");
else
this._value.addStyleClass("hidden");
this._memoryCountersPane.refresh();
},

setRange: function(minValue, maxValue)
{
this._range.textContent = WebInspector.UIString("[ %d - %d ]", minValue, maxValue);
},

updateCurrentValue: function(countersEntry)
{
this._value.textContent =  WebInspector.UIString(this._currentValueLabel, this.valueGetter(countersEntry));
},

clearCurrentValueAndMarker: function(ctx)
{
this._value.textContent = "";
this.restoreImageUnderMarker(ctx);
},

get visible()
{
return this._swatch.checked;
},

saveImageUnderMarker: function(ctx, x, y, radius)
{
const w = radius + 1;
var imageData = ctx.getImageData(x - w, y - w, 2 * w, 2 * w);
this._imageUnderMarker = {
x: x - w,
y: y - w,
imageData: imageData };
},

restoreImageUnderMarker: function(ctx)
{
if (!this.visible)
return;
if (this._imageUnderMarker)
ctx.putImageData(this._imageUnderMarker.imageData, this._imageUnderMarker.x, this._imageUnderMarker.y);
this.discardImageUnderMarker();
},

discardImageUnderMarker: function()
{
delete this._imageUnderMarker;
}
}


WebInspector.MemoryStatistics.prototype = {
_onRecordsCleared: function()
{
this._counters = [];
},

setMainTimelineGrid: function(timelineGrid)
{
this._mainTimelineGrid = timelineGrid;
},

setTopPosition: function(top)
{
this._memorySidebarView.element.style.top = top + "px";
this._updateSize();
},

setSidebarWidth: function(width)
{
if (this._ignoreSidebarResize)
return;
this._ignoreSidebarResize = true;
this._memorySidebarView.setSidebarWidth(width);
this._ignoreSidebarResize = false;
},

_sidebarResized: function(event)
{
if (this._ignoreSidebarResize)
return;
this._ignoreSidebarResize = true;
this._timelinePanel.splitView.setSidebarWidth(event.data);
this._ignoreSidebarResize = false;
},

_updateSize: function()
{
var width = this._mainTimelineGrid.dividersElement.offsetWidth + 1;
this._canvasContainer.style.width = width + "px";

var height = this._canvasContainer.offsetHeight - this._currentValuesBar.offsetHeight;
this._canvas.width = width;
this._canvas.height = height;
},

_onRecordAdded: function(event)
{
var statistics = this._counters;
function addStatistics(record)
{
var counters = record["counters"];
if (!counters)
return;
statistics.push({
time: record.endTime || record.startTime,
documentCount: counters["documents"],
nodeCount: counters["nodes"],
listenerCount: counters["jsEventListeners"]
});
}
WebInspector.TimelinePresentationModel.forAllRecords([event.data], null, addStatistics);
},

_draw: function()
{
this._calculateVisibleIndexes();
this._calculateXValues();
this._clear();

this._setVerticalClip(10, this._canvas.height - 20);
for (var i = 0; i < this._counterUI.length; i++)
this._drawGraph(this._counterUI[i]);
},

_calculateVisibleIndexes: function()
{
var calculator = this._timelinePanel.calculator;
var start = calculator.minimumBoundary() * 1000;
var end = calculator.maximumBoundary() * 1000;
var firstIndex = 0;
var lastIndex = this._counters.length - 1;
for (var i = 0; i < this._counters.length; i++) {
var time = this._counters[i].time;
if (time <= start) {
firstIndex = i;
} else {
if (end < time)
break;
lastIndex = i;
}
}

this._minimumIndex = firstIndex;


this._maximumIndex = lastIndex;


this._minTime = start;
this._maxTime = end;
},

_onClick: function(event)
{
var x = event.x - event.target.offsetParent.offsetLeft
var i = this._recordIndexAt(x);
var counter = this._counters[i];
this._timelinePanel.revealRecordAt(counter.time / 1000);
},

_onMouseOut: function(event)
{
delete this._markerXPosition;

var ctx = this._canvas.getContext("2d");
for (var i = 0; i < this._counterUI.length; i++)
this._counterUI[i].clearCurrentValueAndMarker(ctx);
},

_onMouseOver: function(event)
{
this._onMouseMove(event);
},

_onMouseMove: function(event)
{
var x = event.x - event.target.offsetParent.offsetLeft
this._markerXPosition = x;
this._refreshCurrentValues();
},

_refreshCurrentValues: function()
{
if (!this._counters.length)
return;
if (this._markerXPosition === undefined)
return;
var i = this._recordIndexAt(this._markerXPosition);

for (var j = 0; j < this._counterUI.length; j++)
this._counterUI[j].updateCurrentValue(this._counters[i]);

this._highlightCurrentPositionOnGraphs(this._markerXPosition, i);
},

_recordIndexAt: function(x)
{
var i;
for (i = this._minimumIndex + 1; i <= this._maximumIndex; i++) {
var statX = this._counters[i].x;
if (x < statX)
break;
}
i--;
return i;
},

_highlightCurrentPositionOnGraphs: function(x, index)
{
var ctx = this._canvas.getContext("2d");
for (var i = 0; i < this._counterUI.length; i++) {
var counterUI = this._counterUI[i];
if (!counterUI.visible)
continue;
counterUI.restoreImageUnderMarker(ctx);
}

const radius = 2;
for (var i = 0; i < this._counterUI.length; i++) {
var counterUI = this._counterUI[i];
if (!counterUI.visible)
continue;
var y = counterUI.graphYValues[index];
counterUI.saveImageUnderMarker(ctx, x, y, radius);
}

for (var i = 0; i < this._counterUI.length; i++) {
var counterUI = this._counterUI[i];
if (!counterUI.visible)
continue;
var y = counterUI.graphYValues[index];
ctx.beginPath();
ctx.arc(x, y, radius, 0, Math.PI*2, true);
ctx.lineWidth = 1;
ctx.fillStyle = counterUI.graphColor;
ctx.strokeStyle = counterUI.graphColor;
ctx.fill();
ctx.stroke();
ctx.closePath();
}
},

visible: function()
{
return this._memorySidebarView.isShowing();
},

show: function()
{
var anchor =   (this._containerAnchor.nextSibling);
this._memorySidebarView.show(this._timelinePanel.element, anchor);
this._updateSize();
this._refreshDividers();
setTimeout(this._draw.bind(this), 0);
},

refresh: function()
{
this._updateSize();
this._refreshDividers();
this._draw();
this._refreshCurrentValues();
},

hide: function()
{
this._memorySidebarView.detach();
},

_refreshDividers: function()
{
this._timelineGrid.updateDividers(this._timelinePanel.calculator);
},

_setVerticalClip: function(originY, height)
{
this._originY = originY;
this._clippedHeight = height;
},

_calculateXValues: function()
{
if (!this._counters.length)
return;

var width = this._canvas.width;
var xFactor = width / (this._maxTime - this._minTime);

this._counters[this._minimumIndex].x = 0;
for (var i = this._minimumIndex + 1; i < this._maximumIndex; i++)
this._counters[i].x = xFactor * (this._counters[i].time - this._minTime);
this._counters[this._maximumIndex].x = width;
},

_drawGraph: function(counterUI)
{
var canvas = this._canvas;
var ctx = canvas.getContext("2d");
var width = canvas.width;
var height = this._clippedHeight;
var originY = this._originY;
var valueGetter = counterUI.valueGetter;

if (!this._counters.length)
return;

var maxValue;
var minValue;
for (var i = this._minimumIndex; i <= this._maximumIndex; i++) {
var value = valueGetter(this._counters[i]);
if (minValue === undefined || value < minValue)
minValue = value;
if (maxValue === undefined || value > maxValue)
maxValue = value;
}

counterUI.setRange(minValue, maxValue);

if (!counterUI.visible)
return;

var yValues = counterUI.graphYValues;
yValues.length = this._counters.length;

var maxYRange = maxValue - minValue;
var yFactor = maxYRange ? height / (maxYRange) : 1;

ctx.beginPath();
var currentY = originY + (height - (valueGetter(this._counters[this._minimumIndex])- minValue) * yFactor);
ctx.moveTo(0, currentY);
for (var i = this._minimumIndex; i <= this._maximumIndex; i++) {
var x = this._counters[i].x;
ctx.lineTo(x, currentY);
currentY = originY + (height - (valueGetter(this._counters[i])- minValue) * yFactor);
ctx.lineTo(x, currentY);

yValues[i] = currentY;
}
ctx.lineTo(width, currentY);
ctx.lineWidth = 1;
ctx.strokeStyle = counterUI.graphColor;
ctx.stroke();
ctx.closePath();
},

_clear: function() {
var ctx = this._canvas.getContext("2d");
ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
for (var i = 0; i < this._counterUI.length; i++)
this._counterUI[i].discardImageUnderMarker();
}
}

;



WebInspector.TimelineModel = function()
{
this._records = [];
this._stringPool = new StringPool();
this._minimumRecordTime = -1;
this._maximumRecordTime = -1;
this._collectionEnabled = false;

WebInspector.timelineManager.addEventListener(WebInspector.TimelineManager.EventTypes.TimelineEventRecorded, this._onRecordAdded, this);
}

WebInspector.TimelineModel.TransferChunkLengthBytes = 5000000;

WebInspector.TimelineModel.RecordType = {
Root: "Root",
Program: "Program",
EventDispatch: "EventDispatch",

BeginFrame: "BeginFrame",
ScheduleStyleRecalculation: "ScheduleStyleRecalculation",
RecalculateStyles: "RecalculateStyles",
InvalidateLayout: "InvalidateLayout",
Layout: "Layout",
Paint: "Paint",
ScrollLayer: "ScrollLayer",
DecodeImage: "DecodeImage",
ResizeImage: "ResizeImage",
CompositeLayers: "CompositeLayers",

ParseHTML: "ParseHTML",

TimerInstall: "TimerInstall",
TimerRemove: "TimerRemove",
TimerFire: "TimerFire",

XHRReadyStateChange: "XHRReadyStateChange",
XHRLoad: "XHRLoad",
EvaluateScript: "EvaluateScript",

MarkLoad: "MarkLoad",
MarkDOMContent: "MarkDOMContent",

TimeStamp: "TimeStamp",
Time: "Time",
TimeEnd: "TimeEnd",

ScheduleResourceRequest: "ScheduleResourceRequest",
ResourceSendRequest: "ResourceSendRequest",
ResourceReceiveResponse: "ResourceReceiveResponse",
ResourceReceivedData: "ResourceReceivedData",
ResourceFinish: "ResourceFinish",

FunctionCall: "FunctionCall",
GCEvent: "GCEvent",

RequestAnimationFrame: "RequestAnimationFrame",
CancelAnimationFrame: "CancelAnimationFrame",
FireAnimationFrame: "FireAnimationFrame"
}

WebInspector.TimelineModel.Events = {
RecordAdded: "RecordAdded",
RecordsCleared: "RecordsCleared"
}

WebInspector.TimelineModel.startTimeInSeconds = function(record)
{
return record.startTime / 1000;
}

WebInspector.TimelineModel.endTimeInSeconds = function(record)
{
return (typeof record.endTime === "undefined" ? record.startTime : record.endTime) / 1000;
}

WebInspector.TimelineModel.durationInSeconds = function(record)
{
return WebInspector.TimelineModel.endTimeInSeconds(record) - WebInspector.TimelineModel.startTimeInSeconds(record);
}


WebInspector.TimelineModel.aggregateTimeForRecord = function(total, rawRecord)
{
var childrenTime = 0;
var children = rawRecord["children"] || [];
for (var i = 0; i < children.length; ++i) {
WebInspector.TimelineModel.aggregateTimeForRecord(total, children[i]);
childrenTime += WebInspector.TimelineModel.durationInSeconds(children[i]);
}
var categoryName = WebInspector.TimelinePresentationModel.recordStyle(rawRecord).category.name;
var ownTime = WebInspector.TimelineModel.durationInSeconds(rawRecord) - childrenTime;
total[categoryName] = (total[categoryName] || 0) + ownTime;
}


WebInspector.TimelineModel.aggregateTimeByCategory = function(total, addend)
{
for (var category in addend)
total[category] = (total[category] || 0) + addend[category];
}

WebInspector.TimelineModel.prototype = {
startRecord: function()
{
if (this._collectionEnabled)
return;
this.reset();
WebInspector.timelineManager.start(30);
this._collectionEnabled = true;
},

stopRecord: function()
{
if (!this._collectionEnabled)
return;
WebInspector.timelineManager.stop();
this._collectionEnabled = false;
},

get records()
{
return this._records;
},

_onRecordAdded: function(event)
{
if (this._collectionEnabled)
this._addRecord(event.data);
},

_addRecord: function(record)
{
this._stringPool.internObjectStrings(record);
this._records.push(record);
this._updateBoundaries(record);
this.dispatchEventToListeners(WebInspector.TimelineModel.Events.RecordAdded, record);
},


loadFromFile: function(file, progress)
{
var delegate = new WebInspector.TimelineModelLoadFromFileDelegate(this, progress);
var fileReader = this._createFileReader(file, delegate);
var loader = new WebInspector.TimelineModelLoader(this, fileReader, progress);
fileReader.start(loader);
},


loadFromURL: function(url, progress)
{
var delegate = new WebInspector.TimelineModelLoadFromFileDelegate(this, progress);
var urlReader = new WebInspector.ChunkedXHRReader(url, delegate);
var loader = new WebInspector.TimelineModelLoader(this, urlReader, progress);
urlReader.start(loader);
},

_createFileReader: function(file, delegate)
{
return new WebInspector.ChunkedFileReader(file, WebInspector.TimelineModel.TransferChunkLengthBytes, delegate);
},

_createFileWriter: function(fileName, callback)
{
var stream = new WebInspector.FileOutputStream();
stream.open(fileName, callback);
},

saveToFile: function()
{
var now = new Date();
var fileName = "TimelineRawData-" + now.toISO8601Compact() + ".json";
function callback(stream)
{
var saver = new WebInspector.TimelineSaver(stream);
saver.save(this._records, window.navigator.appVersion);
}
this._createFileWriter(fileName, callback.bind(this));
},

reset: function()
{
this._records = [];
this._stringPool.reset();
this._minimumRecordTime = -1;
this._maximumRecordTime = -1;
this.dispatchEventToListeners(WebInspector.TimelineModel.Events.RecordsCleared);
},

minimumRecordTime: function()
{
return this._minimumRecordTime;
},

maximumRecordTime: function()
{
return this._maximumRecordTime;
},

_updateBoundaries: function(record)
{
var startTime = WebInspector.TimelineModel.startTimeInSeconds(record);
var endTime = WebInspector.TimelineModel.endTimeInSeconds(record);

if (this._minimumRecordTime === -1 || startTime < this._minimumRecordTime)
this._minimumRecordTime = startTime;
if (this._maximumRecordTime === -1 || endTime > this._maximumRecordTime)
this._maximumRecordTime = endTime;
},


recordOffsetInSeconds: function(rawRecord)
{
return WebInspector.TimelineModel.startTimeInSeconds(rawRecord) - this._minimumRecordTime;
},

__proto__: WebInspector.Object.prototype
}


WebInspector.TimelineModelLoader = function(model, reader, progress)
{
this._model = model;
this._reader = reader;
this._progress = progress;
this._buffer = "";
this._firstChunk = true;
}

WebInspector.TimelineModelLoader.prototype = {

write: function(chunk)
{
var data = this._buffer + chunk;
var lastIndex = 0;
var index;
do {
index = lastIndex;
lastIndex = WebInspector.findBalancedCurlyBrackets(data, index);
} while (lastIndex !== -1)

var json = data.slice(0, index) + "]";
this._buffer = data.slice(index);

if (!index)
return;


if (!this._firstChunk)
json = "[0" + json;

var items;
try {
items =   (JSON.parse(json));
} catch (e) {
WebInspector.showErrorMessage("Malformed timeline data.");
this._model.reset();
this._reader.cancel();
this._progress.done();
return;
}

if (this._firstChunk) {
this._version = items[0];
this._firstChunk = false;
this._model.reset();
}


for (var i = 1, size = items.length; i < size; ++i)
this._model._addRecord(items[i]);
},

close: function() { }
}


WebInspector.TimelineModelLoadFromFileDelegate = function(model, progress)
{
this._model = model;
this._progress = progress;
}

WebInspector.TimelineModelLoadFromFileDelegate.prototype = {
onTransferStarted: function()
{
this._progress.setTitle(WebInspector.UIString("Loading\u2026"));
},


onChunkTransferred: function(reader)
{
if (this._progress.isCanceled()) {
reader.cancel();
this._progress.done();
this._model.reset();
return;
}

var totalSize = reader.fileSize();
if (totalSize) {
this._progress.setTotalWork(totalSize);
this._progress.setWorked(reader.loadedSize());
}
},

onTransferFinished: function()
{
this._progress.done();
},


onError: function(reader, event)
{
this._progress.done();
this._model.reset();
switch (event.target.error.code) {
case FileError.NOT_FOUND_ERR:
WebInspector.showErrorMessage(WebInspector.UIString("File \"%s\" not found.", reader.fileName()));
break;
case FileError.NOT_READABLE_ERR:
WebInspector.showErrorMessage(WebInspector.UIString("File \"%s\" is not readable", reader.fileName()));
break;
case FileError.ABORT_ERR:
break;
default:
WebInspector.showErrorMessage(WebInspector.UIString("An error occurred while reading the file \"%s\"", reader.fileName()));
}
}
}


WebInspector.TimelineSaver = function(stream)
{
this._stream = stream;
}

WebInspector.TimelineSaver.prototype = {

save: function(records, version)
{
this._records = records;
this._recordIndex = 0;
this._prologue = "[" + JSON.stringify(version);

this._writeNextChunk(this._stream);
},

_writeNextChunk: function(stream)
{
const separator = ",\n";
var data = [];
var length = 0;

if (this._prologue) {
data.push(this._prologue);
length += this._prologue.length;
delete this._prologue;
} else {
if (this._recordIndex === this._records.length) {
stream.close();
return;
}
data.push("");
}
while (this._recordIndex < this._records.length) {
var item = JSON.stringify(this._records[this._recordIndex]);
var itemLength = item.length + separator.length;
if (length + itemLength > WebInspector.TimelineModel.TransferChunkLengthBytes)
break;
length += itemLength;
data.push(item);
++this._recordIndex;
}
if (this._recordIndex === this._records.length)
data.push(data.pop() + "]");
stream.write(data.join(separator), this._writeNextChunk.bind(this));
}
}
;



WebInspector.TimelineOverviewPane = function(model)
{
WebInspector.View.call(this);
this.element.id = "timeline-overview-panel";

this._windowStartTime = 0;
this._windowEndTime = Infinity;
this._eventDividers = [];

this._model = model;

this._topPaneSidebarElement = document.createElement("div");
this._topPaneSidebarElement.id = "timeline-overview-sidebar";

var overviewTreeElement = document.createElement("ol");
overviewTreeElement.className = "sidebar-tree";
this._topPaneSidebarElement.appendChild(overviewTreeElement);
this.element.appendChild(this._topPaneSidebarElement);

var topPaneSidebarTree = new TreeOutline(overviewTreeElement);

this._currentMode = WebInspector.TimelineOverviewPane.Mode.Events;

this._overviewItems = {};
this._overviewItems[WebInspector.TimelineOverviewPane.Mode.Events] = new WebInspector.SidebarTreeElement("timeline-overview-sidebar-events",
WebInspector.UIString("Events"));
if (Capabilities.timelineSupportsFrameInstrumentation) {
this._overviewItems[WebInspector.TimelineOverviewPane.Mode.Frames] = new WebInspector.SidebarTreeElement("timeline-overview-sidebar-frames",
WebInspector.UIString("Frames"));
}
this._overviewItems[WebInspector.TimelineOverviewPane.Mode.Memory] = new WebInspector.SidebarTreeElement("timeline-overview-sidebar-memory",
WebInspector.UIString("Memory"));

for (var mode in this._overviewItems) {
var item = this._overviewItems[mode];
item.onselect = this.setMode.bind(this, mode);
topPaneSidebarTree.appendChild(item);
}

this._overviewItems[this._currentMode].revealAndSelect(false);

this._overviewContainer = this.element.createChild("div", "fill");
this._overviewContainer.id = "timeline-overview-container";

this._overviewGrid = new WebInspector.TimelineGrid();
this._overviewGrid.element.id = "timeline-overview-grid";
this._overviewGrid.itemsGraphsElement.id = "timeline-overview-timelines";

this._overviewContainer.appendChild(this._overviewGrid.element);

this._heapGraph = new WebInspector.HeapGraph(this._model);
this._heapGraph.element.id = "timeline-overview-memory";
this._overviewGrid.element.insertBefore(this._heapGraph.element, this._overviewGrid.itemsGraphsElement);

this._overviewWindow = new WebInspector.TimelineOverviewWindow(this._overviewContainer, this._overviewGrid.dividersLabelBarElement);
this._overviewWindow.addEventListener(WebInspector.TimelineOverviewWindow.Events.WindowChanged, this._onWindowChanged, this);

var separatorElement = document.createElement("div");
separatorElement.id = "timeline-overview-separator";
this.element.appendChild(separatorElement);

this._categoryStrips = new WebInspector.TimelineCategoryStrips(this._model);
this._overviewGrid.itemsGraphsElement.appendChild(this._categoryStrips.element);

var categories = WebInspector.TimelinePresentationModel.categories();
for (var category in categories)
categories[category].addEventListener(WebInspector.TimelineCategory.Events.VisibilityChanged, this._onCategoryVisibilityChanged, this);

this._overviewGrid.setScrollAndDividerTop(0, 0);
this._overviewCalculator = new WebInspector.TimelineOverviewCalculator();

model.addEventListener(WebInspector.TimelineModel.Events.RecordAdded, this._onRecordAdded, this);
model.addEventListener(WebInspector.TimelineModel.Events.RecordsCleared, this._reset, this);
}

WebInspector.TimelineOverviewPane.MinSelectableSize = 12;

WebInspector.TimelineOverviewPane.WindowScrollSpeedFactor = .3;

WebInspector.TimelineOverviewPane.ResizerOffset = 3.5; 

WebInspector.TimelineOverviewPane.Mode = {
Events: "Events",
Frames: "Frames",
Memory: "Memory"
};

WebInspector.TimelineOverviewPane.Events = {
ModeChanged: "ModeChanged",
WindowChanged: "WindowChanged"
};

WebInspector.TimelineOverviewPane.prototype = {
wasShown: function()
{
this._update();
},

onResize: function()
{
this._update();
},

setMode: function(newMode)
{
if (this._currentMode === newMode)
return;

this._currentMode = newMode;
this._setFrameMode(this._currentMode === WebInspector.TimelineOverviewPane.Mode.Frames);
switch (this._currentMode) {
case WebInspector.TimelineOverviewPane.Mode.Events:
case WebInspector.TimelineOverviewPane.Mode.Frames:
this._heapGraph.hide();
this._overviewGrid.itemsGraphsElement.removeStyleClass("hidden");
break;
case WebInspector.TimelineOverviewPane.Mode.Memory:
this._overviewGrid.itemsGraphsElement.addStyleClass("hidden");
this._heapGraph.show();
}
this._overviewItems[this._currentMode].revealAndSelect(false);
this.dispatchEventToListeners(WebInspector.TimelineOverviewPane.Events.ModeChanged, this._currentMode);
this._update();
},

_setFrameMode: function(enabled)
{
if (!enabled === !this._frameOverview)
return;
if (enabled) {
this._frameOverview = new WebInspector.TimelineFrameOverview(this._model);
this._frameOverview.show(this._overviewContainer);
} else {
this._frameOverview.detach();
this._frameOverview = null;
this._overviewGrid.itemsGraphsElement.removeStyleClass("hidden");
this._categoryStrips.update();
}
},

_onCategoryVisibilityChanged: function(event)
{
if (this._currentMode === WebInspector.TimelineOverviewPane.Mode.Events)
this._categoryStrips.update();
},

_update: function()
{
delete this._refreshTimeout;

this._updateWindow();
this._overviewCalculator.setWindow(this._model.minimumRecordTime(), this._model.maximumRecordTime());
this._overviewCalculator.setDisplayWindow(0, this._overviewContainer.clientWidth);

if (this._heapGraph.visible)
this._heapGraph.update();
else if (this._frameOverview)
this._frameOverview.update();
else
this._categoryStrips.update();

this._overviewGrid.updateDividers(this._overviewCalculator);
this._updateEventDividers();
},

_updateEventDividers: function()
{
var records = this._eventDividers;
this._overviewGrid.removeEventDividers();
var dividers = [];
for (var i = 0; i < records.length; ++i) {
var record = records[i];
var positions = this._overviewCalculator.computeBarGraphPercentages(record);
var dividerPosition = Math.round(positions.start * 10);
if (dividers[dividerPosition])
continue;
var divider = WebInspector.TimelinePresentationModel.createEventDivider(record.type);
divider.style.left = positions.start + "%";
dividers[dividerPosition] = divider;
}
this._overviewGrid.addEventDividers(dividers);
},


sidebarResized: function(width)
{
this._overviewContainer.style.left = width + "px";
this._topPaneSidebarElement.style.width = width + "px";
this._update();
},


addFrame: function(frame)
{
this._frameOverview.addFrame(frame);
this._scheduleRefresh();
},


zoomToFrame: function(frame)
{
var window = this._frameOverview.framePosition(frame);
if (!window)
return;

this._overviewWindow._setWindowPosition(window.start, window.end);
},

_onRecordAdded: function(event)
{
var record = event.data;
var eventDividers = this._eventDividers;
function addEventDividers(record)
{
if (WebInspector.TimelinePresentationModel.isEventDivider(record))
eventDividers.push(record);
}
WebInspector.TimelinePresentationModel.forAllRecords([record], addEventDividers);
this._scheduleRefresh();
},

_reset: function()
{
this._windowStartTime = 0;
this._windowEndTime = Infinity;
this._overviewWindow.reset();
this._overviewCalculator.reset();
this._eventDividers = [];
this._overviewGrid.updateDividers(this._overviewCalculator);
if (this._frameOverview)
this._frameOverview.reset();
this._update();
},

windowStartTime: function()
{
return this._windowStartTime || this._model.minimumRecordTime();
},

windowEndTime: function()
{
return this._windowEndTime < Infinity ? this._windowEndTime : this._model.maximumRecordTime();
},

windowLeft: function()
{
return this._overviewWindow.windowLeft;
},

windowRight: function()
{
return this._overviewWindow.windowRight;
},

_onWindowChanged: function()
{
if (this._ignoreWindowChangedEvent)
return;
if (this._frameOverview) {
var times = this._frameOverview.getWindowTimes(this.windowLeft(), this.windowRight());
this._windowStartTime = times.startTime;
this._windowEndTime = times.endTime;
} else {
var absoluteMin = this._model.minimumRecordTime();
var absoluteMax = this._model.maximumRecordTime();
this._windowStartTime = absoluteMin + (absoluteMax - absoluteMin) * this.windowLeft();
this._windowEndTime = absoluteMin + (absoluteMax - absoluteMin) * this.windowRight();
}
this.dispatchEventToListeners(WebInspector.TimelineOverviewPane.Events.WindowChanged);
},


setWindowTimes: function(left, right)
{
this._windowStartTime = left;
this._windowEndTime = right;
this._updateWindow();
},

_updateWindow: function()
{
var offset = this._model.minimumRecordTime();
var timeSpan = this._model.maximumRecordTime() - offset;
var left = this._windowStartTime ? (this._windowStartTime - offset) / timeSpan : 0;
var right = this._windowEndTime < Infinity ? (this._windowEndTime - offset) / timeSpan : 1;
this._ignoreWindowChangedEvent = true;
this._overviewWindow._setWindow(left, right);
this._ignoreWindowChangedEvent = false;
},


setShowShortEvents: function(value)
{
this._categoryStrips.setShowShortEvents(value);
},

_scheduleRefresh: function()
{
if (this._refreshTimeout)
return;
if (!this.isShowing())
return;
this._refreshTimeout = setTimeout(this._update.bind(this), 300);
},

__proto__: WebInspector.View.prototype
}


WebInspector.TimelineOverviewWindow = function(parentElement, dividersLabelBarElement)
{
this._parentElement = parentElement;
this._dividersLabelBarElement = dividersLabelBarElement;

WebInspector.installDragHandle(this._parentElement, this._startWindowSelectorDragging.bind(this), this._windowSelectorDragging.bind(this), this._endWindowSelectorDragging.bind(this), "ew-resize");
WebInspector.installDragHandle(this._dividersLabelBarElement, this._startWindowDragging.bind(this), this._windowDragging.bind(this), this._endWindowDragging.bind(this), "ew-resize");

this.windowLeft = 0.0;
this.windowRight = 1.0;

this._parentElement.addEventListener("mousewheel", this._onMouseWheel.bind(this), true);
this._parentElement.addEventListener("dblclick", this._resizeWindowMaximum.bind(this), true);

this._overviewWindowElement = document.createElement("div");
this._overviewWindowElement.className = "timeline-overview-window";
parentElement.appendChild(this._overviewWindowElement);

this._overviewWindowBordersElement = document.createElement("div");
this._overviewWindowBordersElement.className = "timeline-overview-window-rulers";
parentElement.appendChild(this._overviewWindowBordersElement);

var overviewDividersBackground = document.createElement("div");
overviewDividersBackground.className = "timeline-overview-dividers-background";
parentElement.appendChild(overviewDividersBackground);

this._leftResizeElement = document.createElement("div");
this._leftResizeElement.className = "timeline-window-resizer";
this._leftResizeElement.style.left = 0;
parentElement.appendChild(this._leftResizeElement);
WebInspector.installDragHandle(this._leftResizeElement, null, this._leftResizeElementDragging.bind(this), null, "ew-resize");

this._rightResizeElement = document.createElement("div");
this._rightResizeElement.className = "timeline-window-resizer timeline-window-resizer-right";
this._rightResizeElement.style.right = 0;
parentElement.appendChild(this._rightResizeElement);
WebInspector.installDragHandle(this._rightResizeElement, null, this._rightResizeElementDragging.bind(this), null, "ew-resize");
}

WebInspector.TimelineOverviewWindow.Events = {
WindowChanged: "WindowChanged"
}

WebInspector.TimelineOverviewWindow.prototype = {
reset: function()
{
this.windowLeft = 0.0;
this.windowRight = 1.0;

this._overviewWindowElement.style.left = "0%";
this._overviewWindowElement.style.width = "100%";
this._overviewWindowBordersElement.style.left = "0%";
this._overviewWindowBordersElement.style.right = "0%";
this._leftResizeElement.style.left = "0%";
this._rightResizeElement.style.left = "100%";
},


_leftResizeElementDragging: function(event)
{
this._resizeWindowLeft(event.pageX - this._parentElement.offsetLeft);
event.preventDefault();
},


_rightResizeElementDragging: function(event)
{
this._resizeWindowRight(event.pageX - this._parentElement.offsetLeft);
event.preventDefault();
},


_startWindowSelectorDragging: function(event)
{
var position = event.pageX - this._parentElement.offsetLeft;
this._overviewWindowSelector = new WebInspector.TimelineOverviewPane.WindowSelector(this._parentElement, position);
return true;
},


_windowSelectorDragging: function(event)
{
this._overviewWindowSelector._updatePosition(event.pageX - this._parentElement.offsetLeft);
event.preventDefault();
},


_endWindowSelectorDragging: function(event)
{
var window = this._overviewWindowSelector._close(event.pageX - this._parentElement.offsetLeft);
delete this._overviewWindowSelector;
if (window.end === window.start) { 
var middle = window.end;
window.start = Math.max(0, middle - WebInspector.TimelineOverviewPane.MinSelectableSize / 2);
window.end = Math.min(this._parentElement.clientWidth, middle + WebInspector.TimelineOverviewPane.MinSelectableSize / 2);
} else if (window.end - window.start < WebInspector.TimelineOverviewPane.MinSelectableSize) {
if (this._parentElement.clientWidth - window.end > WebInspector.TimelineOverviewPane.MinSelectableSize)
window.end = window.start + WebInspector.TimelineOverviewPane.MinSelectableSize;
else
window.start = window.end - WebInspector.TimelineOverviewPane.MinSelectableSize;
}
this._setWindowPosition(window.start, window.end);
},


_startWindowDragging: function(event)
{
var windowLeft = this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.ResizerOffset;
this._dragOffset = windowLeft - event.pageX;
return true;
},


_windowDragging: function(event)
{
var windowLeft = this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.ResizerOffset;
var start = this._dragOffset + event.pageX;
this._moveWindow(start);
event.preventDefault();
},


_endWindowDragging: function(event)
{
delete this._dragOffset;
},


_moveWindow: function(start)
{
var windowLeft = this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.ResizerOffset;
var windowRight = this._rightResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.ResizerOffset;
var windowSize = windowRight - windowLeft;
var end = start + windowSize;

if (start < 0) {
start = 0;
end = windowSize;
}

if (end > this._parentElement.clientWidth) {
end = this._parentElement.clientWidth;
start = end - windowSize;
}
this._setWindowPosition(start, end);
},


_resizeWindowLeft: function(start)
{

if (start < 10)
start = 0;
else if (start > this._rightResizeElement.offsetLeft -  4)
start = this._rightResizeElement.offsetLeft - 4;
this._setWindowPosition(start, null);
},


_resizeWindowRight: function(end)
{

if (end > this._parentElement.clientWidth - 10)
end = this._parentElement.clientWidth;
else if (end < this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.MinSelectableSize)
end = this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.MinSelectableSize;
this._setWindowPosition(null, end);
},

_resizeWindowMaximum: function()
{
this._setWindowPosition(0, this._parentElement.clientWidth);
},


_setWindow: function(left, right)
{
var clientWidth = this._parentElement.clientWidth;
this._setWindowPosition(left * clientWidth, right * clientWidth);
},


_setWindowPosition: function(start, end)
{
var clientWidth = this._parentElement.clientWidth;
const rulerAdjustment = 1 / clientWidth;
if (typeof start === "number") {
this.windowLeft = start / clientWidth;
this._leftResizeElement.style.left = this.windowLeft * 100 + "%";
this._overviewWindowElement.style.left = this.windowLeft * 100 + "%";
this._overviewWindowBordersElement.style.left = (this.windowLeft - rulerAdjustment) * 100 + "%";
}
if (typeof end === "number") {
this.windowRight = end / clientWidth;
this._rightResizeElement.style.left = this.windowRight * 100 + "%";
}
this._overviewWindowElement.style.width = (this.windowRight - this.windowLeft) * 100 + "%";
this._overviewWindowBordersElement.style.right = (1 - this.windowRight + 2 * rulerAdjustment) * 100 + "%";
this.dispatchEventToListeners(WebInspector.TimelineOverviewWindow.Events.WindowChanged);
},


_onMouseWheel: function(event)
{
const zoomFactor = 1.1;
const mouseWheelZoomSpeed = 1 / 120;

if (typeof event.wheelDeltaY === "number" && event.wheelDeltaY) {
var referencePoint = event.pageX - this._parentElement.offsetLeft;
this._zoom(Math.pow(zoomFactor, -event.wheelDeltaY * mouseWheelZoomSpeed), referencePoint);
}
if (typeof event.wheelDeltaX === "number" && event.wheelDeltaX) {
var windowLeft = this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.ResizerOffset;
var start = windowLeft - Math.round(event.wheelDeltaX * WebInspector.TimelineOverviewPane.WindowScrollSpeedFactor);
this._moveWindow(start);
event.preventDefault();
}
},


_zoom: function(factor, referencePoint)
{
var left = this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.ResizerOffset;
var right = this._rightResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.ResizerOffset;

var delta = factor * (right - left);
if (factor < 1 && delta < WebInspector.TimelineOverviewPane.MinSelectableSize)
return;
var max = this._parentElement.clientWidth;
left = Math.max(0, Math.min(max - delta, referencePoint + (left - referencePoint) * factor));
right = Math.min(max, left + delta);
this._setWindowPosition(left, right);
},

__proto__: WebInspector.Object.prototype
}


WebInspector.TimelineOverviewCalculator = function()
{
}

WebInspector.TimelineOverviewCalculator.prototype = {

computePosition: function(time)
{
return (time - this._minimumBoundary) / this.boundarySpan() * this._workingArea + this.paddingLeft;
},

computeBarGraphPercentages: function(record)
{
var start = (WebInspector.TimelineModel.startTimeInSeconds(record) - this._minimumBoundary) / this.boundarySpan() * 100;
var end = (WebInspector.TimelineModel.endTimeInSeconds(record) - this._minimumBoundary) / this.boundarySpan() * 100;
return {start: start, end: end};
},


setWindow: function(minimum, maximum)
{
this._minimumBoundary = minimum >= 0 ? minimum : undefined;
this._maximumBoundary = maximum >= 0 ? maximum : undefined;
},


setDisplayWindow: function(paddingLeft, clientWidth)
{
this._workingArea = clientWidth - paddingLeft;
this.paddingLeft = paddingLeft;
},

reset: function()
{
this.setWindow();
},

formatTime: function(value)
{
return Number.secondsToString(value);
},

maximumBoundary: function()
{
return this._maximumBoundary;
},

minimumBoundary: function()
{
return this._minimumBoundary;
},

boundarySpan: function()
{
return this._maximumBoundary - this._minimumBoundary;
}
}


WebInspector.TimelineOverviewPane.WindowSelector = function(parent, position)
{
this._startPosition = position;
this._width = parent.offsetWidth;
this._windowSelector = document.createElement("div");
this._windowSelector.className = "timeline-window-selector";
this._windowSelector.style.left = this._startPosition + "px";
this._windowSelector.style.right = this._width - this._startPosition +  + "px";
parent.appendChild(this._windowSelector);
}

WebInspector.TimelineOverviewPane.WindowSelector.prototype = {
_createSelectorElement: function(parent, left, width, height)
{
var selectorElement = document.createElement("div");
selectorElement.className = "timeline-window-selector";
selectorElement.style.left = left + "px";
selectorElement.style.width = width + "px";
selectorElement.style.top = "0px";
selectorElement.style.height = height + "px";
parent.appendChild(selectorElement);
return selectorElement;
},

_close: function(position)
{
position = Math.max(0, Math.min(position, this._width));
this._windowSelector.parentNode.removeChild(this._windowSelector);
return this._startPosition < position ? {start: this._startPosition, end: position} : {start: position, end: this._startPosition};
},

_updatePosition: function(position)
{
position = Math.max(0, Math.min(position, this._width));
if (position < this._startPosition) {
this._windowSelector.style.left = position + "px";
this._windowSelector.style.right = this._width - this._startPosition + "px";
} else {
this._windowSelector.style.left = this._startPosition + "px";
this._windowSelector.style.right = this._width - position + "px";
}
}
}


WebInspector.HeapGraph = function(model)
{
this._canvas = document.createElement("canvas");
this._model = model;

this._maxHeapSizeLabel = document.createElement("div");
this._maxHeapSizeLabel.addStyleClass("max");
this._maxHeapSizeLabel.addStyleClass("memory-graph-label");
this._minHeapSizeLabel = document.createElement("div");
this._minHeapSizeLabel.addStyleClass("min");
this._minHeapSizeLabel.addStyleClass("memory-graph-label");

this._element = document.createElement("div");
this._element.addStyleClass("hidden");
this._element.appendChild(this._canvas);
this._element.appendChild(this._maxHeapSizeLabel);
this._element.appendChild(this._minHeapSizeLabel);
}

WebInspector.HeapGraph.prototype = {

get element()
{
return this._element;
},


get visible()
{
return !this.element.hasStyleClass("hidden");
},

show: function()
{
this.element.removeStyleClass("hidden");
},

hide: function()
{
this.element.addStyleClass("hidden");
},

update: function()
{
var records = this._model.records;
if (!records.length)
return;

const yPadding = 5;
this._canvas.width = this.element.clientWidth;
this._canvas.height = this.element.clientHeight - yPadding;

const lowerOffset = 3;
var maxUsedHeapSize = 0;
var minUsedHeapSize = 100000000000;
var minTime = this._model.minimumRecordTime();
var maxTime = this._model.maximumRecordTime();;
WebInspector.TimelinePresentationModel.forAllRecords(records, function(r) {
maxUsedHeapSize = Math.max(maxUsedHeapSize, r.usedHeapSize || maxUsedHeapSize);
minUsedHeapSize = Math.min(minUsedHeapSize, r.usedHeapSize || minUsedHeapSize);
});
minUsedHeapSize = Math.min(minUsedHeapSize, maxUsedHeapSize);

var width = this._canvas.width;
var height = this._canvas.height - lowerOffset;
var xFactor = width / (maxTime - minTime);
var yFactor = height / (maxUsedHeapSize - minUsedHeapSize);

var histogram = new Array(width);
WebInspector.TimelinePresentationModel.forAllRecords(records, function(r) {
if (!r.usedHeapSize)
return;
var x = Math.round((WebInspector.TimelineModel.endTimeInSeconds(r) - minTime) * xFactor);
var y = Math.round((r.usedHeapSize - minUsedHeapSize) * yFactor);
histogram[x] = Math.max(histogram[x] || 0, y);
});

var ctx = this._canvas.getContext("2d");
this._clear(ctx);


height = height + 1;

ctx.beginPath();
var initialY = 0;
for (var k = 0; k < histogram.length; k++) {
if (histogram[k]) {
initialY = histogram[k];
break;
}
}
ctx.moveTo(0, height - initialY);

for (var x = 0; x < histogram.length; x++) {
if (!histogram[x])
continue;
ctx.lineTo(x, height - histogram[x]);
}

ctx.lineWidth = 0.5;
ctx.strokeStyle = "rgba(20,0,0,0.8)";
ctx.stroke();

ctx.fillStyle = "rgba(214,225,254, 0.8);";
ctx.lineTo(width, 60);
ctx.lineTo(0, 60);
ctx.lineTo(0, height - initialY);
ctx.fill();
ctx.closePath();

this._maxHeapSizeLabel.textContent = Number.bytesToString(maxUsedHeapSize);
this._minHeapSizeLabel.textContent = Number.bytesToString(minUsedHeapSize);
},

_clear: function(ctx)
{
ctx.fillStyle = "rgba(255,255,255,0.8)";
ctx.fillRect(0, 0, this._canvas.width, this._canvas.height);
},
}


WebInspector.TimelineCategoryStrips = function(model)
{
this._model = model;
this.element = document.createElement("canvas");
this._context = this.element.getContext("2d");

this._fillStyles = {};
var categories = WebInspector.TimelinePresentationModel.categories();
for (var category in categories)
this._fillStyles[category] = WebInspector.TimelinePresentationModel.createFillStyleForCategory(this._context, 0, WebInspector.TimelineCategoryStrips._innerStripHeight, categories[category]);

this._disabledCategoryFillStyle = WebInspector.TimelinePresentationModel.createFillStyle(this._context, 0, WebInspector.TimelineCategoryStrips._innerStripHeight,
"rgb(218, 218, 218)", "rgb(170, 170, 170)", "rgb(143, 143, 143)");

this._disabledCategoryBorderStyle = "rgb(143, 143, 143)";
}


WebInspector.TimelineCategoryStrips._canvasHeight = 60;

WebInspector.TimelineCategoryStrips._numberOfStrips = 3;

WebInspector.TimelineCategoryStrips._stripHeight = Math.round(WebInspector.TimelineCategoryStrips._canvasHeight  / WebInspector.TimelineCategoryStrips._numberOfStrips);

WebInspector.TimelineCategoryStrips._stripPadding = 4;

WebInspector.TimelineCategoryStrips._innerStripHeight = WebInspector.TimelineCategoryStrips._stripHeight - 2 * WebInspector.TimelineCategoryStrips._stripPadding;

WebInspector.TimelineCategoryStrips.prototype = {
update: function()
{

this.element.width = this.element.parentElement.clientWidth;
this.element.height = WebInspector.TimelineCategoryStrips._canvasHeight;

var timeOffset = this._model.minimumRecordTime();
var timeSpan = this._model.maximumRecordTime() - timeOffset;
var scale = this.element.width / timeSpan;

var lastBarByGroup = [];

this._context.fillStyle = "rgba(0, 0, 0, 0.05)";
for (var i = 1; i < WebInspector.TimelineCategoryStrips._numberOfStrips; i += 2)
this._context.fillRect(0.5, i * WebInspector.TimelineCategoryStrips._stripHeight + 0.5, this.element.width, WebInspector.TimelineCategoryStrips._stripHeight);

function appendRecord(record)
{
var isLong = WebInspector.TimelineModel.durationInSeconds(record) > WebInspector.TimelinePresentationModel.shortRecordThreshold;
if (!(this._showShortEvents || isLong))
return;
if (record.type === WebInspector.TimelineModel.RecordType.BeginFrame)
return;
var recordStart = Math.floor((WebInspector.TimelineModel.startTimeInSeconds(record) - timeOffset) * scale);
var recordEnd = Math.ceil((WebInspector.TimelineModel.endTimeInSeconds(record) - timeOffset) * scale);
var category = WebInspector.TimelinePresentationModel.categoryForRecord(record);
if (category.overviewStripGroupIndex < 0)
return;
var bar = lastBarByGroup[category.overviewStripGroupIndex];

const barsMergeThreshold = 2;
if (bar && bar.category === category && bar.end + barsMergeThreshold >= recordStart) {
if (recordEnd > bar.end)
bar.end = recordEnd;
return;
}
if (bar)
this._renderBar(bar.start, bar.end, bar.category);
lastBarByGroup[category.overviewStripGroupIndex] = { start: recordStart, end: recordEnd, category: category };
}
WebInspector.TimelinePresentationModel.forAllRecords(this._model.records, appendRecord.bind(this));
for (var i = 0; i < lastBarByGroup.length; ++i) {
if (lastBarByGroup[i])
this._renderBar(lastBarByGroup[i].start, lastBarByGroup[i].end, lastBarByGroup[i].category);
}
},


setShowShortEvents: function(value)
{
this._showShortEvents = value;
this.update();
},

_renderBar: function(begin, end, category)
{
var x = begin + 0.5;
var y = category.overviewStripGroupIndex * WebInspector.TimelineCategoryStrips._stripHeight + WebInspector.TimelineCategoryStrips._stripPadding + 0.5;
var width = Math.max(end - begin, 1);

this._context.save();
this._context.translate(x, y);
this._context.fillStyle = category.hidden ? this._disabledCategoryFillStyle : this._fillStyles[category.name];
this._context.fillRect(0, 0, width, WebInspector.TimelineCategoryStrips._innerStripHeight);
this._context.strokeStyle = category.hidden ? this._disabledCategoryBorderStyle : category.borderColor;
this._context.strokeRect(0, 0, width, WebInspector.TimelineCategoryStrips._innerStripHeight);
this._context.restore();
}
}


WebInspector.TimelineFrameOverview = function(model)
{
WebInspector.View.call(this);
this.element = document.createElement("canvas");
this.element.className = "timeline-frame-overview-bars fill";
this._model = model;
this.reset();

this._outerPadding = 4;
this._maxInnerBarWidth = 10;


this._actualPadding = 5;
this._actualOuterBarWidth = this._maxInnerBarWidth + this._actualPadding;

this._context = this.element.getContext("2d");

this._fillStyles = {};
var categories = WebInspector.TimelinePresentationModel.categories();
for (var category in categories)
this._fillStyles[category] = WebInspector.TimelinePresentationModel.createFillStyleForCategory(this._context, this._maxInnerBarWidth, 0, categories[category]);
}

WebInspector.TimelineFrameOverview.prototype = {
reset: function()
{
this._recordsPerBar = 1;
this._barTimes = [];
this._frames = [];
},

update: function()
{
const minBarWidth = 4;
this._framesPerBar = Math.max(1, this._frames.length * minBarWidth / this.element.clientWidth);
this._barTimes = [];
var visibleFrames = this._aggregateFrames(this._framesPerBar);

const paddingTop = 4;



const targetFPS = 30;
var fullBarLength = 1.0 / targetFPS;
if (fullBarLength < this._medianFrameLength)
fullBarLength = Math.min(this._medianFrameLength * 2, this._maxFrameLength);

var scale = (this.element.clientHeight - paddingTop) / fullBarLength;
this._renderBars(visibleFrames, scale);
},


addFrame: function(frame)
{
this._frames.push(frame);
},

framePosition: function(frame)
{
var frameNumber = this._frames.indexOf(frame);
if (frameNumber < 0)
return;
var barNumber = Math.floor(frameNumber / this._framesPerBar);
var firstBar = this._framesPerBar > 1 ? barNumber : Math.max(barNumber - 1, 0);
var lastBar = this._framesPerBar > 1 ? barNumber : Math.min(barNumber + 1, this._barTimes.length - 1);
return {
start: Math.ceil(this._barNumberToScreenPosition(firstBar) - this._actualPadding / 2),
end: Math.floor(this._barNumberToScreenPosition(lastBar + 1) - this._actualPadding / 2)
}
},


_aggregateFrames: function(framesPerBar)
{
var visibleFrames = [];
var durations = [];

this._maxFrameLength = 0;

for (var barNumber = 0, currentFrame = 0; currentFrame < this._frames.length; ++barNumber) {
var barStartTime = this._frames[currentFrame].startTime;
var longestFrame = null;

for (var lastFrame = Math.min(Math.floor((barNumber + 1) * framesPerBar), this._frames.length);
currentFrame < lastFrame; ++currentFrame) {
if (!longestFrame || longestFrame.duration < this._frames[currentFrame].duration)
longestFrame = this._frames[currentFrame];
}
var barEndTime = this._frames[currentFrame - 1].endTime;
if (longestFrame) {
this._maxFrameLength = Math.max(this._maxFrameLength, longestFrame.duration);
visibleFrames.push(longestFrame);
this._barTimes.push({ startTime: barStartTime, endTime: barEndTime });
durations.push(longestFrame.duration);
}
}
this._medianFrameLength = durations.qselect(Math.floor(durations.length / 2));
return visibleFrames;
},


_renderBars: function(frames, scale)
{

this.element.width = this.element.clientWidth;
this.element.height = this.element.clientHeight;

const maxPadding = 5;
this._actualOuterBarWidth = Math.min((this.element.width - 2 * this._outerPadding) / frames.length, this._maxInnerBarWidth + maxPadding);
this._actualPadding = Math.min(Math.floor(this._actualOuterBarWidth / 3), maxPadding);

var barWidth = this._actualOuterBarWidth - this._actualPadding;
for (var i = 0; i < frames.length; ++i)
this._renderBar(this._barNumberToScreenPosition(i), barWidth, frames[i], scale);

this._drawFPSMarks(scale);
},


_barNumberToScreenPosition: function(n)
{
return this._outerPadding + this._actualOuterBarWidth * n;
},


_drawFPSMarks: function(scale)
{
const fpsMarks = [30, 60];

this._context.save();
this._context.beginPath();
this._context.font = "9px monospace";
this._context.textAlign = "right";
this._context.textBaseline = "top";

const labelPadding = 2;
var lineHeight = 12;
var labelTopMargin = 0;

for (var i = 0; i < fpsMarks.length; ++i) {
var fps = fpsMarks[i];

var y = this.element.height - Math.floor(1.0 / fps * scale) - 0.5;
var label = fps + " FPS ";
var labelWidth = this._context.measureText(label).width;
var labelX = this.element.width;
var labelY;

if (labelTopMargin < y - lineHeight)
labelY = y - lineHeight;
else if (y + lineHeight < this.element.height)
labelY = y;
else
break; 

this._context.moveTo(0, y);
this._context.lineTo(this.element.width, y);

this._context.fillStyle = "rgba(255, 255, 255, 0.75)";
this._context.fillRect(labelX - labelWidth - labelPadding, labelY, labelWidth + 2 * labelPadding, lineHeight);
this._context.fillStyle = "rgb(0, 0, 0)";
this._context.fillText(label, labelX, labelY);
labelTopMargin = labelY + lineHeight;
}
this._context.strokeStyle = "rgb(51, 51, 51)";
this._context.stroke();
this._context.restore();
},

_renderBar: function(left, width, frame, scale)
{
var categories = Object.keys(WebInspector.TimelinePresentationModel.categories());
if (!categories.length)
return;
var x = Math.floor(left) + 0.5;
width = Math.floor(width);

for (var i = 0, bottomOffset = this.element.height; i < categories.length; ++i) {
var category = categories[i];
var duration = frame.timeByCategory[category];

if (!duration)
continue;
var height = duration * scale;
var y = Math.floor(bottomOffset - height) + 0.5;

this._context.save();
this._context.translate(x, 0);
this._context.scale(width / this._maxInnerBarWidth, 1);
this._context.fillStyle = this._fillStyles[category];
this._context.fillRect(0, y, this._maxInnerBarWidth, Math.floor(height));
this._context.restore();

this._context.strokeStyle = WebInspector.TimelinePresentationModel.categories()[category].borderColor;
this._context.strokeRect(x, y, width, Math.floor(height));
bottomOffset -= height - 1;
}

var nonCPUTime = frame.duration - frame.cpuTime;
var y0 = Math.floor(bottomOffset - nonCPUTime * scale) + 0.5;
var y1 = Math.floor(bottomOffset) + 0.5;

this._context.strokeStyle = "rgb(90, 90, 90)";
this._context.beginPath();
this._context.moveTo(x, y1);
this._context.lineTo(x, y0);
this._context.lineTo(x + width, y0);
this._context.lineTo(x + width, y1);
this._context.stroke();
},

getWindowTimes: function(windowLeft, windowRight)
{
var windowSpan = this.element.clientWidth;
var leftOffset = windowLeft * windowSpan - this._outerPadding + this._actualPadding;
var rightOffset = windowRight * windowSpan - this._outerPadding;
var bars = this.element.children;
var firstBar = Math.floor(Math.max(leftOffset, 0) / this._actualOuterBarWidth);
var lastBar = Math.min(Math.floor(rightOffset / this._actualOuterBarWidth), this._barTimes.length - 1);
const snapToRightTolerancePixels = 3;
return {
startTime: firstBar >= this._barTimes.length ? Infinity : this._barTimes[firstBar].startTime,
endTime: rightOffset + snapToRightTolerancePixels > windowSpan ? Infinity : this._barTimes[lastBar].endTime
}
},

__proto__: WebInspector.View.prototype
}


WebInspector.TimelineWindowFilter = function(pane)
{
this._pane = pane;
}

WebInspector.TimelineWindowFilter.prototype = {

accept: function(record)
{
return record.lastChildEndTime >= this._pane._windowStartTime && record.startTime <= this._pane._windowEndTime;
}
}
;



WebInspector.TimelinePresentationModel = function()
{
this._linkifier = new WebInspector.Linkifier();
this._glueRecords = false;
this._filters = [];
this.reset();
}

WebInspector.TimelinePresentationModel.shortRecordThreshold = 0.015;

WebInspector.TimelinePresentationModel.categories = function()
{
if (WebInspector.TimelinePresentationModel._categories)
return WebInspector.TimelinePresentationModel._categories;
WebInspector.TimelinePresentationModel._categories = {
program: new WebInspector.TimelineCategory("program", WebInspector.UIString("Program"), -1, "#BBBBBB", "#DDDDDD", "#FFFFFF"),
loading: new WebInspector.TimelineCategory("loading", WebInspector.UIString("Loading"), 0, "#5A8BCC", "#8EB6E9", "#70A2E3"),
scripting: new WebInspector.TimelineCategory("scripting", WebInspector.UIString("Scripting"), 1, "#D8AA34", "#F3D07A", "#F1C453"),
rendering: new WebInspector.TimelineCategory("rendering", WebInspector.UIString("Rendering"), 2, "#8266CC", "#AF9AEB", "#9A7EE6"),
painting: new WebInspector.TimelineCategory("painting", WebInspector.UIString("Painting"), 2, "#5FA050", "#8DC286", "#71B363")
};
return WebInspector.TimelinePresentationModel._categories;
};


WebInspector.TimelinePresentationModel._initRecordStyles = function()
{
if (WebInspector.TimelinePresentationModel._recordStylesMap)
return WebInspector.TimelinePresentationModel._recordStylesMap;

var recordTypes = WebInspector.TimelineModel.RecordType;
var categories = WebInspector.TimelinePresentationModel.categories();

var recordStyles = {};
recordStyles[recordTypes.Root] = { title: "#root", category: categories["loading"] };
recordStyles[recordTypes.Program] = { title: WebInspector.UIString("Program"), category: categories["program"] };
recordStyles[recordTypes.EventDispatch] = { title: WebInspector.UIString("Event"), category: categories["scripting"] };
recordStyles[recordTypes.BeginFrame] = { title: WebInspector.UIString("Frame Start"), category: categories["rendering"] };
recordStyles[recordTypes.ScheduleStyleRecalculation] = { title: WebInspector.UIString("Schedule Style Recalculation"), category: categories["rendering"] };
recordStyles[recordTypes.RecalculateStyles] = { title: WebInspector.UIString("Recalculate Style"), category: categories["rendering"] };
recordStyles[recordTypes.InvalidateLayout] = { title: WebInspector.UIString("Invalidate Layout"), category: categories["rendering"] };
recordStyles[recordTypes.Layout] = { title: WebInspector.UIString("Layout"), category: categories["rendering"] };
recordStyles[recordTypes.Paint] = { title: WebInspector.UIString("Paint"), category: categories["painting"] };
recordStyles[recordTypes.ScrollLayer] = { title: WebInspector.UIString("Scroll"), category: categories["painting"] };
recordStyles[recordTypes.DecodeImage] = { title: WebInspector.UIString("Image Decode"), category: categories["painting"] };
recordStyles[recordTypes.ResizeImage] = { title: WebInspector.UIString("Image Resize"), category: categories["painting"] };
recordStyles[recordTypes.CompositeLayers] = { title: WebInspector.UIString("Composite Layers"), category: categories["painting"] };
recordStyles[recordTypes.ParseHTML] = { title: WebInspector.UIString("Parse"), category: categories["loading"] };
recordStyles[recordTypes.TimerInstall] = { title: WebInspector.UIString("Install Timer"), category: categories["scripting"] };
recordStyles[recordTypes.TimerRemove] = { title: WebInspector.UIString("Remove Timer"), category: categories["scripting"] };
recordStyles[recordTypes.TimerFire] = { title: WebInspector.UIString("Timer Fired"), category: categories["scripting"] };
recordStyles[recordTypes.XHRReadyStateChange] = { title: WebInspector.UIString("XHR Ready State Change"), category: categories["scripting"] };
recordStyles[recordTypes.XHRLoad] = { title: WebInspector.UIString("XHR Load"), category: categories["scripting"] };
recordStyles[recordTypes.EvaluateScript] = { title: WebInspector.UIString("Evaluate Script"), category: categories["scripting"] };
recordStyles[recordTypes.ResourceSendRequest] = { title: WebInspector.UIString("Send Request"), category: categories["loading"] };
recordStyles[recordTypes.ResourceReceiveResponse] = { title: WebInspector.UIString("Receive Response"), category: categories["loading"] };
recordStyles[recordTypes.ResourceFinish] = { title: WebInspector.UIString("Finish Loading"), category: categories["loading"] };
recordStyles[recordTypes.FunctionCall] = { title: WebInspector.UIString("Function Call"), category: categories["scripting"] };
recordStyles[recordTypes.ResourceReceivedData] = { title: WebInspector.UIString("Receive Data"), category: categories["loading"] };
recordStyles[recordTypes.GCEvent] = { title: WebInspector.UIString("GC Event"), category: categories["scripting"] };
recordStyles[recordTypes.MarkDOMContent] = { title: WebInspector.UIString("DOMContent event"), category: categories["scripting"] };
recordStyles[recordTypes.MarkLoad] = { title: WebInspector.UIString("Load event"), category: categories["scripting"] };
recordStyles[recordTypes.TimeStamp] = { title: WebInspector.UIString("Stamp"), category: categories["scripting"] };
recordStyles[recordTypes.Time] = { title: WebInspector.UIString("Time"), category: categories["scripting"] };
recordStyles[recordTypes.TimeEnd] = { title: WebInspector.UIString("Time End"), category: categories["scripting"] };
recordStyles[recordTypes.ScheduleResourceRequest] = { title: WebInspector.UIString("Schedule Request"), category: categories["loading"] };
recordStyles[recordTypes.RequestAnimationFrame] = { title: WebInspector.UIString("Request Animation Frame"), category: categories["scripting"] };
recordStyles[recordTypes.CancelAnimationFrame] = { title: WebInspector.UIString("Cancel Animation Frame"), category: categories["scripting"] };
recordStyles[recordTypes.FireAnimationFrame] = { title: WebInspector.UIString("Animation Frame Fired"), category: categories["scripting"] };

WebInspector.TimelinePresentationModel._recordStylesMap = recordStyles;
return recordStyles;
}


WebInspector.TimelinePresentationModel.recordStyle = function(record)
{
var recordStyles = WebInspector.TimelinePresentationModel._initRecordStyles();
var result = recordStyles[record.type];
if (!result) {
result = {
title: WebInspector.UIString("Unknown: %s", record.type),
category: WebInspector.TimelinePresentationModel.categories()["program"]
};
recordStyles[record.type] = result;
}
return result;
}

WebInspector.TimelinePresentationModel.categoryForRecord = function(record)
{
return WebInspector.TimelinePresentationModel.recordStyle(record).category;
}

WebInspector.TimelinePresentationModel.isEventDivider = function(record)
{
var recordTypes = WebInspector.TimelineModel.RecordType;
if (record.type === recordTypes.TimeStamp)
return true;
if (record.type === recordTypes.MarkDOMContent || record.type === recordTypes.MarkLoad) {
var mainFrame = WebInspector.resourceTreeModel.mainFrame;
if (mainFrame && mainFrame.id === record.frameId)
return true;
}
return false;
}


WebInspector.TimelinePresentationModel.forAllRecords = function(recordsArray, preOrderCallback, postOrderCallback)
{
if (!recordsArray)
return;
var stack = [{array: recordsArray, index: 0}];
while (stack.length) {
var entry = stack[stack.length - 1];
var records = entry.array;
if (entry.index < records.length) {
var record = records[entry.index];
if (preOrderCallback && preOrderCallback(record))
return;
if (record.children)
stack.push({array: record.children, index: 0, record: record});
else if (postOrderCallback && postOrderCallback(record))
return;
++entry.index;
} else {
if (entry.record && postOrderCallback && postOrderCallback(entry.record))
return;
stack.pop();
}
}
}


WebInspector.TimelinePresentationModel.needsPreviewElement = function(recordType)
{
if (!recordType)
return false;
const recordTypes = WebInspector.TimelineModel.RecordType;
switch (recordType) {
case recordTypes.ScheduleResourceRequest:
case recordTypes.ResourceSendRequest:
case recordTypes.ResourceReceiveResponse:
case recordTypes.ResourceReceivedData:
case recordTypes.ResourceFinish:
return true;
default:
return false;
}
}


WebInspector.TimelinePresentationModel.createEventDivider = function(recordType, title)
{
var eventDivider = document.createElement("div");
eventDivider.className = "resources-event-divider";
var recordTypes = WebInspector.TimelineModel.RecordType;

if (recordType === recordTypes.MarkDOMContent)
eventDivider.className += " resources-blue-divider";
else if (recordType === recordTypes.MarkLoad)
eventDivider.className += " resources-red-divider";
else if (recordType === recordTypes.TimeStamp)
eventDivider.className += " resources-orange-divider";
else if (recordType === recordTypes.BeginFrame)
eventDivider.className += " timeline-frame-divider";

if (title)
eventDivider.title = title;

return eventDivider;
}

WebInspector.TimelinePresentationModel._hiddenRecords = { }
WebInspector.TimelinePresentationModel._hiddenRecords[WebInspector.TimelineModel.RecordType.MarkDOMContent] = 1;
WebInspector.TimelinePresentationModel._hiddenRecords[WebInspector.TimelineModel.RecordType.MarkLoad] = 1;
WebInspector.TimelinePresentationModel._hiddenRecords[WebInspector.TimelineModel.RecordType.ScheduleStyleRecalculation] = 1;
WebInspector.TimelinePresentationModel._hiddenRecords[WebInspector.TimelineModel.RecordType.InvalidateLayout] = 1;

WebInspector.TimelinePresentationModel.prototype = {

addFilter: function(filter)
{
this._filters.push(filter);
},


removeFilter: function(filter)
{
var index = this._filters.indexOf(filter);
if (index !== -1)
this._filters.splice(index, 1);
},

rootRecord: function()
{
return this._rootRecord;
},

frames: function()
{
return this._frames;
},

reset: function()
{
this._linkifier.reset();
this._rootRecord = new WebInspector.TimelinePresentationModel.Record(this, { type: WebInspector.TimelineModel.RecordType.Root }, null, null, null, false);
this._sendRequestRecords = {};
this._scheduledResourceRequests = {};
this._timerRecords = {};
this._requestAnimationFrameRecords = {};
this._timeRecords = {};
this._frames = [];
this._minimumRecordTime = -1;
this._lastInvalidateLayout = {};
this._lastScheduleStyleRecalculation = {};
},

addFrame: function(frame)
{
this._frames.push(frame);
},

addRecord: function(record)
{
if (this._minimumRecordTime === -1 || record.startTime < this._minimumRecordTime)
this._minimumRecordTime = WebInspector.TimelineModel.startTimeInSeconds(record);

var records;
if (record.type === WebInspector.TimelineModel.RecordType.Program)
records = record.children;
else
records = [record];

var formattedRecords = [];
var recordsCount = records.length;
for (var i = 0; i < recordsCount; ++i)
formattedRecords.push(this._innerAddRecord(records[i], this._rootRecord));
return formattedRecords;
},

_innerAddRecord: function(record, parentRecord)
{
const recordTypes = WebInspector.TimelineModel.RecordType;
var isHiddenRecord = record.type in WebInspector.TimelinePresentationModel._hiddenRecords;
var origin;
if (!isHiddenRecord) {
var newParentRecord = this._findParentRecord(record);
if (newParentRecord) {
origin = parentRecord;
parentRecord = newParentRecord;
}
}

var children = record.children;
var scriptDetails;
if (record.data && record.data["scriptName"]) {
scriptDetails = {
scriptName: record.data["scriptName"],
scriptLine: record.data["scriptLine"]
}
};

if ((record.type === recordTypes.TimerFire || record.type === recordTypes.FireAnimationFrame) && children && children.length) {
var childRecord = children[0];
if (childRecord.type === recordTypes.FunctionCall) {
scriptDetails = {
scriptName: childRecord.data["scriptName"],
scriptLine: childRecord.data["scriptLine"]
};
children = childRecord.children.concat(children.slice(1));
}
}

var formattedRecord = new WebInspector.TimelinePresentationModel.Record(this, record, parentRecord, origin, scriptDetails, isHiddenRecord);

if (isHiddenRecord)
return formattedRecord;

formattedRecord.collapsed = (parentRecord === this._rootRecord);

var childrenCount = children ? children.length : 0;
for (var i = 0; i < childrenCount; ++i)
this._innerAddRecord(children[i], formattedRecord);

formattedRecord.calculateAggregatedStats(WebInspector.TimelinePresentationModel.categories());

if (origin) {
var lastChildEndTime = formattedRecord.lastChildEndTime;
var aggregatedStats = formattedRecord.aggregatedStats;
for (var currentRecord = formattedRecord.parent; !currentRecord.isRoot(); currentRecord = currentRecord.parent) {
currentRecord._cpuTime += formattedRecord._cpuTime;
if (currentRecord.lastChildEndTime < lastChildEndTime)
currentRecord.lastChildEndTime = lastChildEndTime;
for (var category in aggregatedStats)
currentRecord.aggregatedStats[category] += aggregatedStats[category];
}
}
origin = formattedRecord.origin();
if (!origin.isRoot()) {
origin.selfTime -= formattedRecord.endTime - formattedRecord.startTime;
}
return formattedRecord;
},

_findParentRecord: function(record)
{
if (!this._glueRecords)
return null;
var recordTypes = WebInspector.TimelineModel.RecordType;

switch (record.type) {
case recordTypes.ResourceReceiveResponse:
case recordTypes.ResourceFinish:
case recordTypes.ResourceReceivedData:
return this._sendRequestRecords[record.data["requestId"]];

case recordTypes.ResourceSendRequest:
return this._rootRecord;

case recordTypes.TimerFire:
return this._timerRecords[record.data["timerId"]];

case recordTypes.ResourceSendRequest:
return this._scheduledResourceRequests[record.data["url"]];

case recordTypes.FireAnimationFrame:
return this._requestAnimationFrameRecords[record.data["id"]];

case recordTypes.Time:
return this._rootRecord;

case recordTypes.TimeEnd:
return this._timeRecords[record.data["message"]];
}
},

setGlueRecords: function(glue)
{
this._glueRecords = glue;
},

invalidateFilteredRecords: function()
{
delete this._filteredRecords;
},

filteredRecords: function()
{
if (this._filteredRecords)
return this._filteredRecords;

var recordsInWindow = [];

var stack = [{children: this._rootRecord.children, index: 0, parentIsCollapsed: false}];
while (stack.length) {
var entry = stack[stack.length - 1];
var records = entry.children;
if (records && entry.index < records.length) {
var record = records[entry.index];
++entry.index;

if (this.isVisible(record)) {
++record.parent._invisibleChildrenCount;
if (!entry.parentIsCollapsed)
recordsInWindow.push(record);
}

record._invisibleChildrenCount = 0;

stack.push({children: record.children,
index: 0,
parentIsCollapsed: (entry.parentIsCollapsed || record.collapsed),
parentRecord: record,
windowLengthBeforeChildrenTraversal: recordsInWindow.length});
} else {
stack.pop();
if (entry.parentRecord)
entry.parentRecord._visibleChildrenCount = recordsInWindow.length - entry.windowLengthBeforeChildrenTraversal;
}
}

this._filteredRecords = recordsInWindow;
return recordsInWindow;
},

filteredFrames: function(startTime, endTime)
{
function compareStartTime(value, object)
{
return value - object.startTime;
}
function compareEndTime(value, object)
{
return value - object.endTime;
}
var firstFrame = insertionIndexForObjectInListSortedByFunction(startTime, this._frames, compareStartTime);
var lastFrame = insertionIndexForObjectInListSortedByFunction(endTime, this._frames, compareEndTime);
while (lastFrame < this._frames.length && this._frames[lastFrame].endTime <= endTime)
++lastFrame;
return this._frames.slice(firstFrame, lastFrame);
},

isVisible: function(record)
{
for (var i = 0; i < this._filters.length; ++i) {
if (!this._filters[i].accept(record))
return false;
}
return true;
},


generateMainThreadBarPopupContent: function(info)
{
var firstTaskIndex = info.firstTaskIndex;
var lastTaskIndex = info.lastTaskIndex;
var tasks = info.tasks;
var messageCount = lastTaskIndex - firstTaskIndex + 1;
var cpuTime = 0;

for (var i = firstTaskIndex; i <= lastTaskIndex; ++i) {
var task = tasks[i];
cpuTime += task.endTime - task.startTime;
}
var startTime = tasks[firstTaskIndex].startTime;
var endTime = tasks[lastTaskIndex].endTime;
var duration = endTime - startTime;
var offset = this._minimumRecordTime;

var contentHelper = new WebInspector.TimelinePresentationModel.PopupContentHelper(WebInspector.UIString("CPU"));
var durationText = WebInspector.UIString("%s (at %s)", Number.secondsToString(duration, true),
Number.secondsToString(startTime - offset, true));
contentHelper._appendTextRow(WebInspector.UIString("Duration"), durationText);
contentHelper._appendTextRow(WebInspector.UIString("CPU time"), Number.secondsToString(cpuTime, true));
contentHelper._appendTextRow(WebInspector.UIString("Message Count"), messageCount);
return contentHelper._contentTable;
},

__proto__: WebInspector.Object.prototype
}


WebInspector.TimelinePresentationModel.Record = function(presentationModel, record, parentRecord, origin, scriptDetails, hidden)
{
this._linkifier = presentationModel._linkifier;
this._aggregatedStats = [];
this._record = record;
this._children = [];
if (!hidden && parentRecord) {
this.parent = parentRecord;
parentRecord.children.push(this);
}
if (origin)
this._origin = origin;

this._selfTime = this.endTime - this.startTime;
this._lastChildEndTime = this.endTime;
this._startTimeOffset = this.startTime - presentationModel._minimumRecordTime;

if (record.data && record.data["url"])
this.url = record.data["url"];
if (scriptDetails) {
this.scriptName = scriptDetails.scriptName;
this.scriptLine = scriptDetails.scriptLine;
}
if (parentRecord && parentRecord.callSiteStackTrace)
this.callSiteStackTrace = parentRecord.callSiteStackTrace;

var recordTypes = WebInspector.TimelineModel.RecordType;
switch (record.type) {
case recordTypes.ResourceSendRequest:

presentationModel._sendRequestRecords[record.data["requestId"]] = this;
break;

case recordTypes.ScheduleResourceRequest:
presentationModel._scheduledResourceRequests[record.data["url"]] = this;
break;

case recordTypes.ResourceReceiveResponse:
var sendRequestRecord = presentationModel._sendRequestRecords[record.data["requestId"]];
if (sendRequestRecord) { 
this.url = sendRequestRecord.url;

sendRequestRecord._refreshDetails();
if (sendRequestRecord.parent !== presentationModel._rootRecord && sendRequestRecord.parent.type === recordTypes.ScheduleResourceRequest)
sendRequestRecord.parent._refreshDetails();
}
break;

case recordTypes.ResourceReceivedData:
case recordTypes.ResourceFinish:
var sendRequestRecord = presentationModel._sendRequestRecords[record.data["requestId"]];
if (sendRequestRecord) 
this.url = sendRequestRecord.url;
break;

case recordTypes.TimerInstall:
this.timeout = record.data["timeout"];
this.singleShot = record.data["singleShot"];
presentationModel._timerRecords[record.data["timerId"]] = this;
break;

case recordTypes.TimerFire:
var timerInstalledRecord = presentationModel._timerRecords[record.data["timerId"]];
if (timerInstalledRecord) {
this.callSiteStackTrace = timerInstalledRecord.stackTrace;
this.timeout = timerInstalledRecord.timeout;
this.singleShot = timerInstalledRecord.singleShot;
}
break;

case recordTypes.RequestAnimationFrame:
presentationModel._requestAnimationFrameRecords[record.data["id"]] = this;
break;

case recordTypes.FireAnimationFrame:
var requestAnimationRecord = presentationModel._requestAnimationFrameRecords[record.data["id"]];
if (requestAnimationRecord)
this.callSiteStackTrace = requestAnimationRecord.stackTrace;
break;

case recordTypes.Time:
presentationModel._timeRecords[record.data["message"]] = this;
break;

case recordTypes.TimeEnd:
var message = record.data["message"];
var timeRecord = presentationModel._timeRecords[message];
delete presentationModel._timeRecords[message];
if (timeRecord) {
this.timeRecord = timeRecord;
timeRecord.timeEndRecord = this;
var intervalDuration = this.startTime - timeRecord.startTime;
this.intervalDuration = intervalDuration;
timeRecord.intervalDuration = intervalDuration;
}
break;

case recordTypes.ScheduleStyleRecalculation:
presentationModel._lastScheduleStyleRecalculation[this.frameId] = this;
break;

case recordTypes.RecalculateStyles:
var scheduleStyleRecalculationRecord = presentationModel._lastScheduleStyleRecalculation[this.frameId];
if (!scheduleStyleRecalculationRecord)
break;
this.callSiteStackTrace = scheduleStyleRecalculationRecord.stackTrace;
break;

case recordTypes.InvalidateLayout:
presentationModel._lastInvalidateLayout[this.frameId] = this;
break;

case recordTypes.Layout:
var invalidateLayoutRecord = presentationModel._lastInvalidateLayout[this.frameId];
if (invalidateLayoutRecord)
this.callSiteStackTrace = invalidateLayoutRecord.stackTrace || invalidateLayoutRecord.callSiteStackTrace;
if (this.stackTrace)
this.setHasWarning();
presentationModel._lastInvalidateLayout[this.frameId] = null;
break;
}
}

WebInspector.TimelinePresentationModel.Record.prototype = {
get lastChildEndTime()
{
return this._lastChildEndTime;
},

set lastChildEndTime(time)
{
this._lastChildEndTime = time;
},

get selfTime()
{
return this._selfTime;
},

set selfTime(time)
{
this._selfTime = time;
},

get cpuTime()
{
return this._cpuTime;
},

isLong: function()
{
return (this._lastChildEndTime - this.startTime) > WebInspector.TimelinePresentationModel.shortRecordThreshold;
},


isRoot: function()
{
return this.type === WebInspector.TimelineModel.RecordType.Root;
},


origin: function()
{
return this._origin || this.parent;
},


get children()
{
return this._children;
},


get visibleChildrenCount()
{
return this._visibleChildrenCount || 0;
},


get invisibleChildrenCount()
{
return this._invisibleChildrenCount || 0;
},


get category()
{
return WebInspector.TimelinePresentationModel.recordStyle(this._record).category
},


get title()
{
return this.type === WebInspector.TimelineModel.RecordType.TimeStamp ? this._record.data["message"] :
WebInspector.TimelinePresentationModel.recordStyle(this._record).title;
},


get startTime()
{
return WebInspector.TimelineModel.startTimeInSeconds(this._record);
},


get endTime()
{
return WebInspector.TimelineModel.endTimeInSeconds(this._record);
},


get data()
{
return this._record.data;
},


get type()
{
return this._record.type;
},


get frameId()
{
return this._record.frameId;
},


get usedHeapSizeDelta()
{
return this._record.usedHeapSizeDelta || 0;
},


get usedHeapSize()
{
return this._record.usedHeapSize;
},


get stackTrace()
{
if (this._record.stackTrace && this._record.stackTrace.length)
return this._record.stackTrace;
return null;
},

containsTime: function(time)
{
return this.startTime <= time && time <= this.endTime;
},


generatePopupContent: function(callback)
{
if (WebInspector.TimelinePresentationModel.needsPreviewElement(this.type))
WebInspector.DOMPresentationUtils.buildImagePreviewContents(this.url, false, this._generatePopupContentWithImagePreview.bind(this, callback));
else
this._generatePopupContentWithImagePreview(callback);
},


_generatePopupContentWithImagePreview: function(callback, previewElement)
{
var contentHelper = new WebInspector.TimelinePresentationModel.PopupContentHelper(this.title);
var text = WebInspector.UIString("%s (at %s)", Number.secondsToString(this._lastChildEndTime - this.startTime, true),
Number.secondsToString(this._startTimeOffset));
contentHelper._appendTextRow(WebInspector.UIString("Duration"), text);

if (this._children.length) {
contentHelper._appendTextRow(WebInspector.UIString("Self Time"), Number.secondsToString(this._selfTime, true));
contentHelper._appendTextRow(WebInspector.UIString("CPU Time"), Number.secondsToString(this._cpuTime, true));
contentHelper._appendElementRow(WebInspector.UIString("Aggregated Time"),
WebInspector.TimelinePresentationModel._generateAggregatedInfo(this._aggregatedStats));
}
const recordTypes = WebInspector.TimelineModel.RecordType;


var callSiteStackTraceLabel;
var callStackLabel;

switch (this.type) {
case recordTypes.GCEvent:
contentHelper._appendTextRow(WebInspector.UIString("Collected"), Number.bytesToString(this.data["usedHeapSizeDelta"]));
break;
case recordTypes.TimerInstall:
case recordTypes.TimerFire:
case recordTypes.TimerRemove:
contentHelper._appendTextRow(WebInspector.UIString("Timer ID"), this.data["timerId"]);
if (typeof this.timeout === "number") {
contentHelper._appendTextRow(WebInspector.UIString("Timeout"), Number.secondsToString(this.timeout / 1000));
contentHelper._appendTextRow(WebInspector.UIString("Repeats"), !this.singleShot);
}
break;
case recordTypes.FireAnimationFrame:
contentHelper._appendTextRow(WebInspector.UIString("Callback ID"), this.data["id"]);
break;
case recordTypes.FunctionCall:
contentHelper._appendElementRow(WebInspector.UIString("Location"), this._linkifyScriptLocation());
break;
case recordTypes.ScheduleResourceRequest:
case recordTypes.ResourceSendRequest:
case recordTypes.ResourceReceiveResponse:
case recordTypes.ResourceReceivedData:
case recordTypes.ResourceFinish:
contentHelper._appendElementRow(WebInspector.UIString("Resource"), WebInspector.linkifyResourceAsNode(this.url));
if (previewElement)
contentHelper._appendElementRow(WebInspector.UIString("Preview"), previewElement);
if (this.data["requestMethod"])
contentHelper._appendTextRow(WebInspector.UIString("Request Method"), this.data["requestMethod"]);
if (typeof this.data["statusCode"] === "number")
contentHelper._appendTextRow(WebInspector.UIString("Status Code"), this.data["statusCode"]);
if (this.data["mimeType"])
contentHelper._appendTextRow(WebInspector.UIString("MIME Type"), this.data["mimeType"]);
if (this.data["encodedDataLength"])
contentHelper._appendTextRow(WebInspector.UIString("Encoded Data Length"), WebInspector.UIString("%d Bytes", this.data["encodedDataLength"]));
break;
case recordTypes.EvaluateScript:
if (this.data && this.url)
contentHelper._appendElementRow(WebInspector.UIString("Script"), this._linkifyLocation(this.url, this.data["lineNumber"]));
break;
case recordTypes.Paint:
contentHelper._appendTextRow(WebInspector.UIString("Location"), WebInspector.UIString("(%d, %d)", this.data["x"], this.data["y"]));
contentHelper._appendTextRow(WebInspector.UIString("Dimensions"), WebInspector.UIString("%d × %d", this.data["width"], this.data["height"]));
break;
case recordTypes.RecalculateStyles: 
callSiteStackTraceLabel = WebInspector.UIString("Styles invalidated");
callStackLabel = WebInspector.UIString("Styles recalculation forced");
break;
case recordTypes.Layout:
callSiteStackTraceLabel = WebInspector.UIString("Layout invalidated");
if (this.stackTrace) {
callStackLabel = WebInspector.UIString("Layout forced");
contentHelper._appendTextRow(WebInspector.UIString("Note"), WebInspector.UIString("Forced synchronous layout is a possible performance bottleneck."));
}
break;
case recordTypes.Time:
case recordTypes.TimeEnd:
contentHelper._appendTextRow(WebInspector.UIString("Message"), this.data["message"]);
if (typeof this.intervalDuration === "number")
contentHelper._appendTextRow(WebInspector.UIString("Interval Duration"), Number.secondsToString(this.intervalDuration, true));
break;
default:
if (this.details())
contentHelper._appendTextRow(WebInspector.UIString("Details"), this.details());
break;
}

if (this.scriptName && this.type !== recordTypes.FunctionCall)
contentHelper._appendElementRow(WebInspector.UIString("Function Call"), this._linkifyScriptLocation());

if (this.usedHeapSize) {
if (this.usedHeapSizeDelta) {
var sign = this.usedHeapSizeDelta > 0 ? "+" : "-";
contentHelper._appendTextRow(WebInspector.UIString("Used Heap Size"),
WebInspector.UIString("%s (%s%s)", Number.bytesToString(this.usedHeapSize), sign, Number.bytesToString(this.usedHeapSizeDelta)));
} else if (this.category === WebInspector.TimelinePresentationModel.categories().scripting)
contentHelper._appendTextRow(WebInspector.UIString("Used Heap Size"), Number.bytesToString(this.usedHeapSize));
}

if (this.callSiteStackTrace)
contentHelper._appendStackTrace(callSiteStackTraceLabel || WebInspector.UIString("Call Site stack"), this.callSiteStackTrace, this._linkifyCallFrame.bind(this));

if (this.stackTrace)
contentHelper._appendStackTrace(callStackLabel || WebInspector.UIString("Call Stack"), this.stackTrace, this._linkifyCallFrame.bind(this));

callback(contentHelper._contentTable);
},

_refreshDetails: function()
{
delete this._details;
},


details: function()
{
if (!this._details)
this._details = this._getRecordDetails();
return this._details;
},

_getRecordDetails: function()
{
switch (this.type) {
case WebInspector.TimelineModel.RecordType.GCEvent:
return WebInspector.UIString("%s collected", Number.bytesToString(this.data["usedHeapSizeDelta"]));
case WebInspector.TimelineModel.RecordType.TimerFire:
return this._linkifyScriptLocation(this.data["timerId"]);
case WebInspector.TimelineModel.RecordType.FunctionCall:
return this._linkifyScriptLocation();
case WebInspector.TimelineModel.RecordType.FireAnimationFrame:
return this._linkifyScriptLocation(this.data["id"]);
case WebInspector.TimelineModel.RecordType.EventDispatch:
return this.data ? this.data["type"] : null;
case WebInspector.TimelineModel.RecordType.Paint:
return this.data["width"] + "\u2009\u00d7\u2009" + this.data["height"];
case WebInspector.TimelineModel.RecordType.DecodeImage:
return this.data["imageType"];
case WebInspector.TimelineModel.RecordType.ResizeImage:
return this.data["cached"] ? WebInspector.UIString("cached") : WebInspector.UIString("non-cached");
case WebInspector.TimelineModel.RecordType.TimerInstall:
case WebInspector.TimelineModel.RecordType.TimerRemove:
return this._linkifyTopCallFrame(this.data["timerId"]);
case WebInspector.TimelineModel.RecordType.RequestAnimationFrame:
case WebInspector.TimelineModel.RecordType.CancelAnimationFrame:
return this._linkifyTopCallFrame(this.data["id"]);
case WebInspector.TimelineModel.RecordType.ParseHTML:
case WebInspector.TimelineModel.RecordType.RecalculateStyles:
return this._linkifyTopCallFrame();
case WebInspector.TimelineModel.RecordType.EvaluateScript:
return this.url ? this._linkifyLocation(this.url, this.data["lineNumber"], 0) : null;
case WebInspector.TimelineModel.RecordType.XHRReadyStateChange:
case WebInspector.TimelineModel.RecordType.XHRLoad:
case WebInspector.TimelineModel.RecordType.ScheduleResourceRequest:
case WebInspector.TimelineModel.RecordType.ResourceSendRequest:
case WebInspector.TimelineModel.RecordType.ResourceReceivedData:
case WebInspector.TimelineModel.RecordType.ResourceReceiveResponse:
case WebInspector.TimelineModel.RecordType.ResourceFinish:
return WebInspector.displayNameForURL(this.url);
case WebInspector.TimelineModel.RecordType.Time:
case WebInspector.TimelineModel.RecordType.TimeEnd:
case WebInspector.TimelineModel.RecordType.TimeStamp:
return this.data["message"];
default:
return this._linkifyScriptLocation() || this._linkifyTopCallFrame() || null;
}
},


_linkifyLocation: function(url, lineNumber, columnNumber)
{

columnNumber = columnNumber ? columnNumber - 1 : 0;
return this._linkifier.linkifyLocation(url, lineNumber - 1, columnNumber, "timeline-details");
},

_linkifyCallFrame: function(callFrame)
{
return this._linkifyLocation(callFrame.url, callFrame.lineNumber, callFrame.columnNumber);
},


_linkifyTopCallFrame: function(defaultValue)
{
if (this.stackTrace)
return this._linkifyCallFrame(this.stackTrace[0]);
if (this.callSiteStackTrace)
return this._linkifyCallFrame(this.callSiteStackTrace[0]);
return defaultValue;
},


_linkifyScriptLocation: function(defaultValue)
{
return this.scriptName ? this._linkifyLocation(this.scriptName, this.scriptLine, 0) : defaultValue;
},

calculateAggregatedStats: function(categories)
{
this._aggregatedStats = {};
for (var category in categories)
this._aggregatedStats[category] = 0;
this._cpuTime = this._selfTime;

for (var index = this._children.length; index; --index) {
var child = this._children[index - 1];
for (var category in categories)
this._aggregatedStats[category] += child._aggregatedStats[category];
}
for (var category in this._aggregatedStats)
this._cpuTime += this._aggregatedStats[category];
this._aggregatedStats[this.category.name] += this._selfTime;
},

get aggregatedStats()
{
return this._aggregatedStats;
},

setHasWarning: function()
{
this.hasWarning = true;
for (var parent = this.parent; parent && !parent.childHasWarning; parent = parent.parent)
parent.childHasWarning = true;
}
}


WebInspector.TimelinePresentationModel._generateAggregatedInfo = function(aggregatedStats)
{
var cell = document.createElement("span");
cell.className = "timeline-aggregated-info";
for (var index in aggregatedStats) {
var label = document.createElement("div");
label.className = "timeline-aggregated-category timeline-" + index;
cell.appendChild(label);
var text = document.createElement("span");
text.textContent = Number.secondsToString(aggregatedStats[index], true);
cell.appendChild(text);
}
return cell;
}


WebInspector.TimelinePresentationModel.PopupContentHelper = function(title)
{
this._contentTable = document.createElement("table");
var titleCell = this._createCell(WebInspector.UIString("%s - Details", title), "timeline-details-title");
titleCell.colSpan = 2;
var titleRow = document.createElement("tr");
titleRow.appendChild(titleCell);
this._contentTable.appendChild(titleRow);
}

WebInspector.TimelinePresentationModel.PopupContentHelper.prototype = {

_createCell: function(content, styleName)
{
var text = document.createElement("label");
text.appendChild(document.createTextNode(content));
var cell = document.createElement("td");
cell.className = "timeline-details";
if (styleName)
cell.className += " " + styleName;
cell.textContent = content;
return cell;
},

_appendTextRow: function(title, content)
{
var row = document.createElement("tr");
row.appendChild(this._createCell(title, "timeline-details-row-title"));
row.appendChild(this._createCell(content, "timeline-details-row-data"));
this._contentTable.appendChild(row);
},


_appendElementRow: function(title, content, titleStyle)
{
var row = document.createElement("tr");
var titleCell = this._createCell(title, "timeline-details-row-title");
if (titleStyle)
titleCell.addStyleClass(titleStyle);
row.appendChild(titleCell);
var cell = document.createElement("td");
cell.className = "timeline-details";
cell.appendChild(content);
row.appendChild(cell);
this._contentTable.appendChild(row);
},

_appendStackTrace: function(title, stackTrace, callFrameLinkifier)
{
this._appendTextRow("", "");
var framesTable = document.createElement("table");
for (var i = 0; i < stackTrace.length; ++i) {
var stackFrame = stackTrace[i];
var row = document.createElement("tr");
row.className = "timeline-details";
row.appendChild(this._createCell(stackFrame.functionName ? stackFrame.functionName : WebInspector.UIString("(anonymous function)"), "timeline-function-name"));
row.appendChild(this._createCell(" @ "));
var linkCell = document.createElement("td");
var urlElement = callFrameLinkifier(stackFrame);
linkCell.appendChild(urlElement);
row.appendChild(linkCell);
framesTable.appendChild(row);
}
this._appendElementRow(title, framesTable, "timeline-stacktrace-title");
}
}

WebInspector.TimelinePresentationModel.generatePopupContentForFrame = function(frame)
{
var contentHelper = new WebInspector.TimelinePresentationModel.PopupContentHelper(WebInspector.UIString("Frame"));
var durationInSeconds = frame.endTime - frame.startTime;
var durationText = WebInspector.UIString("%s (at %s)", Number.secondsToString(frame.endTime - frame.startTime, true),
Number.secondsToString(frame.startTimeOffset, true));
contentHelper._appendTextRow(WebInspector.UIString("Duration"), durationText);
contentHelper._appendTextRow(WebInspector.UIString("FPS"), Math.floor(1 / durationInSeconds));
contentHelper._appendTextRow(WebInspector.UIString("CPU time"), Number.secondsToString(frame.cpuTime, true));
contentHelper._appendElementRow(WebInspector.UIString("Aggregated Time"),
WebInspector.TimelinePresentationModel._generateAggregatedInfo(frame.timeByCategory));

return contentHelper._contentTable;
}


WebInspector.TimelinePresentationModel.generatePopupContentForFrameStatistics = function(statistics)
{

function formatTimeAndFPS(time)
{
return WebInspector.UIString("%s (%.0f FPS)", Number.secondsToString(time, true), 1 / time);
}

var contentHelper = new WebInspector.TimelinePresentationModel.PopupContentHelper(WebInspector.UIString("Selected Range"));

contentHelper._appendTextRow(WebInspector.UIString("Selected range"), WebInspector.UIString("%s\u2013%s (%d frames)",
Number.secondsToString(statistics.startOffset, true), Number.secondsToString(statistics.endOffset, true), statistics.frameCount));
contentHelper._appendTextRow(WebInspector.UIString("Minimum Time"), formatTimeAndFPS(statistics.minDuration));
contentHelper._appendTextRow(WebInspector.UIString("Average Time"), formatTimeAndFPS(statistics.average));
contentHelper._appendTextRow(WebInspector.UIString("Maximum Time"), formatTimeAndFPS(statistics.maxDuration));
contentHelper._appendTextRow(WebInspector.UIString("Standard Deviation"), Number.secondsToString(statistics.stddev, true));
contentHelper._appendElementRow(WebInspector.UIString("Time by category"),
WebInspector.TimelinePresentationModel._generateAggregatedInfo(statistics.timeByCategory));

return contentHelper._contentTable;
}


WebInspector.TimelinePresentationModel.createFillStyle = function(context, width, height, color0, color1, color2)
{
var gradient = context.createLinearGradient(0, 0, width, height);
gradient.addColorStop(0, color0);
gradient.addColorStop(0.25, color1);
gradient.addColorStop(0.75, color1);
gradient.addColorStop(1, color2);
return gradient;
}


WebInspector.TimelinePresentationModel.createFillStyleForCategory = function(context, width, height, category)
{
return WebInspector.TimelinePresentationModel.createFillStyle(context, width, height, category.fillColorStop0, category.fillColorStop1, category.borderColor);
}


WebInspector.TimelinePresentationModel.createStyleRuleForCategory = function(category)
{
var selector = ".timeline-category-" + category.name + " .timeline-graph-bar, " +
".timeline-category-statusbar-item.timeline-category-" + category.name + " .timeline-category-checkbox, " +
".popover .timeline-" + category.name + ", " +
".timeline-category-" + category.name + " .timeline-tree-icon"

return selector + " { background-image: -webkit-linear-gradient(" +
category.fillColorStop0 + ", " + category.fillColorStop1 + " 25%, " + category.fillColorStop1 + " 75%, " + category.borderColor + ");" +
" border-color: " + category.borderColor +
"}";
}


WebInspector.TimelinePresentationModel.Filter = function()
{
}

WebInspector.TimelinePresentationModel.Filter.prototype = {

accept: function(record) { return false; }
}


WebInspector.TimelineCategory = function(name, title, overviewStripGroupIndex, borderColor, fillColorStop0, fillColorStop1)
{
this.name = name;
this.title = title;
this.overviewStripGroupIndex = overviewStripGroupIndex;
this.borderColor = borderColor;
this.fillColorStop0 = fillColorStop0;
this.fillColorStop1 = fillColorStop1;
this.hidden = false;
}

WebInspector.TimelineCategory.Events = {
VisibilityChanged: "VisibilityChanged"
};

WebInspector.TimelineCategory.prototype = {

get hidden()
{
return this._hidden;
},

set hidden(hidden)
{
this._hidden = hidden;
this.dispatchEventToListeners(WebInspector.TimelineCategory.Events.VisibilityChanged, this);
},

__proto__: WebInspector.Object.prototype
}
;



WebInspector.TimelineFrameController = function(model, overviewPane, presentationModel)
{
this._lastFrame = null;
this._model = model;
this._overviewPane = overviewPane;
this._presentationModel = presentationModel;
this._model.addEventListener(WebInspector.TimelineModel.Events.RecordAdded, this._onRecordAdded, this);
this._model.addEventListener(WebInspector.TimelineModel.Events.RecordsCleared, this._onRecordsCleared, this);

var records = model.records;
for (var i = 0; i < records.length; ++i)
this._addRecord(records[i]);
}

WebInspector.TimelineFrameController.prototype = {
_onRecordAdded: function(event)
{
this._addRecord(event.data);
},

_onRecordsCleared: function()
{
this._lastFrame = null;
},

_addRecord: function(record)
{
var records;
if (record.type === WebInspector.TimelineModel.RecordType.Program)
records = record["children"] || [];
else
records = [record];
records.forEach(this._innerAddRecord, this);
},

_innerAddRecord: function(record)
{
if (record.type === WebInspector.TimelineModel.RecordType.BeginFrame && this._lastFrame)
this._flushFrame(record);
else {
if (!this._lastFrame)
this._lastFrame = this._createFrame(record);
WebInspector.TimelineModel.aggregateTimeForRecord(this._lastFrame.timeByCategory, record);
this._lastFrame.cpuTime += WebInspector.TimelineModel.durationInSeconds(record);
}
},

_flushFrame: function(record)
{
this._lastFrame.endTime = WebInspector.TimelineModel.startTimeInSeconds(record);
this._lastFrame.duration = this._lastFrame.endTime - this._lastFrame.startTime;
this._overviewPane.addFrame(this._lastFrame);
this._presentationModel.addFrame(this._lastFrame);
this._lastFrame = this._createFrame(record);
},

_createFrame: function(record)
{
var frame = new WebInspector.TimelineFrame();
frame.startTime = WebInspector.TimelineModel.startTimeInSeconds(record);
frame.startTimeOffset = this._model.recordOffsetInSeconds(record);
return frame;
},

dispose: function()
{
this._model.removeEventListener(WebInspector.TimelineModel.Events.RecordAdded, this._onRecordAdded, this);
this._model.removeEventListener(WebInspector.TimelineModel.Events.RecordsCleared, this._onRecordsCleared, this);
}
}


WebInspector.FrameStatistics = function(frames)
{
this.frameCount = frames.length;
this.minDuration = Infinity;
this.maxDuration = 0;
this.timeByCategory = {};
this.startOffset = frames[0].startTimeOffset;
var lastFrame = frames[this.frameCount - 1];
this.endOffset = lastFrame.startTimeOffset + lastFrame.duration;

var totalDuration = 0;
var sumOfSquares = 0;
for (var i = 0; i < this.frameCount; ++i) {
var duration = frames[i].duration;
totalDuration += duration;
sumOfSquares += duration * duration;
this.minDuration = Math.min(this.minDuration, duration);
this.maxDuration = Math.max(this.maxDuration, duration);
WebInspector.TimelineModel.aggregateTimeByCategory(this.timeByCategory, frames[i].timeByCategory);
}
this.average = totalDuration / this.frameCount;
var variance = sumOfSquares / this.frameCount - this.average * this.average;
this.stddev = Math.sqrt(variance);
}


WebInspector.TimelineFrame = function()
{
this.timeByCategory = {};
this.cpuTime = 0;
}
;


WebInspector.TimelinePanel = function()
{
WebInspector.Panel.call(this, "timeline");
this.registerRequiredCSS("timelinePanel.css");

this._model = new WebInspector.TimelineModel();
this._presentationModel = new WebInspector.TimelinePresentationModel();

this._overviewModeSetting = WebInspector.settings.createSetting("timelineOverviewMode", WebInspector.TimelineOverviewPane.Mode.Events);
this._glueRecordsSetting = WebInspector.settings.createSetting("timelineGlueRecords", true);

this._overviewPane = new WebInspector.TimelineOverviewPane(this._model);
this._overviewPane.addEventListener(WebInspector.TimelineOverviewPane.Events.WindowChanged, this._invalidateAndScheduleRefresh.bind(this, false));
this._overviewPane.addEventListener(WebInspector.TimelineOverviewPane.Events.ModeChanged, this._overviewModeChanged, this);
this._overviewPane.show(this.element);

this.element.addEventListener("contextmenu", this._contextMenu.bind(this), false);
this.element.tabIndex = 0;

this._sidebarBackgroundElement = document.createElement("div");
this._sidebarBackgroundElement.className = "sidebar split-view-sidebar-left timeline-sidebar-background";
this.element.appendChild(this._sidebarBackgroundElement);

this.createSidebarViewWithTree();
this.element.appendChild(this.splitView.resizerElement());

this._containerElement = this.splitView.element;
this._containerElement.id = "timeline-container";
this._containerElement.addEventListener("scroll", this._onScroll.bind(this), false);

this._timelineMemorySplitter = this.element.createChild("div");
this._timelineMemorySplitter.id = "timeline-memory-splitter";
WebInspector.installDragHandle(this._timelineMemorySplitter, this._startSplitterDragging.bind(this), this._splitterDragging.bind(this), this._endSplitterDragging.bind(this), "ns-resize");
this._timelineMemorySplitter.addStyleClass("hidden");
this._memoryStatistics = new WebInspector.MemoryStatistics(this, this._model, this.splitView.sidebarWidth());
WebInspector.settings.memoryCounterGraphsHeight = WebInspector.settings.createSetting("memoryCounterGraphsHeight", 150);

var itemsTreeElement = new WebInspector.SidebarSectionTreeElement(WebInspector.UIString("RECORDS"), {}, true);
this.sidebarTree.appendChild(itemsTreeElement);

this._sidebarListElement = document.createElement("div");
this.sidebarElement.appendChild(this._sidebarListElement);

this._containerContentElement = this.splitView.mainElement;
this._containerContentElement.id = "resources-container-content";

this._timelineGrid = new WebInspector.TimelineGrid();
this._itemsGraphsElement = this._timelineGrid.itemsGraphsElement;
this._itemsGraphsElement.id = "timeline-graphs";
this._containerContentElement.appendChild(this._timelineGrid.element);
this._timelineGrid.gridHeaderElement.id = "timeline-grid-header";
this._memoryStatistics.setMainTimelineGrid(this._timelineGrid);
this.element.appendChild(this._timelineGrid.gridHeaderElement);

this._topGapElement = document.createElement("div");
this._topGapElement.className = "timeline-gap";
this._itemsGraphsElement.appendChild(this._topGapElement);

this._graphRowsElement = document.createElement("div");
this._itemsGraphsElement.appendChild(this._graphRowsElement);

this._bottomGapElement = document.createElement("div");
this._bottomGapElement.className = "timeline-gap";
this._itemsGraphsElement.appendChild(this._bottomGapElement);

this._expandElements = document.createElement("div");
this._expandElements.id = "orphan-expand-elements";
this._itemsGraphsElement.appendChild(this._expandElements);

this._calculator = new WebInspector.TimelineCalculator(this._model);
var shortRecordThresholdTitle = Number.secondsToString(WebInspector.TimelinePresentationModel.shortRecordThreshold);
this._showShortRecordsTitleText = WebInspector.UIString("Show the records that are shorter than %s", shortRecordThresholdTitle);
this._hideShortRecordsTitleText = WebInspector.UIString("Hide the records that are shorter than %s", shortRecordThresholdTitle);
this._createStatusBarItems();

this._frameMode = false;
this._boundariesAreValid = true;
this._scrollTop = 0;

this._popoverHelper = new WebInspector.PopoverHelper(this.element, this._getPopoverAnchor.bind(this), this._showPopover.bind(this));
this.element.addEventListener("mousemove", this._mouseMove.bind(this), false);
this.element.addEventListener("mouseout", this._mouseOut.bind(this), false);


this.toggleFilterButton.toggled = true;
this._showShortEvents = this.toggleFilterButton.toggled;
this._overviewPane.setShowShortEvents(this._showShortEvents);

this._timeStampRecords = [];
this._expandOffset = 15;

this._headerLineCount = 1;

this._mainThreadTasks =   ([]);
this._mainThreadMonitoringEnabled = false;
if (WebInspector.settings.showCpuOnTimelineRuler.get() && Capabilities.timelineCanMonitorMainThread)
this._enableMainThreadMonitoring();

this._createFileSelector();

this._model.addEventListener(WebInspector.TimelineModel.Events.RecordAdded, this._onTimelineEventRecorded, this);
this._model.addEventListener(WebInspector.TimelineModel.Events.RecordsCleared, this._onRecordsCleared, this);

this._registerShortcuts();

this._allRecordsCount = 0;

this._presentationModel.addFilter(new WebInspector.TimelineWindowFilter(this._overviewPane));
this._presentationModel.addFilter(new WebInspector.TimelineCategoryFilter()); 
this._presentationModel.addFilter(new WebInspector.TimelineIsLongFilter(this));
}


WebInspector.TimelinePanel.rowHeight = 18;

WebInspector.TimelinePanel.prototype = {

_startSplitterDragging: function(event)
{
this._dragOffset = this._timelineMemorySplitter.offsetTop + 2 - event.pageY;
return true;
},


_splitterDragging: function(event)
{
var top = event.pageY + this._dragOffset
this._setSplitterPosition(top);
event.preventDefault();
},


_endSplitterDragging: function(event)
{
delete this._dragOffset;
this._memoryStatistics.show();
WebInspector.settings.memoryCounterGraphsHeight.set(this.splitView.element.offsetHeight);
},

_setSplitterPosition: function(top)
{
const overviewHeight = 90;
const sectionMinHeight = 100;
top = Number.constrain(top, overviewHeight + sectionMinHeight, this.element.offsetHeight - sectionMinHeight);

this.splitView.element.style.height = (top - overviewHeight) + "px";
this._timelineMemorySplitter.style.top = (top - 2) + "px";
this._memoryStatistics.setTopPosition(top);
this._containerElementHeight = this._containerElement.clientHeight;
this.onResize();
},

get calculator()
{
return this._calculator;
},

get statusBarItems()
{
return this._statusBarButtons.select("element").concat([
this._miscStatusBarItems,
this.recordsCounter,
this.frameStatistics
]);
},

defaultFocusedElement: function()
{
return this.element;
},

_createStatusBarItems: function()
{
this._statusBarButtons = [];

this.toggleFilterButton = new WebInspector.StatusBarButton(this._hideShortRecordsTitleText, "timeline-filter-status-bar-item");
this.toggleFilterButton.addEventListener("click", this._toggleFilterButtonClicked, this);
this._statusBarButtons.push(this.toggleFilterButton);

this.toggleTimelineButton = new WebInspector.StatusBarButton(WebInspector.UIString("Record"), "record-profile-status-bar-item");
this.toggleTimelineButton.addEventListener("click", this._toggleTimelineButtonClicked, this);
this._statusBarButtons.push(this.toggleTimelineButton);

this.clearButton = new WebInspector.StatusBarButton(WebInspector.UIString("Clear"), "clear-status-bar-item");
this.clearButton.addEventListener("click", this._clearPanel, this);
this._statusBarButtons.push(this.clearButton);

this.garbageCollectButton = new WebInspector.StatusBarButton(WebInspector.UIString("Collect Garbage"), "garbage-collect-status-bar-item");
this.garbageCollectButton.addEventListener("click", this._garbageCollectButtonClicked, this);
this._statusBarButtons.push(this.garbageCollectButton);

this._glueParentButton = new WebInspector.StatusBarButton(WebInspector.UIString("Glue asynchronous events to causes"), "glue-async-status-bar-item");
this._glueParentButton.toggled = this._glueRecordsSetting.get();
this._presentationModel.setGlueRecords(this._glueParentButton.toggled);
this._glueParentButton.addEventListener("click", this._glueParentButtonClicked, this);
this._statusBarButtons.push(this._glueParentButton);

this._miscStatusBarItems = document.createElement("div");
this._miscStatusBarItems.className = "status-bar-items";

this._statusBarFilters = this._miscStatusBarItems.createChild("div");
var categories = WebInspector.TimelinePresentationModel.categories();
for (var categoryName in categories) {
var category = categories[categoryName];
if (category.overviewStripGroupIndex < 0)
continue;
this._statusBarFilters.appendChild(this._createTimelineCategoryStatusBarCheckbox(category, this._onCategoryCheckboxClicked.bind(this, category)));
}

this.recordsCounter = document.createElement("span");
this.recordsCounter.className = "timeline-records-stats";

this.frameStatistics = document.createElement("span");
this.frameStatistics.className = "timeline-records-stats hidden";
function getAnchor()
{
return this.frameStatistics;
}
this._frameStatisticsPopoverHelper = new WebInspector.PopoverHelper(this.frameStatistics, getAnchor.bind(this), this._showFrameStatistics.bind(this));
},

_createTimelineCategoryStatusBarCheckbox: function(category, onCheckboxClicked)
{
var labelContainer = document.createElement("div");
labelContainer.addStyleClass("timeline-category-statusbar-item");
labelContainer.addStyleClass("timeline-category-" + category.name);
labelContainer.addStyleClass("status-bar-item");

var label = document.createElement("label");
var checkElement = document.createElement("input");
checkElement.type = "checkbox";
checkElement.className = "timeline-category-checkbox";
checkElement.checked = true;
checkElement.addEventListener("click", onCheckboxClicked, false);
label.appendChild(checkElement);

var typeElement = document.createElement("span");
typeElement.className = "type";
typeElement.textContent = category.title;
label.appendChild(typeElement);

labelContainer.appendChild(label);
return labelContainer;
},

_onCategoryCheckboxClicked: function(category, event)
{
category.hidden = !event.target.checked;
this._invalidateAndScheduleRefresh(true);
},


_setOperationInProgress: function(indicator)
{
this._operationInProgress = !!indicator;
for (var i = 0; i < this._statusBarButtons.length; ++i)
this._statusBarButtons[i].setEnabled(!this._operationInProgress);
this._glueParentButton.setEnabled(!this._operationInProgress && !this._frameController);
this._miscStatusBarItems.removeChildren();
this._miscStatusBarItems.appendChild(indicator ? indicator.element : this._statusBarFilters);
},

_registerShortcuts: function()
{
var shortcut = WebInspector.KeyboardShortcut;
var modifiers = shortcut.Modifiers;
var section = WebInspector.shortcutsScreen.section(WebInspector.UIString("Timeline Panel"));

this._shortcuts[shortcut.makeKey("e", modifiers.CtrlOrMeta)] = this._toggleTimelineButtonClicked.bind(this);
section.addKey(shortcut.shortcutToString("e", modifiers.CtrlOrMeta), WebInspector.UIString("Start/stop recording"));

if (InspectorFrontendHost.canSave()) {
this._shortcuts[shortcut.makeKey("s", modifiers.CtrlOrMeta)] = this._saveToFile.bind(this);
section.addKey(shortcut.shortcutToString("s", modifiers.CtrlOrMeta), WebInspector.UIString("Save timeline data"));
}

this._shortcuts[shortcut.makeKey("o", modifiers.CtrlOrMeta)] = this._fileSelectorElement.click.bind(this._fileSelectorElement);
section.addKey(shortcut.shortcutToString("o", modifiers.CtrlOrMeta), WebInspector.UIString("Load timeline data"));
},

_createFileSelector: function()
{
if (this._fileSelectorElement)
this.element.removeChild(this._fileSelectorElement);

var fileSelectorElement = document.createElement("input");
fileSelectorElement.type = "file";
fileSelectorElement.style.zIndex = -1;
fileSelectorElement.style.position = "absolute";
fileSelectorElement.onchange = this._loadFromFile.bind(this);
this.element.appendChild(fileSelectorElement);
this._fileSelectorElement = fileSelectorElement;
},

_contextMenu: function(event)
{
var contextMenu = new WebInspector.ContextMenu(event);
if (InspectorFrontendHost.canSave())
contextMenu.appendItem(WebInspector.UIString("Save Timeline data\u2026"), this._saveToFile.bind(this), this._operationInProgress);
contextMenu.appendItem(WebInspector.UIString("Load Timeline data\u2026"), this._fileSelectorElement.click.bind(this._fileSelectorElement), this._operationInProgress);
contextMenu.show();
},

_saveToFile: function()
{
if (this._operationInProgress)
return;
this._model.saveToFile();
},

_loadFromFile: function()
{
var progressIndicator = this._prepareToLoadTimeline();
if (!progressIndicator)
return;
this._model.loadFromFile(this._fileSelectorElement.files[0], progressIndicator);
this._createFileSelector();
},


loadFromURL: function(url)
{
var progressIndicator = this._prepareToLoadTimeline();
if (!progressIndicator)
return;
this._model.loadFromURL(url, progressIndicator);
},


_prepareToLoadTimeline: function()
{
if (this._operationInProgress)
return null;
if (this.toggleTimelineButton.toggled) {
this.toggleTimelineButton.toggled = false;
this._model.stopRecord();
}
var progressIndicator = new WebInspector.ProgressIndicator();
progressIndicator.addEventListener(WebInspector.ProgressIndicator.Events.Done, this._setOperationInProgress.bind(this, null));
this._setOperationInProgress(progressIndicator);
return progressIndicator;
},

_rootRecord: function()
{
return this._presentationModel.rootRecord();
},

_updateRecordsCounter: function(recordsInWindowCount)
{
this.recordsCounter.textContent = WebInspector.UIString("%d of %d records shown", recordsInWindowCount, this._allRecordsCount);
},

_updateFrameStatistics: function(frames)
{
if (frames.length) {
this._lastFrameStatistics = new WebInspector.FrameStatistics(frames);
var details = WebInspector.UIString("avg: %s, \u03c3: %s",
Number.secondsToString(this._lastFrameStatistics.average, true), Number.secondsToString(this._lastFrameStatistics.stddev, true));
} else
this._lastFrameStatistics = null;
this.frameStatistics.textContent = WebInspector.UIString("%d of %d frames shown", frames.length, this._presentationModel.frames().length);
if (details) {
this.frameStatistics.appendChild(document.createTextNode(" ("));
this.frameStatistics.createChild("span", "timeline-frames-stats").textContent = details;
this.frameStatistics.appendChild(document.createTextNode(")"));
}
},


_showFrameStatistics: function(anchor, popover)
{
popover.show(WebInspector.TimelinePresentationModel.generatePopupContentForFrameStatistics(this._lastFrameStatistics), anchor);
},

_updateEventDividers: function()
{
this._timelineGrid.removeEventDividers();
var clientWidth = this._graphRowsElementWidth;
var dividers = [];

for (var i = 0; i < this._timeStampRecords.length; ++i) {
var record = this._timeStampRecords[i];
var positions = this._calculator.computeBarGraphWindowPosition(record);
var dividerPosition = Math.round(positions.left);
if (dividerPosition < 0 || dividerPosition >= clientWidth || dividers[dividerPosition])
continue;
var divider = WebInspector.TimelinePresentationModel.createEventDivider(record.type, record.title);
divider.style.left = dividerPosition + "px";
dividers[dividerPosition] = divider;
}
this._timelineGrid.addEventDividers(dividers);
},

_updateFrameBars: function(frames)
{
var clientWidth = this._graphRowsElementWidth;
if (this._frameContainer)
this._frameContainer.removeChildren();
else {
const frameContainerBorderWidth = 1;
this._frameContainer = document.createElement("div");
this._frameContainer.addStyleClass("fill");
this._frameContainer.addStyleClass("timeline-frame-container");
this._frameContainer.style.height = this._headerLineCount * WebInspector.TimelinePanel.rowHeight + frameContainerBorderWidth + "px";
this._frameContainer.addEventListener("dblclick", this._onFrameDoubleClicked.bind(this), false);
}

var dividers = [ this._frameContainer ];

for (var i = 0; i < frames.length; ++i) {
var frame = frames[i];
var frameStart = this._calculator.computePosition(frame.startTime);
var frameEnd = this._calculator.computePosition(frame.endTime);

var frameStrip = document.createElement("div");
frameStrip.className = "timeline-frame-strip";
var actualStart = Math.max(frameStart, 0);
var width = frameEnd - actualStart;
frameStrip.style.left = actualStart + "px";
frameStrip.style.width = width + "px";
frameStrip._frame = frame;

const minWidthForFrameInfo = 60;
if (width > minWidthForFrameInfo)
frameStrip.textContent = Number.secondsToString(frame.endTime - frame.startTime, true);

this._frameContainer.appendChild(frameStrip);

if (actualStart > 0) {
var frameMarker = WebInspector.TimelinePresentationModel.createEventDivider(WebInspector.TimelineModel.RecordType.BeginFrame);
frameMarker.style.left = frameStart + "px";
dividers.push(frameMarker);
}
}
this._timelineGrid.addEventDividers(dividers);
},

_onFrameDoubleClicked: function(event)
{
var frameBar = event.target.enclosingNodeOrSelfWithClass("timeline-frame-strip");
if (!frameBar)
return;
this._overviewPane.zoomToFrame(frameBar._frame);
},

_overviewModeChanged: function(event)
{
var mode = event.data;
var shouldShowMemory = mode === WebInspector.TimelineOverviewPane.Mode.Memory;
var frameMode = mode === WebInspector.TimelineOverviewPane.Mode.Frames;
this._overviewModeSetting.set(mode);
if (frameMode !== this._frameMode) {
this._frameMode = frameMode;
this._glueParentButton.setEnabled(!frameMode);
this._presentationModel.setGlueRecords(this._glueParentButton.toggled && !frameMode);
this._repopulateRecords();

if (frameMode) {
this.element.addStyleClass("timeline-frame-overview");
this.recordsCounter.addStyleClass("hidden");
this.frameStatistics.removeStyleClass("hidden");
this._frameController = new WebInspector.TimelineFrameController(this._model, this._overviewPane, this._presentationModel);
} else {
this._frameController.dispose();
this._frameController = null;
this.element.removeStyleClass("timeline-frame-overview");
this.recordsCounter.removeStyleClass("hidden");
this.frameStatistics.addStyleClass("hidden");
}
}
if (shouldShowMemory === this._memoryStatistics.visible())
return;
if (!shouldShowMemory) {
this._timelineMemorySplitter.addStyleClass("hidden");
this._memoryStatistics.hide();
this.splitView.element.style.height = "auto";
this.splitView.element.style.bottom = "0";
this.onResize();
} else {
this._timelineMemorySplitter.removeStyleClass("hidden");
this._memoryStatistics.show();
this.splitView.element.style.bottom = "auto";
this._setSplitterPosition(WebInspector.settings.memoryCounterGraphsHeight.get());
}
},

_toggleTimelineButtonClicked: function()
{
if (this._operationInProgress)
return;
if (this.toggleTimelineButton.toggled)
this._model.stopRecord();
else {
this._model.startRecord();
WebInspector.userMetrics.TimelineStarted.record();
}
this.toggleTimelineButton.toggled = !this.toggleTimelineButton.toggled;
},

_toggleFilterButtonClicked: function()
{
this.toggleFilterButton.toggled = !this.toggleFilterButton.toggled;
this._showShortEvents = this.toggleFilterButton.toggled;
this._overviewPane.setShowShortEvents(this._showShortEvents);
this.toggleFilterButton.element.title = this._showShortEvents ? this._hideShortRecordsTitleText : this._showShortRecordsTitleText;
this._invalidateAndScheduleRefresh(true);
},

_garbageCollectButtonClicked: function()
{
ProfilerAgent.collectGarbage();
},

_glueParentButtonClicked: function()
{
var newValue = !this._glueParentButton.toggled;
this._glueParentButton.toggled = newValue;
this._presentationModel.setGlueRecords(newValue);
this._glueRecordsSetting.set(newValue);
this._repopulateRecords();
},

_repopulateRecords: function()
{
this._resetPanel();
this._automaticallySizeWindow = false;
var records = this._model.records;
for (var i = 0; i < records.length; ++i)
this._innerAddRecordToTimeline(records[i]);
this._invalidateAndScheduleRefresh(false);
},

_onTimelineEventRecorded: function(event)
{
if (this._innerAddRecordToTimeline(event.data))
this._invalidateAndScheduleRefresh(false);
},

_innerAddRecordToTimeline: function(record)
{
if (record.type === WebInspector.TimelineModel.RecordType.Program) {
this._mainThreadTasks.push({
startTime: WebInspector.TimelineModel.startTimeInSeconds(record),
endTime: WebInspector.TimelineModel.endTimeInSeconds(record)
});
}

var records = this._presentationModel.addRecord(record);
this._allRecordsCount += records.length;
var timeStampRecords = this._timeStampRecords;
var hasVisibleRecords = false;
var presentationModel = this._presentationModel;
function processRecord(record)
{
if (WebInspector.TimelinePresentationModel.isEventDivider(record))
timeStampRecords.push(record);
hasVisibleRecords |= presentationModel.isVisible(record);
}
WebInspector.TimelinePresentationModel.forAllRecords(records, processRecord);

function isAdoptedRecord(record)
{
return record.parent !== presentationModel.rootRecord;
}

return hasVisibleRecords || records.some(isAdoptedRecord);
},

sidebarResized: function(event)
{
var width = event.data;
this._sidebarBackgroundElement.style.width = width + "px";
this.onResize();
this._overviewPane.sidebarResized(width);
this._memoryStatistics.setSidebarWidth(width);
this._timelineGrid.gridHeaderElement.style.left = width + "px";
},

onResize: function()
{
this._closeRecordDetails();
this._scheduleRefresh(false);
this._graphRowsElementWidth = this._graphRowsElement.offsetWidth;
this._timelineGrid.gridHeaderElement.style.width = this._itemsGraphsElement.offsetWidth + "px";
this._containerElementHeight = this._containerElement.clientHeight;
var minFloatingStatusBarItemsOffset = document.getElementById("panel-status-bar").totalOffsetLeft() + this._statusBarButtons.length * WebInspector.StatusBarButton.width;
this._miscStatusBarItems.style.left = Math.max(minFloatingStatusBarItemsOffset, this.splitView.sidebarWidth()) + "px";
},

_clearPanel: function()
{
this._model.reset();
},

_onRecordsCleared: function()
{
this._resetPanel();
this._invalidateAndScheduleRefresh(true);
},

_resetPanel: function()
{
this._presentationModel.reset();
this._timeStampRecords = [];
this._boundariesAreValid = false;
this._adjustScrollPosition(0);
this._closeRecordDetails();
this._allRecordsCount = 0;
this._automaticallySizeWindow = true;
this._mainThreadTasks = [];
},

elementsToRestoreScrollPositionsFor: function()
{
return [this._containerElement];
},

wasShown: function()
{
WebInspector.Panel.prototype.wasShown.call(this);
if (!WebInspector.TimelinePanel._categoryStylesInitialized) {
WebInspector.TimelinePanel._categoryStylesInitialized = true;
this._injectCategoryStyles();
}
this._overviewPane.setMode(this._overviewModeSetting.get());
this._refresh();
},

willHide: function()
{
this._closeRecordDetails();
WebInspector.Panel.prototype.willHide.call(this);
},

_onScroll: function(event)
{
this._closeRecordDetails();
this._scrollTop = this._containerElement.scrollTop;
var dividersTop = Math.max(0, this._scrollTop);
this._timelineGrid.setScrollAndDividerTop(this._scrollTop, dividersTop);
this._scheduleRefresh(true);
},

_invalidateAndScheduleRefresh: function(preserveBoundaries)
{
this._presentationModel.invalidateFilteredRecords();
delete this._searchResults;
this._scheduleRefresh(preserveBoundaries);
},


_scheduleRefresh: function(preserveBoundaries)
{
this._closeRecordDetails();
this._boundariesAreValid &= preserveBoundaries;

if (!this.isShowing())
return;

if (preserveBoundaries)
this._refresh();
else {
if (!this._refreshTimeout)
this._refreshTimeout = setTimeout(this._refresh.bind(this), 300);
}
},

_refresh: function()
{
if (this._refreshTimeout) {
clearTimeout(this._refreshTimeout);
delete this._refreshTimeout;
}

this._timelinePaddingLeft = !this._overviewPane.windowLeft() ? this._expandOffset : 0;
this._calculator.setWindow(this._overviewPane.windowStartTime(), this._overviewPane.windowEndTime());
this._calculator.setDisplayWindow(this._timelinePaddingLeft, this._graphRowsElementWidth);

var recordsInWindowCount = this._refreshRecords();
this._updateRecordsCounter(recordsInWindowCount);
if (!this._boundariesAreValid) {
this._updateEventDividers();
var frames = this._frameController && this._presentationModel.filteredFrames(this._overviewPane.windowStartTime(), this._overviewPane.windowEndTime());
if (frames) {
this._updateFrameStatistics(frames);
const maxFramesForFrameBars = 30;
if  (frames.length && frames.length < maxFramesForFrameBars) {
this._timelineGrid.removeDividers();
this._updateFrameBars(frames);
} else
this._timelineGrid.updateDividers(this._calculator);
} else
this._timelineGrid.updateDividers(this._calculator);
if (this._mainThreadMonitoringEnabled)
this._refreshMainThreadBars();
}
if (this._memoryStatistics.visible())
this._memoryStatistics.refresh();
this._boundariesAreValid = true;
},

revealRecordAt: function(time)
{
var recordToReveal;
function findRecordToReveal(record)
{
if (record.containsTime(time)) {
recordToReveal = record;
return true;
}

if (!recordToReveal || record.endTime < time && recordToReveal.endTime < record.endTime)
recordToReveal = record;
return false;
}
WebInspector.TimelinePresentationModel.forAllRecords(this._presentationModel.rootRecord().children, null, findRecordToReveal);


if (!recordToReveal) {
this._containerElement.scrollTop = 0;
return;
}

this._revealRecord(recordToReveal);
},

_revealRecord: function(recordToReveal)
{

var treeUpdated = false;
for (var parent = recordToReveal.parent; parent !== this._rootRecord(); parent = parent.parent) {
treeUpdated = treeUpdated || parent.collapsed;
parent.collapsed = false;
}
if (treeUpdated)
this._invalidateAndScheduleRefresh(true);

var recordsInWindow = this._presentationModel.filteredRecords();
var index = recordsInWindow.indexOf(recordToReveal);
this._containerElement.scrollTop = index * WebInspector.TimelinePanel.rowHeight;
},

_refreshRecords: function()
{
var recordsInWindow = this._presentationModel.filteredRecords();


var visibleTop = this._scrollTop;
var visibleBottom = visibleTop + this._containerElementHeight;

const rowHeight = WebInspector.TimelinePanel.rowHeight;


var startIndex = Math.max(0, Math.min(Math.floor(visibleTop / rowHeight) - this._headerLineCount, recordsInWindow.length - 1));
var endIndex = Math.min(recordsInWindow.length, Math.ceil(visibleBottom / rowHeight));
var lastVisibleLine = Math.max(0, Math.floor(visibleBottom / rowHeight) - this._headerLineCount);
if (this._automaticallySizeWindow && recordsInWindow.length > lastVisibleLine) {
this._automaticallySizeWindow = false;

var windowStartTime = startIndex ? recordsInWindow[startIndex].startTime : this._model.minimumRecordTime();
this._overviewPane.setWindowTimes(windowStartTime, recordsInWindow[Math.max(0, lastVisibleLine - 1)].endTime);
recordsInWindow = this._presentationModel.filteredRecords();
endIndex = Math.min(recordsInWindow.length, lastVisibleLine);
}


const top = (startIndex * rowHeight) + "px";
this._topGapElement.style.height = top;
this.sidebarElement.style.top = top;
this._bottomGapElement.style.height = (recordsInWindow.length - endIndex) * rowHeight + "px";


var listRowElement = this._sidebarListElement.firstChild;
var width = this._graphRowsElementWidth;
this._itemsGraphsElement.removeChild(this._graphRowsElement);
var graphRowElement = this._graphRowsElement.firstChild;
var scheduleRefreshCallback = this._invalidateAndScheduleRefresh.bind(this, true);
this._itemsGraphsElement.removeChild(this._expandElements);
this._expandElements.removeChildren();

for (var i = 0; i < endIndex; ++i) {
var record = recordsInWindow[i];
var isEven = !(i % 2);

if (i < startIndex) {
var lastChildIndex = i + record.visibleChildrenCount;
if (lastChildIndex >= startIndex && lastChildIndex < endIndex) {
var expandElement = new WebInspector.TimelineExpandableElement(this._expandElements);
var positions = this._calculator.computeBarGraphWindowPosition(record);
expandElement._update(record, i, positions.left - this._expandOffset, positions.width);
}
} else {
if (!listRowElement) {
listRowElement = new WebInspector.TimelineRecordListRow().element;
this._sidebarListElement.appendChild(listRowElement);
}
if (!graphRowElement) {
graphRowElement = new WebInspector.TimelineRecordGraphRow(this._itemsGraphsElement, scheduleRefreshCallback).element;
this._graphRowsElement.appendChild(graphRowElement);
}

listRowElement.row.update(record, isEven, visibleTop);
graphRowElement.row.update(record, isEven, this._calculator, this._expandOffset, i);

listRowElement = listRowElement.nextSibling;
graphRowElement = graphRowElement.nextSibling;
}
}


while (listRowElement) {
var nextElement = listRowElement.nextSibling;
listRowElement.row.dispose();
listRowElement = nextElement;
}
while (graphRowElement) {
var nextElement = graphRowElement.nextSibling;
graphRowElement.row.dispose();
graphRowElement = nextElement;
}

this._itemsGraphsElement.insertBefore(this._graphRowsElement, this._bottomGapElement);
this._itemsGraphsElement.appendChild(this._expandElements);
this._adjustScrollPosition((recordsInWindow.length + this._headerLineCount) * rowHeight);
this._updateSearchHighlight(false);

return recordsInWindow.length;
},

_refreshMainThreadBars: function()
{
const barOffset = 3;
const minGap = 3;

var minWidth = WebInspector.TimelineCalculator._minWidth;
var widthAdjustment = minWidth / 2;

var width = this._graphRowsElementWidth;
var boundarySpan = this._overviewPane.windowEndTime() - this._overviewPane.windowStartTime();
var scale = boundarySpan / (width - minWidth - this._timelinePaddingLeft);
var startTime = this._overviewPane.windowStartTime() - this._timelinePaddingLeft * scale;
var endTime = startTime + width * scale;

var tasks = this._mainThreadTasks;

function compareEndTime(value, task)
{
return value < task.endTime ? -1 : 1;
}

var taskIndex = insertionIndexForObjectInListSortedByFunction(startTime, tasks, compareEndTime);

var container = this._cpuBarsElement;
var element = container.firstChild;
var lastElement;
var lastLeft;
var lastRight;

while (taskIndex < tasks.length) {
var task = tasks[taskIndex];
if (task.startTime > endTime)
break;
taskIndex++;

var left = Math.max(0, this._calculator.computePosition(task.startTime) + barOffset - widthAdjustment);
var right = Math.min(width, this._calculator.computePosition(task.endTime) + barOffset + widthAdjustment);

if (lastElement) {
var gap = Math.floor(left) - Math.ceil(lastRight);
if (gap < minGap) {
lastRight = right;
lastElement._tasksInfo.lastTaskIndex = taskIndex;
continue;
}
lastElement.style.width = (lastRight - lastLeft) + "px";
}

if (!element)
element = container.createChild("div", "timeline-graph-bar");

element.style.left = left + "px";
element._tasksInfo = {tasks: tasks, firstTaskIndex: taskIndex, lastTaskIndex: taskIndex};
lastLeft = left;
lastRight = right;

lastElement = element;
element = element.nextSibling;
}

if (lastElement)
lastElement.style.width = (lastRight - lastLeft) + "px";

while (element) {
var nextElement = element.nextSibling;
element._tasksInfo = null;
container.removeChild(element);
element = nextElement;
}
},

_enableMainThreadMonitoring: function()
{
var container = this._timelineGrid.gridHeaderElement;
this._cpuBarsElement = container.createChild("div", "timeline-cpu-bars");

const headerBorderWidth = 1;
const headerMargin = 2;

var headerHeight = this._headerLineCount * WebInspector.TimelinePanel.rowHeight;
this.sidebarElement.firstChild.style.height = headerHeight + "px";
this._timelineGrid.dividersLabelBarElement.style.height = headerHeight + headerMargin + "px";
this._itemsGraphsElement.style.top = headerHeight + headerBorderWidth + "px";

this._mainThreadMonitoringEnabled = true;
},

_adjustScrollPosition: function(totalHeight)
{

if ((this._scrollTop + this._containerElementHeight) > totalHeight + 1)
this._containerElement.scrollTop = (totalHeight - this._containerElement.offsetHeight);
},

_getPopoverAnchor: function(element)
{
return element.enclosingNodeOrSelfWithClass("timeline-graph-bar") ||
element.enclosingNodeOrSelfWithClass("timeline-tree-item") ||
element.enclosingNodeOrSelfWithClass("timeline-frame-strip");
},

_mouseOut: function(e)
{
this._hideRectHighlight();
},

_mouseMove: function(e)
{
var anchor = this._getPopoverAnchor(e.target);

const recordType = WebInspector.TimelineModel.RecordType;
if (anchor && anchor.row && (anchor.row._record.type === recordType.Paint || anchor.row._record.type === recordType.Layout))
this._highlightRect(anchor.row._record);
else
this._hideRectHighlight();
},

_highlightRect: function(record)
{
if (this._highlightedRect === record.data)
return;
this._highlightedRect = record.data;
DOMAgent.highlightRect(this._highlightedRect.x, this._highlightedRect.y, this._highlightedRect.width, this._highlightedRect.height, WebInspector.Color.PageHighlight.Content.toProtocolRGBA(), WebInspector.Color.PageHighlight.ContentOutline.toProtocolRGBA());
},

_hideRectHighlight: function()
{
if (this._highlightedRect) {
delete this._highlightedRect;
DOMAgent.hideHighlight();
}
},


_showPopover: function(anchor, popover)
{
if (anchor.hasStyleClass("timeline-frame-strip")) {
var frame = anchor._frame;
popover.show(WebInspector.TimelinePresentationModel.generatePopupContentForFrame(frame), anchor);
} else {
if (anchor.row && anchor.row._record)
anchor.row._record.generatePopupContent(showCallback);
else if (anchor._tasksInfo)
popover.show(this._presentationModel.generateMainThreadBarPopupContent(anchor._tasksInfo), anchor);
}

function showCallback(popupContent)
{
popover.show(popupContent, anchor);
}
},

_closeRecordDetails: function()
{
this._popoverHelper.hidePopover();
},

_injectCategoryStyles: function()
{
var style = document.createElement("style");
var categories = WebInspector.TimelinePresentationModel.categories();

style.textContent = Object.values(categories).map(WebInspector.TimelinePresentationModel.createStyleRuleForCategory).join("\n");
document.head.appendChild(style);
},

jumpToNextSearchResult: function()
{
this._jumpToAdjacentRecord(1);
},

jumpToPreviousSearchResult: function()
{
this._jumpToAdjacentRecord(-1);
},

_jumpToAdjacentRecord: function(offset)
{
if (!this._searchResults || !this._searchResults.length || !this._selectedSearchResult)
return;
var index = this._searchResults.indexOf(this._selectedSearchResult);
index = (index + offset + this._searchResults.length) % this._searchResults.length;
this._selectSearchResult(index);
this._highlightSelectedSearchResult(true);
},

_selectSearchResult: function(index)
{
this._selectedSearchResult = this._searchResults[index];
WebInspector.searchController.updateCurrentMatchIndex(index, this);
},

_highlightSelectedSearchResult: function(revealRecord)
{
this._clearHighlight();
if (this._searchFilter)
return;

var record = this._selectedSearchResult;
if (!record)
return;

for (var element = this._sidebarListElement.firstChild; element; element = element.nextSibling) {
if (element.row._record === record) {
element.row.highlight(this._searchRegExp, this._highlightDomChanges);
return;
}
}

if (revealRecord)
this._revealRecord(record);
},

_clearHighlight: function()
{
if (this._highlightDomChanges)
WebInspector.revertDomChanges(this._highlightDomChanges);
this._highlightDomChanges = [];
},


_updateSearchHighlight: function(revealRecord)
{
if (this._searchFilter || !this._searchRegExp) {
this._clearHighlight();
return;
}

if (!this._searchResults)
this._updateSearchResults();

this._highlightSelectedSearchResult(revealRecord);
},

_updateSearchResults: function() {
var searchRegExp = this._searchRegExp;
if (!searchRegExp)
return;

var matches = [];
var presentationModel = this._presentationModel;

function processRecord(record)
{
if (presentationModel.isVisible(record) && WebInspector.TimelineRecordListRow.testContentMatching(record, searchRegExp))
matches.push(record);
return false;
}
WebInspector.TimelinePresentationModel.forAllRecords(presentationModel.rootRecord().children, processRecord);

var matchesCount = matches.length;
if (matchesCount) {
this._searchResults = matches;
WebInspector.searchController.updateSearchMatchesCount(matchesCount, this);

var selectedIndex = matches.indexOf(this._selectedSearchResult);
if (selectedIndex === -1)
selectedIndex = 0;
this._selectSearchResult(selectedIndex);
} else {
WebInspector.searchController.updateSearchMatchesCount(0, this);
delete this._selectedSearchResult;
}
},

searchCanceled: function()
{
this._clearHighlight();
delete this._searchResults;
delete this._selectedSearchResult;
delete this._searchRegExp;
},


canFilter: function()
{
return true;
},

performFilter: function(searchQuery)
{
this._presentationModel.removeFilter(this._searchFilter);
delete this._searchFilter;
this.searchCanceled();
if (searchQuery) {
this._searchFilter = new WebInspector.TimelineSearchFilter(createPlainTextSearchRegex(searchQuery, "i"));
this._presentationModel.addFilter(this._searchFilter);
}
this._invalidateAndScheduleRefresh(true);
},

performSearch: function(searchQuery)
{
this._searchRegExp = createPlainTextSearchRegex(searchQuery, "i");
delete this._searchResults;
this._updateSearchHighlight(true);
},

__proto__: WebInspector.Panel.prototype
}


WebInspector.TimelineCalculator = function(model)
{
this._model = model;
}

WebInspector.TimelineCalculator._minWidth = 5;

WebInspector.TimelineCalculator.prototype = {

computePosition: function(time)
{
return (time - this._minimumBoundary) / this.boundarySpan() * this._workingArea + this.paddingLeft;
},

computeBarGraphPercentages: function(record)
{
var start = (record.startTime - this._minimumBoundary) / this.boundarySpan() * 100;
var end = (record.startTime + record.selfTime - this._minimumBoundary) / this.boundarySpan() * 100;
var endWithChildren = (record.lastChildEndTime - this._minimumBoundary) / this.boundarySpan() * 100;
var cpuWidth = record.cpuTime / this.boundarySpan() * 100;
return {start: start, end: end, endWithChildren: endWithChildren, cpuWidth: cpuWidth};
},

computeBarGraphWindowPosition: function(record)
{
var percentages = this.computeBarGraphPercentages(record);
var widthAdjustment = 0;

var left = this.computePosition(record.startTime);
var width = (percentages.end - percentages.start) / 100 * this._workingArea;
if (width < WebInspector.TimelineCalculator._minWidth) {
widthAdjustment = WebInspector.TimelineCalculator._minWidth - width;
left -= widthAdjustment / 2;
width += widthAdjustment;
}
var widthWithChildren = (percentages.endWithChildren - percentages.start) / 100 * this._workingArea + widthAdjustment;
var cpuWidth = percentages.cpuWidth / 100 * this._workingArea + widthAdjustment;
if (percentages.endWithChildren > percentages.end)
widthWithChildren += widthAdjustment;
return {left: left, width: width, widthWithChildren: widthWithChildren, cpuWidth: cpuWidth};
},

setWindow: function(minimumBoundary, maximumBoundary)
{
this._minimumBoundary = minimumBoundary;
this._maximumBoundary = maximumBoundary;
},


setDisplayWindow: function(paddingLeft, clientWidth)
{
this._workingArea = clientWidth - WebInspector.TimelineCalculator._minWidth - paddingLeft;
this.paddingLeft = paddingLeft;
},

formatTime: function(value)
{
return Number.secondsToString(value + this._minimumBoundary - this._model.minimumRecordTime());
},

maximumBoundary: function()
{
return this._maximumBoundary;
},

minimumBoundary: function()
{
return this._minimumBoundary;
},

boundarySpan: function()
{
return this._maximumBoundary - this._minimumBoundary;
}
}


WebInspector.TimelineRecordListRow = function()
{
this.element = document.createElement("div");
this.element.row = this;
this.element.style.cursor = "pointer";
var iconElement = document.createElement("span");
iconElement.className = "timeline-tree-icon";
this.element.appendChild(iconElement);

this._typeElement = document.createElement("span");
this._typeElement.className = "type";
this.element.appendChild(this._typeElement);

var separatorElement = document.createElement("span");
separatorElement.className = "separator";
separatorElement.textContent = " ";

this._dataElement = document.createElement("span");
this._dataElement.className = "data dimmed";

this.element.appendChild(separatorElement);
this.element.appendChild(this._dataElement);
}

WebInspector.TimelineRecordListRow.prototype = {
update: function(record, isEven, offset)
{
this._record = record;
this._offset = offset;

this.element.className = "timeline-tree-item timeline-category-" + record.category.name;
if (isEven)
this.element.addStyleClass("even");
if (record.hasWarning)
this.element.addStyleClass("warning");
else if (record.childHasWarning)
this.element.addStyleClass("child-warning");

this._typeElement.textContent = record.title;

if (this._dataElement.firstChild)
this._dataElement.removeChildren();
var details = record.details();
if (details) {
var detailsContainer = document.createElement("span");
if (typeof details === "object") {
detailsContainer.appendChild(document.createTextNode("("));
detailsContainer.appendChild(details);
detailsContainer.appendChild(document.createTextNode(")"));
} else
detailsContainer.textContent = "(" + details + ")";
this._dataElement.appendChild(detailsContainer);
}
},

highlight: function(regExp, domChanges)
{
var matchInfo = this.element.textContent.match(regExp);
if (matchInfo)
WebInspector.highlightSearchResult(this.element, matchInfo.index, matchInfo[0].length, domChanges);
},

dispose: function()
{
this.element.parentElement.removeChild(this.element);
}
}


WebInspector.TimelineRecordListRow.testContentMatching = function(record, regExp)
{
return regExp.test(record.title + " (" + record.details() + ")");
}


WebInspector.TimelineRecordGraphRow = function(graphContainer, scheduleRefresh)
{
this.element = document.createElement("div");
this.element.row = this;

this._barAreaElement = document.createElement("div");
this._barAreaElement.className = "timeline-graph-bar-area";
this.element.appendChild(this._barAreaElement);

this._barWithChildrenElement = document.createElement("div");
this._barWithChildrenElement.className = "timeline-graph-bar with-children";
this._barWithChildrenElement.row = this;
this._barAreaElement.appendChild(this._barWithChildrenElement);

this._barCpuElement = document.createElement("div");
this._barCpuElement.className = "timeline-graph-bar cpu"
this._barCpuElement.row = this;
this._barAreaElement.appendChild(this._barCpuElement);

this._barElement = document.createElement("div");
this._barElement.className = "timeline-graph-bar";
this._barElement.row = this;
this._barAreaElement.appendChild(this._barElement);

this._expandElement = new WebInspector.TimelineExpandableElement(graphContainer);
this._expandElement._element.addEventListener("click", this._onClick.bind(this));

this._scheduleRefresh = scheduleRefresh;
}

WebInspector.TimelineRecordGraphRow.prototype = {
update: function(record, isEven, calculator, expandOffset, index)
{
this._record = record;
this.element.className = "timeline-graph-side timeline-category-" + record.category.name + (isEven ? " even" : "");
var barPosition = calculator.computeBarGraphWindowPosition(record);
this._barWithChildrenElement.style.left = barPosition.left + "px";
this._barWithChildrenElement.style.width = barPosition.widthWithChildren + "px";
this._barElement.style.left = barPosition.left + "px";
this._barElement.style.width = barPosition.width + "px";
this._barCpuElement.style.left = barPosition.left + "px";
this._barCpuElement.style.width = barPosition.cpuWidth + "px";
this._expandElement._update(record, index, barPosition.left - expandOffset, barPosition.width);
},

_onClick: function(event)
{
this._record.collapsed = !this._record.collapsed;
this._scheduleRefresh(false);
},

dispose: function()
{
this.element.parentElement.removeChild(this.element);
this._expandElement._dispose();
}
}


WebInspector.TimelineExpandableElement = function(container)
{
this._element = document.createElement("div");
this._element.className = "timeline-expandable";

var leftBorder = document.createElement("div");
leftBorder.className = "timeline-expandable-left";
this._element.appendChild(leftBorder);

container.appendChild(this._element);
}

WebInspector.TimelineExpandableElement.prototype = {
_update: function(record, index, left, width)
{
const rowHeight = WebInspector.TimelinePanel.rowHeight;
if (record.visibleChildrenCount || record.invisibleChildrenCount) {
this._element.style.top = index * rowHeight + "px";
this._element.style.left = left + "px";
this._element.style.width = Math.max(12, width + 25) + "px";
if (!record.collapsed) {
this._element.style.height = (record.visibleChildrenCount + 1) * rowHeight + "px";
this._element.addStyleClass("timeline-expandable-expanded");
this._element.removeStyleClass("timeline-expandable-collapsed");
} else {
this._element.style.height = rowHeight + "px";
this._element.addStyleClass("timeline-expandable-collapsed");
this._element.removeStyleClass("timeline-expandable-expanded");
}
this._element.removeStyleClass("hidden");
} else
this._element.addStyleClass("hidden");
},

_dispose: function()
{
this._element.parentElement.removeChild(this._element);
}
}


WebInspector.TimelineCategoryFilter = function()
{
}

WebInspector.TimelineCategoryFilter.prototype = {

accept: function(record)
{
return !record.category.hidden && record.type !== WebInspector.TimelineModel.RecordType.BeginFrame;
}
}


WebInspector.TimelineIsLongFilter = function(panel)
{
this._panel = panel;
}

WebInspector.TimelineIsLongFilter.prototype = {

accept: function(record)
{
return this._panel._showShortEvents || record.isLong();
}
}


WebInspector.TimelineSearchFilter = function(regExp)
{
this._regExp = regExp;
}

WebInspector.TimelineSearchFilter.prototype = {


accept: function(record)
{
return WebInspector.TimelineRecordListRow.testContentMatching(record, this._regExp);
}
}
