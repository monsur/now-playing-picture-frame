const serverUri = "ws://" + window.location.host + "/server";
var socket;
var i = 0;

var getTrackElement = function (data) {
  var elem = $("<div />", { class: "spotifyContent" });
  elem.css("right", "0px");

  var left = $("<div />", { class: "spotifyLeft" });
  var title = $("<div />", { class: "songTitle" }).text(data.name);
  var artist = $("<div />", { class: "artist" }).text(data.artists[0]);
  var album = $("<div />", { class: "album" }).text(data.album);
  left.append(title, artist, album);

  var right = $("<div />", { class: "spotifyRight" });
  var albumArt = $("<img />", { class: "albumArt", src: data.image });
  right.append(albumArt);

  elem.append(left, right);
  return elem;
};

var onMessage = function (data) {
  var backgroundImage =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

  var elem = null;
  if (data.productUrl) {
    if (data.mediaMetadata.hasOwnProperty("photo")) {
      elem = $("<img />", { src: data.baseUrl + "=w2048-h1024" });
    } else if (data.mediaMetadata.hasOwnProperty("video")) {
      elem = $("<video />").attr("autoplay", "true").attr("muted", "muted").attr("src", data.baseUrl + "=dv");
      elem[0].volume = 0;
    }
    elem.css("position", "absolute");

    width = window.innerWidth;
    height = window.innerHeight;
    if (width > height) {
      width =
        (window.innerHeight * data.mediaMetadata.width) /
        data.mediaMetadata.height;
    } else {
      height =
        (window.innerWidth * data.mediaMetadata.height) /
        data.mediaMetadata.width;
    }
    elem.height(height).width(width);
    elem.css("left", (window.innerWidth - width) / 2);
    elem.css("top", (window.innerHeight - height) / 2);
  } else {
    elem = getTrackElement(data);
    backgroundImage = data.image;
  }
  elem.css("position", "absolute");
  elem.css("z-index", i * -1);
  elem.hide();

  document.body.style.backgroundImage = "url(" + backgroundImage + ")";

  var active = $("#items").find(":first-child");
  if (active.length === 0) {
    $("#items").append(elem);
    elem.show();
  } else {
    $("#items").append(elem);
    active.fadeOut(1000, function () {
      active.remove();
    });
    elem.fadeIn(1250);
  }

  i = i === 1000000 ? 0 : i + 1;
};

$(function () {
  socket = new WebSocket(serverUri);
  socket.addEventListener("message", (event) => {
    console.log(event.data);
    onMessage(JSON.parse(event.data));
  });
});
