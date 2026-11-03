package com.example.web

/**
 * The single-page browser UI (Arabic-first, English when the browser isn't Arabic; light & dark).
 * Kept free of `$` so it can live in a raw string; texts are in the page's own dictionary because
 * they are rendered by the desktop browser, not by the app.
 */
internal object WebPage {
    val HTML = """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark">
<title>شارك أبك</title>
<style>
:root{--bg:#eef1f8;--surface:#fff;--line:rgba(15,23,42,.09);--txt:#0f172a;--mute:#5b6577;--pri:#4338ca;--ok:#047857;--err:#b91c1c;--chip:rgba(67,56,202,.08)}
@media (prefers-color-scheme:dark){:root{--bg:#070d1d;--surface:#111c38;--line:rgba(255,255,255,.08);--txt:#eef2ff;--mute:#9aa6c8;--pri:#a5b4fc;--ok:#34d399;--err:#f87171;--chip:rgba(165,180,252,.1)}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--txt);font-family:"IBM Plex Sans Arabic",system-ui,-apple-system,"Segoe UI",Tahoma,sans-serif;line-height:1.55;min-height:100vh;
background-image:radial-gradient(560px 380px at 95% -8%,rgba(29,58,138,.3),transparent 62%),radial-gradient(480px 380px at -12% 40%,rgba(91,33,182,.18),transparent 62%)}
.wrap{max-width:880px;margin:0 auto;padding:24px 16px 48px}
header{display:flex;align-items:center;gap:14px;margin-bottom:20px}
.brand{width:52px;height:52px;flex:none;border-radius:17px;padding:11px;background:linear-gradient(135deg,#38bdf8,#4f46e5 55%,#a78bfa);box-shadow:0 10px 24px -10px #4f46e5}
h1{margin:0;font-size:1.35rem}
.sub{margin:0;color:var(--mute);font-size:.88rem}
.card{background:var(--surface);border:1px solid var(--line);border-radius:22px;padding:18px}
.hidden{display:none!important}
.pin{max-width:380px;margin:40px auto;text-align:center}
.pin input{width:100%;font-size:2rem;letter-spacing:.6em;text-align:center;padding:12px;border-radius:16px;border:1px solid var(--line);background:transparent;color:var(--txt);direction:ltr;margin:14px 0}
.btn{display:inline-flex;align-items:center;justify-content:center;gap:6px;border:0;cursor:pointer;text-decoration:none;color:#fff;font:inherit;font-weight:600;padding:11px 18px;border-radius:14px;background:linear-gradient(135deg,#6d72f3,#4338ca)}
.btn.wide{width:100%}
.btn.ghost{background:var(--chip);color:var(--pri)}
.err{color:var(--err);font-size:.88rem;min-height:1.3em}
.tabs{display:flex;gap:8px;margin-bottom:14px}
.tab{flex:1;padding:11px;border-radius:14px;border:1px solid var(--line);background:var(--surface);color:var(--mute);font:inherit;font-weight:600;cursor:pointer}
.tab.on{background:linear-gradient(135deg,#6d72f3,#4338ca);color:#fff;border-color:transparent}
.search{width:100%;padding:11px 14px;border-radius:14px;border:1px solid var(--line);background:var(--surface);color:var(--txt);font:inherit;margin-bottom:12px}
.chips{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:12px}
.chip{padding:6px 12px;border-radius:12px;border:0;background:var(--chip);color:var(--pri);font:inherit;font-size:.85rem;cursor:pointer}
.chip.on{background:var(--pri);color:var(--surface)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:10px}
.item{display:flex;align-items:center;gap:12px;background:var(--surface);border:1px solid var(--line);border-radius:18px;padding:12px}
.ic{width:42px;height:42px;flex:none;border-radius:13px;display:grid;place-items:center;color:#fff;font-weight:700;background:linear-gradient(145deg,#818cf8,#4338ca);overflow:hidden}
.ic img{width:100%;height:100%;object-fit:cover}
.ic.file{background:linear-gradient(145deg,#38bdf8,#0369a1)}
.info{flex:1;min-width:0}
.info b{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-weight:600}
.info span{display:block;color:var(--mute);font-size:.8rem;direction:ltr;unicode-bidi:isolate;text-align:start;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.dl{flex:none;width:40px;height:40px;border-radius:12px;display:grid;place-items:center;background:var(--chip);color:var(--pri);text-decoration:none;font-size:1.15rem}
.empty{color:var(--mute);text-align:center;padding:30px}
.drop{border:2px dashed var(--line);border-radius:22px;padding:34px 16px;text-align:center;color:var(--mute);cursor:pointer;transition:.15s}
.drop.over{border-color:var(--pri);background:var(--chip)}
.drop b{display:block;color:var(--txt);font-size:1.05rem;margin-bottom:4px}
.up{margin-top:12px;display:flex;flex-direction:column;gap:8px}
.uprow{background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:10px 12px;font-size:.88rem}
.uprow .top{display:flex;justify-content:space-between;gap:8px}
.uprow .name{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.bar{height:6px;border-radius:4px;background:var(--chip);margin-top:7px;overflow:hidden}
.bar i{display:block;height:100%;width:0;background:linear-gradient(90deg,#38bdf8,#4f46e5)}
.ok{color:var(--ok)} .bad{color:var(--err)}
textarea{width:100%;min-height:90px;padding:12px;border-radius:14px;border:1px solid var(--line);background:transparent;color:var(--txt);font:inherit;margin:8px 0}
h3{margin:18px 0 6px;font-size:1rem}
footer{margin-top:26px;text-align:center;color:var(--mute);font-size:.78rem}
</style>
</head>
<body>
<div class="wrap">
  <header>
    <div class="brand"><svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 10.5 12 6.5l8 4v8L12 22.5l-8-4z"/><path d="M4 10.5l8 4 8-4M12 14.5v8"/><path d="M12 11.5V1.8M8.6 5.2 12 1.8l3.5 3.4"/></svg></div>
    <div><h1 data-t="title"></h1><p class="sub" data-t="sub"></p></div>
  </header>

  <section id="login" class="card pin hidden">
    <b data-t="pinTitle"></b>
    <p class="sub" data-t="pinHint"></p>
    <input id="pin" inputmode="numeric" maxlength="4" autocomplete="one-time-code" autofocus>
    <div class="err" id="pinErr"></div>
    <button class="btn wide" id="pinBtn" data-t="enter"></button>
  </section>

  <section id="main" class="hidden">
    <div class="tabs">
      <button class="tab on" data-tab="from" data-t="tabFrom"></button>
      <button class="tab" data-tab="to" data-t="tabTo"></button>
    </div>

    <div id="from">
      <input class="search" id="q" data-tp="search">
      <div class="chips">
        <button class="chip on" data-k="all" data-t="kAll"></button>
        <button class="chip" data-k="app" data-t="kApps"></button>
        <button class="chip" data-k="received_app" data-t="kReceived"></button>
        <button class="chip" data-k="file" data-t="kFiles"></button>
      </div>
      <div class="grid" id="list"></div>
      <div class="empty hidden" id="empty" data-t="empty"></div>
    </div>

    <div id="to" class="hidden">
      <div class="drop" id="drop"><b data-t="dropTitle"></b><span data-t="dropHint"></span></div>
      <input type="file" id="files" multiple class="hidden">
      <div class="up" id="up"></div>
      <h3 data-t="textTitle"></h3>
      <textarea id="txt" data-tp="textHint"></textarea>
      <button class="btn" id="txtBtn" data-t="sendText"></button>
      <span class="sub" id="txtMsg"></span>
    </div>
  </section>

  <footer data-t="footer"></footer>
</div>
<script>
var AR = {title:"شارك أبك",sub:"نقل الملفات بين هذا الحاسوب والهاتف عبر الشبكة المحلية",pinTitle:"أدخل الرمز",pinHint:"الرمز المكوّن من 4 أرقام يظهر على شاشة الهاتف",enter:"دخول",
 pinBad:"الرمز غير صحيح. بعد 5 محاولات خاطئة يتغيّر الرمز على الهاتف.",tabFrom:"من الهاتف",tabTo:"إلى الهاتف",search:"ابحث…",kAll:"الكل",kApps:"التطبيقات المثبتة",kReceived:"المستلمة",kFiles:"الملفات",
 empty:"لا شيء هنا",dropTitle:"اسحب الملفات إلى هنا",dropHint:"أو انقر للاختيار. التطبيقات تُحفظ في «شارك أبك» والباقي في Download/ShareAPK",
 textTitle:"إرسال نص",textHint:"اكتب أو الصق نصاً ليظهر على الهاتف",sendText:"إرسال",sent:"وصل ✓",failed:"فشل",waiting:"بالانتظار",download:"تنزيل",
 footer:"شارك أبك · الصفحة تعمل ما دامت نافذة «من المتصفح» مفتوحة على الهاتف",lost:"انتهت الجلسة، أدخل الرمز من جديد"};
var EN = {title:"ShareAPK",sub:"Move files between this computer and the phone over the local network",pinTitle:"Enter the code",pinHint:"The 4-digit code is shown on the phone",enter:"Enter",
 pinBad:"Wrong code. After 5 wrong tries the phone shows a new one.",tabFrom:"From phone",tabTo:"To phone",search:"Search…",kAll:"All",kApps:"Installed apps",kReceived:"Received",kFiles:"Files",
 empty:"Nothing here",dropTitle:"Drop files here",dropHint:"or click to choose. Apps go to ShareAPK, everything else to Download/ShareAPK",
 textTitle:"Send text",textHint:"Type or paste text to show on the phone",sendText:"Send",sent:"Delivered ✓",failed:"Failed",waiting:"Waiting",download:"Download",
 footer:"ShareAPK · this page works while “From browser” is open on the phone",lost:"Session ended, enter the code again"};
var ar = (navigator.language || "").toLowerCase().indexOf("ar") === 0;
var T = ar ? AR : EN;
if (!ar) { document.documentElement.lang = "en"; document.documentElement.dir = "ltr"; document.title = EN.title; }
document.querySelectorAll("[data-t]").forEach(function(e){ e.textContent = T[e.getAttribute("data-t")]; });
document.querySelectorAll("[data-tp]").forEach(function(e){ e.placeholder = T[e.getAttribute("data-tp")]; });
function el(id){ return document.getElementById(id); }
function fmt(n){ if (n <= 0) return ""; var u=["B","KB","MB","GB"], i=0; while (n >= 1024 && i < 3) { n/=1024; i++; } return (i ? n.toFixed(1) : n) + " " + u[i]; }
function show(view){ el("login").classList.toggle("hidden", view !== "login"); el("main").classList.toggle("hidden", view !== "main"); if (view === "login") el("pin").focus(); }

function login(){
  el("pinErr").textContent = "";
  fetch("/api/login", {method:"POST", body:el("pin").value}).then(function(r){
    if (r.ok) { load(); } else { el("pinErr").textContent = T.pinBad; el("pin").select(); }
  });
}
el("pinBtn").onclick = login;
el("pin").addEventListener("keydown", function(e){ if (e.key === "Enter") login(); });
el("pin").addEventListener("input", function(){ if (el("pin").value.length === 4) login(); });

var items = [], kind = "all";
function load(){
  fetch("/api/items").then(function(r){
    if (r.status === 401) { show("login"); return null; }
    return r.json();
  }).then(function(data){ if (!data) return; items = data; show("main"); render(); });
}
function render(){
  var q = el("q").value.trim().toLowerCase(), list = el("list");
  list.innerHTML = "";
  var shown = items.filter(function(it){ return (kind === "all" || it.kind === kind) && (!q || (it.name + " " + it.subtitle).toLowerCase().indexOf(q) >= 0); });
  shown.forEach(function(it){
    var row = document.createElement("div"); row.className = "item";
    var ic = document.createElement("div"); ic.className = "ic" + (it.kind === "file" ? " file" : "");
    if (it.icon) { var img = document.createElement("img"); img.loading = "lazy"; img.src = "/icon/" + encodeURIComponent(it.id); ic.appendChild(img); }
    else ic.textContent = (it.name.trim()[0] || "?").toUpperCase();
    var info = document.createElement("div"); info.className = "info";
    var b = document.createElement("b"); b.textContent = it.name;
    var s = document.createElement("span"); s.textContent = [it.subtitle, fmt(it.size)].filter(Boolean).join(" · ");
    info.appendChild(b); info.appendChild(s);
    var a = document.createElement("a"); a.className = "dl"; a.href = "/dl/" + encodeURIComponent(it.id); a.title = T.download; a.textContent = "↓";
    row.appendChild(ic); row.appendChild(info); row.appendChild(a); list.appendChild(row);
  });
  el("empty").classList.toggle("hidden", shown.length > 0);
}
el("q").addEventListener("input", render);
document.querySelectorAll(".chip").forEach(function(c){ c.onclick = function(){
  document.querySelectorAll(".chip").forEach(function(x){ x.classList.remove("on"); }); c.classList.add("on"); kind = c.getAttribute("data-k"); render(); }; });
document.querySelectorAll(".tab").forEach(function(t){ t.onclick = function(){
  document.querySelectorAll(".tab").forEach(function(x){ x.classList.toggle("on", x === t); });
  var to = t.getAttribute("data-tab") === "to"; el("from").classList.toggle("hidden", to); el("to").classList.toggle("hidden", !to);
  if (!to) load(); }; });

var queue = [], busy = false;
function addFiles(list){
  for (var i = 0; i < list.length; i++) {
    var f = list[i], row = document.createElement("div"); row.className = "uprow";
    row.innerHTML = '<div class="top"><span class="name"></span><span class="st"></span></div><div class="bar"><i></i></div>';
    row.querySelector(".name").textContent = f.name; row.querySelector(".st").textContent = T.waiting;
    el("up").appendChild(row); queue.push({file:f, row:row});
  }
  next();
}
function next(){
  if (busy || !queue.length) return;
  busy = true;
  var job = queue.shift(), xhr = new XMLHttpRequest(), st = job.row.querySelector(".st"), bar = job.row.querySelector(".bar i");
  xhr.open("POST", "/api/upload?name=" + encodeURIComponent(job.file.name));
  xhr.upload.onprogress = function(e){ if (e.lengthComputable) { var p = Math.round(e.loaded * 100 / e.total); bar.style.width = p + "%"; st.textContent = p + "%"; } };
  xhr.onload = function(){
    if (xhr.status === 401) { st.textContent = T.lost; st.className = "st bad"; show("login"); }
    else if (xhr.status < 300) { bar.style.width = "100%"; st.textContent = T.sent; st.className = "st ok"; }
    else { st.textContent = T.failed; st.className = "st bad"; }
    busy = false; next();
  };
  xhr.onerror = function(){ st.textContent = T.failed; st.className = "st bad"; busy = false; next(); };
  xhr.send(job.file);
}
var drop = el("drop");
drop.onclick = function(){ el("files").click(); };
el("files").onchange = function(){ addFiles(el("files").files); el("files").value = ""; };
["dragenter","dragover"].forEach(function(n){ drop.addEventListener(n, function(e){ e.preventDefault(); drop.classList.add("over"); }); });
["dragleave","drop"].forEach(function(n){ drop.addEventListener(n, function(e){ e.preventDefault(); drop.classList.remove("over"); }); });
drop.addEventListener("drop", function(e){ if (e.dataTransfer && e.dataTransfer.files) addFiles(e.dataTransfer.files); });
el("txtBtn").onclick = function(){
  var v = el("txt").value; if (!v.trim()) return;
  fetch("/api/text", {method:"POST", headers:{"Content-Type":"text/plain;charset=utf-8"}, body:v}).then(function(r){
    if (r.status === 401) { show("login"); return; }
    el("txtMsg").textContent = r.ok ? T.sent : T.failed; if (r.ok) el("txt").value = "";
  });
};
load();
</script>
</body>
</html>
""".trimIndent()
}
