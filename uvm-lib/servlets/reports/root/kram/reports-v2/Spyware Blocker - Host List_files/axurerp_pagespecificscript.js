
var PageName = 'Spyware Blocker - Host List';
var PageId = 'p511d1852b3f54afc9259e9ec9e89fcfc'
document.title = 'Spyware Blocker - Host List';

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

var u2 = document.getElementById('u2');
gv_vAlignTable['u2'] = 'top';
var u1 = document.getElementById('u1');

u1.style.cursor = 'pointer';
if (bIE) u1.attachEvent("onclick", Clicku1);
else u1.addEventListener("click", Clicku1, true);
function Clicku1(e)
{

if (true) {

	self.location.href="Phish Blocker - Host Summary.html" + GetQuerystring();

}

}
gv_vAlignTable['u1'] = 'top';
var u6 = document.getElementById('u6');
gv_vAlignTable['u6'] = 'top';
var u0 = document.getElementById('u0');
gv_vAlignTable['u0'] = 'top';
var u5 = document.getElementById('u5');
gv_vAlignTable['u5'] = 'top';
var u4 = document.getElementById('u4');
gv_vAlignTable['u4'] = 'top';
var u3 = document.getElementById('u3');
gv_vAlignTable['u3'] = 'top';
if (window.OnLoad) OnLoad();
