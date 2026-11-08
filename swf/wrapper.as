stop();
createEmptyMovieClip("movie",1);
movie._lockroot = true;
movie.loadMovie("duo.swf");
function coopInfo(){return movie.coopInfo();}
function coopStart(seed){return movie.coopStart(seed);}
function coopInput(a,b){return movie.coopInput(a,b);}
function coopState(){return movie.coopState();}
function coopDebug(action,p,n){return movie.coopDebug(action,p,n);}
function coopSnapshot(){return movie.coopSnapshot();}
function coopBootReady(){return movie.coopBootReady();}
function coopSetQuality(q){return movie.coopSetQuality(q);}
function coopView(){return movie.coopView();}
function coopScanReset(){return movie.coopScanReset();}
function coopChecksumSlice(frame){return movie.coopChecksumSlice(frame);}
flash.external.ExternalInterface.addCallback("coopInfo",this,coopInfo);
flash.external.ExternalInterface.addCallback("coopStart",this,coopStart);
flash.external.ExternalInterface.addCallback("coopInput",this,coopInput);
flash.external.ExternalInterface.addCallback("coopState",this,coopState);
flash.external.ExternalInterface.addCallback("coopDebug",this,coopDebug);
flash.external.ExternalInterface.addCallback("coopSnapshot",this,coopSnapshot);
flash.external.ExternalInterface.addCallback("coopBootReady",this,coopBootReady);
flash.external.ExternalInterface.addCallback("coopSetQuality",this,coopSetQuality);
flash.external.ExternalInterface.addCallback("coopView",this,coopView);
flash.external.ExternalInterface.addCallback("coopScanReset",this,coopScanReset);
flash.external.ExternalInterface.addCallback("coopChecksumSlice",this,coopChecksumSlice);
