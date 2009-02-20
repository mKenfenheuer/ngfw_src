
var PageName = 'Protocol Control - Summary';
var PageId = 'pa33eb7dd913b4b3ebc65de09da8e04a4'
document.title = 'Protocol Control - Summary';

if (top.location != self.location)
{
	if (parent.HandleMainFrameChanged) {
		parent.HandleMainFrameChanged();
	}
}

var $OnLoadVariable = '';

var $CSUM;

var hasQuery = false;
var query = window.location.hash.substring(1);
if (query.length > 0) hasQuery = true;
var vars = query.split("&");
for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (pair[0].length > 0) eval("$" + pair[0] + " = decodeURI(pair[1]);");
} 

if (hasQuery && $CSUM != 1) {
alert('Prototype Warning: Variable values were truncated.');
}

function GetQuerystring() {
    return encodeURI('#OnLoadVariable=' + $OnLoadVariable + '&CSUM=1');
}

function PopulateVariables(value) {
  value = value.replace(/\[\[OnLoadVariable\]\]/g, $OnLoadVariable);
  value = value.replace(/\[\[PageName\]\]/g, PageName);
  return value;
}

function OnLoad() {

}

var u16 = document.getElementById('u16');

u16.style.cursor = 'pointer';
if (bIE) u16.attachEvent("onclick", Clicku16);
else u16.addEventListener("click", Clicku16, true);
function Clicku16(e)
{

if (true) {

	self.location.href="Protocol Control - User Summary.html" + GetQuerystring();

}

}
gv_vAlignTable['u16'] = 'top';
var u7 = document.getElementById('u7');

u7.style.cursor = 'pointer';
if (bIE) u7.attachEvent("onclick", Clicku7);
else u7.addEventListener("click", Clicku7, true);
function Clicku7(e)
{

if (true) {

	self.location.href="Protocol Control - Host Summary.html" + GetQuerystring();

}

}
gv_vAlignTable['u7'] = 'top';
var u15 = document.getElementById('u15');
gv_vAlignTable['u15'] = 'top';
var u2 = document.getElementById('u2');

var u19 = document.getElementById('u19');
gv_vAlignTable['u19'] = 'top';
var u13 = document.getElementById('u13');

u13.style.cursor = 'pointer';
if (bIE) u13.attachEvent("onclick", Clicku13);
else u13.addEventListener("click", Clicku13, true);
function Clicku13(e)
{

if (true) {

	self.location.href="Protocol Control - User Summary.html" + GetQuerystring();

}

}
gv_vAlignTable['u13'] = 'top';
var u22 = document.getElementById('u22');
gv_vAlignTable['u22'] = 'top';
var u12 = document.getElementById('u12');

var u5 = document.getElementById('u5');

var u8 = document.getElementById('u8');

u8.style.cursor = 'pointer';
if (bIE) u8.attachEvent("onclick", Clicku8);
else u8.addEventListener("click", Clicku8, true);
function Clicku8(e)
{

if (true) {

	self.location.href="Protocol Control - Host Summary.html" + GetQuerystring();

}

}
gv_vAlignTable['u8'] = 'top';
var u10 = document.getElementById('u10');
gv_vAlignTable['u10'] = 'top';
var u0 = document.getElementById('u0');
gv_vAlignTable['u0'] = 'top';
var u21 = document.getElementById('u21');
gv_vAlignTable['u21'] = 'top';
var u17 = document.getElementById('u17');
gv_vAlignTable['u17'] = 'top';
var u3 = document.getElementById('u3');

var u14 = document.getElementById('u14');
gv_vAlignTable['u14'] = 'top';
var u6 = document.getElementById('u6');

var u9 = document.getElementById('u9');
gv_vAlignTable['u9'] = 'top';
var u20 = document.getElementById('u20');
gv_vAlignTable['u20'] = 'top';
var u1 = document.getElementById('u1');

u1.style.cursor = 'pointer';
if (bIE) u1.attachEvent("onclick", Clicku1);
else u1.addEventListener("click", Clicku1, true);
function Clicku1(e)
{

if (true) {

	self.location.href="Protocol Control - Global Incident.html" + GetQuerystring();

}

}
gv_vAlignTable['u1'] = 'top';
var u11 = document.getElementById('u11');

var u18 = document.getElementById('u18');
gv_vAlignTable['u18'] = 'top';
var u4 = document.getElementById('u4');

if (window.OnLoad) OnLoad();
