const $=(s,p=document)=>p.querySelector(s);
const $$=(s,p=document)=>[...p.querySelectorAll(s)];
const layers=[
  {name:"STATIONERY",depth:0.58,color:"#d95036",note:"Three objects. Four shadows. The arithmetic has already become unreliable.",threshold:"The envelope is addressed to the inside of this drawer.",items:[
    {shape:"letter",name:"Letter to the interior",type:"UNDELIVERABLE CORRESPONDENCE",description:"The address reads: beneath the ordinary things. The postmark is dated three Thursdays from now.",action:"TURN IT OVER",result:"The reverse side is a floor plan of the drawer you are standing in."},
    {shape:"key",name:"Key without a lock",type:"MILDLY IMPATIENT HARDWARE",description:"Warm to the touch. It vibrates whenever somebody nearby says there is probably a reasonable explanation.",action:"TRY THE KEY",result:"Every lock in the room sighs, but none of them open."},
    {shape:"clock",name:"Borrowed minute",type:"TEMPORAL OFFICE SUPPLY",description:"A minute removed from 3:17 p.m. It has been sitting here so long that it is beginning to feel like an hour.",action:"SPEND THE MINUTE",result:"For sixty seconds, nothing is late."}]},
  {name:"WEATHER",depth:4.2,color:"#97cfd2",note:"Local weather has formed beneath the pencils. Forecast: increasingly indoors.",threshold:"The rain is falling upward toward another handle.",items:[
    {shape:"cloud",name:"Private weather",type:"DESKTOP CLIMATE",description:"A small overcast system that follows unfinished paperwork. It rains only on sentences containing the word “final.”",action:"ASK FOR SUN",result:"The cloud considers this feedback and produces one polite sunbeam."},
    {shape:"window",name:"Window to Tuesday",type:"MISFILED VIEW",description:"The view is unmistakably Tuesday, although today has repeatedly denied being Tuesday.",action:"OPEN THE WINDOW",result:"A breeze enters carrying the smell of printer paper and distant applause."},
    {shape:"fish",name:"Air fish",type:"UNLICENSED FAUNA",description:"It swims through the room’s least certain air. It appears to be late for a meeting in the ceiling.",action:"OFFER DIRECTIONS",result:"The fish nods, turns left, and disappears behind the word “ceiling.”"}]},
  {name:"NIGHT SHIFT",depth:19.7,color:"#9b82b5",note:"The drawer now has its own moon. Human Resources has not approved a night shift.",threshold:"A door has appeared in the moonlight. It is drawer-shaped.",items:[
    {shape:"moon",name:"Office moon",type:"UNAUTHORIZED SATELLITE",description:"Installed without a work order. Its phases correspond to the amount of coffee remaining in a cup upstairs.",action:"CHANGE THE PHASE",result:"The moon becomes full. Somewhere above, an empty mug becomes mysterious."},
    {shape:"door",name:"Door marked smaller",type:"ACCESS POINT / DIMINISHING",description:"The sign says ‘employees must become 12% smaller before entering.’ The policy appears to be self-enforcing.",action:"KNOCK THREE TIMES",result:"Something exactly your size knocks twice from the other side."},
    {shape:"planet",name:"Spare planet",type:"EXECUTIVE PAPERWEIGHT",description:"Mostly ocean, one tiny filing cabinet, no known meetings. Its orbit passes beneath the desk every nine minutes.",action:"SPIN THE PLANET",result:"A tiny civilization invents the sticky note, celebrates, and immediately loses one."}]},
  {name:"THE BOTTOM",depth:Infinity,color:"#edc84e",note:"The bottom has been located. It is looking back up.",threshold:"No further drawer is visible. This has never stopped it before.",items:[]}
];

let current=-1,moved=new Set(),sound=true,audio,drag=null;
const intro=$("#intro"),deskZone=$("#deskZone"),shell=$("#drawerShell"),contents=$("#contents"),threshold=$("#threshold"),deeper=$("#deeperButton");

function tone(freq=220,duration=.12,type="sine"){
  if(!sound)return;
  audio=audio||new (window.AudioContext||window.webkitAudioContext)();
  const o=audio.createOscillator(),g=audio.createGain();o.type=type;o.frequency.setValueAtTime(freq,audio.currentTime);g.gain.setValueAtTime(.035,audio.currentTime);g.gain.exponentialRampToValueAtTime(.001,audio.currentTime+duration);o.connect(g).connect(audio.destination);o.start();o.stop(audio.currentTime+duration);
}
function buildMap(){
  $("#depthMap").innerHTML=layers.map((l,i)=>`<li data-layer="${i}"><span>${String(i+1).padStart(2,"0")} ${l.name}</span><i></i></li>`).join("");
}
function setNote(text,index=current+1){
  $("#noteNumber").textContent=`NOTE ${String(index).padStart(2,"0")}`;$("#noteText").textContent=text;
  const note=$("#fieldNote");note.animate([{transform:"rotate(-1deg) translateY(8px)",opacity:.3},{transform:"rotate(-1deg)",opacity:1}],{duration:350});
}
function updateMap(){
  $$("#depthMap li").forEach((li,i)=>{li.classList.toggle("active",i===current);li.classList.toggle("visited",i<current)});
}
function openExperience(){
  tone(110,.5,"triangle");intro.style.opacity=0;intro.style.transform="translateY(-30px)";
  setTimeout(()=>{intro.remove();deskZone.classList.add("visible");nextLayer()},700);
}
function nextLayer(){
  current++;
  if(current>=layers.length-1){showEnding();return}
  moved.clear();threshold.classList.remove("ready");shell.classList.remove("open");
  const layer=layers[current];
  setTimeout(()=>{
    document.documentElement.style.setProperty("--layer-color",layer.color);
    $("#drawerLabel").textContent=`DRAWER ${String(current+1).padStart(2,"0")} / ${layer.name}`;
    $("#drawerLabel").nextElementSibling.textContent=current===0?"depth under review":`${layer.depth} metres below desk`;
    $("#depthValue").textContent=`${layer.depth.toFixed(2)} m`;
    setNote(layer.note);renderObjects(layer);updateMap();shell.classList.add("open");tone(95-current*8,.65,"sawtooth");
  },420);
}
function renderObjects(layer){
  contents.innerHTML="";
  const positions=[[7,10],[41,42],[68,8]];
  layer.items.forEach((item,i)=>{
    const button=document.createElement("button");button.className="object";button.style.left=positions[i][0]+"%";button.style.top=positions[i][1]+"%";button.dataset.index=i;
    button.innerHTML=`<span class="shape ${item.shape}"></span><span class="object-label">${item.name}</span>`;
    button.addEventListener("pointerdown",startDrag);button.addEventListener("click",()=>{if(button.dataset.pointerInspected){delete button.dataset.pointerInspected;return}inspectArtifact(item,i)});contents.append(button);
  });
}
function startDrag(e){
  const el=e.currentTarget,box=contents.getBoundingClientRect(),r=el.getBoundingClientRect();drag={el,box,dx:e.clientX-r.left,dy:e.clientY-r.top,startX:e.clientX,startY:e.clientY};el.setPointerCapture(e.pointerId);el.addEventListener("pointermove",moveDrag);el.addEventListener("pointerup",endDrag,{once:true});tone(260,.05);
}
function moveDrag(e){
  if(!drag)return;drag.el.style.left=Math.max(0,Math.min(drag.box.width-drag.el.offsetWidth,e.clientX-drag.box.left-drag.dx))+"px";drag.el.style.top=Math.max(0,Math.min(drag.box.height-drag.el.offsetHeight,e.clientY-drag.box.top-drag.dy))+"px";
}
function endDrag(e){
  const d=drag;if(!d)return;d.el.removeEventListener("pointermove",moveDrag);drag=null;
  const distance=Math.hypot(e.clientX-d.startX,e.clientY-d.startY);
  if(distance<7){d.el.dataset.pointerInspected="1";inspectArtifact(layers[current].items[+d.el.dataset.index],+d.el.dataset.index);return}
  moved.add(d.el.dataset.index);tone(180,.08,"triangle");
  if(moved.size>=layers[current].items.length){$("#thresholdText").textContent=layers[current].threshold;threshold.classList.add("ready");setNote("All objects displaced. The drawer has revealed a second opinion about down.");}
}
function inspectArtifact(item,index){
  if(!item)return;tone(330,.16,"triangle");
  $("#dialogIndex").textContent=`CATALOGUE ${String(current+1).padStart(2,"0")}.${String(index+1).padStart(2,"0")}`;
  $("#artifactStage").style.background=layers[current].color;$("#artifactStage").innerHTML=`<span class="shape ${item.shape}"></span>`;
  $("#artifactType").textContent=item.type;$("#artifactTitle").textContent=item.name;$("#artifactDescription").textContent=item.description;
  const action=$("#artifactAction");action.textContent=item.action;action.onclick=()=>{action.textContent=item.result;action.disabled=true;tone(440,.3,"triangle");};
  $("#artifactDialog").showModal();
}
function showEnding(){
  shell.classList.remove("open");setTimeout(()=>{const ending=$("#ending");ending.classList.add("visible");ending.setAttribute("aria-hidden","false");tone(55,1.4,"sine")},550);
}
function restart(){location.reload()}

$("#openButton").addEventListener("click",openExperience);
$("#drawerHandle").addEventListener("click",()=>{if(current<0)openExperience();else shell.classList.toggle("open")});
deeper.addEventListener("click",nextLayer);
$("#dialogClose").addEventListener("click",()=>$("#artifactDialog").close());
$("#artifactDialog").addEventListener("click",e=>{if(e.target===$("#artifactDialog"))$("#artifactDialog").close()});
$("#restartButton").addEventListener("click",restart);
$("#soundToggle").addEventListener("click",e=>{sound=!sound;e.currentTarget.textContent=sound?"SOUND ON":"SOUND OFF";e.currentTarget.setAttribute("aria-pressed",String(sound));if(sound)tone(300,.1)});
buildMap();
