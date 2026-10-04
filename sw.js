/* 그림책 미술키트: 인터넷이 없어도 열리도록 파일을 기기에 보관해요 */
const CACHE="artkit-v1";
const CORE=["./","./index.html","./manifest.webmanifest","./icons/icon-192.png","./icons/icon-512.png"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(CORE)).then(()=>self.skipWaiting()))});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener("fetch",e=>{
  const req=e.request;if(req.method!=="GET")return;
  const url=new URL(req.url);
  const keep=res=>{if(res&&(res.ok||res.type==="opaque")){const cp=res.clone();caches.open(CACHE).then(c=>c.put(req,cp))}return res};
  /* 앱 파일: 새 버전을 먼저 받고, 인터넷이 없으면 보관본 */
  if(url.origin===self.location.origin){
    e.respondWith(fetch(req).then(keep).catch(()=>caches.match(req).then(m=>m||caches.match("./index.html"))));return;
  }
  /* 글꼴·파일 만들기 도구: 보관본 먼저 */
  if(/(^|\.)cdnjs\.cloudflare\.com$|^fonts\.(googleapis|gstatic)\.com$/.test(url.hostname)){
    e.respondWith(caches.match(req).then(m=>m||fetch(req).then(keep)));
  }
});
