var wsPath = "ws://localhost:8080/rvspush";

/*
 * HTM标记等DECODE
 */
function decodeText(text) {
	if (text == null) {
		return "";
	}
	return ((text.replace(/<(.+?)>/gi,"&lt;$1&gt;")).replace(/ /gi,"&nbsp;")).replace(/\n/gi,"<br>");
};

/*
 * HTM标记等ENCODE
 */
function encodeText(text) {
	if (text == null) {
		return "";
	}
	return ((text.replace(/&lt;(.+?)&gt;/gi,"<$1>")).replace(/&nbsp;/gi," ").replace(/<br>/gi,"\n"));
};

/*
 * URLSummary取得
 */
function getURLSummary() {
    var url = window.location.href;
    return URL_RE.test(url) ? url.replace(/.*#(.*)/, "$1") : "";
}


function blink(ele, cls, times) {
	var i = 0, t = false, c = "", times = times || 2;
	if (t)
		return;

	t = setInterval(function() {
		i++;
		if (i % 2) {
			ele.addClass(cls);
		} else {
			ele.removeClass(cls);
		}
		if (i == 2 * times) {
			clearInterval(t);
			ele.removeClass(cls);
		}
	}, 200);

	ele.bind("mouseover blur", function() {
		clearInterval(t);
		ele.removeClass(cls);
	});
};

var griddata_update = function(gridlist, keycellname, keyvalue, cellname, updatevalue, multirecords){
	//得到显示到界面的id集合
	var gridlistsize = gridlist.length;
	if (gridlistsize === 0) return;

	for (var igridlist = 0; igridlist < gridlistsize; igridlist++) {
		var gridvalue = gridlist[igridlist];
		if (gridvalue[keycellname] == keyvalue) {
			gridvalue[cellname] = updatevalue;
			if(!multirecords) return;
		}
	}
}

var griddata_remove = function(gridlist, keycellname, keyvalue, multirecords){
	//得到显示到界面的id集合
	var gridlistsize = gridlist.length;
	if (gridlistsize === 0) return;

	for (var igridlist = gridlistsize - 1; igridlist >= 0; igridlist--) {
		var gridvalue = gridlist[igridlist];
		if (gridvalue[keycellname] == keyvalue) {
			gridlist.splice(igridlist, 1);
			if (!multirecords) return;
		}
	}
}
/* 数组查询原型方法追加 TODO */
//Array.prototype.indexOf=function(substr,start){
// var ta,rt,d='\0';
// if(start!=null){ta=this.slice(start);rt=start;}else{ta=this;rt=0;}
// var str=d+ta.join(d)+d,t=str.indexOf(d+substr+d);
// if(t==-1)return -1;rt+=str.slice(0,t).replace(/[^\0]/g,'').length;
// return rt;
//}

function Json_to_String(O) {

var S = [];
var J = "";
if (Object.prototype.toString.apply(O) === '[object Array]') {
for (var i = 0; i < O.length; i++)
S.push(Json_to_String(O[i]));
J = '[' + S.join(',') + ']';
}
else if (Object.prototype.toString.apply(O) === '[object Date]') {
J = "new Date(" + O.getTime() + ")";
}
else if (Object.prototype.toString.apply(O) === '[object RegExp]' || Object.prototype.toString.apply(O) === '[object Function]') {
J = O.toString();
}
else if (Object.prototype.toString.apply(O) === '[object Object]') {
for (var i in O) {
var tO = typeof (O[i]) == 'string' ? '"' + O[i] + '"' : (typeof (O[i]) === 'object' ? Json_to_String(O[i]) : O[i]);
S.push("\"" + i + "\":" + tO);
}
J = '{' + S.join(',') + '}';
}

return J;
};

function loadJs(path, callback) {
	var _doc = document.getElementsByTagName('head')[0];
	var _scr = document.createElement('script');
	_scr.setAttribute('type', 'text/javascript');
	_scr.setAttribute('src', path);

	if (callback) {
		_scr.onload = callback;
		_scr.onreadystatechange = handleReadyStateChange;
//		_scr.onerror = handleError;
	}
	_doc.appendChild(_scr);

	function handleReadyStateChange() {
		var state;

		state = _scr.readyState;
		if (state === 'loaded' || state === "complete") {
			callback();
		}
	}
}

function loadCss(path, callback) {
	var _doc = document.getElementsByTagName('head')[0];
	var _scr = document.createElement('link');
	_scr.setAttribute('rel', 'stylesheet');
	_scr.setAttribute('type', 'text/css');
	_scr.setAttribute('href', path);

	_doc.appendChild(_scr);

	if (callback) {
		callback();
	}
}