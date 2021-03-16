<div class="alert alert-danger section action" ng-show="!isEmpty(error)" role="alert">{{error}}</div>
<div class="section action">
	<label class="checkbox-inline">
		<input ng-model="playlist.active" type="checkbox"> Active</label>
</div>
<div class="section action">* Name *
	<input class="form-control" ng-model="playlist.name" required type="text">
</div>
<div class="section action">Description
	<input class="form-control" ng-model="playlist.description" type="text">
</div>
<div class="section action">* Type *
	<select class="form-control multiselect-ui" ng-model="playlist.type" required>
		<option value="ONLINE">Online Stream</option>
		<option value="LOCAL_SEQUENCED">Local sequenced folder</option>
		<option value="LOCAL_RANDOMIZED">Local random folder</option>
		<option value="LOCAL_RESUMING">Local resuming folder</option>
		<option value="LOCAL_RESUMING_SAME">Local same resuming folder</option>
		<option value="LOCAL_RESUMING_NEXT">Local next resuming folder</option>
	</select>
</div>
<div class="section action">* Stream URL Or Local folder name *
	<input class="form-control" ng-model="playlist.urlOrFolder" required type="text">
</div>
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
	</select>
</div>
<div class="section action">
	<label class="checkbox-inline"><input ng-model="playlist.playingGeneralBumpers" type="checkbox"> Playing General Bumpers</label>
</div>
<div class="section action">Special Bumper folder name
	<input class="form-control" ng-model="playlist.specialBumperFolder" type="text">
</div>
								