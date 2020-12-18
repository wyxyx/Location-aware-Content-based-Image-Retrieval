<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<!-- 上述3个meta标签*必须*放在最前面，任何其他内容都*必须*跟随其后！ -->
<meta name="description" content="">
<meta name="author" content="">
<title>检索结果</title>
<!-- Bootstrap core CSS -->
<link href="/JsAndCss/bootstrap-3.3.7.min.css" rel="stylesheet">
<!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
<link href="/JsAndCss/ie10-viewport-bug-workaround.css" rel="stylesheet">
<!-- Custom styles for this template -->
<link href="/JsAndCss/starter-template.css" rel="stylesheet">
<!-- Just for debugging purposes. Don't actually copy these 2 lines! -->
<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->
<script src="/JsAndCss/ie-emulation-modes-warning.js"></script>
<!-- Bootstrap core JavaScriptd
    ================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="/JsAndCss/jquery-1.12.4.min.js"></script>
<script src="/JsAndCss/bootstrap-3.3.7.min.js"></script>
<!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
<script src="/JsAndCss/ie10-viewport-bug-workaround.js"></script>

<!--分页控件 -->
<link href="/JsAndCss/bootstrap-pagination.min.css" rel="stylesheet">
<script src="/JsAndCss/bootstrap-pagination.min.js"></script>

<style>
body {
	padding-top: 0px;
	height: 100%;
}

#main {
	width: 100%;
	height: 100%;
	background: red;
}

#mapBackgroundDiv {
	background: rgba(56, 56, 56, 0.5);
	width: 100%;
	height: 100%;
}

#map {
	height: 80%;
	width: 80%;
}

.center-in-center {
	position: absolute;
	top: 50%;
	left: 50%;
	-webkit-transform: translate(-50%, -50%);
	-moz-transform: translate(-50%, -50%);
	-ms-transform: translate(-50%, -50%);
	-o-transform: translate(-50%, -50%);
	transform: translate(-50%, -50%);
}

.persona-grid {
	margin-top: 20px;
	margin-bottom: 0px;
}

//
调整图片位置使用！

	    
@media ( min-width : 480px) {
	.persona-grid .row {
		display: flex;
	}
}

.resultImage {
	height: 190px;
	width: auto;
	padding-bottom: 10px;
}
</style>
</head>

<body>
	<div class="container row-container">
		<div class="row">
			<div class="col-sm-12">
				<h1 class="text-center">图片检索</h1>
				<form method="post" action="/searchImage"
					enctype="multipart/form-data">
					<input name="imageFile" type="file" style="display: none">
					<div class="input-group" style="z-index: 0;">
						<input name="searchStr" type="text" class="form-control"
							placeholder=""> <input id="shapeType" name="shapeType"
							type="text" style="display: none" value="0"> <input
							id="p1Lng" name="p1Lng" type="text" style="display: none"
							value="50"> <input id="p1Lat" name="p1Lat" type="text"
							style="display: none" value="50"> <input id="p2Lng"
							name="p2Lng" type="text" style="display: none" value="60">
						<input id="p2Lat" name="p2Lat" type="text" style="display: none"
							value="60"> <input name="lng" id="lngText" type="text"
							value="0" style="display: none" /> <input name="lat"
							id="latText" type="text" value="0" style="display: none" />
						<div class="input-group-addon btn" id="showMapDiv">
							<span class="glyphicon glyphicon-map-marker" aria-hidden="true"></span>
						</div>
						<div class="input-group-addon btn"
							onclick="$('input[name=imageFile]').click();">选择图片</div>
						<span class="input-group-btn">
							<button class="btn btn-info btn-search " type="submit">搜索</button>
						</span>
					</div>
				</form>
			</div>
		</div>
	</div>

	<div class="persona-grid">
		<div class="container row-container">
			<div class="row ">
				<c:forEach var="imagePath" items="${resultImagesPath }">
					<div class="col-sm-6 col-md-3 ">
						<a href="${imagePath }" class=""> <img src="${imagePath }"
							class="resultImage img-responsive center-block">
						</a>
					</div>
				</c:forEach>
			</div>
		</div>
	</div>

	<div class="container row-container">
		<div class="row">
			<div class="col-sm-12">
				<ul id="paginator" class="pagination" data-total="${total }"
					data-pageindex="${pageIndex }" data-pagesize="16"
					data-pagenumberformatestring="{pageNumber}" data-pagegroupsize="10"
					data-firstpagetext="首页"
					data-layoutscheme="firstpage,pagenumber,righttext">
				</ul>
			</div>
		</div>
	</div>

	<div id="mapBackgroundDiv" class="center-in-center"
		style="visibility: hidden;">
		<div id="map" class="center-in-center"></div>
	</div>

</body>
<script type="text/javascript">
	$('input[name=imageFile]').change(function() {
		$('input[name=searchStr]').val($(this).val());
	});
	
    var paginator = BootstrapPagination($("#paginator"), {
        pageChanged: function (pageIndex, pageSize) {
            //alert("styles");
            // location.href = "https://www.baidu.com";
            //alert("result?pageIndex=" + pageIndex + "&pageSize=" + pageSize);
            location.href = "result?pageIndex=" + pageIndex + "&pageSize=" + pageSize;
        },
    });
    
    var isShowMap = 0;

    // 显示地图
    $('#showMapDiv').bind("click",function(event){
        document.getElementById('mapBackgroundDiv').style.visibility = "";
        isShowMap = 1;
    });

    // 隐藏地图
    $('#mapBackgroundDiv').bind("click",function(event){
      if(isShowMap == 1) {
        document.getElementById('mapBackgroundDiv').style.visibility = "hidden";
        isShowMap = 0;
      }
    });

    // 为div元素绑定click事件
    $('#map').bind("click",function(event){
        event.stopPropagation();    //  阻止事件冒泡
    });


    // 截取number小数点后7位
    function dealNum(number) {
      var numStr = number.toString();
      for(var i=0; i<numStr.length; i++) {
        if(numStr.charAt(i) == '.') break;
      }
      if(numStr.length > i+8) return numStr.substring(0, i+8);
      else return numStr;
    }

 // 初始化
    function initMap() {
      	document.getElementById('map').style.visibility = "";
      	console.log("初始地图");
      
	   	// 创建map
	   	var myLatlng = {lng: -95.712891 , lat: 37.09024 };
	   	var map = new google.maps.Map(document.getElementById('map'), {
	        zoom: 8,
	        center: myLatlng,
	        zoomControl: true,
	        mapTypeControl: true,
	        scaleControl: true,
	        streetViewControl: true,
	        rotateControl: true,
	      });
	      map.setZoom(8);
	      //map.setCenter(myLatlng);
	      
	   	// 获得当前位置
	      if (navigator.geolocation) {
	        navigator.geolocation.getCurrentPosition(function(position) {
	          myLatlng = {
	            lat: position.coords.latitude,
	            lng: position.coords.longitude
	          };
	          console.log(myLatlng.lng + " | " + myLatlng.lat);
	        	//$("#p1Lng").val(myLatlng.lng);
		  		//$("#p1Lat").val(myLatlng.lat);
	          //map.setCenter(myLatlng);
	        }, function() {
	          //alert("获取您的位置失败")
	        });
	      } else {
	        alert("您的浏览器不支持获得位置");
	      }
	      console.log(myLatlng.lng+ " | | " + myLatlng.lat);
	      overlay  = new google.maps.Marker({
	        position: myLatlng,
	        map: map,
	        title: "经度：" + myLatlng.lng + " 纬度：" + myLatlng.lat
	      });
      //$("#p1Lng").val(myLatlng.lng );
		//$("#p1Lat").val(myLatlng.lat);

      // 添加绘图面板
      var drawingManager = new google.maps.drawing.DrawingManager({
  	    drawingMode: google.maps.drawing.OverlayType.MARKER,
  	    //drawingMode: google.maps.drawing.OverlayType.RECTANGLE,
  	    drawingControl: true,
  	    drawingControlOptions: {
  	      position: google.maps.ControlPosition.TOP_CENTER,
  	      //drawingModes: ['marker', 'circle', 'polygon', 'polyline', 'rectangle']
  	      drawingModes: ['marker']
  	    },
  	    //markerOptions: {icon: 'https://developers.google.com/maps/documentation/javascript/examples/full/images/beachflag.png'},
  	    circleOptions: {
  	      fillColor: '#ffff00',
  	      fillOpacity: 0,
  	      strokeWeight: 5,
  	      clickable: false,
  	      editable: true,
  	      zIndex: 1
  	    }
  	  });
      drawingManager.setMap(map);
      // 添加响应事件
	  	google.maps.event.addListener(drawingManager, 'overlaycomplete', function(event) {
	  		//console.log(drawingManager);
	  	  	if(overlay!=null) overlay.setVisible(false);
	  	  	overlay = event.overlay;
	  	  	//console.log(event);
	  	});
      
      // 添加点击maker事件
	  	google.maps.event.addListener(drawingManager, 'markercomplete', function(marker) {
	  		console.log("marker : " + marker.getPosition().lng() + "  " + marker.getPosition().lat());
	  		$("#shapeType").val(0);
	  		$("#p1Lng").val(marker.getPosition().lng());
	  		$("#p1Lat").val(marker.getPosition().lat());
	  		marker.setTitle("经度 : " + marker.getPosition().lng() + " 纬度  : " + marker.getPosition().lat());
	  		//console.log(drawingManager);
	  	  	//if(overlay!=null) overlay.setVisible(false);
	  	  	//overlay = event.overlay;
	  	  	//console.log(event);
	  	});
      
		// 添加点击画矩形事件
	  	google.maps.event.addListener(drawingManager, 'rectanglecomplete', function(rectangle) {
	  		var NorthEast = rectangle.getBounds().getNorthEast();
	  		var SouthWest = rectangle.getBounds().getSouthWest();
	  		console.log("rectangle : 西北 : " + SouthWest.lng() + " " + NorthEast.lat() + " - 东南  : " + NorthEast.lng() + " " + SouthWest.lat());
	  		$("#shapeType").val(1);
	  		$("#p1Lng").val(SouthWest.lng());
	  		$("#p1Lat").val(NorthEast.lat());
	  		$("#p2Lng").val(NorthEast.lng());
	  		$("#p2Lat").val(SouthWest.lat());
	  		//console.log(drawingManager);
	  	  	//if(overlay!=null) overlay.setVisible(false);
	  	  	//overlay = event.overlay;
	  	  	//console.log(event);
	  	});
 }
</script>

<!--调用googleMapApi -->
<script async defer
	src="https://maps.googleapis.com/maps/api/js?key=AIzaSyAa9vmqGfudziveQey4iLvtUD1NnI6jHrw&callback=initMap&libraries=drawing">
</script>

</html>