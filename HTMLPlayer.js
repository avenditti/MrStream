var vid = document.getElementById("video");
function pauseVid() {
	vid.pause();
}
function playVid() {
	vid.play();
}
function getTime() {
	alert("time:" + vid.currentTime);
}
function seekTo(time) {
	vid.currentTime = time;
}
vid.addEventListener("waiting", function() {
	alert("buffering");
	pauseVid();
});
vid.addEventListener("canplaythrough", function() {
	pauseVid();
	alert("canPlayThrough");	
});
function setVolume(vol) {
	video.volume = vol * .001;
}
