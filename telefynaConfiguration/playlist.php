<div class="alert alert-danger section action" ng-show="!isEmpty(error)" role="alert">{{error}}</div>
<div class="section action" ng-if="edit != 0 && edit != 1 && !isEmpty(config.playlists) && config.playlists.length != 1">
	<label class="checkbox-inline">
		<input ng-model="playlist.active" type="checkbox"> Active</label>
</div>
<div class="section action">* Name *
	<input class="form-control" ng-model="playlist.name" required type="text">
</div>
<div class="section action">Description
	<input class="form-control" ng-model="playlist.description" type="text">
</div>
<?php include 'playlistType.php';?>
<div class="section action">
	<label ng-if="playlist.type == 'ONLINE'">* Stream URL *</label>
	<label ng-if="playlist.type != 'ONLINE'">* Local folder name, separate with # to use additional programs in other folders *</label>
	<input class="form-control" ng-model="playlist.urlOrFolder" required type="text">
</div>
<div class="section action" ng-if="playlist.type != 'ONLINE'">
	<label class="checkbox-inline"><input ng-model="playlist.usingExternalStorage" type="checkbox"> Using external Storage</label>
</div>
<div ng-if="playlist.usingExternalStorage == true">playlist, bumper and lowerThird folders will be retrived from the /telefyna folder in SDCard/USB drive attached if any exists else Internal storage</div>
<div class="section action select-color">Preview Color
	<select class="color-selector" ng-change="changeSelectedColor()" ng-model="playlist.color">
		<option data-color="#00cc99" value="#00cc99"></option>
		<option data-color="#00ccff" value="#00ccff"></option>
		<option data-color="#6666ff" value="#6666ff"></option>
		<option data-color="#28a745" value="#28a745"></option>
		<option data-color="#ffff66" value="#ffff66"></option>
		<option data-color="#66ff33" value="#66ff33"></option>
		<option data-color="#ff6600" value="#ff6600"></option>
		<option data-color="#ff33cc" value="#ff33cc"></option>
		<option data-color="#666699" value="#666699"></option>
		<option data-color="#e0ebeb" value="#e0ebeb"></option>
		<option data-color="#990099" value="#990099"></option>
		<option data-color="#993333" value="#993333"></option>
		<option data-color="#808080" value="#808080"></option>
		<option data-color="#ccccff" value="#ccccff"></option>
		<option data-color="#336600" value="#336600"></option>
		<option data-color="#99ff99" value="#99ff99"></option>
		<option data-color="#66ffcc" value="#66ffcc"></option>
		<option data-color="#ccffcc" value="#ccffcc"></option>
		<option data-color="#ffccff" value="#ffccff"></option>
		<option data-color="#0060aa" value="#0060aa"></option>
		<option data-color="#f9b724" value="#f9b724"></option>
		<option data-color="#775549" value="#775549"></option>
		<option data-color="#2c0f7d" value="#2c0f7d"></option>
		<option data-color="#607d8b" value="#607d8b"></option>
	</select>
</div>
<div class="section action" ng-if="playlist.type == 'LOCAL_SEQUENCED' || playlist.type == 'LOCAL_RANDOMIZED'">
	<label class="checkbox-inline"><input ng-model="playlist.playingGeneralBumpers" type="checkbox"> Playing General Bumpers</label>
</div>
<div class="section action" ng-if="playlist.type == 'LOCAL_SEQUENCED' || playlist.type == 'LOCAL_RANDOMIZED'">Special Bumper folder name
	<input class="form-control" ng-model="playlist.specialBumperFolder" type="text">
</div>
								